FROM amazoncorretto:17
RUN mkdir /tmp
ADD https://github.com/JWThewes/reusable-asset-sql-based-streaming-analytics/suites/15647102284/artifacts/893063792 /tmp
RUN unzip /tmp/sql-based-streaming-analytics-data-access-ui.jar.zip
RUN pwd && ls /tmp
COPY /tmp/sql-based-streaming-analytics-data-access-ui.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]