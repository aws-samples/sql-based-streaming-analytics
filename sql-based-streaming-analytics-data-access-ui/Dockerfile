FROM amazoncorretto:17
ADD https://github.com/JWThewes/reusable-asset-sql-based-streaming-analytics/releases/download/0.0.1-SNAPSHOT/sql-based-streaming-analytics-data-access-ui.jar /tmp/app.jar
COPY /tmp/app.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]