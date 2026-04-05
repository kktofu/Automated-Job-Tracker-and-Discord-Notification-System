package com.example.jobbot.repository;

import com.example.jobbot.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByChannelId(String channelId);
}
