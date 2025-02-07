FROM mcr.microsoft.com/openjdk/jdk:21-mariner AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN tdnf install -y tar

RUN chmod +x mvnw && sed -i 's/\r$//' mvnw
RUN ./mvnw dependency:go-offline

COPY src src

RUN ./mvnw clean install -DskipTests

RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

FROM eclipse-temurin:21-jdk-alpine AS alpine-base
RUN apk update
WORKDIR /app
# https://learn.microsoft.com/en-us/azure/app-service/configure-custom-container?tabs=alpine&pivots=container-linux#enable-ssh
COPY docker/sshd_config /etc/ssh/
#COPY docker/entrypoint.sh .

# Start and enable SSH
RUN apk add openssh \
    && echo "root:Docker!" | chpasswd \
#    && chmod +x entrypoint.sh \
    && cd /etc/ssh/ \
    && ssh-keygen -A \
    && apk add openrc
RUN rc-update add sshd

#ENTRYPOINT [ "entrypoint.sh" ]
EXPOSE 2222


FROM alpine-base
#VOLUME /tmp

WORKDIR /app
COPY --from=build /app/target/simple-hello-world-0.0.1-SNAPSHOT.jar simple-hello-world.jar

RUN apk add supervisor
COPY docker/supervisord.conf /etc/supervisord.conf
# Run supervisord
CMD ["supervisord", "-c", "/etc/supervisord.conf"]

ENTRYPOINT ["java","-jar","simple-hello-world.jar"]
EXPOSE 80 2222