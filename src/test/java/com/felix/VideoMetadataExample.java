package com.felix;

import java.io.File;
import java.util.Objects;

public class VideoMetadataExample {

    public static void main(String[] args) {
//        test1();
        test2();
    }

    public static void test1()
    {
        String inputPath = "D:\\data\\watermark\\bg-test.mp4";
        String outputPath = "D:\\data\\watermark\\bg-testout.mp4";
        StealthMarker marker = new StealthMarker();
        marker.setContentId("AI_CONTENT_20250001");
        marker.setProvider("BoGuan AI");
        marker.setContentType("synthetic_video");
        marker.setAuthor("BoGuan");
        marker.setCopyright("© 2025 AI Service Provider");
        marker.setSyntheticSource("BoGuan AI");
        boolean result = VideoMetadataMarker.addStealthMetadata(inputPath, outputPath, marker);
        System.out.println("添加隐式标识结果：" + result);
        StealthMarker readMarker = VideoMetadataMarker.readStealthMetadata(outputPath);
        System.out.println("读取隐式标识结果：" + readMarker);
    }

    public static void test2()
    {
        String inputPath = "D:\\data\\watermark\\bg-test.mp4";
        String outputPath = "D:\\data\\watermark\\bg-testout.mp4";
        // 初始化组件
        FFmpegMetadataMarker marker = new FFmpegMetadataMarker();
        StealthMarkerManager manager = new StealthMarkerManager();

        System.out.println("=== FFmpeg隐式标识测试 ===\n");

        // 创建测试标识
        StealthMarker testMarker = manager.createStandardMarker(
                "synthetic_video",
                "BoGuan AI",
                "AI_CONTENT_20250001",
                "BoGuan",
                "BoGuan AI"
        );
        testMarker.setCopyright("© 2025 Introtec Service Provider");
        testMarker.setVersion("1.0");

        // 验证标识格式
        if (!manager.validateMarker(testMarker)) {
            System.err.println("标识格式验证失败!");
            return;
        }

        System.out.println("创建的标识: " + testMarker);
        System.out.println("标识格式验证: 通过\n");

        try {

            // 检查输入文件是否存在
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                System.out.println("测试输入文件不存在，创建模拟测试...");
                // 在实际环境中，这里应该使用真实的视频文件
                System.out.println("请将代码中的文件路径替换为您的实际视频文件路径");
                return;
            }

            System.out.println("输入文件: " + inputPath);
            System.out.println("输出文件: " + outputPath);

            // 测试1: 添加隐式标识
            System.out.println("\n1. 测试添加隐式标识...");
            boolean addSuccess = marker.addStealthMetadata(inputPath, outputPath, testMarker);

            if (addSuccess) {
                System.out.println("✅ 隐式标识添加成功");

                // 测试2: 读取隐式标识
                System.out.println("\n2. 测试读取隐式标识...");
                StealthMarker readMarker = marker.readStealthMetadata(outputPath);

                if (readMarker != null) {
                    System.out.println("✅ 隐式标识读取成功");
                    System.out.println("读取到的标识: " + readMarker);

                    // 测试3: 验证标识完整性
                    System.out.println("\n3. 测试标识完整性验证...");
                    boolean integrityVerified = marker.verifyMarkerIntegrity(outputPath, testMarker);

                    if (integrityVerified) {
                        System.out.println("✅ 标识完整性验证通过");
                    } else {
                        System.out.println("❌ 标识完整性验证失败");
                    }

                    // 测试4: 比较原始标识和读取的标识
                    System.out.println("\n4. 测试标识一致性...");
                    boolean consistency = compareMarkers(testMarker, readMarker);

                    if (consistency) {
                        System.out.println("✅ 标识一致性验证通过");
                    } else {
                        System.out.println("❌ 标识一致性验证失败");
                        printMarkerComparison(testMarker, readMarker);
                    }

                } else {
                    System.out.println("❌ 隐式标识读取失败");
                }

            } else {
                System.out.println("❌ 隐式标识添加失败");
            }

//            // 测试5: 批量处理演示
//            System.out.println("\n5. 批量处理演示...");
//            String inputDir = "input_videos";
//            String outputDir = "output_videos";
//
//            File inputDirFile = new File(inputDir);
//            if (inputDirFile.exists() && inputDirFile.isDirectory()) {
//                marker.batchProcessVideos(inputDir, outputDir, testMarker);
//            } else {
//                System.out.println("批量处理输入目录不存在，跳过此测试");
//                System.out.println("请创建 '" + inputDir + "' 目录并放入测试视频文件");
//            }

        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== 测试完成 ===");
    }

    private static boolean compareMarkers(StealthMarker expected, StealthMarker actual) {
        return Objects.equals(expected.getContentType(), actual.getContentType()) &&
                Objects.equals(expected.getProvider(), actual.getProvider()) &&
                Objects.equals(expected.getContentId(), actual.getContentId()) &&
                Objects.equals(expected.getVersion(), actual.getVersion());
    }

    private static void printMarkerComparison(StealthMarker expected, StealthMarker actual) {
        System.out.println("期望: " + expected);
        System.out.println("实际: " + actual);

        if (!Objects.equals(expected.getContentType(), actual.getContentType())) {
            System.out.println("❌ ContentType不匹配");
        }
        if (!Objects.equals(expected.getProvider(), actual.getProvider())) {
            System.out.println("❌ ProviderCode不匹配");
        }
        if (!Objects.equals(expected.getContentId(), actual.getContentId())) {
            System.out.println("❌ ContentId不匹配");
        }
    }

}
