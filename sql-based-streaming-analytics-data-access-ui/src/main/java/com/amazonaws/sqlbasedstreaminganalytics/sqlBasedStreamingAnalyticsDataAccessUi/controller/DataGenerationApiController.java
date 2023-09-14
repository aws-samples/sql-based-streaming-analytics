/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the 'license' file accompanying this file. This file is distributed on an 'AS IS' BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 *
 */

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

    @PostMapping("/toggle") public void toggleDataGeneration(@RequestBody DataGenerationRequest dataGenerationRequest) {
        if (kinesisProducerService.isGeneratingRecords()) {
            kinesisProducerService.stopGeneratingRecords();
        } else {
            kinesisProducerService.startGeneratingRecords(dataGenerationRequest.partitionKey(),
                    dataGenerationRequest.fakerForPartitionKey(), dataGenerationRequest.data(),
                    dataGenerationRequest.fakerForData(), dataGenerationRequest.numRequestsPer2Seconds());
            System.out.println(dataGenerationRequest);
        }
    }

}
