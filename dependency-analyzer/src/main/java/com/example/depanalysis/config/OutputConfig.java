package com.example.depanalysis.config;

import lombok.Data;

/**
 * JSON输出配置
 */
@Data
public class OutputConfig {
    /**
     * 输出目录，默认为 target/reports
     */
    private String outputDirectory = "output/reports";
    
    /**
     * 文件名前缀，默认为 analysis
     */
    private String fileNamePrefix = "analysis";
}