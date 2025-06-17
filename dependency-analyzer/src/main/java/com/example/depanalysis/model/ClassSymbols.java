package com.example.depanalysis.model;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 类符号信息
 */
@Data
public class ClassSymbols {
    /**
     * 类的限定名
     */
    private final String classQualifiedName;
    
    /**
     * 字段名集合（实例字段和静态字段）
     */
    private final Set<String> fields = new HashSet<>();
    
    /**
     * 枚举常量名集合
     */
    private final Set<String> enumConstants = new HashSet<>();
    
    /**
     * 注解类型名集合
     */
    private final Set<String> annotations = new HashSet<>();
    
    /**
     * 是否作为类型被引用
     */
    private boolean referencedAsType = false;
    
    public ClassSymbols(String classQualifiedName) {
        this.classQualifiedName = classQualifiedName;
    }
}