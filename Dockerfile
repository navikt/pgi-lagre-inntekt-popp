FROM eclipse-temurin:21-jre

RUN apt-get update && apt-get install -y \
  curl \
  dumb-init \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY java-opts.sh /app
RUN chmod +x /app/java-opts.sh

COPY build/libs/pgi-lagre-inntekt-popp.jar /app/app.jar

ENV TZ="Europe/Oslo"

ENTRYPOINT ["/usr/bin/dumb-init", "--"]

CMD ["bash", "-c", "source java-opts.sh && exec java ${DEFAULT_JVM_OPTS} ${JAVA_OPTS} -jar app.jar $@"]