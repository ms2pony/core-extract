package com.example.javaparser.demo;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * JavaSymbolSolver功能演示
 */
@Slf4j
public class JavaSymbolSolverDemo {
    
    public static void main(String[] args) {
        JavaSymbolSolverDemo demo = new JavaSymbolSolverDemo();
        
        // Demo 1: 基本符号解析
        demo.basicSymbolResolutionDemo();
        
        // Demo 2: 方法解析
        demo.methodResolutionDemo();
    }
    
    /**
     * Demo 1: 基本符号解析
     */
    public void basicSymbolResolutionDemo() {
        log.info("=== Demo 1: 基本符号解析 ===");
        
        String javaCode = 
            "import java.util.List;\n" +
            "import java.util.ArrayList;\n" +
            "public class SymbolDemo {\n" +
            "    public void testMethod() {\n" +
            "        List<String> list = new ArrayList<>();\n" +
            "        list.add(\"test\");\n" +
            "        String result = list.get(0);\n" +
            "    }\n" +
            "}";
        
        try {
            // 设置类型解析器
            TypeSolver typeSolver = new CombinedTypeSolver(
                new ReflectionTypeSolver(),
                new JavaParserTypeSolver(new File("src"))
            );
            
            JavaParser javaParser = new JavaParser();
            javaParser.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver));
            
            CompilationUnit cu = javaParser.parse(javaCode).getResult().get();
            
            // 查找方法调用并解析
            cu.accept(new SymbolResolutionVisitor(), null);
            
        } catch (Exception e) {
            log.error("符号解析失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Demo 2: 方法解析
     */
    public void methodResolutionDemo() {
        log.info("=== Demo 2: 方法解析 ===");
        
        String javaCode = 
            "public class MethodDemo {\n" +
            "    private String name;\n" +
            "    public void setName(String name) {\n" +
            "        this.name = name;\n" +
            "    }\n" +
            "    public void processName() {\n" +
            "        setName(\"test\");\n" +
            "        String result = getName();\n" +
            "    }\n" +
            "    public String getName() {\n" +
            "        return name;\n" +
            "    }\n" +
            "}";
        
        try {
            TypeSolver typeSolver = new CombinedTypeSolver(
                new ReflectionTypeSolver()
            );
            
            JavaParser javaParser = new JavaParser();
            javaParser.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver));
            
            CompilationUnit cu = javaParser.parse(javaCode).getResult().get();
            
            cu.accept(new MethodResolutionVisitor(), null);
            
        } catch (Exception e) {
            log.error("方法解析失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 符号解析访问者
     */
    private static class SymbolResolutionVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(MethodCallExpr methodCall, Void arg) {
            super.visit(methodCall, arg);
            
            try {
                ResolvedMethodDeclaration resolved = methodCall.resolve();
                log.info("解析方法调用: {} -> {}", 
                    methodCall.toString(), 
                    resolved.getQualifiedSignature());
            } catch (Exception e) {
                log.warn("无法解析方法调用: {}", methodCall.toString());
            }
        }
    }
    
    /**
     * 方法解析访问者
     */
    private static class MethodResolutionVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(MethodCallExpr methodCall, Void arg) {
            super.visit(methodCall, arg);
            
            try {
                ResolvedMethodDeclaration resolved = methodCall.resolve();
                log.info("方法调用: {} 解析为: {}", 
                    methodCall.getNameAsString(), 
                    resolved.getQualifiedName());
                log.info("  声明类: {}", resolved.declaringType().getQualifiedName());
                log.info("  返回类型: {}", resolved.getReturnType().describe());
            } catch (Exception e) {
                log.warn("无法解析方法: {}", methodCall.getNameAsString());
            }
        }
    }
}