package com.felix.watermark.metadata.images.XMP;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AIGC图片元数据读取工具类
 * 用于解析符合GB 45438-2025规范的AIGC元数据
 */
public class AIGCMetadataReader {

    // TC260命名空间URI
    private static final String TC260_NAMESPACE_URI = "http://www.tc260.org.cn/ns/AIGC/1.0/";

    /**
     * AIGC元数据对象 - 与写入类保持一致
     */
    public static class AIGCMetadata {
        private String label;
        private String contentProducer;
        private String produceID;
        private String reservedCode1;
        private String contentPropagator;
        private String propagateID;
        private String reservedCode2;
        private String imageTitle;

        public AIGCMetadata() {
            this.label = "";
            this.contentProducer = "";
            this.produceID = "";
            this.reservedCode1 = "";
            this.contentPropagator = "";
            this.propagateID = "";
            this.reservedCode2 = "";
            this.imageTitle = "";
        }

        // Getter和Setter方法
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label != null ? label : ""; }

        public String getContentProducer() { return contentProducer; }
        public void setContentProducer(String contentProducer) { this.contentProducer = contentProducer != null ? contentProducer : ""; }

        public String getProduceID() { return produceID; }
        public void setProduceID(String produceID) { this.produceID = produceID != null ? produceID : ""; }

        public String getReservedCode1() { return reservedCode1; }
        public void setReservedCode1(String reservedCode1) { this.reservedCode1 = reservedCode1 != null ? reservedCode1 : ""; }

        public String getContentPropagator() { return contentPropagator; }
        public void setContentPropagator(String contentPropagator) { this.contentPropagator = contentPropagator != null ? contentPropagator : ""; }

        public String getPropagateID() { return propagateID; }
        public void setPropagateID(String propagateID) { this.propagateID = propagateID != null ? propagateID : ""; }

        public String getReservedCode2() { return reservedCode2; }
        public void setReservedCode2(String reservedCode2) { this.reservedCode2 = reservedCode2 != null ? reservedCode2 : ""; }

        public String getImageTitle() { return imageTitle; }
        public void setImageTitle(String imageTitle) { this.imageTitle = imageTitle != null ? imageTitle : ""; }

        @Override
        public String toString() {
            return "AIGCMetadata{" +
                    "label='" + label + '\'' +
                    ", contentProducer='" + contentProducer + '\'' +
                    ", produceID='" + produceID + '\'' +
                    ", reservedCode1='" + reservedCode1 + '\'' +
                    ", contentPropagator='" + contentPropagator + '\'' +
                    ", propagateID='" + propagateID + '\'' +
                    ", reservedCode2='" + reservedCode2 + '\'' +
                    ", imageTitle='" + imageTitle + '\'' +
                    '}';
        }

        /**
         * 检查是否包含有效的AIGC标识
         */
        public boolean hasValidAIGCContent() {
            return label != null && !label.isEmpty() &&
                    contentProducer != null && !contentProducer.isEmpty();
        }
    }

    /**
     * 从图片文件中读取AIGC元数据
     * 支持JPEG和PNG格式
     */
    public static AIGCMetadata readAIGCMetadata(File imageFile) throws IOException {
        if (!imageFile.exists()) {
            throw new FileNotFoundException("图片文件不存在: " + imageFile.getAbsolutePath());
        }

        byte[] fileData = readFileToByteArray(imageFile);

        // 根据文件格式选择解析方法
        if (isJPEG(fileData)) {
            return readFromJPEG(fileData);
        } else if (isPNG(fileData)) {
            return readFromPNG(fileData);
        } else {
            throw new IOException("不支持的图片格式，仅支持JPEG和PNG");
        }
    }

    /**
     * 从JPEG文件中读取AIGC元数据
     */
    private static AIGCMetadata readFromJPEG(byte[] fileData) throws IOException {
        int position = 2; // 跳过SOI

        while (position < fileData.length - 1) {
            if (fileData[position] == (byte)0xFF) {
                int marker = fileData[position + 1] & 0xFF;

                // 检查文件结束标记
                if (marker == 0xD9) { // EOI
                    break;
                }

                // 检查APP1段 (XMP)
                if (marker == 0xE1 && isXMPSegment(fileData, position)) {
                    String xmpContent = extractXMPContent(fileData, position);
                    if (!xmpContent.isEmpty()) {
                        AIGCMetadata metadata = parseXMPContent(xmpContent);
                        if (metadata != null) {
                            return metadata;
                        }
                    }
                }

                position = skipSegment(fileData, position);
            } else {
                position++;
            }
        }

        return null; // 未找到AIGC元数据
    }

    /**
     * 从PNG文件中读取AIGC元数据
     */
    private static AIGCMetadata readFromPNG(byte[] fileData) throws IOException {
        int position = 8; // 跳过PNG签名

        while (position < fileData.length) {
            if (position + 8 > fileData.length) break;

            int chunkLength = readIntBE(fileData, position);
            String chunkType = new String(fileData, position + 4, 4, StandardCharsets.US_ASCII);

            // 检查块长度有效性
            if (chunkLength < 0 || position + 12 + chunkLength > fileData.length) {
                break;
            }

            // 检查XMP块
            if ("iTXt".equals(chunkType) && isXMPChunk(fileData, position)) {
                String xmpContent = extractXMPContentFromChunk(fileData, position);
                if (!xmpContent.isEmpty()) {
                    AIGCMetadata metadata = parseXMPContent(xmpContent);
                    if (metadata != null) {
                        return metadata;
                    }
                }
            }

            position += 12 + chunkLength;
        }

        return null; // 未找到AIGC元数据
    }

    /**
     * 解析XMP内容，提取AIGC元数据
     */
    private static AIGCMetadata parseXMPContent(String xmpContent) {
        try {
            AIGCMetadata metadata = new AIGCMetadata();

            // 提取图片标题
            String title = extractXmlElement(xmpContent, "dc:title");
            if (title != null) {
                metadata.setImageTitle(unescapeXml(title));
            }

            // 提取AIGC JSON数据
            String aigcJson = extractXmlElement(xmpContent, "TC260:AIGC");
            if (aigcJson != null) {
                parseAIGCJson(metadata, aigcJson);
                return metadata;
            }
        } catch (Exception e) {
            System.err.println("解析XMP内容失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 解析AIGC JSON数据
     */
    private static void parseAIGCJson(AIGCMetadata metadata, String jsonString) {
        try {
            // 简单的JSON解析，处理标准格式
            metadata.setLabel(extractJsonField(jsonString, "Label"));
            metadata.setContentProducer(extractJsonField(jsonString, "ContentProducer"));
            metadata.setProduceID(extractJsonField(jsonString, "ProduceID"));
            metadata.setReservedCode1(extractJsonField(jsonString, "ReservedCode1"));
            metadata.setContentPropagator(extractJsonField(jsonString, "ContentPropagator"));
            metadata.setPropagateID(extractJsonField(jsonString, "PropagateID"));
            metadata.setReservedCode2(extractJsonField(jsonString, "ReservedCode2"));
        } catch (Exception e) {
            System.err.println("解析AIGC JSON失败: " + e.getMessage());
        }
    }

    /**
     * 从JSON字符串中提取字段值
     */
    private static String extractJsonField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\":\"(.*?)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String value = matcher.group(1);
            // 处理JSON转义字符
            return value.replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\b", "\b")
                    .replace("\\f", "\f")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");
        }
        return "";
    }

    /**
     * 从XML内容中提取元素内容
     */
    private static String extractXmlElement(String xml, String elementName) {
        Pattern pattern = Pattern.compile("<" + elementName + ">(.*?)</" + elementName + ">", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * XML字符反转义
     */
    private static String unescapeXml(String text) {
        return text.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }

    // ========== 辅助方法（与写入类保持一致） ==========

    private static boolean isJPEG(byte[] data) {
        return data.length >= 2 && data[0] == (byte)0xFF && data[1] == (byte)0xD8;
    }

    private static boolean isPNG(byte[] data) {
        byte[] signature = {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (data.length < 8) return false;
        for (int i = 0; i < 8; i++) {
            if (data[i] != signature[i]) return false;
        }
        return true;
    }

    private static boolean isXMPSegment(byte[] data, int position) {
        if (position + 35 >= data.length) return false;
        byte[] xmpId = "http://ns.adobe.com/xap/1.0/".getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < xmpId.length; i++) {
            if (data[position + 4 + i] != xmpId[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isXMPChunk(byte[] data, int position) {
        try {
            String keyword = extractNullTerminatedString(data, position + 8, 50);
            return "XML:com.adobe.xmp".equals(keyword);
        } catch (Exception e) {
            return false;
        }
    }

    private static String extractNullTerminatedString(byte[] data, int start, int maxLength) {
        int end = start;
        while (end < data.length && end - start < maxLength && data[end] != 0) {
            end++;
        }
        return new String(data, start, end - start, StandardCharsets.UTF_8);
    }

    private static String extractXMPContent(byte[] data, int position) {
        if (position + 4 >= data.length) return "";
        int segmentLength = ((data[position + 2] & 0xFF) << 8) | (data[position + 3] & 0xFF);
        if (segmentLength < 31) return "";

        int xmpStart = position + 4 + 29; // 跳过标记和标识符
        int xmpLength = segmentLength - 2 - 29;

        if (xmpStart + xmpLength <= data.length && xmpLength > 0) {
            return new String(data, xmpStart, xmpLength, StandardCharsets.UTF_8);
        }
        return "";
    }

    private static String extractXMPContentFromChunk(byte[] data, int position) {
        int dataStart = position + 8;
        // 跳过关键字和空终止符
        int offset = 0;
        while (dataStart + offset < data.length && data[dataStart + offset] != 0) {
            offset++;
        }
        offset += 5; // 跳过空终止符和4个标志字节

        int chunkLength = readIntBE(data, position);
        int xmpLength = chunkLength - offset;

        if (xmpLength > 0 && dataStart + offset + xmpLength <= data.length) {
            return new String(data, dataStart + offset, xmpLength, StandardCharsets.UTF_8);
        }
        return "";
    }

    private static int skipSegment(byte[] data, int position) {
        if (position + 4 > data.length) return data.length;
        int segmentLength = ((data[position + 2] & 0xFF) << 8) | (data[position + 3] & 0xFF);
        return position + 2 + segmentLength;
    }

    private static int readIntBE(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) |
                ((data[offset + 1] & 0xFF) << 16) |
                ((data[offset + 2] & 0xFF) << 8) |
                (data[offset + 3] & 0xFF);
    }

    private static byte[] readFileToByteArray(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }

    /**
     * 检查文件是否包含AIGC元数据
     */
    public static boolean hasAIGCMetadata(File imageFile) {
        try {
            AIGCMetadata metadata = readAIGCMetadata(imageFile);
            return metadata != null && metadata.hasValidAIGCContent();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取TC260命名空间URI
     */
    public static String getTC260NamespaceUri() {
        return TC260_NAMESPACE_URI;
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        try {
            // 测试读取AIGC元数据
            File testFile = new File("D:\\data\\watermark\\XN-AIGC.png");
            if (testFile.exists()) {
                System.out.println("正在解析AIGC元数据: " + testFile.getName());

                AIGCMetadata metadata = readAIGCMetadata(testFile);
                if (metadata != null) {
                    System.out.println("成功读取AIGC元数据:");
                    System.out.println("  图片标题: " + metadata.getImageTitle());
                    System.out.println("  Label: " + metadata.getLabel());
                    System.out.println("  ContentProducer: " + metadata.getContentProducer());
                    System.out.println("  ProduceID: " + metadata.getProduceID());
                    System.out.println("  ReservedCode1: " + metadata.getReservedCode1());
                    System.out.println("  ContentPropagator: " + metadata.getContentPropagator());
                    System.out.println("  PropagateID: " + metadata.getPropagateID());
                    System.out.println("  ReservedCode2: " + metadata.getReservedCode2());
                    System.out.println("  是否包含有效AIGC内容: " + metadata.hasValidAIGCContent());
                } else {
                    System.out.println("未找到AIGC元数据");
                }
            } else {
                System.out.println("测试文件不存在: " + testFile.getAbsolutePath());
            }

            // 测试检查功能
            System.out.println("\n检查文件是否包含AIGC元数据: " + hasAIGCMetadata(testFile));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
