package com.example.depanalysis;

import com.example.depanalysis.api.AnalysisConfig;
import com.example.depanalysis.api.AnalysisReport;
import com.example.depanalysis.api.DependencyAnalyzer;
import com.example.depanalysis.impl.DependencyAnalyzerImpl;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * 依赖分析器测试类
 */
public class DependencyAnalyzerTest {
    
    private DependencyAnalyzer analyzer;
    private AnalysisConfig config;
    
    @Before
    public void setUp() {
        analyzer = new DependencyAnalyzerImpl();
        config = new AnalysisConfig();
        
        // 添加源码根目录
        config.getSourceRoots().add(Paths.get("src/test/resources/app"));
        config.getSourceRoots().add(Paths.get("src/test/resources/common"));
        config.setIgnoreUnresolved(true);
    }
    
    @Test
    public void testAnalyzeMethod() throws Exception {
        // 创建测试用的Java文件
        File testFile = new File("src/test/resources/TestClass.java");
        
        // 分析指定方法
        AnalysisReport report = analyzer.analyze(testFile, "testMethod", config);
        
        // 验证结果
        assertNotNull(report);
        assertNotNull(report.getClasses());
        
        // 验证只包含指定源码根目录的包
        report.getClasses().keySet().forEach(className -> {
            assertFalse("不应包含JDK包", className.startsWith("java."));
            assertFalse("不应包含JDK包", className.startsWith("javax."));
        });
    }
}