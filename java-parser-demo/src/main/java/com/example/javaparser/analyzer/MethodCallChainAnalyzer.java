package com.example.javaparser.analyzer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 方法调用链分析器
 */
@Slf4j
public class MethodCallChainAnalyzer {

    private final JavaParser javaParser;
    private final Map<String, ClassCuFileInfo> classCuFileInfoMap = new HashMap<>();
    private final Map<String, MethodInfo> methodInfoMap = new HashMap<>();
    private final Set<String> analyzedMethods = new HashSet<>();

    // 1. 访问过的节点 HashSet，设计一个节点的唯一标识，节点一般为枚举、类、常量、等等
    private final Set<String> visitedNodes = new HashSet<>();

    // 2. 依赖的结构 这是输出，放进时会去重
    private final Set<String> dependencies = new LinkedHashSet<>();

    // 3. 待被dfs的节点 List，上一轮结构遍历完，从List取出一个节点，先看看访问过没有，没有访问过就去访问
    private final Queue<String> pendingNodes = new LinkedList<>();

    // 4. 当前cu所在的全路径包名，辅助访问过的节点
    private String currentPackageName;

    // 5. 放入存放CombinedTypeSolver的非jar包里的包(全路径包名)即项目模块的全路径包名，作用是过滤依赖的节点(非项目包名的过滤)
    private final Set<String> projectPackageNames = Arrays.stream(new String[] {
            "com.paut.tender.mgt.api",
            "com.paut.tender.mgt.business",
            "com.paut.tender.mgt.contract",
            "com.paut.tender.mgt.job"
    }).collect(Collectors.toSet());

    // 6. 分析调用者的类
    private JavaParserFacade facade;

    public MethodCallChainAnalyzer(String... sourcePaths) {
        // 设置类型解析器
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());

        // 添加源码路径
        for (String sourcePath : sourcePaths) {
            File sourceDir = new File(sourcePath);
            if (sourceDir.exists() && sourceDir.isDirectory()) {
                typeSolver.add(new JavaParserTypeSolver(sourceDir));
            }
        }

        this.javaParser = new JavaParser();
        this.javaParser.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver));
        facade = JavaParserFacade.get(typeSolver);

        // 解析所有Java文件
        for (String sourcePath : sourcePaths) {
            parseJavaFiles(sourcePath);
        }
    }

    /**
     * 分析指定方法的调用链
     */
    public void analyzeMethodCallChain(String classFilePath, String methodName) {
        log.info("=== 开始分析方法调用链: {}.{} ===", classFilePath, methodName);

        // String methodKey = className + "." + methodName;
        MethodInfo targetMethod = findMethod(classFilePath, methodName);

        // CompilationUnit cu = javaParser.parse(classFilePath).getResult().orElse(null);
        // if (cu == null) {
        //     log.warn("解析文件失败: {}", classFilePath);
        //     return;
        // }

        // if (targetMethod == null) {
        // log.warn("未找到目标方法: {}", methodKey);
        // return;
        // }

        // log.info("找到目标方法: {} (文件: {})", methodKey, targetMethod.getFilePath());

        targetMethod.getMethodDeclaration().accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodCallExpr callExpr, Void arg) {
                super.visit(callExpr, arg);
                try {
                    callExpr.getScope().ifPresent(scope->{
                        ResolvedType type = facade.getType(scope);
                        if(type.isReferenceType()){
                            // 修改这一行：使用正确的类型
                            ResolvedReferenceTypeDeclaration typeDeclaration = type.asReferenceType().getTypeDeclaration().get();
                            
                            // 如果你需要获取类名，可以这样做：
                            String className = typeDeclaration.getQualifiedName();
                            pendingNodes.add(className);
                            System.out.println("调用的类型: " + className + "是ReferenceType");

                            
                        }else{
                            System.out.println("MethodCallExpr:"+callExpr.getName().asString()+"不是ReferenceType");
                        }
                    });

                    // TypeSolver typeSolver = callExpr.getScope().get().calculateResolvedType();
                    // 通过 callExpr.getScope().get().calculateResolvedType() 判断当前 callExpr 的 scope 是否是类

                    // ResolvedMethodDeclaration resolved = callExpr.resolve();
                    // System.out.println("🔍 " + callExpr + " → " +
                    //         resolved.getQualifiedSignature() +
                    //         " （from " + resolved.declaringType().getQualifiedName() + ")");
                } catch (Exception e) {
                    System.out.println("❌ 无法解析: " + callExpr + "，原因: " + e.getMessage());
                }
            }
        },null);

        // 分析调用链
        // analyzeCallChain(targetMethod, 0);

        log.info("=== 方法调用链分析完成 ===");
    }

    /**
     * 递归分析调用链
     */
    // private void analyzeCallChain(MethodInfo methodInfo, int depth) {
    //     String indent = createIndent(depth);
    //     String methodKey = methodInfo.getClassName() + "." + methodInfo.getMethodName();

    //     if (analyzedMethods.contains(methodKey)) {
    //         log.info("{}📞 {} (已分析，避免循环)", indent, methodKey);
    //         return;
    //     }

    //     analyzedMethods.add(methodKey);
    //     log.info("{}📞 {} ({}:{})", indent, methodKey, methodInfo.getFilePath(), methodInfo.getLineNumber());

    //     // 查找该方法中的所有方法调用
    //     if (methodInfo.getMethodDeclaration() != null) {
    //         compilationUnits.get(methodInfo.getFilePath())
    //                 .findAll(ClassOrInterfaceDeclaration.class)
    //                 .forEach(clazz -> {
    //                     clazz.findAll(MethodDeclaration.class).stream()
    //                             .filter(m -> m.getSignature().equals(methodInfo.getMethodDeclaration().getSignature()))
    //                             .findFirst()
    //                             .ifPresent(m -> m.accept(new MethodCallAnalysisVisitor(depth + 1), null));
    //                 });
    //     }
    // }

    /**
     * 创建缩进字符串 (Java 8 兼容)
     */
    private String createIndent(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }

    /**
     * 查找指定的方法
     */
    private MethodInfo findMethod(String className, String methodName) {
        for (MethodInfo methodInfo : methodInfoMap.values()) {
            if (methodInfo.getFilePath().endsWith(className) &&
                    methodInfo.getMethodName().equals(methodName)) {
                return methodInfo;
            }
        }
        return null;
    }

    /**
     * 解析指定路径下的所有Java文件
     */
    private void parseJavaFiles(String sourcePath) {
        try {
            Path startPath = Paths.get(sourcePath);
            if (!Files.exists(startPath)) {
                log.warn("源码路径不存在: {}", sourcePath);
                return;
            }

            try (Stream<Path> paths = Files.walk(startPath)) {
                paths.filter(path -> path.toString().endsWith(".java"))
                        .forEach(this::parseJavaFile);
            }
        } catch (IOException e) {
            log.error("解析源码路径失败: {}", sourcePath, e);
        }
    }

    /**
     * 解析单个Java文件
     */
    private void parseJavaFile(Path javaFilePath) {
        try {
            CompilationUnit cu = javaParser.parse(javaFilePath).getResult().orElse(null);
            if (cu == null) {
                log.warn("解析文件失败: {}", javaFilePath);
                return;
            }

            ClassCuFileInfo classCuFileInfo = new ClassCuFileInfo();
            classCuFileInfo.setCu(cu);
            
            String filePath = javaFilePath.toString();

            classCuFileInfo.setFilePath(filePath);
            classCuFileInfoMap.put(cu.getPackageDeclaration().get().getNameAsString(), classCuFileInfo);
            // 提取方法信息
            cu.accept(new MethodInfoExtractor(filePath), null);

        } catch (Exception e) {
            log.error("解析Java文件失败: {}", javaFilePath, e);
        }
    }

    /**
     * 方法信息提取器
     */
    private class MethodInfoExtractor extends VoidVisitorAdapter<Void> {
        private final String filePath;
        private String currentClassName;

        public MethodInfoExtractor(String filePath) {
            this.filePath = filePath;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration clazz, Void arg) {
            currentClassName = clazz.getFullyQualifiedName().orElse(clazz.getNameAsString());
            super.visit(clazz, arg);
        }

        @Override
        public void visit(MethodDeclaration method, Void arg) {
            if (currentClassName != null) {
                MethodInfo methodInfo = new MethodInfo();
                methodInfo.setClassName(currentClassName);
                methodInfo.setMethodName(method.getNameAsString());
                methodInfo.setFilePath(filePath);
                methodInfo.setLineNumber(method.getBegin().map(pos -> pos.line).orElse(-1));
                methodInfo.setMethodDeclaration(method);

                String key = currentClassName + "." + method.getNameAsString();
                methodInfoMap.put(key, methodInfo);
            }
            super.visit(method, arg);
        }
    }

    /**
     * 方法调用分析访问者
     */
    // private class MethodCallAnalysisVisitor extends VoidVisitorAdapter<Void> {
    //     private final int depth;

    //     public MethodCallAnalysisVisitor(int depth) {
    //         this.depth = depth;
    //     }

    //     @Override
    //     public void visit(MethodCallExpr methodCall, Void arg) {
    //         super.visit(methodCall, arg);

    //         try {
    //             ResolvedMethodDeclaration resolved = methodCall.resolve();
    //             String declaringType = resolved.declaringType().getQualifiedName();
    //             String methodName = resolved.getName();

    //             // 查找对应的方法信息
    //             MethodInfo calledMethod = findMethod(declaringType, methodName);
    //             if (calledMethod != null) {
    //                 analyzeCallChain(calledMethod, depth);
    //             } else {
    //                 String indent = createIndent(depth);
    //                 log.info("{}📞 {}.{} (外部方法或无法解析)", indent, declaringType, methodName);
    //             }

    //         } catch (Exception e) {
    //             String indent = createIndent(depth);
    //             log.info("{}📞 {} (无法解析)", indent, methodCall.toString());
    //         }
    //     }
    // }

    /**
     * 方法信息数据类
     */
    @Data
    public static class MethodInfo {
        private String className;
        private String methodName;
        private String filePath;
        private int lineNumber;
        private MethodDeclaration methodDeclaration;
    }

    /**
     * 类文件cu信息数据类
     */
    @Data
    public static class ClassCuFileInfo {
        private String filePath;
        private CompilationUnit cu;
    }
}