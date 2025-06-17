package com.paut.tender.mgt.inspect;


import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;

import java.io.File;
import java.io.FileNotFoundException;

public class CodeInspectApplication {
    public static void main(String[] args) throws FileNotFoundException {
        MethodCallAnalyzer analyzer = new MethodCallAnalyzer();

        String filePath = "k:/gitlab/tender-mgt/tender-api/src/main/java/com/paut/tender/mgt/api/controller/order/PurchaserOrderSettlementController.java";
        int startLine = 267;
        int endLine = 282;

        MethodCallAnalysisResult result = analyzer.analyzeMethodCalls(filePath, startLine, endLine);

        System.out.println("分析结果:");
        System.out.println("文件: " + result.getFilePath());
        System.out.println("行范围: " + result.getStartLine() + "-" + result.getEndLine());
        System.out.println("找到的方法调用:");

        for (MethodCallInfo callInfo : result.getMethodCalls()) {
            System.out.println("  方法: " + callInfo.getMethodName());
            System.out.println("  位置: 行 " + callInfo.getLine() + ", 列 " + callInfo.getColumn());
            System.out.println("  所属类: " + callInfo.getDeclaringClass());
            System.out.println("  完全限定名: " + callInfo.getQualifiedName());
            System.out.println("  完全限定类名: " + callInfo.getClassQualifiedName());
            System.out.println("  包名: " + callInfo.getPackageName());
            if (callInfo.getParameterTypes() != null) {
                System.out.println("  参数类型: " + callInfo.getParameterTypes());
            }
            if (callInfo.getErrorMessage() != null) {
                System.out.println("  错误信息: " + callInfo.getErrorMessage());
            }

            // 测试是否能从compilationUnitMap中找到对应的CompilationUnit
            String classQualifiedName = callInfo.getClassQualifiedName();
            if (classQualifiedName != null && !classQualifiedName.isEmpty() &&
                    !classQualifiedName.startsWith("未知") && !classQualifiedName.startsWith("scopeException")) {
                String classFilePath = analyzer.getClassFilePathMap().get(classQualifiedName);

                File file = new File(classFilePath);
                if (!file.exists()) {
                    throw new IllegalArgumentException("文件不存在: " + classFilePath);
                }

                ParseResult<CompilationUnit> parseResult = analyzer.getJavaParser().parse(file);

                if (!parseResult.isSuccessful()) {
                    throw new RuntimeException("解析文件失败: " + classFilePath);
                }

                CompilationUnit cu = parseResult.getResult().orElse(null);

                if (cu != null) {
                    System.out.println("  ✓ 成功从cuMap中找到CompilationUnit: " + classQualifiedName);
                    // 可以进一步验证CompilationUnit的内容
                    if (cu.getPackageDeclaration().isPresent()) {
                        System.out.println("    - 包声明: " + cu.getPackageDeclaration().get().getNameAsString());
                    }
                    System.out.println("    - 类数量: " + cu.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class).size());
                } else {
                    System.out.println("  ✗ 无法从cuMap中找到CompilationUnit: " + classQualifiedName);
                    // 输出所有可用的qualifiedName以便调试
                    System.out.println("    可用的qualifiedName列表:");
                    analyzer.getAllQualifiedNames().stream()
                            .filter(name -> name.contains(classQualifiedName.substring(classQualifiedName.lastIndexOf('.') + 1)))
                            .forEach(name -> System.out.println("      - " + name));
                }
            } else {
                System.out.println("  - 跳过测试（qualifiedName无效或为错误状态）: " + classQualifiedName);
            }

            System.out.println();
        }

        // 输出compilationUnitMap的统计信息
        System.out.println("=== CompilationUnitMap 统计信息 ===");
        System.out.println("总共加载的类数量: " + analyzer.getAllQualifiedNames().size());
        System.out.println("前10个已加载的类:");
        analyzer.getAllQualifiedNames().stream()
                .limit(10)
                .forEach(name -> System.out.println("  - " + name));
    }
}