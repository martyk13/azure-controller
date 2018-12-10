# azure-controller
## Prerequisites
- Java
- Maven
- Docker
- MongoDB
- An Azure account (and azure cli installed locally)
- Create the auth file and export the path as described here: [Azure setup guide](https://docs.microsoft.com/en-us/java/azure/java-sdk-azure-get-started?view=azure-java-stable)
## Build
Navigate to the directories containing the **pom.xml** file and run the following command:
```
mvn clean install
```
You can then check the Docker container was created successfully using:
```
docker images
REPOSITORY                  TAG                 IMAGE ID            CREATED             SIZE
kenesys/analysis-platform   0.0.1-SNAPSHOT      e4b64855a448        19 hours ago        137 MB
```
## Running
The application can either be run locally using :
```
scripts/local-deploy.sh --start
```
And stopped using
```
scripts/local-deploy.sh --stop
```
And navigating to *http://localhost:8080/swagger-ui.html*
this will direct any logging output to the files in the */log* directory

or pushed and deployed to azure using the script provided in the 'scripts' directory:
```
# Configuration options - set in your environment before running script - setting ${AZ_REGION} and ${AZ_NAME_QUALIFIER} should suffice

// The Azure region in which to create the resources (default eastus)
export AZ_REGION=westeurope

// A qualifer to append to resource names in Azure (default empty)
export AZ_NAME_QUALIFIER="your-name-qualifier-with-no-special-chars"

// The name of the Resource Group (default analysisPlatformGroup) -  (will have the ${AZ_NAME_QUALIFIER} appended)
export AZ_RG_NAME=analysisPlatformGroup

// The name of the private Docker Container repo (default analysisPlatformRepo) -  (will have the ${AZ_NAME_QUALIFIER} appended)
export AZ_DOCKER_REPO=analysisPlatformRepo

// The name of the cosmosDB (runs in MongoDB mode) (default apdb) -  (will have the ${AZ_NAME_QUALIFIER} appended)
export AZ_MONGO_DB=${AZ_MONGO_DB:-apdb}

// The version of the Analysis Platform components to use
export AP_VERSION=0.0.1-SNAPSHOT
export APC_VERSION=${AP_VERSION}

// The Azure Tag version qualifier
export AP_TAG=v0.1

# Create the Azure deployment (with optional debug output)
./deploy-to-azure.sh [--debug] --create

# Remove the Azure deployment
./deploy-to-azure.sh --delete
```
*Note:* To deploy to azure using the script JQ is needed for json parsing:
```
yum install epel-release

yum install jq
```
