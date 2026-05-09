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
public class CakeResumeScraper implements JobScraper {

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
            // CakeResume search URL
            String url = "https://www.cakeresume.com/jobs/" + keyword;
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            // Wait for job cards to appear
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(@href, '/jobs/')]")));

            List<WebElement> jobElements = driver.findElements(By.cssSelector(".JobSearchHits-module-scss-module__E7HalG__list"));

            for (WebElement element : jobElements) {
                try {
                    WebElement titleEl = element.findElement(By.cssSelector("a.JobSearchItem-module-scss-module___szW4W__jobTitle"));
                    String title = titleEl.getText();
                    String link = titleEl.getAttribute("href");
                    
                    WebElement companyEl = element.findElement(By.cssSelector("a.JobSearchItem-module-scss-module___szW4W__companyName"));
                    String company = companyEl.getText();

                    if (link != null && !link.isBlank()) {
                        // Extract ID from link or use link as ID
                        String jobId = "cake-" + link.substring(link.lastIndexOf("/") + 1);
                        jobs.add(new JobInfo(jobId, title, company, link));
                    }
                } catch (Exception e) {
                    // Skip individual parsing failure
                }
            }
        } catch (Exception e) {
            System.err.println("CakeResume Scraper Error: " + e.getMessage());
        } finally {
            driver.quit();
        }
        return jobs;
    }

    @Override
    public String getSourceName() {
        return "CakeResume";
    }
}
