package com.example.depanalysis;

import com.example.depanalysis.config.AnalysisConfig;
import com.example.depanalysis.api.AnalysisReport;
import com.example.depanalysis.api.DependencyAnalyzer;
import com.example.depanalysis.impl.DependencyAnalyzerImpl;
import com.example.depanalysis.util.JsonExporter;
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
        config.getSourceRoots().add(Paths.get("K:\\power-script\\core-extract\\output\\step2-set\\tender-api\\src\\main\\java"));
        config.getSourceRoots().add(Paths.get("K:\\power-script\\core-extract\\output\\step2-set\\tender-business\\src\\main\\java"));
        config.getSourceRoots().add(Paths.get("K:\\power-script\\core-extract\\output\\step2-set\\tender-contract\\src\\main\\java"));
        config.getSourceRoots().add(Paths.get("K:\\power-script\\core-extract\\output\\step2-set\\tender-job\\src\\main\\java"));
        config.setIgnoreUnresolved(true);
    }
    
    @Test
    public void testAnalyzeMethod() throws Exception {
        // 创建测试用的Java文件
        File testFile = new File("K:\\power-script\\core-extract\\output\\step2-set\\tender-api\\src\\main\\java\\com\\paut\\tender\\mgt\\api\\controller\\order\\PurchaserOrderSettlementController.java");
        
        // 分析指定方法
        AnalysisReport report = analyzer.analyze(testFile, "search", config);
        
        // 验证结果
        assertNotNull(report);
        assertNotNull(report.getClasses());
        
        // 使用默认配置导出JSON
        JsonExporter.exportToJson(report, testFile.getName(), "search");
        
        // 验证只包含指定源码根目录的包
        report.getClasses().keySet().forEach(className -> {
            assertFalse("不应包含JDK包", className.startsWith("java."));
            assertFalse("不应包含JDK包", className.startsWith("javax."));
        });
    }
    
    // @Test
    // public void testCustomOutputConfig() throws Exception {
    //     File testFile = new File("K:\\power-script\\core-extract\\output\\step2-set\\tender-api\\src\\main\\java\\com\\paut\\tender\\mgt\\api\\controller\\order\\PurchaserOrderSettlementController.java");
        
    //     AnalysisReport report = analyzer.analyze(testFile, "search", config);
        
    //     // 自定义输出配置
    //     OutputConfig outputConfig = new OutputConfig();
    //     outputConfig.setOutputDirectory("target/custom-reports");
    //     outputConfig.setFileNamePrefix("dependency");
        
    //     JsonExporter.exportToJson(report, testFile.getName(), "search", outputConfig);
        
    //     assertNotNull(report);
    // }
}