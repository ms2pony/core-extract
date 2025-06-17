package com.paut.tender.mgt.inspect;

import lombok.Data;

import java.util.List;

@Data
public class MethodCallInfo {
    private String methodName;
    private int line;
    private int column;
    private String declaringClass;
    private String qualifiedName;
    private String classQualifiedName;  // 新增：方法调用的完全限定类名
    private String packageName;
    private List<String> parameterTypes;
    private String errorMessage;
    private Boolean resolved;
}