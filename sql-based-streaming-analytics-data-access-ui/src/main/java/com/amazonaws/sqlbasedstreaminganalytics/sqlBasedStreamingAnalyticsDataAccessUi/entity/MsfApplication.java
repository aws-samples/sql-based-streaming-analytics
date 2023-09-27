// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.entity;

public class MsfApplication {
    private final String applicationName;
    private final String applicationStatusAsString;
    private final String applicationArn;

    public MsfApplication(String applicationName, String applicationStatusAsString, String applicationArn) {

        this.applicationName = applicationName;
        this.applicationStatusAsString = applicationStatusAsString;
        this.applicationArn = applicationArn;
    }

    public String getApplicationStatusAsString() {
        return applicationStatusAsString;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getApplicationArn() {
        return applicationArn;
    }
}