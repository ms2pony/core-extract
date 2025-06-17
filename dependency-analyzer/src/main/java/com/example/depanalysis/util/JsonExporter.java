package com.example.depanalysis.util;

import com.example.depanalysis.api.AnalysisReport;
import com.example.depanalysis.config.OutputConfig;
import com.example.depanalysis.model.ClassSymbols;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * JSON导出工具
 */
public class JsonExporter {
    
    /**
     * 导出分析报告为JSON文件
     */
    public static void exportToJson(AnalysisReport report, String fileName, String methodName, OutputConfig config) {
        try {
            // 创建输出目录
            Files.createDirectories(Paths.get(config.getOutputDirectory()));
            
            // 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String outputFile = String.format("%s/%s_%s_%s_%s.json", 
                    config.getOutputDirectory(),
                    config.getFileNamePrefix(),
                    fileName.replace(".java", ""), 
                    methodName, 
                    timestamp);
            
            // 构建JSON
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"sourceFile\": \"").append(escape(fileName)).append("\",\n");
            json.append("  \"method\": \"").append(escape(methodName)).append("\",\n");
            json.append("  \"analysisTime\": \"").append(LocalDateTime.now()).append("\",\n");
            json.append("  \"totalClasses\": ").append(report.getClasses().size()).append(",\n");
            json.append("  \"dependencies\": {\n");
            
            boolean first = true;
            for (Map.Entry<String, ClassSymbols> entry : report.getClasses().entrySet()) {
                if (!first) json.append(",\n");
                first = false;
                
                String className = entry.getKey();
                ClassSymbols symbols = entry.getValue();
                
                json.append("    \"").append(escape(className)).append("\": {\n");
                json.append("      \"fields\": ").append(symbols.getFields().size()).append(",\n");
                json.append("      \"enums\": ").append(symbols.getEnumConstants().size()).append(",\n");
                json.append("      \"annotations\": ").append(symbols.getAnnotations().size()).append(",\n");
                json.append("      \"isType\": ").append(symbols.isReferencedAsType()).append("\n");
                json.append("    }");
            }
            
            json.append("\n  }\n}");
            
            // 写入文件
            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(json.toString());
            }
            
            System.out.println("JSON报告已导出: " + outputFile);
            
        } catch (IOException e) {
            System.err.println("导出失败: " + e.getMessage());
        }
    }
    
    /**
     * 使用默认配置导出
     */
    public static void exportToJson(AnalysisReport report, String fileName, String methodName) {
        exportToJson(report, fileName, methodName, new OutputConfig());
    }
    
    private static String escape(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"");
    }
}