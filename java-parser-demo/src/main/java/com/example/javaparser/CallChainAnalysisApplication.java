package com.example.javaparser;

import com.example.javaparser.analyzer.MethodCallChainAnalyzer;
import lombok.extern.slf4j.Slf4j;

/**
 * 方法调用链分析应用程序
 */
@Slf4j
public class CallChainAnalysisApplication {
    
    public static void main(String[] args) {
        log.info("启动方法调用链分析程序...");
        
        try {
            // 设置源码路径 - 指向提取的项目源码
            String[] sourcePaths = {
                "k:\\power-script\\core-extract\\output\\step2-set\\tender-api\\src\\main\\java",
                "k:\\power-script\\core-extract\\output\\step2-set\\tender-business\\src\\main\\java",
                "k:\\power-script\\core-extract\\output\\step2-set\\tender-contract\\src\\main\\java",
                "k:\\power-script\\core-extract\\output\\step2-set\\tender-job\\src\\main\\java"
            };
            
            // 创建分析器
            MethodCallChainAnalyzer analyzer = new MethodCallChainAnalyzer(sourcePaths);
            
            // 分析指定方法的调用链
            // 目标方法: PurchaserOrderSettlementController.search
            analyzer.analyzeMethodCallChain("PurchaserOrderSettlementController", "search");
            
        } catch (Exception e) {
            log.error("分析过程中发生错误: {}", e.getMessage(), e);
        }
        
        log.info("方法调用链分析完成");
    }
}