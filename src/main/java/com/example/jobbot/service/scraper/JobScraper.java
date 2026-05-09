package com.example.jobbot.service.scraper;

import com.example.jobbot.service.JobScraperService.JobInfo;
import java.util.List;

public interface JobScraper {
    List<JobInfo> searchJobs(String keyword);
    String getSourceName();
}
