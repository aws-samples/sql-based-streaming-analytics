package com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.controller;

import com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.config.DataAccessUiConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;

import java.util.Random;

@RestController
@RequestMapping("/demo")
public class DemoController {

    private final KinesisClient
            kinesisClient =
            KinesisClient.builder().httpClient(UrlConnectionHttpClient.builder().build()).build();
    private final DataAccessUiConfigurationProperties dataAccessUiConfigurationProperties;

    public DemoController(DataAccessUiConfigurationProperties dataAccessUiConfigurationProperties) {
        this.dataAccessUiConfigurationProperties = dataAccessUiConfigurationProperties;
    }

    @GetMapping public void addRecord() {
        Random random = new Random();
        char randomChar = (char) (random.nextInt(26) + 'a');
        kinesisClient.putRecord(PutRecordRequest
                .builder()
                .streamName(dataAccessUiConfigurationProperties.outputStreamName())
                .partitionKey(randomChar + "")
                .data(SdkBytes.fromUtf8String("This is a message"))
                .build());
    }

}
