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

package com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.service;

import com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.entity.MsfApplication;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.kinesisanalyticsv2.KinesisAnalyticsV2Client;
import software.amazon.awssdk.services.kinesisanalyticsv2.model.*;

import java.util.ArrayList;
import java.util.List;

@Service
public class MsfApplicationService {

    KinesisAnalyticsV2Client
            kinesisAnalyticsClient =
            KinesisAnalyticsV2Client.builder().httpClient(UrlConnectionHttpClient.builder().build()).build();

    private List<MsfApplication> msfApplicationList = new ArrayList<>();

    public List<MsfApplication> getMsfApplications() {
        return msfApplicationList;
    }

    public void startMsfApplication(String applicationName) {
        kinesisAnalyticsClient.startApplication(StartApplicationRequest
                .builder()
                .applicationName(applicationName)
                .build());
    }

    public void stopMsfApplication(String applicationName) {
        kinesisAnalyticsClient.stopApplication(StopApplicationRequest
                .builder()
                .applicationName(applicationName)
                .build());
    }

    @Scheduled(initialDelay = 0, fixedDelay = 5000) private void refreshMsfApplicationList() {
        ListApplicationsRequest request = ListApplicationsRequest.builder().build();
        ListApplicationsResponse response = kinesisAnalyticsClient.listApplications(request);
        List<MsfApplication> newMsfApplicationList = new ArrayList<>();
        for (ApplicationSummary applicationSummary : response.applicationSummaries()) {
            ListTagsForResourceResponse
                    listTagsForResourceResponse =
                    kinesisAnalyticsClient.listTagsForResource(ListTagsForResourceRequest
                            .builder()
                            .resourceARN(applicationSummary.applicationARN())
                            .build());
            if (listTagsForResourceResponse
                    .tags()
                    .contains(Tag.builder().key("application").value("sqlBasedStreamingAnalytics").build())) {
                newMsfApplicationList.add(new MsfApplication(applicationSummary.applicationName(),
                        applicationSummary.applicationStatusAsString(),
                        applicationSummary.applicationARN()));
            }
        }
        msfApplicationList = newMsfApplicationList;
    }

}
