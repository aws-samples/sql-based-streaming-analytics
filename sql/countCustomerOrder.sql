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
    window_start TIMESTAMP(3),
    window_end TIMESTAMP(3),
    customerId INT,
    orderCount BIGINT
) PARTITIONED BY (customerId)
WITH (
'connector' = 'kinesis',
'stream' = '##OUTPUT_STREAM_NAME##',
'aws.region' = '##REGION##',
'format' = 'json',
'json.timestamp-format.standard' = 'ISO-8601');

INSERT INTO orderOut
SELECT window_start, window_end, customerId, COUNT(customerId) as orderCount
FROM TABLE(
        TUMBLE(TABLE orderIn, DESCRIPTOR(eventTime_ltz), INTERVAL '20' SECONDS))
GROUP BY window_start, window_end, GROUPING SETS ((customerId), ());