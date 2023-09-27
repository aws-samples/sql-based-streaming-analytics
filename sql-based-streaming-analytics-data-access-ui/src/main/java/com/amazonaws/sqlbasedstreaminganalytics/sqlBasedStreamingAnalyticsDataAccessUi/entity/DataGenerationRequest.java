// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.entity;

public record DataGenerationRequest(String partitionKey,
        String data,
        boolean fakerForData,
        boolean fakerForPartitionKey,
        boolean jsonPointerForPartitionKey,
        int numRequestsPer2Seconds) {
}
