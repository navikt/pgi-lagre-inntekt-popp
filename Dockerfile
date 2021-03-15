FROM navikt/java:14

RUN apt-get update && apt-get install -y \
  curl \
  && rm -rf /var/lib/apt/lists/*

COPY build/libs/*.jar ./
