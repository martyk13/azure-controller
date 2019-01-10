#!/usr/bin/env bash

AP_VERSION=${AP_VERSION:-0.0.1-SNAPSHOT}
APC_VERSION=${AP_VERSION}

REPOS=${REPOS:-${HOME}/repos}
AP_HOME=${AP_HOME:-${REPOS}/azure-controller}
AP_LOGS=${AP_LOGS:-${AP_HOME}/log}
AP_LOG=${AP_LOG:-${AP_LOGS}/ap.log}
APC_LOG=${APC_LOG:-${AP_LOGS}/apc.log}
AP_SCRIPTS=${AP_SCRIPTS:-${AP_HOME}/scripts}

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

### Functions ###
createResource() {
    if [ ! -d "${AP_LOGS}" ]; then
        mkdir ${AP_LOGS}
    fi

    # Load the auth file
    source $AZURE_AUTH_LOCATION

    DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8087"

    java -Dspring.data.mongodb.uri='mongodb://localhost:27017/paf' -jar \
        -Dresources.clients.requesturl="http://localhost:8081/deployARMTemplate" \
        ${AP_HOME}/analysis-platform/target/analysis-platform-${AP_VERSION}.jar >> ${AP_LOG} &

    java ${DEBUG} -Dserver.port=8081 -jar \
        -Dazure.login.clientid=$client \
        -Dazure.login.domain=$tenant \
        -Dazure.login.secret=$key \
        ${AP_HOME}/analysis-platform-controller/target/analysis-platform-controller-${APC_VERSION}.jar >> ${APC_LOG} &
}

deleteResource() {
    echo "Stopping the application"
    AP_PID=`ps ef | grep "[a]nalysis-platform-${AP_VERSION}.jar"  | awk '{print $1}'`
    APC_PID=`ps ef | grep "[a]nalysis-platform-controller-${APC_VERSION}.jar"  | awk '{print $1}'`
    kill $AP_PID $APC_PID
}

### MAIN ###
USAGE="USAGE: --start, --stop"

if [ $# -eq 0 ]; then
    echo $USAGE
fi

# Switch between modes using flags
while [ ! $# -eq 0 ]
do
	case "$1" in
		--start)
			createResource
			exit
			;;
		--stop)
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
