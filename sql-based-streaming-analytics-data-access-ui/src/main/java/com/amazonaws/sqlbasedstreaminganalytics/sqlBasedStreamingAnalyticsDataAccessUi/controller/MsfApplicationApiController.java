// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.controller;

import com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.service.MsfApplicationService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/msfApplication")
public class MsfApplicationApiController {

    private final MsfApplicationService msfApplicationService;

    public MsfApplicationApiController(MsfApplicationService msfApplicationService) {
        this.msfApplicationService = msfApplicationService;
    }

    @PostMapping(value = "/start/{applicationName}")
    public void startMsfApplication(@PathVariable String applicationName) {
        msfApplicationService.startMsfApplication(applicationName);
    }

    @PostMapping(value = "/stop/{applicationName}")
    public void stopMsfApplication(@PathVariable String applicationName) {
        msfApplicationService.stopMsfApplication(applicationName);
    }

}
