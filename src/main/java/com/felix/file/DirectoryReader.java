package com.felix.file;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class DirectoryReader {

    /**
     * 使用Files.walk遍历目录（Java 8+）
     */
    public static List<FileInfo> readDirectoryWithWalk(String path) throws IOException {
        List<FileInfo> fileList = new ArrayList<>();

        Files.walk(Paths.get(path))
                .forEach(filePath -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
                        FileInfo fileInfo = new FileInfo();
                        fileInfo.setName(filePath.getFileName().toString());
                        fileInfo.setPath(filePath.toString());
                        fileInfo.setDirectory(attrs.isDirectory());
                        fileInfo.setSize(attrs.size());
                        fileInfo.setLastModified(attrs.lastModifiedTime().toMillis());
                        fileList.add(fileInfo);
                    } catch (IOException e) {
                        System.err.println("无法读取文件属性: " + filePath);
                    }
                });

        return fileList;
    }

    /**
     * 使用FileVisitor进行更灵活的文件遍历
     */
    public static List<FileInfo> readDirectoryWithVisitor(String path) throws IOException {
        List<FileInfo> fileList = new ArrayList<>();

        Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                FileInfo fileInfo = createFileInfo(file, attrs);
                fileList.add(fileInfo);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
//                FileInfo fileInfo = createFileInfo(dir, attrs);
//                fileList.add(fileInfo);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                System.err.println("无法访问文件: " + file + ", 错误: " + exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });

        return fileList;
    }

    private static FileInfo createFileInfo(Path path, BasicFileAttributes attrs) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setName(path.getFileName().toString());
        fileInfo.setPath(path.toString());
        fileInfo.setDirectory(attrs.isDirectory());
        fileInfo.setSize(attrs.size());
        fileInfo.setLastModified(attrs.lastModifiedTime().toMillis());
        if(FileTypeChecker.isPngFile(path.toFile()))
        {
            fileInfo.setType("PNG");
        }else if(FileTypeChecker.isMp4File(path.toFile()))
        {
            fileInfo.setType("MP4");
        }
        return fileInfo;
    }

    /**
     * 文件信息类
     */
    public static class FileInfo {
        private String name;
        private String path;
        private boolean isDirectory;
        private long size;
        private long lastModified;

        private String type;

        // getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public boolean isDirectory() { return isDirectory; }
        public void setDirectory(boolean directory) { isDirectory = directory; }

        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }

        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            String type = isDirectory ? "DIR" : "FILE";
            return String.format("%-6s %-50s %10d bytes", type, name, size);
        }
    }
}