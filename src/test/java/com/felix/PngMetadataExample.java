package com.felix;

import com.alibaba.fastjson.JSON;
import com.felix.images.metadata.PNGMetadataMarker;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class PngMetadataExample {

    public static void main(String[] args) {
        writeMetadata();
    }


    public static void writeMetadata() {
        String sourceFilePath = "D:\\data\\watermark\\xn.png";
        String destFilePath = "D:\\data\\watermark\\xn_out.png";
        try {
            // 创建内容信息
            PNGMetadataMarker.ContentInfo contentInfo = new PNGMetadataMarker.ContentInfo();
            contentInfo.setContentId("AI_CONTENT_20250001");
            contentInfo.setProvider("BoGuan AI");
            contentInfo.setGenerator("Text-to-Image Generator v1.0");
            contentInfo.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new Date()));
            contentInfo.setContentType("synthetic_image");
            contentInfo.setAuthor("BoGuan");
            contentInfo.setCopyright("© 2025 Introtec Service Provider");

            // 添加隐式标识
            boolean success = PNGMetadataMarker.addHiddenIdentifier(
                    new File(sourceFilePath),
                    new File(destFilePath),
                    contentInfo
            );

            if (success) {
                System.out.println("隐式标识添加成功");

                // 读取验证
                PNGMetadataMarker.ContentInfo readInfo = PNGMetadataMarker.readMetadata(new File(destFilePath));

                System.out.println("读取到的元数据:"+ JSON.toJSONString(readInfo));
//                for (Map.Entry<String, String> entry : readInfo.getAllFields().entrySet()) {
//                    System.out.println(entry.getKey() + ": " + entry.getValue());
//                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
