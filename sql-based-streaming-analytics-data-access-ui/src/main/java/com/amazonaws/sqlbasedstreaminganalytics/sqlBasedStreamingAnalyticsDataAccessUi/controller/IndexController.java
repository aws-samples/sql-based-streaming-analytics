package com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.controller;

import com.amazonaws.sqlbasedstreaminganalytics.sqlBasedStreamingAnalyticsDataAccessUi.service.KinesisProducerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping({"/", "/index.html", "/index", "/home"})
public class IndexController {

    private final KinesisProducerService kinesisProducerService;

    public IndexController(KinesisProducerService kinesisProducerService) {
        this.kinesisProducerService = kinesisProducerService;
    }

    @GetMapping public String getIndexPage(Model model) {
        model.addAttribute("randomDataGenerationEnabled", kinesisProducerService.isGeneratingRecords());
        return "index";
    }

}
