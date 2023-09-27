// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.service;

import com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.websocket.SocketHandler;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;

public class RecordProcessorFactory implements ShardRecordProcessorFactory {
    private final SocketHandler socketHandler;

    public RecordProcessorFactory(SocketHandler socketHandler) {
        this.socketHandler = socketHandler;
    }

    @Override
    public ShardRecordProcessor shardRecordProcessor() {
        return new RecordProcessor(socketHandler);
    }
}