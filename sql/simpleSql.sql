-- Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
-- SPDX-License-Identifier: MIT-0

CREATE TABLE orderIn
(
    customerId   INT,
    customerName VARCHAR(800),
    productId    INT,
    productName  VARCHAR(800),
    eventTime_ltz AS PROCTIME()
) PARTITIONED BY (customerId)
WITH (
'connector' = 'kinesis',
'stream' = '##INPUT_STREAM_NAME##',
'aws.region' = '##REGION##',
'scan.stream.initpos' = 'LATEST',
'format' = 'json',
'json.timestamp-format.standard' = 'ISO-8601');

CREATE TABLE orderOut
(
    customerId INT,
    productId  INT
) PARTITIONED BY (customerId)
WITH (
'connector' = 'kinesis',
'stream' = '##OUTPUT_STREAM_NAME##',
'aws.region' = '##REGION##',
'format' = 'json',
'json.timestamp-format.standard' = 'ISO-8601');

INSERT INTO orderOut
SELECT customerId, productId
FROM orderIn;