// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.controller;

import com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.service.KinesisProducerService;
import com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.service.MsfApplicationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping({ "/", "/index.html", "/index", "/home" })
public class IndexController {

    private final KinesisProducerService kinesisProducerService;
    private final MsfApplicationService msfApplicationService;

    public IndexController(KinesisProducerService kinesisProducerService, MsfApplicationService msfApplicationService) {
        this.kinesisProducerService = kinesisProducerService;
        this.msfApplicationService = msfApplicationService;
    }

    @GetMapping
    public String getIndexPage(Model model) {
        model.addAttribute("randomDataGenerationEnabled", kinesisProducerService.isGeneratingRecords());
        model.addAttribute("msfApplicationList", msfApplicationService.getMsfApplications());
        return "index";
    }

}
