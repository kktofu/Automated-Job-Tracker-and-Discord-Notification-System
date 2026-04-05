package com.example.jobbot.repository;

import com.example.jobbot.model.JobHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobHistoryRepository extends JpaRepository<JobHistory, String> {
}
