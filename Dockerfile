FROM gcr.io/distroless/java21-debian13:nonroot

WORKDIR /app

ENV TZ="Europe/Oslo"

COPY build/libs/pgi-lagre-inntekt-popp.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]