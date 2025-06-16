## javaparser和javaparser-symbol-solver-core相关知识

### 相关代码

1. CombinedTypeSolver
```java
CombinedTypeSolver typeSolver = new CombinedTypeSolver();
// 1. 当前项目源码目录
typeSolver.add(new JavaParserTypeSolver(new File("src/main/java")));
// 2. 依赖 Jar 包（例如 Spring、你自己的库等）
typeSolver.add(new JarTypeSolver(new File("path/to/dependency.jar")));
// 3. JDK 类型（必要）
typeSolver.add(new ReflectionTypeSolver());
```

2. 创建JavaSymbolSolver

```java
JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
```

3. 创建一个cu
```java
// 读取并解析 Controller
CompilationUnit cu = StaticJavaParser.parse(new File(
        "k:/power-script/core-extract/.../PurchaserOrderSettlementController.java"));
```
### cu CompilationUnit 常用方法
常见方法
| 结构名                      | 方法（或属性）                        | 示例                       | 说明                 |
| ------------------------ | ------------------------------ | ------------------------ | ------------------ |
| **包声明**                  | `cu.getPackageDeclaration()`   | `package com.example;`   | Java 文件的 `package` |
| **导入列表**                 | `cu.getImports()`              | `import java.util.List;` | 所有 `import` 语句     |
| **类/接口/枚举声明**            | `cu.getTypes()`                | `public class Hello {}`  | 文件中的所有顶级类型声明       |
| **注释**                   | `cu.getAllContainedComments()` | `// 注释`、`/** doc */`     | 包括类上、方法上、字段上的所有注释  |
| **注释 + 包 + import + 类型** | `cu.toString()`                | 整个 Java 文件               | 用于重新生成代码           |


#### cu.findAll 和 findFirst
参数都为 MethodDeclaration.class 之类，
```java
MethodDeclaration search = cu.findFirst(MethodDeclaration.class,
        m -> m.getNameAsString().equals("search")).get();
```
#### resolve 和 scope
```java
// ① 解析“调用者” dataPermissionService 的类型
ResolvedType scopeType = call.getScope().get().calculateResolvedType();
// -> com.paut.tender.mgt.business.service.DataPermissionService

// ② 解析被调用的方法
ResolvedMethodDeclaration resolved = call.resolve();
// -> 返回 buildOrganizationPermissionSql(String, String...) 的 Method 描述
System.out.println(resolved.getQualifiedSignature());
```

#### cu.getTypes()可能返回什么
它返回的是 List<TypeDeclaration<?>>，可能是：

ClassOrInterfaceDeclaration → 类/接口

EnumDeclaration → 枚举

AnnotationDeclaration → 注解类型

### ClassOrInterfaceDeclaration 组成 - 常用

| 方法                       | 类型                               | 示例             |
| ------------------------ | -------------------------------- | -------------- |
| `.getName()`             | `SimpleName`                     | `OrderService` |
| `.getExtendedTypes()`    | `NodeList<ClassOrInterfaceType>` | `BaseService`  |
| `.getImplementedTypes()` | `NodeList<ClassOrInterfaceType>` | `A`, `B`       |
| `.getMethods()`          | `List<MethodDeclaration>`        | 所有方法           |
| `.getFields()`           | `List<FieldDeclaration>`         | 所有字段           |

### MethodDeclaration 组成 - 常用
| 方法                  | 类型                    | 说明            |
| ------------------- | --------------------- | ------------- |
| `.getName()`        | `SimpleName`          | 方法名 `run`     |
| `.getParameters()`  | `List<Parameter>`     | 参数列表          |
| `.getType()`        | `Type`                | 返回类型          |
| `.getBody()`        | `Optional<BlockStmt>` | 方法体语句块        |
| `.getAnnotations()` | 注解列表                  | `@Override` 等 |


### cu 结构图
CompilationUnit
├── PackageDeclaration
├── ImportDeclaration (多个)
└── TypeDeclaration（类/接口/枚举/注解） ← ClassOrInterfaceDeclaration
    ├── FieldDeclaration
    ├── MethodDeclaration
    │   └── BlockStmt（语句块）
    │       ├── ExpressionStmt（表达式语句）
    │       │   └── MethodCallExpr / AssignExpr / ...
    └── ConstructorDeclaration（构造器）

### accept & visit 机制详解（访问者模式）

JavaParser 遍历 AST 是基于 访问者模式，通过 accept(visitor, arg) 自动深度遍历语法树
示例：打印所有方法调用
```java
cu.accept(new VoidVisitorAdapter<Void>() {
    @Override
    public void visit(MethodCallExpr call, Void arg) {
        System.out.println("方法调用: " + call);
        super.visit(call, arg); // 🔁 继续递归
    }
}, null);

```

| 方法                             | 含义                 |
| ------------------------------ | ------------------ |
| `.accept(visitor, arg)`        | 从当前节点开始，递归遍历整个语法树  |
| `VoidVisitorAdapter<T>`        | 常用适配器类，只处理你关心的节点类型 |
| `.visit(MethodDeclaration, T)` | 只在遇到方法定义时执行        |

visit有如下这么多重载方法
```java
visit(ClassOrInterfaceDeclaration clazz, T arg)
visit(MethodDeclaration method, T arg)
visit(FieldDeclaration field, T arg)
visit(MethodCallExpr call, T arg)

```

#### super.visit(...)

super.visit(...) 的意思是：调用父类的 visit() 方法，以继续递归遍历 AST 子节点。

它是“访问者模式”中一个经典用法，不是访问 Java 语言中的 super 成员或父类逻辑，而是：

在自定义的 Visitor 中，调用默认实现，继续向下递归。

#### 进一步解释super.visit(...) -- 背景：访问者模式结构

JavaParser 使用的访问者模式是基于这个类层次的：

```java
class VoidVisitorAdapter<T> implements VoidVisitor<T> {
    public void visit(ClassOrInterfaceDeclaration n, T arg) {
        // 默认实现：递归访问子节点
        visitChildren(n, arg);
    }

    public void visit(MethodDeclaration n, T arg) {
        visitChildren(n, arg);
    }

    ...
}

```

### ClassOrInterfaceDeclaration 的子结构一览表 - 详细

| 内容    | JavaParser 节点                                      | 说明                                                     |
| ----- | -------------------------------------------------- | ------------------------------------------------------ |
| 类名    | `.getName()`                                       | `SimpleName`，类名 `MyClass`                              |
| 泛型声明  | `.getTypeParameters()`                             | `List<TypeParameter>`，如 `<T>`                          |
| 继承的类  | `.getExtendedTypes()`                              | `NodeList<ClassOrInterfaceType>`，如 `extends BaseClass` |
| 实现的接口 | `.getImplementedTypes()`                           | `NodeList<ClassOrInterfaceType>`，如 `implements A, B`   |
| 修饰符   | `.getModifiers()`                                  | `public`, `abstract`, `final`                          |
| 字段    | `.getFields()`                                     | `List<FieldDeclaration>`，类中的变量                         |
| 方法    | `.getMethods()`                                    | `List<MethodDeclaration>`，类中所有方法（不含构造器）                |
| 构造器   | `.getConstructors()`                               | `List<ConstructorDeclaration>`                         |
| 内部类   | `.getMembers()` 中筛选出 `ClassOrInterfaceDeclaration` | 嵌套类、接口                                                 |
| 注解    | `.getAnnotations()`                                | 类上的注解（如 `@Entity`）                                     |
| 所有成员  | `.getMembers()`                                    | `List<BodyDeclaration<?>>`，包括字段、方法、构造器、嵌套类等所有成员        |

ClassOrInterfaceDeclaration
├── name: MyClass
├── typeParameters: <T>
├── extendedTypes: BaseClass
├── implementedTypes: A, B
├── annotations: @Override ...
├── modifiers: public
└── members:
    ├── FieldDeclaration (field)
    ├── FieldDeclaration (list)
    ├── ConstructorDeclaration (MyClass())
    ├── MethodDeclaration (foo)
    ├── MethodDeclaration (toString)
    └── ClassOrInterfaceDeclaration (Inner class)

#### 你可以如何使用这些结构？

1. 遍历所有字段
```java
clazz.getFields().forEach(field -> {
    field.getVariables().forEach(var -> {
        System.out.println("字段名: " + var.getName() + " 类型: " + var.getType());
    });
});

```

2. 遍历所有方法

```java
clazz.getMethods().forEach(method -> {
    System.out.println("方法名: " + method.getName() + " 返回类型: " + method.getType());
});

```

3. 查找内部类

```java
clazz.getMembers().stream()
    .filter(m -> m instanceof ClassOrInterfaceDeclaration)
    .map(m -> (ClassOrInterfaceDeclaration) m)
    .forEach(inner -> System.out.println("内部类: " + inner.getName()));
```

### 哪些类带有accpet方法

| 类名                                   | 表示结构          | 常用方法                    |
| ------------------------------------ | ------------- | ----------------------- |
| `CompilationUnit`                    | Java 文件       | `.accept(visitor, arg)` |
| `ClassOrInterfaceDeclaration`        | 类/接口声明        | `.accept(...)`          |
| `MethodDeclaration`                  | 方法定义          | `.accept(...)`          |
| `ConstructorDeclaration`             | 构造器           | `.accept(...)`          |
| `FieldDeclaration`                   | 字段声明          | `.accept(...)`          |
| `VariableDeclarator`                 | 字段或局部变量       | `.accept(...)`          |
| `AnnotationExpr`                     | 注解            | `.accept(...)`          |
| `MethodCallExpr`                     | 方法调用表达式       | `.accept(...)`          |
| `AssignExpr`                         | 赋值表达式         | `.accept(...)`          |
| `IfStmt`                             | if 语句         | `.accept(...)`          |
| `ForStmt`, `WhileStmt`, `SwitchStmt` | 各种控制语句        | `.accept(...)`          |
| `ReturnStmt`                         | return 语句     | `.accept(...)`          |
| `BlockStmt`                          | 代码块 `{ ... }` | `.accept(...)`          |
| `Parameter`                          | 方法参数          | `.accept(...)`          |
👉 几乎你能想到的 Java 结构，都有 accept(...)

### MethodDeclaration 结构 - 详细
| 内容      | 方法                                                 | 类型                     | 示例                   |
| ------- | -------------------------------------------------- | ---------------------- | -------------------- |
| 方法名     | `getName()`                                        | `SimpleName`           | `run`                |
| 返回类型    | `getType()`                                        | `Type`                 | `String`、`void`      |
| 参数列表    | `getParameters()`                                  | `List<Parameter>`      | `String arg`         |
| 方法体     | `getBody()`                                        | `Optional<BlockStmt>`  | `{ ... }`            |
| 注解列表    | `getAnnotations()`                                 | `List<AnnotationExpr>` | `@Override`          |
| 修饰符     | `getModifiers()`                                   | `EnumSet<Modifier>`    | `public static`      |
| 抛出异常    | `getThrownExceptions()`                            | `List<ReferenceType>`  | `throws IOException` |
| 泛型参数    | `getTypeParameters()`                              | `List<TypeParameter>`  | `<T>`                |
| 是否为抽象方法 | `isAbstract()`                                     | `boolean`              | `true` or `false`    |
| 是否为静态方法 | `isStatic()`                                       | `boolean`              | `true` or `false`    |
| 所在类节点   | `.findAncestor(ClassOrInterfaceDeclaration.class)` | 可选                     | 获取所属类                |
| Javadoc | `getJavadocComment()` / `getJavadoc()`             | 注释                     | `/** xxx */`         |
| 方法签名    | `getSignature()`                                   | `MethodSignature`      | `(String,int)` 等     |
| 方法名字符串  | `getNameAsString()`                                | `String`               | `"run"`              |

#### 举例说明：结构展示

```java
public class Example {
    @Deprecated
    public <T> List<T> getList(String name, int size) throws IOException {
        return new ArrayList<>();
    }
}

```
它的 MethodDeclaration 对象结构为：
getNameAsString() → "getList"

getType() → List<T>

getParameters() → 2 个参数：String name, int size

getTypeParameters() → 1 个泛型：T

getAnnotations() → @Deprecated

getThrownExceptions() → IOException

getBody() → 有返回语句

isStatic() → false

isPublic() → true

## MethodCallExpr不在MethodDeclaration结构中-为什么？

### MethodCallExpr在哪里？

MethodCallExpr（方法调用）不属于 MethodDeclaration 的“直接成员”，它存在于方法体内部，是 BlockStmt（语句块）里的一种表达式语句。

结构层级

MethodDeclaration
└── BlockStmt（方法体）
    ├── ExpressionStmt（语句）
    │   └── MethodCallExpr（表达式）

### MethodCallExpr 是什么？

MethodCallExpr 表示 一次方法调用表达式，如 foo(), user.getName(), Math.max(a, b)。

它是 JavaParser 中的表达式类之一，继承自 Expression，用于表示调用行为


MethodCallExpr 主要有resolve() 和 getScope() 方法

### getScope() 的类型可能有哪些？

| `getScope()` 返回的结构（实际类型） | 表示的调用者                                            |
| ------------------------ | ------------------------------------------------- |
| `NameExpr`               | 普通变量：`user.getName()` 中的 `user`                   |
| `FieldAccessExpr`        | 静态类：`System.out.println()` 中的 `System.out`        |
| `ThisExpr`               | `this.xxx()` 调用                                   |
| `SuperExpr`              | `super.foo()`                                     |
| `MethodCallExpr`         | 链式调用中间层：`getService().getName()` 的 `getService()` |

### resolve 方法

#### 泛型方法调用的处理

```java
MethodCallExpr expr = StaticJavaParser.parseExpression("Collections.<String>emptyList()");
expr.getTypeArguments().ifPresent(typeArgs -> {
    typeArgs.forEach(type -> System.out.println("泛型类型参数: " + type));
});

```
MethodCallExpr 其可以使用getArguments()和getTypeArguments()，分别是 获取实参表达式列表 和 获取泛型参数

## JavaParser 中的表达式分类 - MethodCallExpr只是其中一个

| Java 代码          | JavaParser 表达式类型               | 示例类名   |
| ---------------- | ------------------------------ | ------ |
| `foo()`          | `MethodCallExpr`               | 方法调用   |
| `new A()`        | `ObjectCreationExpr`           | 构造器调用  |
| `"abc"`          | `StringLiteralExpr`            | 字符串    |
| `1 + 2`          | `BinaryExpr`                   | 二元操作   |
| `a = b`          | `AssignExpr`                   | 赋值     |
| `() -> {}`       | `LambdaExpr`                   | Lambda |
| `this`           | `ThisExpr`                     | 当前对象引用 |
| `super.foo()`    | `MethodCallExpr` + `SuperExpr` | 父类方法调用 |
| `Optional.of(1)` | `MethodCallExpr` + `NameExpr`  | 方法链    |

## 一个方法中"所有引用"

| 类型         | 示例                                   | JavaParser 节点                 |
| ---------- | ------------------------------------ | ----------------------------- |
| 方法调用       | `user.getName()`                     | `MethodCallExpr`              |
| 构造器调用      | `new ArrayList<>()`                  | `ObjectCreationExpr`          |
| 字段访问       | `user.name` / `this.name`            | `FieldAccessExpr`, `NameExpr` |
| 静态访问       | `Math.max(...)` / `Constants.VALUE`  | `FieldAccessExpr`             |
| 方法参数使用     | `arg.length()`                       | `NameExpr`（变量引用）              |
| 成员变量引用     | `this.redisUtil.xxx()`               | `NameExpr`（需上下文）              |
| 类名引用       | `List<String>` / `Map<K,V>`          | `ClassOrInterfaceType`        |
| 注解类引用      | `@Autowired`, `@Service`             | `AnnotationExpr` → `Name`     |
| Lambda 中引用 | `list.forEach(e -> user.process(e))` | 嵌套处理                          |


## 方案

### 生成代码阶段

#### 主要流程

1. 复制ClassOrInterfaceDeclaration

ClassOrInterfaceDeclaration classA = ...; // 原始类 A
ClassOrInterfaceDeclaration clonedA = classA.clone();

2. 只保留需要的方法

已经有了一个 MethodDeclaration（可以基于签名匹配）：

```java
// 
List<MethodDeclaration> methods = new ArrayList<>(newOrderCtr.getMethods());

// 直接操作newOrderCtr.getMembers().removeIf会报错，所以先复制一个methods数组去掉自己不需要的方法
methods.removeIf(method ->
    !allowedMethodSignatures.contains(method.getSignature().asString())
);
newOrderCtr.getMembers().removeIf(b -> b instanceof MethodDeclaration);
// 然后将这个复制的methods数组
methods.forEach(newOrderCtr::addMember);
```

⚠️ 注意：用` method.getSignature().asString() `可以避免重载歧义

#### 依赖补全

生成父类，父类在内部模块才复制，不在就不管

### xxx.method() -> 确定xxx类型 相关场景梳理

遇到的 xxx.method() → “确定 xxx 真实类型/接口” 场景整理。

将解决方案分为如下两种：

1.  要让methodCall.resolve()成功的解决方法
    
2.  只通过scope取Declaration的解决方法，具体是指 scope
    

methodCall.scope.calculateResolvedType().asReferenceType().getTypeDeclaration().get().getAllFields()

**特别说明，**CombinedTypeSolver 加源码目录这种方式，必须要所在类以及其调用方法中的所有依赖的源码都要被加入源码目录才行！

|  场景  |  resolve()成功方案  |  scope方案  |
| --- | --- | --- |
|  Foo f = new Bar();  f.m();  |  CombinedTypeSolver 加源码目录即可  |   |
|  成员 / 静态字段调用 this.service.do()  |  CombinedTypeSolver 加源码目录即可  |  methodCall.scope  |
|  参数和返回值  |  直接用声明类型  |   |
|  this / super / 枚举常量  |  this和super不需要  |  this和super不需要  |
|  简单工厂：方法体里直接 return new Impl();  |  解析方法体 ObjectCreationExpr  |   |
|  泛型通配符 List<? extends Foo> l; l.get(0).m()  |  无法静态解析  |  取上界Foo  |
|  lambda / 方法引用 Consumer<Foo> c = FooService::proc;  |  无法静态解析  |   |
|  强转 ((Bar) foo).barSpecific()  |  无法静态解析  |  读取 CastExpr 覆盖静态类型  |
|  if/switch 简单分支工厂  |  无法静态解析  |  写个 20 行的常量传播 Visitor：若分支常量可判定就收敛到具体类  |
|  配置驱动工厂 Class.forName(cfg)...  |  无法静态解析  |  把 cfg (yaml/properties) 解析成 “键 → 实现类” 映射，提前注入  |
|  Spring @Autowired 多实现@Qualifier  |  无法静态解析  |  解析 BeanDefinition  |
|  Lombok  |  无法静态解析  |  用scope  |
|  MyBatis / Feign 等 JDK 动态代理  |  拿到接口定义就行  |   |
|  IoC 容器全局查 Bean ctx.getBean(Foo.class)  |  无法静态解析  |  启动应用 → ApplicationContext#getBeansOfType dump JSON 回灌  |
|  CGLIB / byte-buddy 生成子类  |  无法静态解析 AOP/Java agent 插桩打印 obj.getClass()  |   |
|  字符串反射 + 网络 / DB 配置  |  无法静态解析 集成测试时把真实值 log 下来  |   |
|  Nashorn / Groovy 脚本返回对象  |  无法静态解析 运行期探针  |   |
|  RPC / HTTP 框架返回 DTO  |  无法静态解析  |   |
|  数组协变 Object o = new Foo\[3\];  |   |   |
|  原始泛型 List<X> l = new ArrayList();  |   |  想办法获取 List<X>中的<X>  |
|  JNI/native handle  |   |  根据等号左边的类定义即可找到  |
|  ServiceLoader / SPI  |  项目暂不需要考虑  |  项目暂不需要考虑  |
