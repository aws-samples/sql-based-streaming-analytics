# 1.0.0
Initial version of SQL based streaming analytics using Apache Flink

Features that are included in the first release:
- DataAccessUI to generate and inspect data
- Generic Apache Flink application which is able to download a SQL file from Amazon S3 and execute it as an Apache Flink job running on Amazon Managed Service for Apache Flink
- Cloud Development Kit (CDK) code for the infrastructure
- Automatic restarting of the Apache Flink application when a change on the SQL file in Amazon S3 occurs.