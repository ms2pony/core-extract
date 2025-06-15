package com.example.filecleaner.config;

import lombok.Data;

/**
 * 文件清理配置类
 */
@Data
public class CleanerConfig {
    
    /**
     * 源项目路径
     */
    private String sourcePath = "K:\\tender-mgt";
    
    /**
     * 输出路径
     */
    private String outputPath = "output";
    
    /**
     * 未使用文件存放目录
     */
    private String unusedSetPath = "step1-unsed-set";
    
    /**
     * 保留文件存放目录
     */
    private String keepSetPath = "step2-set";
    
    /**
     * 是否保留根目录下的所有文件
     */
    private boolean keepRootFiles = true;
    
    /**
     * 是否保留resources文件夹
     */
    private boolean keepResourcesFolder = true;
    
    /**
     * 需要保留的文件扩展名
     */
    private String[] keepExtensions = {".java", ".xml"};
    
    /**
     * 需要删除的文件夹名称
     */
    private String[] deleteFolders = {".git"};
}