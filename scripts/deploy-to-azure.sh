#!/usr/bin/env bash

### Functions ###
createResource() {
    echo "Creating resource"

    az group create --name analysisPlatformGroup --location eastus

    # Create the DB
    az cosmosdb create --name apdb --resource-group analysisPlatformGroup --kind MongoDB
    DB_KEY=`az cosmosdb list-keys --name apdb --resource-group analysisPlatformGroup --query "primaryMasterKey"`
    MONGODB_URI="mongodb://apdb:${DB_KEY//\"}@apdb.documents.azure.com:10255/ap?ssl=true&sslverifycertificate=false"

    # Create private docker repo in resource group and push the image
    az acr create --resource-group analysisPlatformGroup --name analysisPlatformRepo --sku Basic --admin-enabled true
    az acr login --name analysisPlatformRepo
    NAMESPACE=`az acr show --name analysisPlatformRepo --query loginServer`
    # Tag and push the AP
    docker tag amplify/analysis-platform:0.0.1-SNAPSHOT ${NAMESPACE//\"}/amplify/analysis-platform:v0.1
    docker push ${NAMESPACE//\"}/amplify/analysis-platform:v0.1
    # Tag and push the APC
    docker tag amplify/analysis-platform-controller:0.0.1-SNAPSHOT ${NAMESPACE//\"}/amplify/analysis-platform-controller:v0.1
    docker push ${NAMESPACE//\"}/amplify/analysis-platform-controller:v0.1

    PASSWORD=`az acr credential show --name analysisPlatformRepo --query "passwords[0].value"`
    # Load the auth file
    source $AZURE_AUTH_LOCATION

    # Start the AP container instance
    az container create \
        --resource-group analysisPlatformGroup \
        --name analysis-platform-controller \
        --image ${NAMESPACE//\"}/amplify/analysis-platform-controller:v0.1 \
        --cpu 1 \
        --memory 1 \
        --registry-login-server ${NAMESPACE//\"} \
        --registry-username analysisPlatformRepo \
        --registry-password ${PASSWORD//\"} \
        --dns-name-label analysis-platform-controller \
        --environment-variables \
            AZURE_LOGIN_CLIENTID=${client} \
            AZURE_LOGIN_DOMAIN=${tenant} \
            AZURE_LOGIN_SECRET=${key}  \
        --ports 8080
    APC_IP=`az container show --name analysis-platform-controller  --resource-group analysisPlatformGroup | jq -r '.ipAddress.ip'`

    # Start the AP container instance
    az container create \
        --resource-group analysisPlatformGroup \
        --name analysis-platform \
        --image ${NAMESPACE//\"}/amplify/analysis-platform:v0.1 \
        --cpu 1 \
        --memory 1 \
        --registry-login-server ${NAMESPACE//\"} \
        --registry-username analysisPlatformRepo \
        --registry-password ${PASSWORD//\"} \
        --dns-name-label analysis-platform \
        --environment-variables \
            SPRING_DATA_MONGODB_URI=${MONGODB_URI} \
            RESOURCES_CLIENTS_REQUESTURL="http://${APC_IP}:8080/deployARMTemplate" \
        --ports 8080
}

deleteResource() {
    echo "Deleting resources"
    NAMESPACE=`az acr show --name analysisPlatformRepo --query loginServer`
    docker rmi ${NAMESPACE//\"}/amplify/analysis-platform:v0.1
    az group delete --name analysisPlatformGroup -y
}

### MAIN ###
USAGE="USAGE: --create(-c), --delete(-d)"

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
		*)
		    echo $USAGE
		    exit
		    ;;
	esac
	shift
done




