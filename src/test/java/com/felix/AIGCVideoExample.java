package com.felix;

import com.felix.watermark.metadata.video.VideoMetadataHandler;

public class AIGCVideoExample {

    public static void main(String[] args) {
        // 创建AIGC元数据
        AIGCMetadata metadata = new AIGCMetadata(
                "1",                // Label
                "001191610133596325171T12566",        // ContentProducer
                "BGVIDEO20250001",        // ProduceID
                "BGAI001",                  // ReservedCode1
                "001191610133596325171T22588",       // ContentPropagator
                "BGVD20250011",           // PropagateID
                "BGAI002"                   // ReservedCode2
        );
        // 处理JPEG文件
        String inputFile = "D:\\data\\watermark\\bg-test.mp4";
        String outputFile = "D:\\data\\watermark\\bg-aigc.mp4";
        boolean success = VideoMetadataHandler.addStealthMetadata(inputFile, outputFile, metadata);
        System.out.println("视频添加元数据水印处理结果: " + (success ? "成功" : "失败"));
        AIGCMetadata metadataRead = VideoMetadataHandler.readStealthMetadata(outputFile);
        if(metadataRead != null)
        {
            System.out.println("视频元数据水印解析结果: " + metadataRead.toJsonString());
        }else{
            System.out.println("元数据为空");
        }
    }

}
