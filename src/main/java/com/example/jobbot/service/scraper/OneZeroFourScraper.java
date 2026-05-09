package com.example.jobbot.service.scraper;

import com.example.jobbot.service.JobScraperService.JobInfo;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class OneZeroFourScraper implements JobScraper {

    @Override
    public List<JobInfo> searchJobs(String keyword) {
        List<JobInfo> jobs = new ArrayList<>();
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);

        try {
            String url = "https://www.104.com.tw/jobs/search/?jobsource=index_s&keyword=" + keyword + "&mode=s&page=1&order=16&searchJobs=1";
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(@href, '/job/')]")));

            List<WebElement> jobElements = driver.findElements(By.cssSelector(".info-container"));

            for (WebElement element : jobElements) {
                try {
                    WebElement titleEl = element.findElement(By.cssSelector("a.info-job__text"));
                    String title = titleEl.getText();
                    String link = titleEl.getAttribute("href");
                    WebElement companyEl = element.findElement(By.cssSelector("a.info-company__text"));
                    String company = companyEl.getText();

                    if (link != null && !link.isBlank()) {
                        String jobId = "104-" + link.split("/job/")[1].split("\\?")[0];
                        jobs.add(new JobInfo(jobId, title, company, link));
                    }
                } catch (Exception e) {
                    // Skip individual parsing failure
                }
            }
        } catch (Exception e) {
            System.err.println("104 Scraper Error: " + e.getMessage());
        } finally {
            driver.quit();
        }
        return jobs;
    }

    @Override
    public String getSourceName() {
        return "104";
    }
}
