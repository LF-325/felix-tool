package com.felix.watermark.metadata.video;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * FFmpeg元数据标记工具类
 * 提供添加、读取和验证隐式标识的功能
 * @author liufei
 */
public class VideoMetadataHandler {

    private static final String FFMPEG_DIR = "";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 添加隐式元数据标识到视频文件
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param metadata 隐式标识信息
     * @return 处理成功返回true，否则返回false
     */
    public static boolean addStealthMetadata(String inputPath, String outputPath, AIGCMetadata metadata) {
        try {
            // 构建FFmpeg命令，确保参数顺序正确
            List<String> command = new ArrayList<>();
            command.add(FFMPEG_DIR+"ffmpeg");
            command.add("-i");
            command.add(inputPath);

            // 添加标准元数据字段
            command.add("-metadata");
            // 使用适用于MP4/MOV格式的JSON格式
            command.add(escapeMetadataValue("AIGC=" + metadata.toVideoJsonString()));

            command.add("-movflags");
            command.add("use_metadata_tags");

            // 使用流复制，不重新编码以保持质量
            command.add("-c");
            command.add("copy");
            
            // 覆盖输出文件
            command.add("-y");
            
            // 输出文件必须是最后一个参数
            command.add(outputPath);

            // 执行命令
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            boolean success = exitCode == 0;

            if (!success) {
                System.err.println("FFmpeg执行失败，退出码: " + exitCode);
                System.err.println("输出: " + output.toString());
            }

            return success;

        } catch (IOException | InterruptedException e) {
            System.err.println("添加元数据时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 转义元数据值中的特殊字符
     */
    private static String escapeMetadataValue(String value) {
        // 在Windows命令行中，如果值包含特殊字符，需要用引号包裹
        if (value.contains(" ") || value.contains(",") || value.contains("=") || value.contains(":")) {
            // 转义内部引号并包裹外部引号
            value = value.replace("\"", "\\\"");
            return "\"" + value + "\"";
        }
        return value;
    }


    /**
     * 读取视频文件中的隐式标识信息
     * @param videoPath 视频文件路径
     * @return 隐式标识信息，如果不存在返回null
     */
    public static AIGCMetadata readStealthMetadata(String videoPath) {
        try {
            // 使用FFprobe读取元数据[citation:8]
            List<String> command = Arrays.asList(
                    FFMPEG_DIR+"ffprobe",
                    "-v", "quiet",
                    "-print_format", "json",
                    "-show_format",
                    "-show_streams",
                    videoPath
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();

            String jsonOutput;
            try (InputStream inputStream = process.getInputStream();
                 Scanner scanner = new Scanner(inputStream).useDelimiter("\\A")) {
                jsonOutput = scanner.hasNext() ? scanner.next() : "";
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("FFprobe执行失败，退出码: " + exitCode);
                return null;
            }

            return parseMetadataFromJson(jsonOutput);

        } catch (IOException | InterruptedException e) {
            System.err.println("读取元数据时发生错误: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从JSON输出中解析元数据
     */
    private static AIGCMetadata parseMetadataFromJson(String jsonOutput) {
        AIGCMetadata metadata = null;
        try {
            JsonNode rootNode = objectMapper.readTree(jsonOutput);
            JsonNode formatNode = rootNode.path("format");
            JsonNode tagsNode = formatNode.path("tags");

            // 首先尝试从标准字段读取
            if (tagsNode.has("AIGC")) {
                String data = tagsNode.get("AIGC").asText();
                metadata = JSON.parseObject(data, AIGCMetadata.class);
            }
        } catch (Exception e) {
            System.err.println("解析元数据JSON时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return metadata;
    }


    /**
     * 批量处理视频文件
     * @param inputDir 输入目录
     * @param outputDir 输出目录
     * @param metadata 隐式标识信息
     */
    public void batchProcessVideos(String inputDir, String outputDir, AIGCMetadata metadata) {
        File directory = new File(inputDir);
        File[] videoFiles = directory.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".mp4") || lower.endsWith(".avi") ||
                    lower.endsWith(".mov") || lower.endsWith(".mkv");
        });

        if (videoFiles != null) {
            System.out.println("找到 " + videoFiles.length + " 个视频文件需要处理");

            for (File videoFile : videoFiles) {
                String outputPath = outputDir + File.separator + videoFile.getName();

                System.out.println("处理文件: " + videoFile.getName());
                boolean success = addStealthMetadata(videoFile.getAbsolutePath(), outputPath, metadata);
                System.out.println("文件 " + videoFile.getName() + " 处理结果: " + (success ? "成功" : "失败"));
            }
        } else {
            System.out.println("输入目录中没有找到视频文件");
        }
    }

    /**
     * AIGC元数据信息实体类
     * 符合GB 45438-2025附录E规范
     */
    public static class AIGCMetadata {
        private String label;           // value1
        private String contentProducer; // value2
        private String produceID;       // value3
        private String reservedCode1;   // value4
        private String contentPropagator; // value5
        private String propagateID;     // value6
        private String reservedCode2;   // value7

        // 构造器
        public AIGCMetadata(String label, String contentProducer, String produceID,
                            String reservedCode1, String contentPropagator,
                            String propagateID, String reservedCode2) {
            this.label = label;
            this.contentProducer = contentProducer;
            this.produceID = produceID;
            this.reservedCode1 = reservedCode1;
            this.contentPropagator = contentPropagator;
            this.propagateID = propagateID;
            this.reservedCode2 = reservedCode2;
        }

        public AIGCMetadata() {

        }

        // Getters and Setters
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public String getContentProducer() { return contentProducer; }
        public void setContentProducer(String contentProducer) { this.contentProducer = contentProducer; }

        public String getProduceID() { return produceID; }
        public void setProduceID(String produceID) { this.produceID = produceID; }

        public String getReservedCode1() { return reservedCode1; }
        public void setReservedCode1(String reservedCode1) { this.reservedCode1 = reservedCode1; }

        public String getContentPropagator() { return contentPropagator; }
        public void setContentPropagator(String contentPropagator) { this.contentPropagator = contentPropagator; }

        public String getPropagateID() { return propagateID; }
        public void setPropagateID(String propagateID) { this.propagateID = propagateID; }

        public String getReservedCode2() { return reservedCode2; }
        public void setReservedCode2(String reservedCode2) { this.reservedCode2 = reservedCode2; }

        public String toVideoJsonString() {
            return String.format(
                    "{\"Label\":\"%s\",\"ContentProducer\":\"%s\",\"ProduceID\":\"%s\"," +
                            "\"ReservedCode1\":\"%s\",\"ContentPropagator\":\"%s\",\"PropagateID\":\"%s\"," +
                            "\"ReservedCode2\":\"%s\"}",
                    escapeJson(label), escapeJson(contentProducer), escapeJson(produceID),
                    escapeJson(reservedCode1), escapeJson(contentPropagator), escapeJson(propagateID),
                    escapeJson(reservedCode2)
            );
        }

        /**
         * JSON字符串转义处理
         */
        private String escapeJson(String str) {
            if (str == null) return "";
            return str.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }

        @Override
        public String toString() {
            return "AIGCMetadata{" +
                    "Label='" + label + '\'' +
                    ", ContentProducer='" + contentProducer + '\'' +
                    ", ProduceID='" + produceID + '\'' +
                    ", ReservedCode1='" + reservedCode1 + '\'' +
                    ", ContentPropagator='" + contentPropagator + '\'' +
                    ", PropagateID='" + propagateID + '\'' +
                    ", ReservedCode2='" + reservedCode2 + '\'' +
                    '}';
        }
    }


    public static void main(String[] args) {
        // 创建AIGC元数据
        AIGCMetadata metadata = new AIGCMetadata(
                "1",                // Label
                "001191610133596325171T12566",        // ContentProducer，值固定，不变
                "BGVIDEO20250001",        // ProduceID
                "BGAI001",                  // ReservedCode1
                "001191610133596325171T22588",       // ContentPropagator，值固定，不变
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
            System.out.println("视频元数据水印解析结果: " + metadataRead.toVideoJsonString());
        }else{
            System.out.println("元数据为空");
        }
    }

}
