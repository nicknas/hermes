FROM openjdk:8

EXPOSE 9000
VOLUME /root/.m2

WORKDIR /usr/src/hermes
ADD ./target/hermes-1.0.0-SNAPSHOT.jar /usr/src/hermes/hermes-1.0.0-SNAPSHOT.jar
ADD ./settings.xml /root/.m2/settings.xml