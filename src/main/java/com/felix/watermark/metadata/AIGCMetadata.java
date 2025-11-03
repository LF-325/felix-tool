package com.felix.watermark.metadata;

/**
 * AIGC元数据信息实体类
 * 符合GB 45438-2025附录E规范
 */
public class AIGCMetadata {
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

    public AIGCMetadata(String label, String contentProducer, String produceID) {
        this.label = label;
        this.contentProducer = contentProducer;
        this.produceID = produceID;
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

    /**
     * 生成符合GB 45438-2025附录E规定的JSON字符串
     */
    public String toJsonString() {
        return String.format(
                "{\"AIGC\":{\"Label\":\"%s\",\"ContentProducer\":\"%s\",\"ProduceID\":\"%s\"," +
                        "\"ReservedCode1\":\"%s\",\"ContentPropagator\":\"%s\",\"PropagateID\":\"%s\"," +
                        "\"ReservedCode2\":\"%s\"}}",
                escapeJson(label), escapeJson(contentProducer), escapeJson(produceID),
                escapeJson(reservedCode1), escapeJson(contentPropagator), escapeJson(propagateID),
                escapeJson(reservedCode2)
        );
    }

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

    /**
     * 从JSON字符串解析AIGC元数据
     */
    public static AIGCMetadata fromJsonString(String jsonStr) {
        try {
            AIGCMetadata metadata = new AIGCMetadata();

            if (jsonStr != null) {
                // 提取各个字段的值
                String[] pairs = jsonStr.split("\"");
                for (int i = 0; i < pairs.length - 1; i++) {
                    if (pairs[i].trim().equals("Label") && i + 2 < pairs.length) {
                        metadata.setLabel(unescapeJson(pairs[i + 2]));
                    } else if (pairs[i].trim().equals("ContentProducer") && i + 2 < pairs.length) {
                        metadata.setContentProducer(unescapeJson(pairs[i + 2]));
                    } else if (pairs[i].trim().equals("ProduceID") && i + 2 < pairs.length) {
                        metadata.setProduceID(unescapeJson(pairs[i + 2]));
                    } else if (pairs[i].trim().equals("ReservedCode1") && i + 2 < pairs.length) {
                        metadata.setReservedCode1(unescapeJson(pairs[i + 2]));
                    } else if (pairs[i].trim().equals("ContentPropagator") && i + 2 < pairs.length) {
                        metadata.setContentPropagator(unescapeJson(pairs[i + 2]));
                    } else if (pairs[i].trim().equals("PropagateID") && i + 2 < pairs.length) {
                        metadata.setPropagateID(unescapeJson(pairs[i + 2]));
                    } else if (pairs[i].trim().equals("ReservedCode2") && i + 2 < pairs.length) {
                        metadata.setReservedCode2(unescapeJson(pairs[i + 2]));
                    }
                }
            }

            return metadata;

        } catch (Exception e) {
            System.err.println("解析AIGC JSON失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * JSON字符串反转义处理
     */
    private static String unescapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\b", "\b")
                .replace("\\f", "\f")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
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
