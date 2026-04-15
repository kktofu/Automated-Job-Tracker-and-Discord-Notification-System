package com.example.jobbot.service;

import com.example.jobbot.model.Subscription;
import com.example.jobbot.repository.SubscriptionRepository;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class DiscordBotService extends ListenerAdapter {

    @Value("${discord.bot.token}")
    private String token;

    private JDA jda;
    private final SubscriptionRepository subscriptionRepository;
    private final ResumeAnalysisService resumeAnalysisService;
    private final JobScraperService jobScraperService;

    public DiscordBotService(SubscriptionRepository subscriptionRepository, 
                             ResumeAnalysisService resumeAnalysisService, 
                             JobScraperService jobScraperService) {
        this.subscriptionRepository = subscriptionRepository;
        this.resumeAnalysisService = resumeAnalysisService;
        this.jobScraperService = jobScraperService;
    }

    @PostConstruct
    public void init() throws InterruptedException {
        if ("YOUR_DISCORD_BOT_TOKEN_HERE".equals(token)) {
            System.err.println("Discord Token is not set! Please update application.properties.");
            return;
        }

        jda = JDABuilder.createDefault(token)
                .addEventListeners(this)
                .build()
                .awaitReady();

        // Register slash commands
        jda.updateCommands().addCommands(
                Commands.slash("subscribe", "Subscribe to job keywords")
                        .addOption(OptionType.STRING, "keyword", "Keyword to search for", true),
                Commands.slash("unsubscribe", "Remove a job keyword subscription")
                        .addOption(OptionType.STRING, "keyword", "Keyword to remove", true),
                Commands.slash("list", "List your subscriptions"),
                Commands.slash("analyze", "上傳履歷 PDF 進行分析並推薦職缺")
                        .addOption(OptionType.ATTACHMENT, "resume", "您的履歷 PDF 檔案", true)
        ).queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // No longer auto-processing attachments in onMessageReceived
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String channelId = event.getChannel().getId();

        if (event.getName().equals("subscribe")) {
            String keyword = event.getOption("keyword").getAsString();
            subscriptionRepository.save(new Subscription(keyword, channelId));
            event.reply("Subscribed to: " + keyword).queue();
        } else if (event.getName().equals("unsubscribe")) {
            String keyword = event.getOption("keyword").getAsString();
            List<Subscription> subs = subscriptionRepository.findByChannelId(channelId);
            subs.stream()
                    .filter(s -> s.getKeyword().equalsIgnoreCase(keyword))
                    .forEach(subscriptionRepository::delete);
            event.reply("Unsubscribed from: " + keyword).queue();
        } else if (event.getName().equals("list")) {
            List<Subscription> subs = subscriptionRepository.findByChannelId(channelId);
            if (subs.isEmpty()) {
                event.reply("You have no subscriptions in this channel.").queue();
            } else {
                StringBuilder sb = new StringBuilder("Current subscriptions:\n");
                subs.forEach(s -> sb.append("- ").append(s.getKeyword()).append("\n"));
                event.reply(sb.toString()).queue();
            }
        } else if (event.getName().equals("analyze")) {
            Message.Attachment attachment = event.getOption("resume").getAsAttachment();
            if (!"pdf".equalsIgnoreCase(attachment.getFileExtension())) {
                event.reply("請上傳 PDF 檔案！").setEphemeral(true).queue();
                return;
            }

            event.deferReply().queue();

            attachment.getProxy().download().thenAccept(inputStream -> {
                try {
                    byte[] pdfBytes = inputStream.readAllBytes();
                    String text = resumeAnalysisService.extractTextFromPdf(pdfBytes);
                    List<String> keywords = resumeAnalysisService.analyzeResumeForKeywords(text);
                    
                    event.getHook().sendMessage("分析完成！根據您的履歷，建議搜尋關鍵字為: " + String.join(", ", keywords)).queue();
                    
                    if (!keywords.isEmpty()) {
                        String searchKeyword = keywords.get(0);
                        event.getHook().sendMessage("正在為您搜尋 " + searchKeyword + " 相關職缺...").queue();
                        
                        CompletableFuture.runAsync(() -> {
                            List<JobScraperService.JobInfo> jobs = jobScraperService.searchJobs(searchKeyword);
                            if (jobs.isEmpty()) {
                                event.getHook().sendMessage("很抱歉，目前沒找到相關職缺。").queue();
                            } else {
                                StringBuilder sb = new StringBuilder("幫您找到以下推薦職缺：\n");
                                jobs.stream().limit(5).forEach(job -> 
                                    sb.append("- [").append(job.title()).append("] (").append(job.company()).append(") \n  ").append(job.link()).append("\n")
                                );
                                event.getHook().sendMessage(sb.toString()).queue();
                            }
                        });
                    }
                } catch (Exception e) {
                    event.getHook().sendMessage("處理履歷時發生錯誤: " + e.getMessage()).queue();
                }
            });
        }
    }

    public void sendMessage(String channelId, String message) {
        MessageChannel channel = jda.getChannelById(MessageChannel.class, channelId);
        if (channel != null) {
            channel.sendMessage(message).queue();
        }
    }
}
