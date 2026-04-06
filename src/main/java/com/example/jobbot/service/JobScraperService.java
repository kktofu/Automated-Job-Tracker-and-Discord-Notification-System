package com.example.jobbot.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class JobScraperService {

    public record JobInfo(String id, String title, String company, String link) {}

    public List<JobInfo> searchJobs(String keyword) {
        List<JobInfo> jobs = new ArrayList<>();
        
        //自動設定 ChromeDriver
        WebDriverManager.chromedriver().setup();

        //設定 Chrome 選項 (使用無頭模式避免彈出視窗)
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // 背景執行
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);

        try {
            // 104 search URL
            String url = "https://www.104.com.tw/jobs/search/?jobsource=index_s&keyword=" + keyword +"&mode=s&page=1&order=16&searchJobs=1";
            driver.get(url);

            // 等待頁面內容載入 (等待職缺列表元素出現)
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//a[contains(@href, '/job/')]")
            ));

            // 抓取職缺資訊
            List<WebElement> jobElements = driver.findElements(By.cssSelector(".info-container"));


            for (WebElement element : jobElements) {
                try {
                    WebElement titleEl = element.findElement(By.cssSelector("a.info-job__text"));
                    String title = titleEl.getText();
                    String link = titleEl.getAttribute("href");
                    WebElement companyEl = element.findElement(By.cssSelector("a.info-company__text"));
                    String company = companyEl.getText();

                    if (link != null && !link.isBlank()) {
                        String jobId = link.split("/job/")[1].split("\\?")[0];
                        jobs.add(new JobInfo(jobId, title, company, link));
                    }
                } catch (Exception e) {
                    // 略過單一元素解析失敗
                }
            }
        } catch (Exception e) {
            System.err.println("爬取過程發生錯誤：" + e.getMessage());
        } finally {
            //關閉瀏覽器
            driver.quit();
        }
        return jobs;
    }
}
