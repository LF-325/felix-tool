package com.felix.file;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.FileInputStream;

public class FileTypeChecker {

    /**
     * 通过文件扩展名判断文件类型
     */
    public static FileType getFileTypeByExtension(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return FileType.UNKNOWN;
        }

        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".png")) {
            return FileType.PNG;
        } else if (fileName.endsWith(".mp4")) {
            return FileType.MP4;
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return FileType.JPEG;
        } else if (fileName.endsWith(".gif")) {
            return FileType.GIF;
        } else if (fileName.endsWith(".avi")) {
            return FileType.AVI;
        } else if (fileName.endsWith(".mov")) {
            return FileType.MOV;
        } else {
            return FileType.UNKNOWN;
        }
    }

    /**
     * 通过文件魔数（文件头）判断文件类型 - 更准确的方式
     */
    public static FileType getFileTypeByMagicNumber(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return FileType.UNKNOWN;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[8];
            int bytesRead = fis.read(header, 0, 8);

            if (bytesRead < 8) {
                return FileType.UNKNOWN;
            }

            // PNG文件头: 89 50 4E 47 0D 0A 1A 0A
            if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E &&
                    header[3] == 0x47 && header[4] == 0x0D && header[5] == 0x0A &&
                    header[6] == 0x1A && header[7] == 0x0A) {
                return FileType.PNG;
            }

            // MP4文件头: 00 00 00 18 66 74 79 70 (可能有变化，这是常见的一种)
            if (header[4] == 0x66 && header[5] == 0x74 && header[6] == 0x79 && header[7] == 0x70) {
                return FileType.MP4;
            }

            // 其他文件类型的魔数检查可以在这里添加

        } catch (IOException e) {
            System.err.println("读取文件头失败: " + file.getAbsolutePath() + ", 错误: " + e.getMessage());
        }

        return FileType.UNKNOWN;
    }

    /**
     * 使用Files.probeContentType判断文件类型（需要系统支持）
     */
    public static FileType getFileTypeByContentType(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return FileType.UNKNOWN;
        }

        try {
            Path path = file.toPath();
            String contentType = Files.probeContentType(path);

            if (contentType != null) {
                switch (contentType) {
                    case "image/png":
                        return FileType.PNG;
                    case "video/mp4":
                        return FileType.MP4;
                    case "image/jpeg":
                        return FileType.JPEG;
                    case "image/gif":
                        return FileType.GIF;
                    case "video/x-msvideo":
                        return FileType.AVI;
                    case "video/quicktime":
                        return FileType.MOV;
                    default:
                        return FileType.UNKNOWN;
                }
            }
        } catch (IOException e) {
            System.err.println("探测文件类型失败: " + file.getAbsolutePath() + ", 错误: " + e.getMessage());
        }

        return FileType.UNKNOWN;
    }

    /**
     * 综合判断文件类型，优先使用魔数，失败时使用扩展名
     */
    public static FileType getFileType(File file) {
        FileType typeByMagic = getFileTypeByMagicNumber(file);
        if (typeByMagic != FileType.UNKNOWN) {
            return typeByMagic;
        }
        return getFileTypeByExtension(file);
    }

    /**
     * 专门判断是否是PNG文件
     */
    public static boolean isPngFile(File file) {
        return getFileType(file) == FileType.PNG;
    }

    /**
     * 专门判断是否是MP4文件
     */
    public static boolean isMp4File(File file) {
        return getFileType(file) == FileType.MP4;
    }

    /**
     * 判断是否是图片文件
     */
    public static boolean isImageFile(File file) {
        FileType type = getFileType(file);
        return type == FileType.PNG || type == FileType.JPEG || type == FileType.GIF;
    }

    /**
     * 判断是否是视频文件
     */
    public static boolean isVideoFile(File file) {
        FileType type = getFileType(file);
        return type == FileType.MP4 || type == FileType.AVI || type == FileType.MOV;
    }

    /**
     * 文件类型枚举
     */
    public enum FileType {
        PNG, MP4, JPEG, GIF, AVI, MOV, UNKNOWN;

        public String getDescription() {
            switch (this) {
                case PNG: return "PNG图像文件";
                case MP4: return "MP4视频文件";
                case JPEG: return "JPEG图像文件";
                case GIF: return "GIF图像文件";
                case AVI: return "AVI视频文件";
                case MOV: return "QuickTime视频文件";
                default: return "未知文件类型";
            }
        }

        public String getMimeType() {
            switch (this) {
                case PNG: return "image/png";
                case MP4: return "video/mp4";
                case JPEG: return "image/jpeg";
                case GIF: return "image/gif";
                case AVI: return "video/x-msvideo";
                case MOV: return "video/quicktime";
                default: return "application/octet-stream";
            }
        }
    }
}
