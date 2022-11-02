# Client

## Description

Network programming Client server.

## Create .jar file

```bash
$ mvn clean package
```

## Docker build

```bash
$ docker build . -tag="username"/client-service:latest
```

## Push image to docker.io

```bash
$ docker push "username"/client-service
```

## Docker compose to run the Application

```bash
$ docker-compose up --build --remove-orphans
```
