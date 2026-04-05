package com.example.jobbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class JobHistory {
    @Id
    private String jobId;

    public JobHistory() {}
    public JobHistory(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
}
