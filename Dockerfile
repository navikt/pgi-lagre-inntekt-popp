FROM navikt/java:14
RUN sudo apt-get -y update && sudo apt-get -y install curl
COPY build/libs/*.jar ./
