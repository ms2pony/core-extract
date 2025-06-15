/**
 * @Author: Your name
 * @Date:   2025-06-15 19:25:23
 * @Last Modified by:   Your name
 * @Last Modified time: 2025-06-15 19:57:21
 */
package com.example.javaparser;

import com.example.javaparser.analyzer.MethodCallChainAnalyzer;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 方法调用链分析应用程序
 */
@Slf4j
public class CallChainAnalysisApplication {
    
    public static void main(String[] args) {
        log.info("启动方法调用链分析程序...");
        // 1. 取启动时的工作目录（等价于 System.getProperty("user.dir")）
        Path baseDir = Paths.get("").toAbsolutePath();

        // 2. 相对于 baseDir 拼装各模块源码目录
        String[] modules = {
            "tender-api",
            "tender-business",
            "tender-contract",
            "tender-job"
        };

        try {
            // 设置源码路径 - 指向提取的项目源码
            String[] sourcePaths = new String[modules.length];
            for (int i = 0; i < modules.length; i++) {
                sourcePaths[i] = baseDir
                        .resolve("output")
                        .resolve("step2-set")
                        .resolve(modules[i])
                        .resolve("src")
                        .resolve("main")
                        .resolve("java")
                        .toString();
                log.info("sourcePath[i] : {}", sourcePaths[i]);
            }
            
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