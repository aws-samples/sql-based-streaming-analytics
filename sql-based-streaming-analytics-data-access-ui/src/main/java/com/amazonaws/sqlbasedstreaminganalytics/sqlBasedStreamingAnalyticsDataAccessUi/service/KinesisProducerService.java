package com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.service;

import com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.config.DataAccessUiConfigurationProperties;
import net.datafaker.Faker;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry;

import java.util.ArrayList;
import java.util.List;

@Service
public class KinesisProducerService {

    private final KinesisClient kinesisClient = KinesisClient.builder().httpClient(UrlConnectionHttpClient.builder().build()).build();
    private final DataAccessUiConfigurationProperties dataAccessUiConfigurationProperties;
    private final Faker faker = new Faker();
    private String partitionKeyValue = "a";
    private boolean useFakerForPartitionKeyValue = false;
    private String body = "";
    private boolean useFakerForBody = false;
    private int recordsToGenerateEvery2Seconds = 1;
    private boolean generatingRecordsRunning = false;

    public KinesisProducerService(DataAccessUiConfigurationProperties dataAccessUiConfigurationProperties) {
        this.dataAccessUiConfigurationProperties = dataAccessUiConfigurationProperties;
    }

    public void startGeneratingRecords(String partitionKeyValue,
                                       boolean useFakerForPartitionKeyValue,
                                       String body,
                                       boolean useFakerForBody,
                                       int recordsToGenerateEvery2Seconds) {
        this.partitionKeyValue = partitionKeyValue;
        this.useFakerForPartitionKeyValue = useFakerForPartitionKeyValue;
        this.body = body;
        this.useFakerForBody = useFakerForBody;
        this.recordsToGenerateEvery2Seconds = recordsToGenerateEvery2Seconds;
        generatingRecordsRunning = true;
    }

    public void stopGeneratingRecords() {
        generatingRecordsRunning = false;
        partitionKeyValue = "";
        useFakerForPartitionKeyValue = false;
        body = "";
        useFakerForBody = false;
        recordsToGenerateEvery2Seconds = 1;
    }

    public boolean isGeneratingRecords() {
        return generatingRecordsRunning;
    }

    @Scheduled(fixedDelay = 2000) private void produceRecords() {
        if (!generatingRecordsRunning) {
            return;
        }
        List<PutRecordsRequestEntry> recordEntries = generateRecords();
        PutRecordsRequest
                putRecordsRequest =
                PutRecordsRequest
                        .builder()
                        .streamName(dataAccessUiConfigurationProperties.inputStreamName())
                        .records(recordEntries)
                        .build();
        kinesisClient.putRecords(putRecordsRequest);
    }

    private List<PutRecordsRequestEntry> generateRecords() {
        List<PutRecordsRequestEntry> recordEntries = new ArrayList<>();
        for (int i = 0; i < recordsToGenerateEvery2Seconds; i++) {
            var recordEntry = generateRecordEntry();
            recordEntries.add(recordEntry);
        }
        return recordEntries;
    }

    private PutRecordsRequestEntry generateRecordEntry() {
        PutRecordsRequestEntry.Builder recordEntryBuilder = PutRecordsRequestEntry.builder();
        if (useFakerForPartitionKeyValue) {
            recordEntryBuilder = recordEntryBuilder.partitionKey(faker.expression(partitionKeyValue));
        } else {
            recordEntryBuilder = recordEntryBuilder.partitionKey(partitionKeyValue);
        }
        if (useFakerForBody) {
            recordEntryBuilder = recordEntryBuilder.data(SdkBytes.fromUtf8String(faker.expression(body)));
        } else {
            recordEntryBuilder = recordEntryBuilder.data(SdkBytes.fromUtf8String(body));
        }
        return recordEntryBuilder.build();
    }

}
