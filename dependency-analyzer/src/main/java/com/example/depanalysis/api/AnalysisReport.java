package com.example.depanalysis.api;

import com.example.depanalysis.model.ClassSymbols;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 分析报告类
 */
@Data
public class AnalysisReport {
    /**
     * 按类限定名分组的符号信息
     */
    private Map<String, ClassSymbols> classes = new HashMap<>();
    
    /**
     * 转换为JSON格式（可选实现）
     * @return JSON字符串
     */
    public String toJson() {
        // TODO: 实现JSON序列化
        return "{}";
    }
}