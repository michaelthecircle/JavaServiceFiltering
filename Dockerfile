FROM eclipse-temurin:21-jre-ubi9-minimal

WORKDIR /app

COPY build/libs/JavaServiceFiltering.jar .

EXPOSE 8080

CMD ["java", "-jar", "JavaServiceFiltering.jar"]
