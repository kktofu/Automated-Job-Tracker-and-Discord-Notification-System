Jobbot - 職缺通知機器人 

  Jobbot 是一個自動化的求職助手。它能讓使用者透過 Discord
  指令訂閱職缺關鍵字，並自動在每天固定時間前往 104 人力銀行
  爬取最新職缺，第一時間將符合條件的機會發送到您的 Discord 頻道。

  ---

  一、核心功能

   * 自動化爬取：利用 Selenium 與無頭瀏覽器 (Headless Chrome)
     模擬真人操作，準確抓取 104 動態加載的職缺資訊。
   * 定時通知：內建 Spring Scheduling，預設每天下午 21:45 (UTC+8)
     自動檢查新職缺。
   * Discord 互動：
       * /subscribe [關鍵字]：訂閱感興趣的職稱或技能。
       * /unsubscribe [關鍵字]：移除不再需要的訂閱。
       * /list：列出當前頻道所有已追蹤的關鍵字。
   * 排除重複：整合 H2 資料庫紀錄已發送過的
     jobId，確保同一個職缺不會重複通知，避免洗頻。
   * 流量保護：每個關鍵字每次更新僅發送前 5
     筆最新職缺，保持頻道整潔。

  ---

  二、技術棧

   * 框架：Spring Boot 3.x / 4.0 (Java 21)
   * 機器人：JDA (Java Discord API) 5.0
   * 爬蟲：Selenium WebDriver + JSoup
   * 資料庫：Spring Data JPA + H2 Database (內嵌式，無需額外安裝)
   * 依賴管理：Maven

  ---
