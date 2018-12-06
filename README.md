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
# Create the Azure deployment
./deploy-to-azure.sh --create

# Remove the Azure deployment
./deploy-to-azure.sh --delete
```