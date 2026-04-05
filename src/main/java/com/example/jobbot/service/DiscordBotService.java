package com.example.jobbot.service;

import com.example.jobbot.model.Subscription;
import com.example.jobbot.repository.SubscriptionRepository;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiscordBotService extends ListenerAdapter {

    @Value("${discord.bot.token}")
    private String token;

    private JDA jda;
    private final SubscriptionRepository subscriptionRepository;

    public DiscordBotService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
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
                Commands.slash("list", "List your subscriptions")
        ).queue();
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
        }
    }

    public void sendMessage(String channelId, String message) {
        MessageChannel channel = jda.getChannelById(MessageChannel.class, channelId);
        if (channel != null) {
            channel.sendMessage(message).queue();
        }
    }
}
