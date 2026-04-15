package com.example.jobbot.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResumeAnalysisService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    public String extractTextFromPdf(byte[] pdfData) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfData)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 分析履歷內容並回傳推薦關鍵字清單
     */
    public List<String> analyzeResumeForKeywords(String resumeText) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return fallbackKeywords(resumeText);
        }

        try {

            Client client = Client.builder().apiKey(geminiApiKey).build();

            
            String prompt = "請分析以下履歷內容，並列出最適合用來在 104 人力銀行搜尋職缺的 3 個關鍵字。" +
                    "請直接回傳關鍵字，並以逗號分隔，不要有任何額外解釋。\n\n" +
                    "履歷內容：\n" + resumeText;

            GenerateContentResponse response =
                    client.models.generateContent(
                            "gemini-3-flash-preview",
                            prompt,
                            null);
            // 呼叫並取得結果
            String result = response.text().trim();
            
            return Arrays.stream(result.split("[,，]"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            System.err.println("Gemini API 呼叫失敗: " + e.getMessage());
            return fallbackKeywords(resumeText);
        }
    }

    private List<String> fallbackKeywords(String resumeText) {
        return Arrays.stream(resumeText.split("\\s+"))
                .map(s -> s.replaceAll("[^a-zA-Z0-9#+]", ""))
                .filter(s -> s.length() > 2)
                .distinct()
                .limit(3)
                .collect(Collectors.toList());
    }
}
