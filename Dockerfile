FROM navikt/java:14
RUN apt-get -y install curl
COPY build/libs/*.jar ./
