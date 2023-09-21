package com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi;

import com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.config.DataAccessUiConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication @EnableScheduling @EnableConfigurationProperties(DataAccessUiConfigurationProperties.class)
public class SqlBasedStreamingAnalyticsDataAccessUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlBasedStreamingAnalyticsDataAccessUiApplication.class, args);
    }

}
