package com.example.depanalysis.impl;

import com.example.depanalysis.api.AnalysisConfig;
import com.example.depanalysis.api.AnalysisReport;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.JavaParserFacade;
import lombok.extern.slf4j.Slf4j;

/**
 * 方法过滤访问器
 * 扫描MethodDeclaration，匹配方法名后执行依赖收集
 */
@Slf4j
public class MethodFilterVisitor extends VoidVisitorAdapter<Void> {
    
    private final String targetMethodName;
    private final JavaParserFacade facade;
    private final AnalysisReport report;
    private final AnalysisConfig config;
    
    public MethodFilterVisitor(String targetMethodName, JavaParserFacade facade, 
                              AnalysisReport report, AnalysisConfig config) {
        this.targetMethodName = targetMethodName;
        this.facade = facade;
        this.report = report;
        this.config = config;
    }
    
    @Override
    public void visit(MethodDeclaration md, Void arg) {
        if (targetMethodName.equals(md.getNameAsString())) {
            log.info("找到目标方法: {}", targetMethodName);
            
            // 创建依赖收集器
            MethodDependencyCollector collector = new MethodDependencyCollector(facade, report, config);
            
            // 访问方法体，收集依赖
            md.accept(collector, null);
            
            // 找到目标方法后不继续调用super.visit，避免重复处理
            return;
        }
        
        // 继续访问其他方法
        super.visit(md, arg);
    }
}