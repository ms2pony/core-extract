
# Dependency Analyzer – English Specification

## 1 · Problem Statement
Implement a static analyser (Java 8) that, given **one Java source file** and a **target method name**, returns a report of **all symbols referenced by that method**, grouped by the qualified name of the class declaring each symbol.

* **Symbols to include**
  * fields (instance / static)
  * enum constants
  * annotation types
  * regular classes referenced as a _type_ (`new X()`, `X.class`, parameter / return / throws, generic bounds)
* **Symbols to exclude**
  * anything unresolved by the configured `TypeSolver`
  * all JDK packages `java.* javax.* jdk.* sun.* org.w3c.* org.xml.*`
  * classes coming from jar files (no `JarTypeSolver` is added)

If a symbol cannot be resolved and `ignoreUnresolved == true`, skip it silently; otherwise propagate the exception.

## 2 · Public API (`com.example.depanalysis.api`)
```java
public interface DependencyAnalyzer {
    AnalysisReport analyze(File javaFile,
                           String methodName,
                           AnalysisConfig cfg) throws IOException;
}
```
```java
public class AnalysisConfig {
    private List<Path> sourceRoots = new ArrayList<>(); // multi‑module roots
    private boolean    ignoreUnresolved = true;
}
```
```java
public class AnalysisReport {
    private Map<String, ClassSymbols> classes = new HashMap<>();
    public String toJson() { /* optional */ }
}
```
```java
public class ClassSymbols {
    private final String classQualifiedName;
    private final Set<String> fields        = new HashSet<>();
    private final Set<String> enumConstants = new HashSet<>();
    private final Set<String> annotations   = new HashSet<>();
    private boolean referencedAsType = false;
}
```

## 3 · Recommended Project Layout
```
dependency-analyzer/
 ├─ pom.xml
 └─ src
     ├─ main/java/com/example/depanalysis/
     │    ├─ api/
     │    ├─ model/
     │    ├─ impl/
     │    └─ util/
     └─ test/java/com/example/depanalysis/
```
* `api/` — public interfaces / DTOs  
* `impl/` — concrete logic / visitors  
* `model/` — internal domain objects  

## 4 · Algorithm Details

### 4.1 TypeSolver
```java
CombinedTypeSolver ts = new CombinedTypeSolver();
for (Path root : cfg.getSourceRoots()) {
    ts.add(new JavaParserTypeSolver(root.toFile()));
}
ts.add(new ReflectionTypeSolver());           // keep JDK types resolvable
/* no JarTypeSolver → jar classes ignored */
JavaParserFacade facade = JavaParserFacade.get(ts);
```

### 4.2 Visitor Chain
```
CompilationUnit
 └─ MethodFilterVisitor
         └─ MethodDependencyCollector
```

* `MethodFilterVisitor` scans `MethodDeclaration`; on name match executes child collector and **does not** call `super.visit`.
* `MethodDependencyCollector` overrides nodes:
  * `AnnotationExpr`
  * `ObjectCreationExpr`, `ClassExpr`, `ClassOrInterfaceType`
  * `FieldAccessExpr`, `NameExpr`
  * (optional) `MethodReferenceExpr`, `InstanceOfExpr`

### 4.3 Storage Helper
```java
private static final List<String> EX_PREFIX =
        Arrays.asList("java.","javax.","jdk.","sun.","org.w3c.","org.xml.");

private boolean isTracked(String qn) {
    return EX_PREFIX.stream().noneMatch(qn::startsWith);
}

private void store(String ownerQN, Consumer<ClassSymbols> mutator) {
    if (!isTracked(ownerQN)) return;
    mutator.accept(report.getClasses()
                         .computeIfAbsent(ownerQN, ClassSymbols::new));
}
```

## 5 · Unit‑Test Expectations
1. Add source roots (`app`, `common`) to `AnalysisConfig`.
2. Run `analyze(...)`.
3. Assert JSON only contains packages from those roots and expected symbol sets.
