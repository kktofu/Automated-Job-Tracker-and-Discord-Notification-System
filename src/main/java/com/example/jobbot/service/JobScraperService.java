package com.example.jobbot.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class JobScraperService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public record JobInfo(String id, String title, String company, String link) {}

    public List<JobInfo> searchJobs(String keyword) {
        List<JobInfo> jobs = new ArrayList<>();
        try {
            // 104 search API URL
            String url = "https://www.104.com.tw/jobs/search/?jobsource=index_s&keyword="+ keyword;

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .get();
            System.out.println(doc);
            Elements jobElements = doc.select("article.job-list-item");

            for (Element element : jobElements) {
                // 取得職缺名稱與連結
                String title = element.attr("data-job-name");
                String company = element.attr("data-cust-name");
                String jobId = element.attr("data-job-no");
                String link = "https://www.104.com.tw/job/" + jobId;

                if (!title.isBlank()) {
                    jobs.add(new JobInfo(jobId, title, company, link));
                }
            }
        } catch (Exception e) {
            System.out.println("例外原因："+ e);
        }
        return jobs;
    }
}
