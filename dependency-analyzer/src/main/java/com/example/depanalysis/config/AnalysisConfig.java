package com.example.depanalysis.config;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 分析配置类
 */
@Data
public class AnalysisConfig {
    /**
     * 多模块源码根目录列表
     */
    private List<Path> sourceRoots = new ArrayList<>();
    
    /**
     * 是否忽略无法解析的符号
     */
    private boolean ignoreUnresolved = true;
}