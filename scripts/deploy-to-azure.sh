#!/usr/bin/env bash

AZ_REGION=${AZ_REGION:-eastus}
AZ_NAME_QUALIFIER=${AZ_NAME_QUALIFIER:-}
AZ_RG_NAME=${AZ_RG_NAME:-analysisPlatformGroup}${AZ_NAME_QUALIFIER}
AZ_DOCKER_REPO=${AZ_DOCKER_REPO:-analysisPlatformRepo}${AZ_NAME_QUALIFIER}
AZ_MONGO_DB=${AZ_MONGO_DB:-apdb}${AZ_NAME_QUALIFIER}
AZ_AP_DEPLOY_NAME=${AZ_AP_DEPLOY_NAME:-analysis-platform}${AZ_NAME_QUALIFIER}
AZ_APC_DEPLOY_NAME=${AZ_AP_DEPLOY_NAME:-analysis-platform-controller}${AZ_NAME_QUALIFIER}
AP_VERSION=${AP_VERSION:-0.0.1-SNAPSHOT}
APC_VERSION=${AP_VERSION}
AP_TAG=${AP_TAG:-v0.1}
debug=false

### Functions ###
INFO() {
  echo ">>>> $1"
}

DEBUG() {
  if [ "${debug}" == "true" ]; then
    echo ">>>> $1"
  fi
}

createResource() {
    INFO "Creating resource group ${AZ_RG_NAME}"
    az group create --name ${AZ_RG_NAME} --location ${AZ_REGION}

    # Create the DB
    DEBUG "Creating cosmosdb ${AZ_MONGO_DB} (mongo mode)"
    az cosmosdb create --name ${AZ_MONGO_DB} --resource-group ${AZ_RG_NAME} --kind MongoDB
    DB_KEY=`az cosmosdb list-keys --name ${AZ_MONGO_DB} --resource-group ${AZ_RG_NAME} --query "primaryMasterKey"`
    MONGODB_URI="mongodb://${AZ_MONGO_DB}:${DB_KEY//\"}@${AZ_MONGO_DB}:10255/ap?ssl=true&sslverifycertificate=false"

    # Create private docker repo in resource group and push the image
    DEBUG "Creating private docker repo ${AZ_DOCKER_REPO}"
    az acr create --resource-group ${AZ_RG_NAME} --name ${AZ_DOCKER_REPO} --sku Basic --admin-enabled true
    az acr login --name ${AZ_DOCKER_REPO}
    NAMESPACE=`az acr show --name ${AZ_DOCKER_REPO} --query loginServer`

    # Tag and push the AP
    DEBUG "Tagging and pushing the Analysis Platform"
    docker tag amplify/analysis-platform:${AP_VERSION} ${NAMESPACE//\"}/amplify/${AZ_AP_DEPLOY_NAME}:${AP_TAG}
    docker push ${NAMESPACE//\"}/amplify/${AZ_AP_DEPLOY_NAME}:${AP_TAG}
    # Tag and push the APC
    DEBUG "Tagging and pushing the Analysis Platform Controller"
    docker tag amplify/analysis-platform-controller:${APC_VERSION} ${NAMESPACE//\"}/amplify/${AZ_APC_DEPLOY_NAME}:${AP_TAG}
    docker push ${NAMESPACE//\"}/amplify/${AZ_APC_DEPLOY_NAME}:${AP_TAG}

    PASSWORD=`az acr credential show --name ${AZ_DOCKER_REPO} --query "passwords[0].value"`
    # Load the auth file
    source $AZURE_AUTH_LOCATION

    # Start the AP container instance
    DEBUG "Starting the Analysis Platform Controller container instance"
    az container create \
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
    APC_IP=`az container show --name ${AZ_APC_DEPLOY_NAME} --resource-group ${AZ_RG_NAME} | jq -r '.ipAddress.ip'`

    # Start the AP container instance
    DEBUG "Starting the Analysis Platform container instance"
    az container create \
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
    NAMESPACE=`az acr show --name ${AZ_DOCKER_REPO} --query loginServer`
    docker rmi ${NAMESPACE//\"}/amplify/${AZ_AP_DEPLOY_NAME}:${AP_TAG}
    docker rmi ${NAMESPACE//\"}/amplify/${AZ_APC_DEPLOY_NAME}:${AP_TAG}
    az group delete --name ${AZ_RG_NAME} -y
}

### MAIN ###
USAGE="USAGE: [--debug] --create(-c), --delete(-d)"

if [ $# -eq 0 ]; then
    echo $USAGE
fi

# Switch between modes using flags
while [ ! $# -eq 0 ]
do
	case "$1" in
		--create | -c)
			createResource
			exit
			;;
		--delete | -d)
			deleteResource
			exit
			;;
                --debug | -x)
                        debug=true
                        ;;
		*)
		    echo $USAGE
		    exit
		    ;;
	esac
	shift
done

