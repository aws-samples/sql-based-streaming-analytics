// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

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

    KinesisAnalyticsV2Client kinesisAnalyticsClient = KinesisAnalyticsV2Client.builder()
            .httpClient(UrlConnectionHttpClient.builder().build()).build();

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

    @Scheduled(initialDelay = 0, fixedDelay = 5000)
    private void refreshMsfApplicationList() {
        ListApplicationsRequest request = ListApplicationsRequest.builder().build();
        ListApplicationsResponse response = kinesisAnalyticsClient.listApplications(request);
        List<MsfApplication> newMsfApplicationList = new ArrayList<>();
        for (ApplicationSummary applicationSummary : response.applicationSummaries()) {
            ListTagsForResourceResponse listTagsForResourceResponse = kinesisAnalyticsClient
                    .listTagsForResource(ListTagsForResourceRequest
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
