# azure-controller
## Prerequisites
- Java
- Maven
- Docker
- MongoDB
- An Azure account (and azure cli installed locally)
## Build
Navigate to the directory containing the **pom.xml** file and run the following command:
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
java -Dspring.data.mongodb.uri='mongodb://localhost:27017/apdb' -jar target/analysis-platform-0.0.1-SNAPSHOT.jar
```
And navigating to *http://localhost:8080/swagger-ui.html*

or pushed and deployed to azure using the script provided in the 'scripts' directory:
```
# Create the Azure deployment
./deploy-to-azure.sh --create

# Remove the Azure deployment
./deploy-to-azure.sh
```