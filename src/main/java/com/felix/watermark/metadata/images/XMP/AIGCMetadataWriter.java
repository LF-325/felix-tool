package com.felix.watermark.metadata.images.XMP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * 符合GB 45438-2025规范的AIGC图片元数据标识工具类
 * 严格遵循XMP规范和TC260标准 - 仅使用XMP隐式标识方案
 */
public class AIGCMetadataWriter {

    private static Logger logger = LoggerFactory.getLogger(AIGCMetadataWriter.class);

    // TC260命名空间URI - 严格按规范定义
    private static final String TC260_NAMESPACE_URI = "http://www.tc260.org.cn/ns/AIGC/1.0/";

    // 标准XMP命名空间
    private static final String XMP_NAMESPACE_URI = "http://ns.adobe.com/xap/1.0/";
    private static final String RDF_NAMESPACE_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String DC_NAMESPACE_URI = "http://purl.org/dc/elements/1.1/";

    /**
     * AIGC元数据对象 - 严格按附录E格式
     */
    public static class AIGCMetadata {
        private String label;
        private String contentProducer;
        private String produceID;
        private String reservedCode1;
        private String contentPropagator;
        private String propagateID;
        private String reservedCode2;

        public AIGCMetadata(String label, String contentProducer, String produceID) {
            this.label = label != null ? label : "";
            this.contentProducer = contentProducer != null ? contentProducer : "";
            this.produceID = produceID != null ? produceID : "";
            this.reservedCode1 = "";
            this.contentPropagator = "";
            this.propagateID = "";
            this.reservedCode2 = "";
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

        /**
         * 生成符合附录E规定的JSON字符串
         * 严格按照XMP方案要求，不添加外层包装
         */
        public String toJsonString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"Label\":").append(quoteJsonValue(label)).append(",");
            sb.append("\"ContentProducer\":").append(quoteJsonValue(contentProducer)).append(",");
            sb.append("\"ProduceID\":").append(quoteJsonValue(produceID)).append(",");
            sb.append("\"ReservedCode1\":").append(quoteJsonValue(reservedCode1)).append(",");
            sb.append("\"ContentPropagator\":").append(quoteJsonValue(contentPropagator)).append(",");
            sb.append("\"PropagateID\":").append(quoteJsonValue(propagateID)).append(",");
            sb.append("\"ReservedCode2\":").append(quoteJsonValue(reservedCode2));
            sb.append("}");
            return sb.toString();
        }

        private String quoteJsonValue(String value) {
            if (value == null || value.isEmpty()) {
                return "\"\"";
            }
            // JSON转义：引号、反斜杠、控制字符
            return "\"" + value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t") + "\"";
        }

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
                    '}';
        }
    }

    /**
     * 为JPEG文件写入AIGC元数据 - 严格按规范写入APP1段XMP字段
     * 符合规范6.c)1)要求：将元数据写入APP1中标签名为XMP的字段
     */
    public static boolean writeAIGCMetadataToJPEG(File inputFile, File outputFile, AIGCMetadata metadata){
        try {
            if (!inputFile.exists()) {
                throw new FileNotFoundException("输入文件不存在: " + inputFile.getAbsolutePath());
            }

            if (inputFile.equals(outputFile)) {
                throw new IllegalArgumentException("输入文件和输出文件不能相同");
            }

            byte[] fileData = readFileToByteArray(inputFile);

            // 验证JPEG文件头
            if (!isValidJPEG(fileData)) {
                throw new IOException("不是有效的JPEG文件");
            }

            // 生成标准XMP数据
            String xmpContent = createStandardXMPContent(metadata);
            byte[] xmpBytes = xmpContent.getBytes(StandardCharsets.UTF_8);

            // 创建新的JPEG数据
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(fileData, 0, 2); // 写入SOI (FF D8)

            int position = 2;
            boolean xmpInserted = false;
            boolean foundAppSegment = false;

            // 遍历JPEG段，查找现有的XMP段或合适的插入位置
            while (position < fileData.length - 1) {
                if (fileData[position] == (byte)0xFF) {
                    int marker = fileData[position + 1] & 0xFF;

                    // 检查文件结束标记
                    if (marker == 0xD9) { // EOI
                        break;
                    }

                    // 检查APP1段 (XMP)
                    if (marker == 0xE1) {
                        if (isXMPSegment(fileData, position)) {
                            // 找到现有XMP段，按规范要求更新它（按属性更新，不整体覆盖）
                            byte[] updatedXMP = updateExistingXMP(fileData, position, metadata);
                            output.write(updatedXMP);
                            xmpInserted = true;
                            position = skipSegment(fileData, position);
                            continue;
                        }
                    }

                    // 标记已找到APP段
                    if (marker >= 0xE0 && marker <= 0xEF) {
                        foundAppSegment = true;
                    }

                    // 在第一个APP段后插入XMP（如果没有找到现有XMP）
                    if (foundAppSegment && !xmpInserted && marker >= 0xE0 && marker <= 0xEF) {
                        // 写入当前APP段
                        int segmentEnd = writeSegment(output, fileData, position);

                        // 插入XMP APP1段
                        writeXMPApp1Segment(output, xmpBytes);
                        xmpInserted = true;

                        position = segmentEnd;
                        continue;
                    }

                    // 写入其他段
                    int segmentEnd = writeSegment(output, fileData, position);
                    position = segmentEnd;
                } else {
                    output.write(fileData[position]);
                    position++;
                }
            }

            // 如果没有找到合适的插入点，在SOI后直接插入
            if (!xmpInserted) {
                ByteArrayOutputStream temp = new ByteArrayOutputStream();
                temp.write(fileData, 0, 2); // SOI
                writeXMPApp1Segment(temp, xmpBytes);
                temp.write(fileData, 2, fileData.length - 2);
                writeByteArrayToFile(outputFile, temp.toByteArray());
            } else {
                writeByteArrayToFile(outputFile, output.toByteArray());
            }
            return true;
        }catch (Exception e){
            logger.error("写入AIGC元数据失败", e);
            return false;
        }
    }

    /**
     * 为PNG文件写入AIGC元数据 - 严格按规范写入iTXt段XMP字段
     * 符合规范6.c)2)要求：将元数据写入类型为iTXt的XMP字段
     */
    public static void writeAIGCMetadataToPNG(File inputFile, File outputFile, AIGCMetadata metadata) throws IOException {
        if (!inputFile.exists()) {
            throw new FileNotFoundException("输入文件不存在: " + inputFile.getAbsolutePath());
        }

        if (inputFile.equals(outputFile)) {
            throw new IllegalArgumentException("输入文件和输出文件不能相同");
        }

        byte[] fileData = readFileToByteArray(inputFile);

        // 验证PNG文件头
        if (!isValidPNG(fileData)) {
            throw new IOException("不是有效的PNG文件");
        }

        String xmpContent = createStandardXMPContent(metadata);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(fileData, 0, 8); // PNG签名

        int position = 8;
        boolean xmpInserted = false;
        boolean passedIHDR = false;

        while (position < fileData.length) {
            if (position + 8 > fileData.length) break;

            int chunkLength = readIntBE(fileData, position);
            String chunkType = new String(fileData, position + 4, 4, StandardCharsets.US_ASCII);

            // 检查块长度有效性
            if (chunkLength < 0 || position + 12 + chunkLength > fileData.length) {
                break;
            }

            // 标记已通过IHDR块
            if ("IHDR".equals(chunkType)) {
                passedIHDR = true;
            }

            // 检查现有XMP块
            if ("iTXt".equals(chunkType) && isXMPChunk(fileData, position)) {
                // 更新现有XMP块 - 按属性更新，不整体覆盖
                byte[] updatedChunk = updateXMPChunk(fileData, position, metadata);
                output.write(updatedChunk);
                xmpInserted = true;
                position += 12 + chunkLength;
                continue;
            }

            // 在IHDR后、IDAT前插入新XMP块
            if (passedIHDR && !xmpInserted && !"IDAT".equals(chunkType) && !"IEND".equals(chunkType)) {
                writeXMPITXtChunk(output, xmpContent);
                xmpInserted = true;
            }

            // 写入当前块
            int chunkEnd = position + 12 + chunkLength;
            if (chunkEnd <= fileData.length) {
                output.write(fileData, position, 12 + chunkLength);
                position = chunkEnd;
            } else {
                break;
            }
        }

        // 如果还没有插入XMP，在文件末尾前插入
        if (!xmpInserted) {
            // 回退到IEND前
            output.reset();
            output.write(fileData, 0, 8);
            position = 8;

            while (position < fileData.length - 12) {
                if (position + 8 > fileData.length) break;

                int chunkLength = readIntBE(fileData, position);
                String chunkType = new String(fileData, position + 4, 4, StandardCharsets.US_ASCII);

                if ("IEND".equals(chunkType)) {
                    // 在IEND前插入XMP
                    writeXMPITXtChunk(output, xmpContent);
                }

                int chunkEnd = position + 12 + chunkLength;
                if (chunkEnd <= fileData.length) {
                    output.write(fileData, position, 12 + chunkLength);
                    position = chunkEnd;
                } else {
                    break;
                }
            }
        }

        writeByteArrayToFile(outputFile, output.toByteArray());
    }

    /**
     * 创建符合XMP标准的完整XMP内容
     * 严格遵循规范6.a)和6.b)要求
     */
    private static String createStandardXMPContent(AIGCMetadata metadata) {
        return "<?xpacket begin=\"\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n" +
                "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\" x:xmptk=\"Adobe XMP Core 5.6-c148 79.164036, 2019/08/13-01:06:57\">\n" +
                " <rdf:RDF xmlns:rdf=\"" + RDF_NAMESPACE_URI + "\">\n" +
                "  <rdf:Description rdf:about=\"\"\n" +
                "    xmlns:dc=\"" + DC_NAMESPACE_URI + "\"\n" +
                "    xmlns:TC260=\"" + TC260_NAMESPACE_URI + "\">\n" +
                "   <dc:title>BoGuan AI Model</dc:title>\n" +
                "   <TC260:AIGC>" + metadata.toJsonString() + "</TC260:AIGC>\n" +
                "  </rdf:Description>\n" +
                " </rdf:RDF>\n" +
                "</x:xmpmeta>\n" +
                "<?xpacket end=\"w\"?>";
    }

    /**
     * 更新现有的XMP段 - 严格按规范要求：按属性更新，不整体覆盖
     */
    private static byte[] updateExistingXMP(byte[] fileData, int position, AIGCMetadata metadata) throws IOException {
        // 提取现有XMP内容
        String existingXMP = extractXMPContent(fileData, position);
        if (existingXMP.isEmpty()) {
            throw new IOException("无法提取现有XMP内容");
        }

        // 解析并更新XMP - 按属性更新TC260命名空间
        String updatedXMP = mergeAIGCIntoXMP(existingXMP, metadata);
        byte[] updatedXMPBytes = updatedXMP.getBytes(StandardCharsets.UTF_8);

        // 创建新的APP1段
        return createXMPApp1Segment(updatedXMPBytes);
    }

    /**
     * 将AIGC数据合并到现有XMP中 - 按属性更新TC260命名空间
     */
    private static String mergeAIGCIntoXMP(String existingXMP, AIGCMetadata metadata) {
        // 检查是否已有TC260命名空间声明
        if (!existingXMP.contains("xmlns:TC260")) {
            // 在rdf:Description中添加TC260命名空间声明
            existingXMP = existingXMP.replaceFirst(
                    "(<rdf:Description[^>]*)",
                    "$1 xmlns:TC260=\"" + TC260_NAMESPACE_URI + "\""
            );
        }

        // 移除现有的AIGC元素（如果存在）- 实现按属性更新
        existingXMP = existingXMP.replaceAll("<TC260:AIGC>.*?</TC260:AIGC>", "");

        // 在rdf:Description内添加新的AIGC元素
        String aigcElement = "<TC260:AIGC>" + metadata.toJsonString() + "</TC260:AIGC>";
        existingXMP = existingXMP.replaceFirst(
                "(</rdf:Description>)",
                aigcElement + "\n   $1"
        );

        return existingXMP;
    }

    /**
     * 创建XMP APP1段
     */
    private static byte[] createXMPApp1Segment(byte[] xmpData) throws IOException {
        ByteArrayOutputStream segment = new ByteArrayOutputStream();

        // APP1标记
        segment.write(0xFF);
        segment.write(0xE1);

        // 段长度 (包括长度字段本身)
        int segmentLength = 2 + 29 + xmpData.length;
        segment.write((segmentLength >> 8) & 0xFF);
        segment.write(segmentLength & 0xFF);

        // XMP标识符 (29字节，包括null终止符)
        byte[] identifier = "http://ns.adobe.com/xap/1.0/\0".getBytes(StandardCharsets.UTF_8);
        if (identifier.length > 29) {
            throw new IOException("XMP标识符长度超过29字节");
        }

        byte[] paddedIdentifier = new byte[29];
        System.arraycopy(identifier, 0, paddedIdentifier, 0, identifier.length);
        segment.write(paddedIdentifier);

        // XMP数据
        segment.write(xmpData);

        return segment.toByteArray();
    }

    /**
     * 写入XMP APP1段到输出流
     */
    private static void writeXMPApp1Segment(OutputStream output, byte[] xmpData) throws IOException {
        byte[] segment = createXMPApp1Segment(xmpData);
        output.write(segment);
    }

    /**
     * 写入XMP iTXt块到PNG
     */
    private static void writeXMPITXtChunk(OutputStream output, String xmpContent) throws IOException {
        byte[] keyword = "XML:com.adobe.xmp".getBytes(StandardCharsets.UTF_8);
        byte[] content = xmpContent.getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream chunkData = new ByteArrayOutputStream();
        chunkData.write(keyword);
        chunkData.write(0x00); // 空终止符
        chunkData.write(0); // 压缩标志 (0=未压缩)
        chunkData.write(0); // 压缩方法
        chunkData.write(0x00); // 语言标签空终止符
        chunkData.write(0x00); // 转换关键字空终止符
        chunkData.write(content);

        byte[] dataBytes = chunkData.toByteArray();

        // 写入块长度
        writeIntBE(output, dataBytes.length);

        // 写入块类型
        output.write("iTXt".getBytes(StandardCharsets.US_ASCII));

        // 写入块数据
        output.write(dataBytes);

        // 计算并写入CRC
        CRC32 crc = new CRC32();
        crc.update("iTXt".getBytes(StandardCharsets.US_ASCII));
        crc.update(dataBytes);
        writeIntBE(output, (int) crc.getValue());
    }

    /**
     * 更新PNG中的XMP块
     */
    private static byte[] updateXMPChunk(byte[] data, int position, AIGCMetadata metadata) throws IOException {
        String existingXMP = extractXMPContentFromChunk(data, position);
        String updatedXMP = mergeAIGCIntoXMP(existingXMP, metadata);
        return createXMPChunk(updatedXMP);
    }

    /**
     * 创建XMP iTXt块
     */
    private static byte[] createXMPChunk(String xmpContent) throws IOException {
        ByteArrayOutputStream chunk = new ByteArrayOutputStream();
        byte[] content = xmpContent.getBytes(StandardCharsets.UTF_8);
        byte[] keyword = "XML:com.adobe.xmp".getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream chunkData = new ByteArrayOutputStream();
        chunkData.write(keyword);
        chunkData.write(0x00); // 空终止符
        chunkData.write(0); // 压缩标志
        chunkData.write(0); // 压缩方法
        chunkData.write(0x00); // 语言标签空终止符
        chunkData.write(0x00); // 转换关键字空终止符
        chunkData.write(content);

        byte[] dataBytes = chunkData.toByteArray();

        writeIntBE(chunk, dataBytes.length);
        chunk.write("iTXt".getBytes(StandardCharsets.US_ASCII));
        chunk.write(dataBytes);

        CRC32 crc = new CRC32();
        crc.update("iTXt".getBytes(StandardCharsets.US_ASCII));
        crc.update(dataBytes);
        writeIntBE(chunk, (int) crc.getValue());

        return chunk.toByteArray();
    }

    // ========== 辅助方法 ==========

    private static boolean isValidJPEG(byte[] data) {
        return data.length >= 2 && data[0] == (byte)0xFF && data[1] == (byte)0xD8;
    }

    private static boolean isValidPNG(byte[] data) {
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

    private static int writeSegment(OutputStream output, byte[] data, int position) throws IOException {
        if (position + 4 > data.length) {
            output.write(data, position, data.length - position);
            return data.length;
        }

        int segmentLength = ((data[position + 2] & 0xFF) << 8) | (data[position + 3] & 0xFF);
        int segmentEnd = position + 2 + segmentLength;

        if (segmentEnd <= data.length) {
            output.write(data, position, 2 + segmentLength);
            return segmentEnd;
        } else {
            output.write(data, position, data.length - position);
            return data.length;
        }
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

    private static void writeIntBE(OutputStream output, int value) throws IOException {
        output.write((value >> 24) & 0xFF);
        output.write((value >> 16) & 0xFF);
        output.write((value >> 8) & 0xFF);
        output.write(value & 0xFF);
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

    private static void writeByteArrayToFile(File file, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        try {
            // 创建符合规范的测试元数据
            AIGCMetadata metadata = new AIGCMetadata("1", "001191610133596325171T12566", "BGIMGPD20250001");
            metadata.setReservedCode1("BGAI001");
            metadata.setContentPropagator("001191610133596325171T22588");
            metadata.setPropagateID("BGIMGPPG20250001");
            metadata.setReservedCode2("BGAI002");

            System.out.println("AIGC JSON: " + metadata.toJsonString());
            System.out.println("TC260命名空间: " + TC260_NAMESPACE_URI);

            // 测试JPEG写入
            File jpegFile = new File("D:\\data\\watermark\\xk.jpg");
            if (jpegFile.exists()) {
                File jpegOutput = new File("D:\\data\\watermark\\XK-AIGC.jpg");
                boolean result = writeAIGCMetadataToJPEG(jpegFile, jpegOutput, metadata);
                if (!result) {
                    System.out.println("JPEG AIGC元数据写入失败 - 符合规范6.c)1)");
                }else{
                    System.out.println("JPEG AIGC元数据写入成功: " + jpegOutput.getAbsolutePath());
                }
            } else {
                System.out.println("JPEG测试文件不存在: " + jpegFile.getAbsolutePath());
            }

            // 测试PNG写入
            File pngFile = new File("D:\\data\\watermark\\xn.png");
            if (pngFile.exists()) {
                File pngOutput = new File("D:\\data\\watermark\\XN-AIGC.png");
                writeAIGCMetadataToPNG(pngFile, pngOutput, metadata);
                System.out.println("PNG AIGC元数据写入完成 - 符合规范6.c)2)");
            } else {
                System.out.println("PNG测试文件不存在: " + pngFile.getAbsolutePath());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}