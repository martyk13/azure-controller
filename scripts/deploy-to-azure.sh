#!/usr/bin/env bash

AZ_REGION=${AZ_REGION:-eastus}
AZ_NAME_QUALIFIER=${AZ_NAME_QUALIFIER:-}
AZ_RG_NAME=${AZ_RG_NAME:-analysisPlatformGroup}${AZ_NAME_QUALIFIER}
AZ_DOCKER_REPO=${AZ_DOCKER_REPO:-analysisPlatformRepo}${AZ_NAME_QUALIFIER}
AZ_MONGO_DB=${AZ_MONGO_DB:-apdb}${AZ_NAME_QUALIFIER}
AZ_AP_NAME=${AZ_AP_NAME:-analysis-platform}
AZ_AP_DEPLOY_NAME=${AZ_AP_DEPLOY_NAME:-${AZ_AP_NAME}}${AZ_NAME_QUALIFIER}
AZ_APC_NAME=${AZ_APC_NAME:-analysis-platform-controller}
AZ_APC_DEPLOY_NAME=${AZ_APC_DEPLOY_NAME:-${AZ_APC_NAME}}${AZ_NAME_QUALIFIER}
AP_VERSION=${AP_VERSION:-0.0.1-SNAPSHOT}
APC_VERSION=${AP_VERSION}
AP_TAG=${AP_TAG:-v0.1}
RUN=
debug=false
doCreate=false
doDelete=false

### Functions ###

USAGE() {
  echo ""
  echo "`basename $0` [--debug] [--create | --delete] [--noex]"
  echo ""
  echo "    --create | -c : Create resources"
  echo "    --delete | -d : Delete resources"
  echo "    --debug  | -x : Emit debug output"
  echo "    --noex   | -n : Do not execute - just show commands that would be executed"
  echo ""
  exit 1
}

INFO() {
  echo "`date +%Y-%m-%d:%H:%M:%S` >>>> $1"
}

DEBUG() {
  if [ "${debug}" == "true" ]; then
    echo "`date +%Y-%m-%d:%H:%M:%S` >>>> $1"
  fi
}

createResource() {
    INFO "Creating resources in Resource Group ${AZ_RG_NAME}"
    ${RUN} az group create --name ${AZ_RG_NAME} --location ${AZ_REGION}

    # Create the DB
    DEBUG "Creating cosmosdb ${AZ_MONGO_DB} (mongo mode)"
    ${RUN} az cosmosdb create --name ${AZ_MONGO_DB} --resource-group ${AZ_RG_NAME} --kind MongoDB
    DB_KEY=`${RUN} az cosmosdb keys list --name ${AZ_MONGO_DB} --resource-group ${AZ_RG_NAME} --query "primaryMasterKey"`
    MONGODB_URI="mongodb://${AZ_MONGO_DB}:${DB_KEY//\"}@${AZ_MONGO_DB}.documents.azure.com:10255/ap?ssl=true&sslverifycertificate=false"

    # Create private docker repo in resource group and push the image
    DEBUG "Creating private docker repo ${AZ_DOCKER_REPO}"
    ${RUN} az acr create --resource-group ${AZ_RG_NAME} --name ${AZ_DOCKER_REPO} --sku Basic --admin-enabled true
    ${RUN} az acr login --name ${AZ_DOCKER_REPO}
    
    NAMESPACE=`${RUN} az acr show --name ${AZ_DOCKER_REPO} --query loginServer`

    # Tag and push the AP
    DEBUG "Tagging and pushing the Analysis Platform"
    ${RUN} docker tag amplify/${AZ_AP_NAME}:${AP_VERSION} ${NAMESPACE//\"}/amplify/${AZ_AP_DEPLOY_NAME}:${AP_TAG}
    ${RUN} docker push ${NAMESPACE//\"}/amplify/${AZ_AP_DEPLOY_NAME}:${AP_TAG}
    # Tag and push the APC
    DEBUG "Tagging and pushing the Analysis Platform Controller"
    ${RUN} docker tag amplify/${AZ_APC_NAME}:${APC_VERSION} ${NAMESPACE//\"}/amplify/${AZ_APC_DEPLOY_NAME}:${AP_TAG}
    ${RUN} docker push ${NAMESPACE//\"}/amplify/${AZ_APC_DEPLOY_NAME}:${AP_TAG}

    PASSWORD=`${RUN} az acr credential show --name ${AZ_DOCKER_REPO} --query "passwords[0].value"`

    # Load the auth file
    source $AZURE_AUTH_LOCATION

    # Start the AP container instance
    DEBUG "Starting the Analysis Platform Controller container instance ${AZ_APC_DEPLOY_NAME}"
    ${RUN} az container create \
        --resource-group ${AZ_RG_NAME} \
        --name ${AZ_APC_DEPLOY_NAME} \
        --image ${NAMESPACE//\"}/amplify/${AZ_APC_DEPLOY_NAME}:${AP_TAG} \
        --cpu 1 \
        --memory 1 \
        --registry-login-server ${NAMESPACE//\"} \
        --registry-username ${AZ_DOCKER_REPO} \
        --registry-password ${PASSWORD//\"} \
        --dns-name-label ${AZ_APC_DEPLOY_NAME} \
        --environment-variables \
            AZURE_LOGIN_CLIENTID=${client} \
            AZURE_LOGIN_DOMAIN=${tenant} \
            AZURE_LOGIN_SECRET=${key}  \
        --ports 8080

    APC_IP=`${RUN} az container show --name ${AZ_APC_DEPLOY_NAME} --resource-group ${AZ_RG_NAME} | jq -r '.ipAddress.ip'`

    # Start the AP container instance
    DEBUG "Starting the Analysis Platform container instance ${AZ_AP_DEPLOY_NAME}"
    ${RUN} az container create \
        --resource-group ${AZ_RG_NAME} \
        --name ${AZ_AP_DEPLOY_NAME} \
        --image ${NAMESPACE//\"}/amplify/${AZ_AP_DEPLOY_NAME}:${AP_TAG} \
        --cpu 1 \
        --memory 1 \
        --registry-login-server ${NAMESPACE//\"} \
        --registry-username ${AZ_DOCKER_REPO} \
        --registry-password ${PASSWORD//\"} \
        --dns-name-label ${AZ_AP_DEPLOY_NAME} \
        --environment-variables \
            SPRING_DATA_MONGODB_URI=${MONGODB_URI} \
            RESOURCES_CLIENTS_REQUESTURL="http://${APC_IP}:8080/deployARMTemplate" \
        --ports 8080
}

deleteResource() {
    INFO "Deleting resources"
    NAMESPACE=`${RUN} az acr show --name ${AZ_DOCKER_REPO} --query loginServer`
    ${RUN} docker rmi ${NAMESPACE//\"}/amplify/${AZ_AP_DEPLOY_NAME}:${AP_TAG}
    ${RUN} docker rmi ${NAMESPACE//\"}/amplify/${AZ_APC_DEPLOY_NAME}:${AP_TAG}
    ${RUN} az group delete --name ${AZ_RG_NAME} -y
}

### MAIN ###

if [ $# -eq 0 ]; then
    USAGE
fi

# Switch between modes using flags
while [ ! $# -eq 0 ]
do
	case "$1" in
		--create | -c)
			doCreate=true
			;;
		--delete | -d)
			doDelete=true
			;;
                --noex | -n)
                        RUN="echo"
                        ;;
                --debug | -x)
                        debug=true
                        ;;
		*)
		    echo $USAGE
		    ;;
	esac
	shift
done

if [ "${doCreate}" == "true" ] && [ "${doDelete}" == "true" ]; then
  echo "Cannot specify to CREATE (--create) and DELETE (--delete) in same command"
  exit 1
fi

if [ "${doCreate}" == "true" ]; then
  createResource
  exit 0
fi

if [ "${doDelete}" == "true" ]; then
  deleteResource
  exit 0
fi

