#FROM ghcr.io/graalvm/native-image-community:23-muslib AS build
FROM container-registry.oracle.com/graalvm/native-image:23-muslib AS build
#FROM container-registry.oracle.com/graalvm/native-image:23 AS build
WORKDIR /home/app/ms

RUN microdnf update -y
RUN microdnf upgrade -y
RUN microdnf install gcc gcc-c++ glibc-devel glibc-static zlib-devel libstdc++-static   gzip tar findutils

# https://github.com/oracle/graal/issues/7692
#RUN microdnf install findutils
RUN groupadd --force -g 1000 app && \
    useradd -g app -G app app
RUN chmod -R 777 /home/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod 777 mvnw && chmod +x mvnw && sed -i 's/\r$//' mvnw

RUN mkdir -p /opt/tmp && chmod 1777 /opt/tmp
ENV TMPDIR=/opt/tmp
ENV MAVEN_OPTS="-Djava.io.tmpdir=/opt/tmp"
ENV JAVA_OPTS="-Djava.io.tmpdir=/opt/tmp"
ENV JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=/opt/tmp"
ENV SVM_CFLAGS="-DLC_ADDRESS=0 -DLC_IDENTIFICATION=0 -DLC_MEASUREMENT=0 -DLC_NAME=LC_TIME -DLC_PAPER=0 -DLC_TELEPHONE=0 -DLM_ID_BASE=0 -DLM_ID_NEWLM=0 -DRTLD_DI_LMID=0 -DLmid_t=unsigned long"

USER app

COPY src src
RUN ls -la /home/app/ms/

#1 BUILD JAR AND USE native-image tool
RUN ./mvnw clean install
RUN native-image -jar target/simplehelloworld-0.0.1-SNAPSHOT.jar  \
#    --static  \
    -H:+UnlockExperimentalVMOptions \
    -H:TempDirectory=/home/app/ms/target/tmp \
    -H:Class=com.example.simplehelloworld.SimpleHelloWorldApplication \
    -H:+StaticExecutableWithDynamicLibC


 # native image build
#RUN ./mvnw native:compile -Pnative
#RUN ./mvnw -Pnative clean package
### RUN ./mvnw -Djava.io.tmpdir=/opt/tmp native:compile -Pnative
#RUN ./mvnw clean package -Pnative,nativeTest

RUN ls -la /home/app/ms/target


FROM alpine:latest AS alpine-base
#FROM frolvlad/alpine-glibc AS alpine-base
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
COPY --from=build /home/app/ms/target/simplehelloworld /app/simple-hello-world
RUN chmod +x simple-hello-world

#RUN apk --no-cache add ca-certificates wget && \
#    wget -q -O /etc/apk/keys/sgerrand.rsa.pub https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub && \
#    wget https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.35-r0/glibc-2.35-r0.apk && \
#    apk add glibc-2.35-r0.apk && \
#    rm glibc-2.35-r0.apk

#RUN apk add supervisor
#COPY docker/supervisord.conf /etc/supervisord.conf
# Run supervisord
# CMD ["supervisord", "-c", "/etc/supervisord.conf"]

#ENTRYPOINT ["/simple-hello-world"]
EXPOSE 80 8080

#CMD ["simple-hello-world"]
