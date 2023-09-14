/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the 'license' file accompanying this file. This file is distributed on an 'AS IS' BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 *
 */

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