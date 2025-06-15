package com.example.javaparser.demo;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * JavaParser基础功能演示
 */
@Slf4j
public class JavaParserBasicDemo {
    
    public static void main(String[] args) {
        JavaParserBasicDemo demo = new JavaParserBasicDemo();
        
        // Demo 1: 解析Java文件并提取基本信息
        demo.parseJavaFileDemo();
        
        // Demo 2: 查找方法调用
        demo.findMethodCallsDemo();
        
        // Demo 3: 查找类和方法
        demo.findClassesAndMethodsDemo();
    }
    
    /**
     * Demo 1: 解析Java文件基本信息
     */
    public void parseJavaFileDemo() {
        log.info("=== Demo 1: 解析Java文件基本信息 ===");
        
        String javaCode = 
            "package com.example;\n" +
            "import java.util.List;\n" +
            "public class TestClass {\n" +
            "    private String name;\n" +
            "    public void setName(String name) {\n" +
            "        this.name = name;\n" +
            "    }\n" +
            "    public String getName() {\n" +
            "        return name;\n" +
            "    }\n" +
            "}";
        
        try {
            JavaParser javaParser = new JavaParser();
            CompilationUnit cu = javaParser.parse(javaCode).getResult().get();
            
            // 获取包名
            cu.getPackageDeclaration().ifPresent(pkg -> 
                log.info("包名: {}", pkg.getNameAsString()));
            
            // 获取导入
            cu.getImports().forEach(imp -> 
                log.info("导入: {}", imp.getNameAsString()));
            
            // 获取类名
            cu.getTypes().forEach(type -> 
                log.info("类名: {}", type.getNameAsString()));
            
        } catch (Exception e) {
            log.error("解析失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Demo 2: 查找方法调用
     */
    public void findMethodCallsDemo() {
        log.info("=== Demo 2: 查找方法调用 ===");
        
        String javaCode = 
            "public class ServiceClass {\n" +
            "    private UserService userService;\n" +
            "    public void processUser() {\n" +
            "        String name = userService.getUserName();\n" +
            "        System.out.println(name);\n" +
            "        userService.updateUser(name);\n" +
            "    }\n" +
            "}";
        
        try {
            JavaParser javaParser = new JavaParser();
            CompilationUnit cu = javaParser.parse(javaCode).getResult().get();
            
            // 使用访问者模式查找方法调用
            cu.accept(new MethodCallVisitor(), null);
            
        } catch (Exception e) {
            log.error("查找方法调用失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Demo 3: 查找类和方法
     */
    public void findClassesAndMethodsDemo() {
        log.info("=== Demo 3: 查找类和方法 ===");
        
        String javaCode = 
            "public class Controller {\n" +
            "    public String search() {\n" +
            "        return \"result\";\n" +
            "    }\n" +
            "    private void helper() {\n" +
            "        // helper method\n" +
            "    }\n" +
            "}";
        
        try {
            JavaParser javaParser = new JavaParser();
            CompilationUnit cu = javaParser.parse(javaCode).getResult().get();
            
            // 查找所有类
            List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
            for (ClassOrInterfaceDeclaration clazz : classes) {
                log.info("找到类: {}", clazz.getNameAsString());
                
                // 查找类中的所有方法
                List<MethodDeclaration> methods = clazz.findAll(MethodDeclaration.class);
                for (MethodDeclaration method : methods) {
                    log.info("  方法: {} (访问修饰符: {})", 
                        method.getNameAsString(), 
                        method.getAccessSpecifier());
                }
            }
            
        } catch (Exception e) {
            log.error("查找类和方法失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 方法调用访问者
     */
    private static class MethodCallVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(MethodCallExpr methodCall, Void arg) {
            super.visit(methodCall, arg);
            
            String methodName = methodCall.getNameAsString();
            String scope = methodCall.getScope().map(Object::toString).orElse("this");
            
            log.info("方法调用: {}.{}", scope, methodName);
        }
    }
}