// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sqlBasedStreamingAnalytics;

import com.amazonaws.services.kinesisanalytics.runtime.KinesisAnalyticsRuntime;
import com.amazonaws.sqlBasedStreamingAnalytics.sql.SqlExecutor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.LocalStreamEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

public class SqlBasedStreamingAnalyticsFlinkJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlBasedStreamingAnalyticsFlinkJob.class);

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment environment;
        Properties applicationProperties;
        if (Files.exists(Paths.get("sql-based-streaming-analytics-flink-job/properties.json"))) {
            LOGGER.info("Starting application in local mode");
            // properties.json file only exists on local, so it's local environment
            environment = LocalStreamEnvironment.createLocalEnvironmentWithWebUI(new Configuration());
            environment.setParallelism(1);
            Map<String, Properties> applicationProperties1 = KinesisAnalyticsRuntime
                    .getApplicationProperties("sql-based-streaming-analytics-flink-job/properties.json");
            applicationProperties = applicationProperties1.get("ENV");
        } else {
            LOGGER.info("Starting application in cloud mode");
            // properties.json doesn't exist assume that we are on Cloud
            environment = StreamExecutionEnvironment.getExecutionEnvironment();
            applicationProperties = KinesisAnalyticsRuntime.getApplicationProperties().get("ENV");
        }
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(environment);
        String sqlFileName = applicationProperties.getProperty("run.file");
        LOGGER.info("Loading SQL file from location {}", sqlFileName);
        new SqlExecutor().extractAndExecuteSql(tableEnv, sqlFileName, applicationProperties);
    }

}
