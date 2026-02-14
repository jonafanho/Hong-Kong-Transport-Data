FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/Hong-Kong-Transport-Data.jar app.jar
RUN mkdir /data
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
