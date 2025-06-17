package com.example.depanalysis.impl;

import com.example.depanalysis.config.AnalysisConfig;
import com.example.depanalysis.api.AnalysisReport;
import com.example.depanalysis.api.DependencyAnalyzer;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 依赖分析器实现类
 */
@Slf4j
public class DependencyAnalyzerImpl implements DependencyAnalyzer {
    
    @Override
    public AnalysisReport analyze(File javaFile, String methodName, AnalysisConfig cfg) throws IOException {
        log.info("开始分析文件: {}, 方法: {}", javaFile.getAbsolutePath(), methodName);
        
        // 创建TypeSolver
        TypeSolver typeSolver = createTypeSolver(cfg);
        JavaParserFacade facade = JavaParserFacade.get(typeSolver);
        
        // 解析Java文件
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(javaFile).getResult()
                .orElseThrow(() -> new IOException("无法解析Java文件: " + javaFile));
        
        // 创建分析报告
        AnalysisReport report = new AnalysisReport();
        
        // 创建方法过滤访问器
        MethodFilterVisitor methodFilter = new MethodFilterVisitor(methodName, facade, report, cfg);
        
        // 开始访问
        cu.accept(methodFilter, null);
        
        log.info("分析完成，找到 {} 个类的依赖", report.getClasses().size());
        return report;
    }
    
    /**
     * 创建TypeSolver
     */
    private TypeSolver createTypeSolver(AnalysisConfig cfg) {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        
        // 添加源码根目录
        for (Path root : cfg.getSourceRoots()) {
            typeSolver.add(new JavaParserTypeSolver(root.toFile()));
        }
        
        // 添加反射类型解析器（保持JDK类型可解析）
        typeSolver.add(new ReflectionTypeSolver());
        
        // 注意：不添加JarTypeSolver，jar包中的类将被忽略
        
        return typeSolver;
    }
}