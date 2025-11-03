package com.felix;

import com.felix.watermark.metadata.images.AIGCMetadataUtils;

import java.io.File;

/**
 * 使用示例
 */
public class AIGCMetadataExample {
    public static void main(String[] args) {
        try {
            // 创建AIGC元数据
            AIGCMetadata metadata = new AIGCMetadata(
                    "1",           // Label
                    "001191610133596325171T12566",          // ContentProducer
                    "BGIMGPD20250002",           // ProduceID
                    "RC001",                  // ReservedCode1
                    "001191610133596325171T22588",       // ContentPropagator
                    "BGIMGPPG20250002",           // PropagateID
                    "RC002"                   // ReservedCode2
            );

            // 处理JPEG文件
            File jpegInput = new File("D:\\data\\watermark\\xk.jpg");
            File jpegOutput = new File("D:\\data\\watermark\\xk-aigc.jpg");

            if (AIGCMetadataUtils.addAIGCMetadata(jpegInput, jpegOutput, metadata)) {
                System.out.println("JPEG文件AIGC标识添加成功");

                // 读取验证
                AIGCMetadata readMetadata = AIGCMetadataUtils.readAIGCMetadata(jpegOutput);
                if (readMetadata != null) {
                    System.out.println("JPG读取到的AIGC元数据: " + readMetadata.toJsonString());
                }
            }

            // 处理PNG文件
            File pngInput = new File("D:\\data\\watermark\\xn.png");
            File pngOutput = new File("D:\\data\\watermark\\xn-aigc.png");

            if (AIGCMetadataUtils.addAIGCMetadata(pngInput, pngOutput, metadata)) {
                System.out.println("PNG文件AIGC标识添加成功");
            }

            // 验证读取
            AIGCMetadata readMetadata = AIGCMetadataUtils.readAIGCMetadata(pngOutput);
            if (readMetadata != null) {
                System.out.println("PNG读取到的AIGC元数据: " + readMetadata.toJsonString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
