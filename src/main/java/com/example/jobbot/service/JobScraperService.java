package com.example.jobbot.service;

import com.example.jobbot.service.scraper.JobScraper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class JobScraperService {

    private final List<JobScraper> scrapers;

    public JobScraperService(List<JobScraper> scrapers) {
        this.scrapers = scrapers;
    }

    public record JobInfo(String id, String title, String company, String link) {}

    public List<JobInfo> searchJobs(String keyword) {
        // 使用 CompletableFuture 進行多執行緒同時爬取
        List<CompletableFuture<List<JobInfo>>> futures = scrapers.stream()
                .map(scraper -> CompletableFuture.supplyAsync(() -> {
                    System.out.println("Starting scraper: " + scraper.getSourceName() + " for keyword: " + keyword);
                    return scraper.searchJobs(keyword);
                }))
                .collect(Collectors.toList());

        // 等待所有爬蟲完成並彙整結果
        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
