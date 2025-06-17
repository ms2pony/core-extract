package com.paut.tender.mgt.inspect;

import lombok.Data;

@Data
public class ScopeInfo {
    private String qualifiedName;
    private String classQualifiedName;  // 新增：方法调用的完全限定类名
    private String errorMessage;
    private String declaringClass;
}
