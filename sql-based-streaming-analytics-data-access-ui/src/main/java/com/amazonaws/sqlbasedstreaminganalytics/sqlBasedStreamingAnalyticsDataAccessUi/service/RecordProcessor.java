package com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.service;

import com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.websocket.SocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.lifecycle.events.*;
import software.amazon.kinesis.processor.ShardRecordProcessor;

public class RecordProcessor implements ShardRecordProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordProcessor.class);
    private final SocketHandler socketHandler;

    public RecordProcessor(SocketHandler socketHandler) {
        this.socketHandler = socketHandler;
    }

    @Override public void initialize(InitializationInput initializationInput) {
        LOGGER.info("Initializing @ Sequence: {}", initializationInput.extendedSequenceNumber());
    }

    @Override public void processRecords(ProcessRecordsInput processRecordsInput) {
        processRecordsInput.records().forEach(r -> {
            String body = SdkBytes.fromByteBuffer(r.data()).asUtf8String();
            LOGGER.info("Processing record pk: {} -- Seq: {} -- Txt: {}", r.partitionKey(), r.sequenceNumber(), body);
            socketHandler.sendMessage(body);
        });

    }

    @Override public void leaseLost(LeaseLostInput leaseLostInput) {
        LOGGER.info("Lost lease, so terminating.");
    }

    @Override public void shardEnded(ShardEndedInput shardEndedInput) {
        LOGGER.info("Reached shard end checkpointing.");
        try {
            shardEndedInput.checkpointer().checkpoint();
        } catch (InvalidStateException | ShutdownException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
        LOGGER.info("Scheduler is shutting down, checkpointing.");
        try {
            shutdownRequestedInput.checkpointer().checkpoint();
        } catch (InvalidStateException | ShutdownException e) {
            throw new RuntimeException(e);
        }
    }
}