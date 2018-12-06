FROM alpine:latest
RUN apk update \
    && apk add openjdk8 \
    && rm -rf /var/cache/apk/*
ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/myservice/app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/usr/share/myservice/app.jar"]