package com.example.depanalysis.api;

import java.io.File;
import java.io.IOException;

/**
 * 依赖分析器接口
 */
public interface DependencyAnalyzer {
    /**
     * 分析指定Java文件中目标方法的依赖关系
     * 
     * @param javaFile 要分析的Java源文件
     * @param methodName 目标方法名
     * @param cfg 分析配置
     * @return 分析报告
     * @throws IOException 文件读取异常
     */
    AnalysisReport analyze(File javaFile, String methodName, AnalysisConfig cfg) throws IOException;
}