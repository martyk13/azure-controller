#!/usr/bin/env bash

### Functions ###
createResource() {
    if [ ! -d "../log" ]; then
        mkdir ../log
    fi
    
    java -Dspring.data.mongodb.uri='mongodb://localhost:27017/paf' -jar -Dresources.clients.requesturl="http://localhost:8081/deployARMTemplate" ../analysis-platform/target/analysis-platform-0.0.1-SNAPSHOT.jar >>../log/ap.log &
    java -Dserver.port=8081 -jar ../analysis-platform-controller/target/analysis-platform-controller-0.0.1-SNAPSHOT.jar >>../log/apc.log &
}

deleteResource() {
    echo "Stopping the application"
    AP_PID=`ps ef | grep "[a]nalysis-platform-0.0.1-SNAPSHOT.jar"  | awk '{print $1}'`
    APC_PID=`ps ef | grep "[a]nalysis-platform-controller-0.0.1-SNAPSHOT.jar"  | awk '{print $1}'`
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