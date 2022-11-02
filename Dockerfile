FROM openjdk:11
COPY target/client-service.jar client-service.jar
EXPOSE ${port}
ENTRYPOINT exec java -jar client-service.jar