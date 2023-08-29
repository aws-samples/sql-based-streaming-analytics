package com.amazonaws.sqlBasedStreamingAnalytics;

import com.amazonaws.services.kinesisanalytics.runtime.KinesisAnalyticsRuntime;
import com.amazonaws.sqlBasedStreamingAnalytics.sql.SqlExecutor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.LocalStreamEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class SqlBasedStreamingAnalyticsFlinkJob {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment environment;
        Properties applicationProperties;
        if (Files.exists(Paths.get("./properties.json"))) {
            // properties.json file only exists on local, so it's local environment
            environment = LocalStreamEnvironment.createLocalEnvironmentWithWebUI(new Configuration());
            environment.setParallelism(1);
            applicationProperties = KinesisAnalyticsRuntime.getApplicationProperties("./properties.json").get("ENV");
        } else {
            // properties.json doesn't exist assume that we are on Cloud
            environment = StreamExecutionEnvironment.getExecutionEnvironment();
            applicationProperties = KinesisAnalyticsRuntime.getApplicationProperties().get("ENV");
        }
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(environment);
        new SqlExecutor().extractAndExecuteSql(tableEnv,
                applicationProperties.getProperty("run.file"),
                applicationProperties);
    }

}
