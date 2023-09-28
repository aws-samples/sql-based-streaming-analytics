// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("data.access.ui")
public record DataAccessUiConfigurationProperties(String inputStreamName, String outputStreamName,
        String kclCheckpointDynamoTableName) {

}
