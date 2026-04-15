package com.example.jobbot.service;

import com.example.jobbot.model.JobHistory;
import com.example.jobbot.model.Subscription;
import com.example.jobbot.repository.JobHistoryRepository;
import com.example.jobbot.repository.SubscriptionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobSchedulerService {

    private final JobScraperService jobScraperService;
    private final SubscriptionRepository subscriptionRepository;
    private final JobHistoryRepository jobHistoryRepository;
    private final DiscordBotService discordBotService;

    public JobSchedulerService(JobScraperService jobScraperService,
                               SubscriptionRepository subscriptionRepository,
                               JobHistoryRepository jobHistoryRepository,
                               DiscordBotService discordBotService) {
        this.jobScraperService = jobScraperService;
        this.subscriptionRepository = subscriptionRepository;
        this.jobHistoryRepository = jobHistoryRepository;
        this.discordBotService = discordBotService;
    }

    // Cron expression for 9 AM daily: "0 0 9 * * *"
    // For testing, run every 30 minutes: "0 0/30 * * * *"
    @Scheduled(cron = "0 45 13 * * *")
    public void runDailyJobCheck() {
        System.out.println("Running daily job check...");
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        System.out.println("目前共有 " + subscriptions.size() + " 筆訂閱");

        for (Subscription sub : subscriptions) {
            try {
                List<JobScraperService.JobInfo> jobs = jobScraperService.searchJobs(sub.getKeyword());

                StringBuilder sb = new StringBuilder();
                sb.append("📢 New jobs found for: **").append(sub.getKeyword()).append("**\n");

                int count = 0;
                for (JobScraperService.JobInfo job : jobs) {
                    if (job.id() == null) continue;

                    if (!jobHistoryRepository.existsById(job.id())) {

                        sb.append("🔹 **").append(job.title())
                                .append("** @ ").append(job.company()).append("\n")
                                .append("🔗 <").append(job.link()).append(">\n\n");

                        jobHistoryRepository.save(new JobHistory(job.id()));
                        count++;
                    }

                    if (count >= 5) break; // 防止洗頻
                }

                if (count > 0) {
                    discordBotService.sendMessage(sub.getChannelId(), sb.toString());
                }
            }catch (Exception e) {
                System.out.println("處理訂閱失敗：" + sub.getKeyword());
            }
        }
    }
}
