package com.example.filecleaner.service;

import com.example.filecleaner.config.CleanerConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 文件清理服务类
 */
@Slf4j
public class FileCleanerService {
    
    private final CleanerConfig config;
    private final List<String> deletedFiles = new ArrayList<>();
    private final List<String> deletedFolders = new ArrayList<>();
    
    public FileCleanerService(CleanerConfig config) {
        this.config = config;
    }
    
    /**
     * 执行文件清理
     */
    public void execute() {
        try {
            log.info("开始执行文件清理任务...");
            
            // 步骤a: 创建输出文件夹
            createOutputDirectories();
            
            // 步骤b: 执行文件剔除
            performFileCleaning();
            
            // 步骤c: 删除空文件夹
            removeEmptyDirectories();
            
            // 步骤d: 打印删除的文件目录树
            printDeletedFilesTree();
            
            log.info("文件清理任务完成！");
            
        } catch (Exception e) {
            log.error("文件清理过程中发生错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 创建输出目录
     */
    private void createOutputDirectories() throws IOException {
        log.info("创建输出目录...");
        
        Path outputPath = Paths.get(config.getOutputPath());
        
        // 如果输出目录存在，先删除
        if (Files.exists(outputPath)) {
            log.info("删除已存在的输出目录: {}", outputPath);
            deleteDirectory(outputPath);
        }
        
        // 创建输出目录
        Files.createDirectories(outputPath);
        Files.createDirectories(outputPath.resolve(config.getUnusedSetPath()));
        Files.createDirectories(outputPath.resolve(config.getKeepSetPath()));
        
        log.info("输出目录创建完成");
    }
    
    /**
     * 执行文件清理
     */
    private void performFileCleaning() throws IOException {
        log.info("开始执行文件剔除...");
        
        Path sourcePath = Paths.get(config.getSourcePath());
        Path unusedSetPath = Paths.get(config.getOutputPath(), config.getUnusedSetPath());
        Path keepSetPath = Paths.get(config.getOutputPath(), config.getKeepSetPath());
        
        Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String dirName = dir.getFileName().toString();
                
                // 检查是否需要删除的文件夹
                if (Arrays.asList(config.getDeleteFolders()).contains(dirName)) {
                    log.info("删除文件夹: {}", dir);
                    copyToUnusedSet(dir, unusedSetPath, sourcePath);
                    deletedFolders.add(dir.toString());
                    return FileVisitResult.SKIP_SUBTREE;
                }
                
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                boolean shouldKeep = shouldKeepFile(file, sourcePath);
                
                if (shouldKeep) {
                    copyToKeepSet(file, keepSetPath, sourcePath);
                } else {
                    copyToUnusedSet(file, unusedSetPath, sourcePath);
                    deletedFiles.add(file.toString());
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        log.info("文件剔除完成");
    }
    
    /**
     * 判断文件是否应该保留
     */
    private boolean shouldKeepFile(Path file, Path sourcePath) {
        String fileName = file.getFileName().toString();
        String relativePath = sourcePath.relativize(file).toString();
        
        // 检查是否在根目录
        boolean isInRoot = !relativePath.contains(File.separator);
        if (isInRoot && config.isKeepRootFiles()) {
            log.debug("保留根目录文件: {}", fileName);
            return true;
        }
        
        // 检查是否在resources文件夹中
        if (config.isKeepResourcesFolder() && relativePath.contains("resources")) {
            log.debug("保留resources文件夹中的文件: {}", fileName);
            return true;
        }
        
        // 检查文件扩展名
        for (String ext : config.getKeepExtensions()) {
            if (fileName.toLowerCase().endsWith(ext.toLowerCase())) {
                log.debug("保留指定扩展名文件: {}", fileName);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 复制文件到保留集合
     */
    private void copyToKeepSet(Path source, Path keepSetPath, Path sourcePath) throws IOException {
        Path relativePath = sourcePath.relativize(source);
        Path targetPath = keepSetPath.resolve(relativePath);
        
        Files.createDirectories(targetPath.getParent());
        Files.copy(source, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }
    
    /**
     * 复制文件到未使用集合
     */
    private void copyToUnusedSet(Path source, Path unusedSetPath, Path sourcePath) throws IOException {
        Path relativePath = sourcePath.relativize(source);
        Path targetPath = unusedSetPath.resolve(relativePath);
        
        Files.createDirectories(targetPath.getParent());
        if (Files.isDirectory(source)) {
            copyDirectory(source, targetPath);
        } else {
            Files.copy(source, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    /**
     * 删除空文件夹
     */
    private void removeEmptyDirectories() throws IOException {
        log.info("删除空文件夹...");
        
        Path keepSetPath = Paths.get(config.getOutputPath(), config.getKeepSetPath());
        
        Files.walkFileTree(keepSetPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (isDirEmpty(dir) && !dir.equals(keepSetPath)) {
                    log.info("删除空文件夹: {}", dir);
                    Files.delete(dir);
                    deletedFolders.add(dir.toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * 打印删除的文件目录树
     */
    private void printDeletedFilesTree() {
        log.info("=== 删除的文件目录树 ===");
        log.info("删除的文件夹 ({} 个):", deletedFolders.size());
        for (String folder : deletedFolders) {
            log.info("  📁 {}", folder);
        }
        
        log.info("删除的文件 ({} 个):", deletedFiles.size());
        for (String file : deletedFiles) {
            log.info("  📄 {}", file);
        }
        log.info("========================");
    }
    
    /**
     * 检查目录是否为空
     */
    private boolean isDirEmpty(Path dir) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            return !dirStream.iterator().hasNext();
        }
    }
    
    /**
     * 删除目录及其所有内容
     */
    private void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * 复制目录
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}