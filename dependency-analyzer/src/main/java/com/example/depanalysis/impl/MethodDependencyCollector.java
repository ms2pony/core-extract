package com.example.depanalysis.impl;

import com.example.depanalysis.config.AnalysisConfig;
import com.example.depanalysis.api.AnalysisReport;
import com.example.depanalysis.model.ClassSymbols;
import com.example.depanalysis.util.SymbolHelper;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * 方法依赖收集器
 * 收集方法中引用的各种符号
 */
@Slf4j
public class MethodDependencyCollector extends VoidVisitorAdapter<Void> {
    
    private static final List<String> EXCLUDED_PREFIXES = 
            Arrays.asList("java.", "javax.", "jdk.", "sun.", "org.w3c.", "org.xml.");
    
    private final JavaParserFacade facade;
    private final AnalysisReport report;
    private final AnalysisConfig config;
    
    public MethodDependencyCollector(JavaParserFacade facade, AnalysisReport report, AnalysisConfig config) {
        this.facade = facade;
        this.report = report;
        this.config = config;
    }
    
//    @Override
//    public void visit(AnnotationExpr n, Void arg) {
//        try {
//            // 处理注解类型
//            String qualifiedName = SymbolHelper.resolveAnnotationType(n, facade);
//            if (qualifiedName != null) {
//                store(qualifiedName, symbols -> symbols.getAnnotations().add(n.getNameAsString()));
//            }
//        } catch (Exception e) {
//            handleResolutionError("注解", n.toString(), e);
//        }
//        super.visit(n, arg);
//    }
    
    @Override
    public void visit(ObjectCreationExpr n, Void arg) {
        try {
            // 处理对象创建表达式 new X()
            String qualifiedName = SymbolHelper.resolveObjectCreationType(n, facade);
            if (qualifiedName != null) {
                store(qualifiedName, symbols -> symbols.setReferencedAsType(true));
            }
        } catch (Exception e) {
            handleResolutionError("对象创建", n.toString(), e);
        }
        super.visit(n, arg);
    }
    
    @Override
    public void visit(ClassExpr n, Void arg) {
        try {
            // 处理类表达式 X.class
            String qualifiedName = SymbolHelper.resolveClassExpr(n, facade);
            if (qualifiedName != null) {
                store(qualifiedName, symbols -> symbols.setReferencedAsType(true));
            }
        } catch (Exception e) {
            handleResolutionError("类表达式", n.toString(), e);
        }
        super.visit(n, arg);
    }
    
    @Override
    public void visit(ClassOrInterfaceType n, Void arg) {
        try {
            // 处理类或接口类型
            String qualifiedName = SymbolHelper.resolveClassOrInterfaceType(n, facade);
            if (qualifiedName != null) {
                store(qualifiedName, symbols -> symbols.setReferencedAsType(true));
            }
        } catch (Exception e) {
            handleResolutionError("类型", n.toString(), e);
        }
        super.visit(n, arg);
    }
    
    @Override
    public void visit(FieldAccessExpr n, Void arg) {
        try {
            // 处理字段访问表达式
            String[] result = SymbolHelper.resolveFieldAccess(n, facade);
            if (result != null && result.length == 2) {
                String ownerQualifiedName = result[0];
                String fieldName = result[1];
                store(ownerQualifiedName, symbols -> symbols.getFields().add(fieldName));
            }
        } catch (Exception e) {
            handleResolutionError("字段访问", n.toString(), e);
        }
        super.visit(n, arg);
    }
    
    @Override
    public void visit(NameExpr n, Void arg) {
        try {
            // 处理名称表达式
            String[] result = SymbolHelper.resolveNameExpr(n, facade);
            if (result != null && result.length == 3) {
                String ownerQualifiedName = result[0];
                String symbolName = result[1];
                String symbolType = result[2]; // "field", "enum", "type"
                
                store(ownerQualifiedName, symbols -> {
                    switch (symbolType) {
                        case "field":
                            symbols.getFields().add(symbolName);
                            break;
                        case "enum":
                            symbols.getEnumConstants().add(symbolName);
                            break;
                        case "type":
                            symbols.setReferencedAsType(true);
                            break;
                    }
                });
            }
        } catch (Exception e) {
            handleResolutionError("名称表达式", n.toString(), e);
        }
        super.visit(n, arg);
    }
    
    /**
     * 存储符号信息
     */
    private void store(String ownerQualifiedName, Consumer<ClassSymbols> mutator) {
        if (!isTracked(ownerQualifiedName)) {
            return;
        }
        
        mutator.accept(report.getClasses()
                .computeIfAbsent(ownerQualifiedName, ClassSymbols::new));
    }
    
    /**
     * 判断是否需要跟踪该类
     */
    private boolean isTracked(String qualifiedName) {
        return EXCLUDED_PREFIXES.stream().noneMatch(qualifiedName::startsWith);
    }
    
    /**
     * 处理解析错误
     */
    private void handleResolutionError(String symbolType, String expression, Exception e) {
        if (config.isIgnoreUnresolved()) {
            log.debug("忽略无法解析的{}: {}", symbolType, expression);
        } else {
            log.error("无法解析{}: {}", symbolType, expression, e);
            throw new RuntimeException("符号解析失败: " + expression, e);
        }
    }
}