package com.paut.tender.mgt.inspect;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MethodCallAnalysisResult {
    private String filePath;
    private int startLine;
    private int endLine;
    private List<MethodCallInfo> methodCalls = new ArrayList<>();
}