// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.service;

import com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.config.DataAccessUiConfigurationProperties;
import com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.websocket.SocketHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.KinesisClientUtil;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.retrieval.polling.PollingConfig;

import java.util.UUID;

@Service
public class KinesisConsumerService {

        private final KinesisAsyncClient kinesisAsyncClient = KinesisClientUtil
                        .createKinesisAsyncClient(KinesisAsyncClient.builder());
        private final DynamoDbAsyncClient dynamoClient = DynamoDbAsyncClient.builder().build();
        private final CloudWatchAsyncClient cloudWatchClient = CloudWatchAsyncClient.builder().build();
        private final DataAccessUiConfigurationProperties dataAccessUiConfigurationProperties;
        private final SocketHandler socketHandler;

        public KinesisConsumerService(DataAccessUiConfigurationProperties dataAccessUiConfigurationProperties,
                        SocketHandler socketHandler) {
                this.dataAccessUiConfigurationProperties = dataAccessUiConfigurationProperties;
                this.socketHandler = socketHandler;
        }

        @PostConstruct
        public void consumeOutputRecords() {
                ConfigsBuilder configsBuilder = new ConfigsBuilder(
                                dataAccessUiConfigurationProperties.outputStreamName(),
                                dataAccessUiConfigurationProperties.outputStreamName(),
                                kinesisAsyncClient,
                                dynamoClient,
                                cloudWatchClient,
                                UUID.randomUUID().toString(),
                                new RecordProcessorFactory(socketHandler))
                                .tableName(dataAccessUiConfigurationProperties.kclCheckpointDynamoTableName());
                Scheduler scheduler = new Scheduler(configsBuilder.checkpointConfig(),
                                configsBuilder.coordinatorConfig(),
                                configsBuilder.leaseManagementConfig(),
                                configsBuilder.lifecycleConfig(),
                                configsBuilder.metricsConfig(),
                                configsBuilder.processorConfig(),
                                configsBuilder
                                                .retrievalConfig()
                                                .retrievalSpecificConfig(new PollingConfig(
                                                                dataAccessUiConfigurationProperties.outputStreamName(),
                                                                kinesisAsyncClient)));
                Thread schedulerThread = new Thread(scheduler);
                schedulerThread.setDaemon(true);
                schedulerThread.start();
        }

}
