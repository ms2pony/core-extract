package com.example.filecleaner;

import com.example.filecleaner.config.CleanerConfig;
import com.example.filecleaner.service.FileCleanerService;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件清理应用程序主入口
 */
@Slf4j
public class FileCleanerApplication {
    
    public static void main(String[] args) {
        log.info("启动文件清理应用程序...");
        
        try {
            // 创建配置
            CleanerConfig config = new CleanerConfig();
            
            // 如果有命令行参数，可以覆盖默认配置
            if (args.length > 0) {
                config.setSourcePath(args[0]);
                log.info("使用命令行指定的源路径: {}", args[0]);
            }
            
            // 创建并执行清理服务
            FileCleanerService cleanerService = new FileCleanerService(config);
            cleanerService.execute();
            
        } catch (Exception e) {
            log.error("应用程序执行失败: {}", e.getMessage(), e);
            System.exit(1);
        }
        
        log.info("文件清理应用程序执行完成");
    }
}