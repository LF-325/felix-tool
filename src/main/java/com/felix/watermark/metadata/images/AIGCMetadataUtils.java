package com.felix.watermark.metadata.images;

import java.io.File;

/**
 * AIGC隐式标识统一工具类
 */
public class AIGCMetadataUtils {

    /**
     * 根据文件类型自动选择处理器并添加AIGC元数据
     */
    public static boolean addAIGCMetadata(File inputFile, File outputFile, AIGCMetadata metadata) {
        String fileName = inputFile.getName().toLowerCase();

        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return JPEGMetadataHandler.addAIGCMetadata(inputFile, outputFile, metadata);
        } else if (fileName.endsWith(".png")) {
            return PNGMetadataHandler.addAIGCMetadata(inputFile, outputFile, metadata);
        } else {
            System.err.println("不支持的图片格式: " + fileName);
            return false;
        }
    }

    /**
     * 根据文件类型读取AIGC元数据
     */
    public static AIGCMetadata readAIGCMetadata(File imageFile) {
        String fileName = imageFile.getName().toLowerCase();
        try {
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                return JPEGMetadataHandler.readAIGCMetadata(imageFile);
            } else if (fileName.endsWith(".png")) {
                return PNGMetadataHandler.readAIGCMetadata(imageFile);
            } else {
                System.err.println("不支持的图片格式: " + fileName);
                return null;
            }
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
