package com.felix.watermark.metadata.images;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputField;
import org.apache.commons.imaging.formats.tiff.fieldtypes.FieldType;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * JPEG格式AIGC标识添加器
 */
public class JPEGMetadataHandler {

    /**
     * 为JPEG图片添加AIGC隐式标识
     */
    public static boolean addAIGCMetadata(File inputFile, File outputFile, AIGCMetadata metadata) {
        try {
            // 生成符合GB 45438-2025标准的JSON字符串
            String aigcJson = metadata.toJsonString();
            System.out.println("准备写入的AIGC JSON: " + aigcJson);

            // 使用Apache Commons Imaging处理JPEG元数据
            return addAIGCToJPEGUsingCommonsImaging(inputFile, outputFile, aigcJson);

        } catch (Exception e) {
            System.err.println("处理JPEG文件时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean addAIGCToJPEGUsingCommonsImaging(File inputFile, File outputFile, String aigcJson) {
        try {
            // 1. 创建符合EXIF规范的UserComment字节数组
            byte[] userCommentBytes = createEXIFUserComment(aigcJson);
            System.out.println("UserComment字节长度: " + userCommentBytes.length);

            // 2. 读取原始图片元数据
            ImageMetadata metadata = Imaging.getMetadata(inputFile);
            TiffOutputSet outputSet = getOrCreateOutputSet(metadata);
            TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();

            // 3. 写入UserComment字段
            exifDirectory.removeField(ExifTagConstants.EXIF_TAG_USER_COMMENT);

            TiffOutputField userCommentField = new TiffOutputField(
                    ExifTagConstants.EXIF_TAG_USER_COMMENT,
                    FieldType.UNDEFINED, // UserComment使用UNDEFINED类型
                    userCommentBytes.length,
                    userCommentBytes
            );

            exifDirectory.add(userCommentField);

            // 4. 写入文件
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                new ExifRewriter().updateExifMetadataLossless(inputFile, fos, outputSet);
                System.out.println("成功写入AIGC元数据到: " + outputFile.getAbsolutePath());

                // 验证写入是否成功
                return verifyAIGCMetadata(outputFile);
            }
        } catch (Exception e) {
            System.err.println("使用Commons Imaging处理失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 创建符合EXIF规范的UserComment字节数组
     * EXIF规范：前8字节为编码标识，后面为实际数据
     */
    private static byte[] createEXIFUserComment(String content) {
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        return contentBytes;
    }

    private static TiffOutputSet getOrCreateOutputSet(ImageMetadata metadata) throws Exception {
        JpegImageMetadata jpegMetadata = (metadata instanceof JpegImageMetadata) ? (JpegImageMetadata) metadata : null;

        if (jpegMetadata != null) {
            TiffImageMetadata exif = jpegMetadata.getExif();
            if (exif != null) {
                return exif.getOutputSet();
            }
        }
        return new TiffOutputSet();
    }

    /**
     * 从JPEG图片读取AIGC元数据
     */
    public static AIGCMetadata readAIGCMetadata(File jpegFile) {
        try {
            System.out.println("开始读取文件: " + jpegFile.getAbsolutePath());
            Metadata metadata = ImageMetadataReader.readMetadata(jpegFile);

            for (Directory directory : metadata.getDirectories()) {
                System.out.println("检查目录: " + directory.getName());

                if (directory.containsTag(ExifDirectoryBase.TAG_USER_COMMENT)) {
                    System.out.println("找到UserComment标签");
                    byte[] userCommentBytes = directory.getByteArray(ExifDirectoryBase.TAG_USER_COMMENT);

                    if (userCommentBytes != null) {
                        System.out.println("UserComment字节长度: " + userCommentBytes.length);

                        String userComment = parseEXIFUserComment(userCommentBytes);
                        System.out.println("读取到的UserComment内容: " + userComment);

                        if (userComment != null && userComment.trim().startsWith("{\"AIGC\":")) {
                            System.out.println("找到AIGC标识");
                            return AIGCMetadata.fromJsonString(userComment);
                        }
                    }
                }
            }

            System.out.println("未找到AIGC元数据");

        } catch (Exception e) {
            System.err.println("读取JPEG元数据失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解析EXIF UserComment字节数组
     */
    private static String parseEXIFUserComment(byte[] userCommentBytes) {
        if (userCommentBytes == null || userCommentBytes.length < 8) {
            return null;
        }

        // 读取编码标识（前8字节）
        String encoding = new String(userCommentBytes, 0, 8, StandardCharsets.US_ASCII).trim();

        try {
            // 根据编码标识解析内容
            if (encoding.startsWith("ASCII")) {
                return new String(userCommentBytes, 8, userCommentBytes.length - 8, StandardCharsets.US_ASCII);
            } else if (encoding.startsWith("UNICODE")) {
                return new String(userCommentBytes, 8, userCommentBytes.length - 8, StandardCharsets.UTF_16);
            } else {
                // 默认尝试UTF-8
                return new String(userCommentBytes, 8, userCommentBytes.length - 8, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            // 如果解析失败，尝试直接使用UTF-8
            return new String(userCommentBytes, 8, userCommentBytes.length - 8, StandardCharsets.UTF_8);
        }
    }

    /**
     * 验证AIGC元数据是否成功写入
     */
    private static boolean verifyAIGCMetadata(File jpegFile) {
        try {
            AIGCMetadata readMetadata = readAIGCMetadata(jpegFile);
            return readMetadata != null;
        } catch (Exception e) {
            System.err.println("验证AIGC元数据失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 调试方法：检查文件的所有EXIF信息
     */
    public static void debugExifMetadata(File jpegFile) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(jpegFile);

            for (Directory directory : metadata.getDirectories()) {
                System.out.println("=== 目录: " + directory.getName() + " ===");
                for (com.drew.metadata.Tag tag : directory.getTags()) {
                    System.out.println(tag.getTagName() + " : " + tag.getDescription());
                }
            }
        } catch (Exception e) {
            System.err.println("调试EXIF信息失败: " + e.getMessage());
        }
    }
}