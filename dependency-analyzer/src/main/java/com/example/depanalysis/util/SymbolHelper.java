package com.example.depanalysis.util;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import lombok.extern.slf4j.Slf4j;

/**
 * 符号解析辅助工具类
 */
@Slf4j
public class SymbolHelper {
    
    /**
     * 解析注解类型
     */
    public static String resolveAnnotationType(AnnotationExpr annotationExpr, JavaParserFacade facade) {
        try {
            ResolvedType resolvedType = facade.getType(annotationExpr);
            return resolvedType.describe();
        } catch (Exception e) {
            log.debug("无法解析注解类型: {}", annotationExpr, e);
            return null;
        }
    }
    
    /**
     * 解析对象创建类型
     */
    public static String resolveObjectCreationType(ObjectCreationExpr expr, JavaParserFacade facade) {
        try {
            ResolvedType resolvedType = facade.getType(expr);
            return resolvedType.describe();
        } catch (Exception e) {
            log.debug("无法解析对象创建类型: {}", expr, e);
            return null;
        }
    }
    
    /**
     * 解析类表达式
     */
    public static String resolveClassExpr(ClassExpr expr, JavaParserFacade facade) {
        try {
            ResolvedType resolvedType = facade.getType(expr.getType());
            return resolvedType.describe();
        } catch (Exception e) {
            log.debug("无法解析类表达式: {}", expr, e);
            return null;
        }
    }
    
    /**
     * 解析类或接口类型
     */
    public static String resolveClassOrInterfaceType(ClassOrInterfaceType type, JavaParserFacade facade) {
        try {
            ResolvedType resolvedType = facade.convertToUsage(type);
            return resolvedType.describe();
        } catch (Exception e) {
            log.debug("无法解析类或接口类型: {}", type, e);
            return null;
        }
    }
    
    /**
     * 解析字段访问
     * @return [ownerQualifiedName, fieldName]
     */
    public static String[] resolveFieldAccess(FieldAccessExpr expr, JavaParserFacade facade) {
        try {
            ResolvedValueDeclaration resolved = facade.solve(expr).getDeclaration().get();
            if (resolved instanceof ResolvedFieldDeclaration) {
                ResolvedFieldDeclaration field = (ResolvedFieldDeclaration) resolved;
                String ownerQualifiedName = field.declaringType().getQualifiedName();
                String fieldName = field.getName();
                return new String[]{ownerQualifiedName, fieldName};
            }
        } catch (Exception e) {
            log.debug("无法解析字段访问: {}", expr, e);
        }
        return null;
    }
    
    /**
     * 解析名称表达式
     * @return [ownerQualifiedName, symbolName, symbolType]
     */
    public static String[] resolveNameExpr(NameExpr expr, JavaParserFacade facade) {
        try {
            ResolvedValueDeclaration resolved = facade.solve(expr.getName()).getDeclaration().get();
            
            if (resolved instanceof ResolvedFieldDeclaration) {
                ResolvedFieldDeclaration field = (ResolvedFieldDeclaration) resolved;
                String ownerQualifiedName = field.declaringType().getQualifiedName();
                String symbolName = field.getName();
                
                // 判断是否为枚举常量
                String symbolType = field.declaringType().isEnum() ? "enum" : "field";
                
                return new String[]{ownerQualifiedName, symbolName, symbolType};
            }
            
            // 其他类型的符号解析可以在这里扩展
            
        } catch (Exception e) {
            log.debug("无法解析名称表达式: {}", expr, e);
        }
        return null;
    }
}