// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.controller;

import com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.entity.DataGenerationRequest;
import com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.service.KinesisProducerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dataGeneration")
public class DataGenerationApiController {

    private final KinesisProducerService kinesisProducerService;

    public DataGenerationApiController(KinesisProducerService kinesisProducerService) {
        this.kinesisProducerService = kinesisProducerService;
    }

    @PostMapping("/toggle")
    public void toggleDataGeneration(@RequestBody DataGenerationRequest dataGenerationRequest) {
        if (kinesisProducerService.isGeneratingRecords()) {
            kinesisProducerService.stopGeneratingRecords();
        } else {
            kinesisProducerService.startGeneratingRecords(dataGenerationRequest.partitionKey(),
                    dataGenerationRequest.fakerForPartitionKey(), dataGenerationRequest.jsonPointerForPartitionKey(),
                    dataGenerationRequest.data(),
                    dataGenerationRequest.fakerForData(), dataGenerationRequest.numRequestsPer2Seconds());
            System.out.println(dataGenerationRequest);
        }
    }

}
