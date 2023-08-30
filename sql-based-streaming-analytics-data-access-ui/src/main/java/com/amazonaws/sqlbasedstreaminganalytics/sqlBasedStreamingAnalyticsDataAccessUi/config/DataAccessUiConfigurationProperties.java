package com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("data.access.ui")
public record DataAccessUiConfigurationProperties(String inputStreamName, String outputStreamName) {

}
