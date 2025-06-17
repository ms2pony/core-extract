package com.paut.tender.mgt.inspect;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Data
public class MethodCallAnalyzer {

    public final JavaParser javaParser;
    private final List<String> projectModules;

    // 1.添加一个成员变量map，用于存储java类文件FilePath Map，ClassqualifiedName为map
    private Map<String, String> classFilePathMap;

    // 分析调用者的类
    private JavaParserFacade facade;

    public MethodCallAnalyzer() {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        
        // 添加项目模块的源码路径
        this.projectModules = Arrays.asList(
        "k:\\gitlab\\tender-mgt\\tender-api\\src\\main\\java",
            "k:\\gitlab\\tender-mgt\\tender-business\\src\\main\\java",
            "k:\\gitlab\\tender-mgt\\tender-contract\\src\\main\\java",
            "k:\\gitlab\\tender-mgt\\tender-job\\src\\main\\java"
        );
        
        for (String modulePath : projectModules) {
            File moduleDir = new File(modulePath);
            if (moduleDir.exists()) {
                combinedTypeSolver.add(new JavaParserTypeSolver(moduleDir));
            }
        }
        
        // 添加Maven依赖的jar包
        addMavenDependencies(combinedTypeSolver);
        
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        this.javaParser = new JavaParser();
        this.javaParser.getParserConfiguration().setSymbolResolver(symbolSolver);

        facade = JavaParserFacade.get(combinedTypeSolver);
        
        // 初始化compilationUnitMap
        this.classFilePathMap = new HashMap<>();
        initializeCompilationUnitMap();
    }

    /**
     * 初始化CompilationUnit映射，优先从缓存读取，否则扫描所有项目模块中的Java文件
     */
    private void initializeCompilationUnitMap() {
        String cacheFilePath = "K:\\gitlab\\tender-mgt\\code-inspect\\src\\cache\\classMapCache.json";
        File cacheFile = new File(cacheFilePath);
        
        // 检查缓存文件是否存在
        if (cacheFile.exists()) {
            log.info("发现缓存文件，从缓存加载CompilationUnit映射: {}", cacheFilePath);
            try {
                loadFromCache(cacheFilePath);
                log.info("从缓存加载CompilationUnit映射完成，共加载 {} 个类文件", classFilePathMap.size());
                return;
            } catch (Exception e) {
                log.warn("从缓存加载失败，将进行正常初始化: {}", e.getMessage());
                // 清空可能部分加载的数据
                classFilePathMap.clear();
            }
        }
        
        // 缓存不存在或加载失败，进行正常初始化
        log.info("开始遍历文件初始化CompilationUnit映射...");
        
        for (String modulePath : projectModules) {
            File moduleDir = new File(modulePath);
            if (moduleDir.exists()) {
                scanJavaFiles(moduleDir);
            } else {
                log.warn("模块路径不存在: {}", modulePath);
            }
        }
        
        log.info("CompilationUnit映射初始化完成，共加载 {} 个类文件", classFilePathMap.size());
        
        // 保存到缓存文件
        try {
            saveToCache(cacheFilePath);
            log.info("CompilationUnit映射已保存到缓存文件: {}", cacheFilePath);
        } catch (Exception e) {
            log.warn("保存缓存文件失败: {}", e.getMessage());
        }
    }
    
    /**
     * 递归扫描目录下的所有Java文件并解析为CompilationUnit
     */
    private void scanJavaFiles(File directory) {
        if (!directory.isDirectory()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                // 递归扫描子目录
                scanJavaFiles(file);
            } else if (file.getName().endsWith(".java")) {
                // 解析Java文件
                parseJavaFile(file);
            }
        }
    }
    
    /**
     * 解析单个Java文件并添加到映射中
     */
    private void parseJavaFile(File javaFile) {
        try {
            ParseResult<CompilationUnit> parseResult = javaParser.parse(javaFile);
            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();
                
                // 获取包名和类名来构建qualifiedName
                String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");
                
                // 查找所有类和接口声明
                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
                    String className = classDecl.getNameAsString();
                    String qualifiedName = packageName.isEmpty() ? className : packageName + "." + className;
                    
                    // 将qualifiedName作为key，CompilationUnit作为value存储
                    classFilePathMap.put(qualifiedName, javaFile.getAbsolutePath());
                    log.debug("已加载类: {} -> {}", qualifiedName, javaFile.getAbsolutePath());
                });
                
            } else {
                log.warn("解析Java文件失败: {}", javaFile.getAbsolutePath());
                if (parseResult.getProblems() != null) {
                    parseResult.getProblems().forEach(problem -> 
                        log.warn("解析问题: {}", problem.getMessage()));
                }
            }
        } catch (Exception e) {
            log.error("解析Java文件时发生错误: {}, 错误: {}", javaFile.getAbsolutePath(), e.getMessage());
        }
    }
    
    /**
     * 获取所有已加载的类的全限定名
     * @return 所有类的全限定名集合
     */
    public Set<String> getAllQualifiedNames() {
        return new HashSet<>(classFilePathMap.keySet());
    }

    private void addMavenDependencies(CombinedTypeSolver combinedTypeSolver) {
        try {
            // 方法1：从Maven本地仓库添加常用依赖
            String userHome = System.getProperty("user.home");
            String mavenRepo = userHome + "/.m2/repository";
            
            // 添加Spring相关jar包
//            addJarIfExists(combinedTypeSolver, mavenRepo + "/org/springframework/spring-context/5.3.21/spring-context-5.3.21.jar");
//            addJarIfExists(combinedTypeSolver, mavenRepo + "/org/springframework/spring-core/5.3.21/spring-core-5.3.21.jar");
//            paut\ifs\ifs-sdk-core\2.2.0-SNAPSHOT\ifs-sdk-core-2.2.0-SNAPSHOT.jar"
            // 添加项目特定的依赖（根据pom.xml中的依赖）
           addJarIfExists(combinedTypeSolver, mavenRepo + "/com/paut/ifs/ifs-sdk-core/2.2.0-SNAPSHOT/ifs-sdk-core-2.2.0-SNAPSHOT.jar");
            
            // 方法2：扫描整个Maven本地仓库（性能较差，但覆盖全面）
            // scanMavenRepository(combinedTypeSolver, mavenRepo);
            
        } catch (Exception e) {
            log.warn("添加Maven依赖时发生错误: {}", e.getMessage());
        }
    }

    private void addJarIfExists(CombinedTypeSolver combinedTypeSolver, String jarPath) {
        try {
            File jarFile = new File(jarPath);
            if (jarFile.exists()) {
                combinedTypeSolver.add(new JarTypeSolver(jarPath));
                log.info("已添加jar依赖: {}", jarPath);
            }
        } catch (Exception e) {
            log.warn("无法添加jar依赖 {}: {}", jarPath, e.getMessage());
        }
    }

    /**
     * 分析指定文件中指定行范围的方法调用
     * 
     * @param filePath 文件路径
     * @param startLine 开始行号
     * @param endLine 结束行号
     * @return 方法调用分析结果
     */
    public MethodCallAnalysisResult analyzeMethodCalls(String filePath, int startLine, int endLine) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new IllegalArgumentException("文件不存在: " + filePath);
            }

            ParseResult<CompilationUnit> parseResult = javaParser.parse(file);
            if (!parseResult.isSuccessful()) {
                throw new RuntimeException("解析文件失败: " + filePath);
            }

            CompilationUnit cu = parseResult.getResult().orElse(null);
            if (cu == null) {
                throw new RuntimeException("无法获取编译单元: " + filePath);
            }

            MethodCallAnalysisResult result = new MethodCallAnalysisResult();
            result.setFilePath(filePath);
            result.setStartLine(startLine);
            result.setEndLine(endLine);
            // 查找指定行范围内的方法调用
            cu.accept(new MethodCallVisitor(startLine, endLine, result), null);

            return result;

        } catch (Exception e) {
            log.error("分析方法调用时发生错误", e);
            throw new RuntimeException("分析方法调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 按需重新生成class文件
     * @throws IOException 
     */
    public void regenerateClass(CompilationUnit cu) throws IOException{
        ClassOrInterfaceDeclaration oldOrderCtr = cu.findAll(ClassOrInterfaceDeclaration.class).get(0);
        MethodDeclaration targetMethod =  oldOrderCtr.getMethods().stream().filter(m->m.getNameAsString().equals("search"))
                                    .findFirst()
                                    .orElseThrow(() -> new RuntimeException("找不到方法 search"));

        Set<String> allowedMethodSignatures = new HashSet<>(Arrays.asList(targetMethod.getSignature().asString()));

        ClassOrInterfaceDeclaration newOrderCtr = oldOrderCtr.clone();

        List<MethodDeclaration> methods = new ArrayList<>(newOrderCtr.getMethods());

        methods.removeIf(method ->
            !allowedMethodSignatures.contains(method.getSignature().asString())
        );
        newOrderCtr.getMembers().removeIf(b -> b instanceof MethodDeclaration);
        methods.forEach(newOrderCtr::addMember);
        CompilationUnit newCu = new CompilationUnit();
        newCu.setPackageDeclaration(cu.getPackageDeclaration().orElse(null));
        newCu.setImports(cu.getImports());
        newCu.addType(newOrderCtr);
        Files.write(Paths.get("K:\\gitlab\\tender-mgt\\code-inspect\\src\\output/newClass.java"), newCu.toString().getBytes());
    }

    /**
     * 方法调用访问器
     */
    private class MethodCallVisitor extends VoidVisitorAdapter<Void> {
        private final int startLine;
        private final int endLine;
        private final MethodCallAnalysisResult result;

        public MethodCallVisitor(int startLine, int endLine, MethodCallAnalysisResult result) {
            this.startLine = startLine;
            this.endLine = endLine;
            this.result = result;
        }

        @Override
        public void visit(MethodCallExpr methodCall, Void arg) {
            super.visit(methodCall, arg);

            // 检查方法调用是否在指定行范围内
            if (methodCall.getBegin().isPresent()) {
                int line = methodCall.getBegin().get().line;
                if (line >= startLine && line <= endLine) {
                    MethodCallInfo callInfo = new MethodCallInfo();
                    List<String> parameterTypes = new ArrayList<>();

                    for (Expression param : methodCall.getArguments()) {
                        try {
                            // 显式声明类型
//                            ResolvedValueDeclaration resolvedDecl = facade.solve(param).getCorrespondingDeclaration();
                            Optional<String> declaringClass = Optional.empty();

                            if(param instanceof FieldAccessExpr){
                                FieldAccessExpr fieldParam = param.asFieldAccessExpr();
                                ScopeInfo scopeInfo = extractScopeInfo(fieldParam);
                                declaringClass = Optional.of(scopeInfo.getClassQualifiedName());
                            }

//                            if (resolvedDecl instanceof ResolvedFieldDeclaration) {
//                                ResolvedFieldDeclaration fieldDecl = resolvedDecl.asField();
//                                declaringClass = Optional.of(fieldDecl.declaringType().getQualifiedName());
//                                System.out.println("参数: " + param + " → 字段所在类: " + declaringClass.get());
//                            } else if (resolvedDecl instanceof ResolvedEnumConstantDeclaration) {
//                                ResolvedEnumConstantDeclaration enumDecl = resolvedDecl.asEnumConstant();
//                                declaringClass = Optional.of(enumDecl.getType().asReferenceType().getQualifiedName());
//                                System.out.println("参数: " + param + " → 枚举常量所在类: " + declaringClass.get());
//                            } else if (resolvedDecl instanceof ResolvedParameterDeclaration) {
//                                System.out.println("参数: " + param + " → 来自方法参数(这个方法调用所在的方法)");
//                            } else if (resolvedDecl instanceof ResolvedValueDeclaration) {
//                                System.out.println("参数: " + param + " → 来自局部变量");
//                            } else {
//                                System.out.println("参数: " + param + " → 无法获取 declaringType，类型: " + resolvedDecl.getClass().getSimpleName());
//                            }

//                            // 现在可以使用 declaringClass.isPresent() 来判断是否为空
//                            if (declaringClass.isPresent()) {
//                                // 执行有 declaringClass 的逻辑
//                                String className = declaringClass.get();
//                                // 在这里添加你的业务逻辑
//                            } else {
//                                // 执行没有 declaringClass 的逻辑
//                            }
                            declaringClass.ifPresent(className->{
                                parameterTypes.add(className);
                            });

                        } catch (Exception e) {
                            System.out.println("参数: " + param + " → 无法解析，原因: " + e.getMessage());
                        }
                    }

                    callInfo.setParameterTypes(parameterTypes);

                    try {
                        // 解析方法调用
                        ResolvedMethodDeclaration resolved = methodCall.resolve();

                        callInfo.setMethodName(methodCall.getNameAsString());
                        callInfo.setLine(line);
                        callInfo.setColumn(methodCall.getBegin().get().column);
                        callInfo.setDeclaringClass(resolved.getClassName());
                        callInfo.setQualifiedName(resolved.getQualifiedName());
                        // 获取完全限定类名：包名 + 类名
                        String classQualifiedName = resolved.getPackageName().isEmpty() ? 
                            resolved.getClassName() : 
                            resolved.getPackageName() + "." + resolved.getClassName();
                        callInfo.setClassQualifiedName(classQualifiedName);
                        callInfo.setPackageName(resolved.getPackageName());
                        
                        // 获取参数类型
//                        List<String> parameterTypes = new ArrayList<>();
//                        for (int i = 0; i < resolved.getNumberOfParams(); i++) {
//                            parameterTypes.add(resolved.getParam(i).getType().describe());
//                        }
//                        callInfo.setParameterTypes(parameterTypes);
                        
                        result.getMethodCalls().add(callInfo);
                        
                        log.info("找到方法调用: {} 在类 {} 中，行号: {}", 
                                methodCall.getNameAsString(), resolved.getClassName(), line);
                                
                    } catch (UnsolvedSymbolException e) {
                        // 专门处理符号无法解析的情况
                        log.warn("无法解析方法调用符号: {} 在行 {}, 可能缺少依赖jar包", 
                                methodCall.getNameAsString(), line);

                        callInfo.setMethodName(methodCall.getNameAsString());
                        callInfo.setLine(line);
                        callInfo.setColumn(methodCall.getBegin().get().column);
                        callInfo.setDeclaringClass("未解析-可能缺少依赖");
                        callInfo.setResolved(false);
//                        callInfo.setErrorMessage("UnsolvedSymbol: " + e.getMessage());
                        
                        // 尝试通过getScope获取调用方法所在的类信息
                        if (methodCall.getScope().isPresent()){
                            ScopeInfo scopeInfo = extractScopeInfo(methodCall.getScope().get());
                            String errorMsg = scopeInfo.getErrorMessage();
                            errorMsg = "UnsolvedSymbol: " + e.getMessage() + errorMsg;
                            callInfo.setErrorMessage(errorMsg);

                            if(scopeInfo.getDeclaringClass() !=null) callInfo.setDeclaringClass(scopeInfo.getDeclaringClass());
                            if(scopeInfo.getClassQualifiedName() !=null) callInfo.setClassQualifiedName(scopeInfo.getClassQualifiedName());
                            if(scopeInfo.getQualifiedName() !=null) callInfo.setQualifiedName(scopeInfo.getQualifiedName());
                        }

                        result.getMethodCalls().add(callInfo);
                    } catch (Exception e) {
                        // 处理其他类型的异常
                        log.warn("解析方法调用时发生其他错误: {} 在行 {}, 错误: {}", 
                                methodCall.getNameAsString(), line, e.getMessage());
                        
                        // 即使无法完全解析，也记录基本信息

                        // 通过Scope中获取classQualifiedName
                        methodCall.getScope().ifPresent(scope->{
                            String info = scope.calculateResolvedType().asReferenceType().getQualifiedName();
                            callInfo.setQualifiedName(info);
                            callInfo.setClassQualifiedName(info);
                        });
                        callInfo.setMethodName(methodCall.getNameAsString());
                        callInfo.setLine(line);
                        callInfo.setColumn(methodCall.getBegin().get().column);
                        callInfo.setDeclaringClass("未知");
                        callInfo.setErrorMessage("Exception："+e.getMessage());
                        // 该情况下只能通过scope获取类信息，这种方式无法获取package
                        // callInfo.setPackageName();
                        
                        result.getMethodCalls().add(callInfo);
                    }
                }
            }
        }

        public ScopeInfo extractScopeInfo(Expression scope){
            ScopeInfo scopeInfo = new ScopeInfo();
            try {
                // 尝试获取scope信息
                String scopeDesc = "未知";
                try {
                    // 尝试解析scope的类型 - 使用显式类型声明替代var
                    ResolvedType scopeType = scope.calculateResolvedType();
                    // 获取scopeType的qualifiedName而不是describe()
                    if (scopeType.isReferenceType()) {
                        scopeDesc = scopeType.asReferenceType().getQualifiedName();
                    } else {
                        scopeDesc = scopeType.describe();
                    }
                    log.info("通过scope获取到调用类信息: {}", scopeDesc);
                    scopeInfo.setQualifiedName(scopeDesc);
                    scopeInfo.setClassQualifiedName(scopeDesc);  // 新增：设置类的完全限定名
                } catch (Exception scopeException) {
                    // 如果scope也无法解析，尝试获取scope的字符串表示
                    scopeDesc = scope.toString();
                    // callInfo.setQualifiedName("scopeException"+scopeInfo);
                    log.info("scope无法完全解析，获取字符串表示: {}", scopeDesc);
                }

                // 将scope信息添加到错误消息中
                scopeInfo.setErrorMessage("Scope: " + scopeDesc);
                // 尝试从scope信息中提取可能的类名
                if (!scopeDesc.equals("未知") && !scopeDesc.isEmpty()) {
                    scopeInfo.setDeclaringClass("推测类型: " + scopeDesc);
                }
                return scopeInfo;
            } catch (Exception scopeAnalysisException) {
                log.warn("分析scope时发生错误: {}", scopeAnalysisException.getMessage());
                scopeInfo.setErrorMessage("ScopeAnalysisError: " + scopeAnalysisException.getMessage());
                return scopeInfo;
            }
        }
    }

    /**
     * 将classFilePathMap保存到缓存文件
     * @param cacheFilePath 缓存文件路径
     * @throws Exception 保存失败时抛出异常
     */
    private void saveToCache(String cacheFilePath) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 确保输出目录存在
        File cacheFile = new File(cacheFilePath);
        File parentDir = cacheFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // 将Map转换为JSON并写入文件
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(cacheFile, classFilePathMap);
    }

    /**
     * 从缓存文件加载classFilePath数据
     * @param cacheFilePath 缓存文件路径
     * @throws Exception 加载失败时抛出异常
     */
    private void loadFromCache(String cacheFilePath) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 读取JSON文件内容
        String jsonContent = new String(Files.readAllBytes(Paths.get(cacheFilePath)), "UTF-8");
        
        // 将JSON转换为Map<String, String>
        TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};
        Map<String, String> loadedMap = objectMapper.readValue(jsonContent, typeRef);
        
        // 赋值给compilationUnitMap
        classFilePathMap.putAll(loadedMap);
    }

    /**
     * 清除缓存文件并重新初始化
     */
    public void clearCacheAndReinitialize() {
        String cacheFilePath = "K:\\gitlab\\tender-mgt\\code-inspect\\src\\output\\classMapCache.json";
        File cacheFile = new File(cacheFilePath);
        
        if (cacheFile.exists()) {
            cacheFile.delete();
            log.info("缓存文件已删除: {}", cacheFilePath);
        }
        
        classFilePathMap.clear();
        initializeCompilationUnitMap();
    }
}
