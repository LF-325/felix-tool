package com.felix;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图片文字生成器 - 用于在模板图片上动态填充文字和二维码
 */
public class ImageTextGenerator {
    
    /**
     * 在模板图片上添加文字和二维码
     * @param templatePath 模板图片路径
     * @param outputPath 输出图片路径
     * @param textElements 要添加的文字元素列表
     * @param qrCodeContent 二维码内容
     * @param qrCodePosition 二维码位置 (x, y, width, height)
     * @throws IOException 图片处理异常
     * @throws WriterException 二维码生成异常
     */
    public static void generateImageWithTextAndQRCode(String templatePath, String outputPath,
                                                    TextElement[] textElements, String qrCodeContent, int[] qrCodePosition)
            throws IOException, WriterException {
        // 读取模板图片
        BufferedImage templateImage = ImageIO.read(new File(templatePath));
        Graphics2D g2d = templateImage.createGraphics();
        
        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // 添加所有文字元素
        for (TextElement element : textElements) {
            drawText(g2d, element);
        }
        
        // 生成并添加二维码
        if (qrCodeContent != null && qrCodePosition != null && qrCodePosition.length == 4) {
            BufferedImage qrCodeImage = generateQRCodeImage(qrCodeContent, qrCodePosition[2], qrCodePosition[3]);
            g2d.drawImage(qrCodeImage, qrCodePosition[0], qrCodePosition[1], null);
        }
        
        // 释放资源
        g2d.dispose();
        
        // 保存处理后的图片
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();
        ImageIO.write(templateImage, "png", outputFile);
    }
    
    /**
     * 在图片上绘制文字（支持换行）
     */
    private static void drawText(Graphics2D g2d, TextElement element) {
        // 设置字体
        Font font = new Font(element.getFontName(), element.getFontStyle(), element.getFontSize());
        g2d.setFont(font);
        
        // 设置文字颜色
        g2d.setColor(element.getColor());
        
        FontMetrics metrics = g2d.getFontMetrics(font);
        int lineHeight = metrics.getHeight();
        
        // 如果设置了最大宽度，则进行自动换行
        if (element.getMaxWidth() > 0) {
            List<String> lines = wrapText(element.getText(), metrics, element.getMaxWidth());
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int x = element.getX();
                int y = element.getY() + (i * lineHeight) + metrics.getAscent();
                
                switch (element.getTextAlignment()) {
                    case CENTER:
                        x = x - metrics.stringWidth(line) / 2;
                        break;
                    case RIGHT:
                        x = x - metrics.stringWidth(line);
                        break;
                    // 默认左对齐
                }
                
                g2d.drawString(line, x, y);
            }
        } else {
            // 不换行的情况
            int x = element.getX();
            int y = element.getY() + metrics.getAscent();
            
            switch (element.getTextAlignment()) {
                case CENTER:
                    x = x - metrics.stringWidth(element.getText()) / 2;
                    break;
                case RIGHT:
                    x = x - metrics.stringWidth(element.getText());
                    break;
                // 默认左对齐
            }
            
            g2d.drawString(element.getText(), x, y);
        }
    }
    
    /**
     * 自动换行处理（改进版，支持中文和特殊符号）
     */
    private static List<String> wrapText(String text, FontMetrics metrics, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String testLine = currentLine.toString() + c;
            
            if (metrics.stringWidth(testLine) > maxWidth && currentLine.length() > 0) {
                // 找到最后一个分隔符的位置
                int lastSeparatorIndex = -1;
                for (int j = currentLine.length() - 1; j >= 0; j--) {
                    char ch = currentLine.charAt(j);
                    if (ch == '→' || ch == ' ' || ch == ',' || ch == '，' || ch == '。' || ch == '.') {
                        lastSeparatorIndex = j;
                        break;
                    }
                }
                
                if (lastSeparatorIndex > 0) {
                    // 在分隔符处换行
                    lines.add(currentLine.substring(0, lastSeparatorIndex + 1));
                    currentLine = new StringBuilder(currentLine.substring(lastSeparatorIndex + 1) + c);
                } else {
                    // 没有找到合适的分隔符，强制换行
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(String.valueOf(c));
                }
            } else {
                currentLine.append(c);
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    /**
     * 生成二维码图片
     */
    private static BufferedImage generateQRCodeImage(String content, int width, int height) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE,
                width, height, hints);
        
        BufferedImage qrImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                qrImage.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        
        return qrImage;
    }
    
    /**
     * 文字元素类 - 用于定义要添加的文字属性
     */
    public static class TextElement {
        public enum TextAlignment { LEFT, CENTER, RIGHT }
        
        private String text;
        private int x, y;
        private String fontName;
        private int fontStyle;
        private int fontSize;
        private Color color;
        private TextAlignment textAlignment;
        private int maxWidth; // 添加最大宽度属性，用于自动换行
        
        // 构造函数
        public TextElement(String text, int x, int y) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.fontName = "宋体";
            this.fontStyle = Font.PLAIN;
            this.fontSize = 12;
            this.color = Color.BLACK;
            this.textAlignment = TextAlignment.LEFT;
            this.maxWidth = 0; // 默认不换行
        }
        
        // 链式调用设置方法
        public TextElement setFontName(String fontName) {
            this.fontName = fontName;
            return this;
        }
        
        public TextElement setFontStyle(int fontStyle) {
            this.fontStyle = fontStyle;
            return this;
        }
        
        public TextElement setFontSize(int fontSize) {
            this.fontSize = fontSize;
            return this;
        }
        
        public TextElement setColor(Color color) {
            this.color = color;
            return this;
        }
        
        public TextElement setTextAlignment(TextAlignment textAlignment) {
            this.textAlignment = textAlignment;
            return this;
        }
        
        // 添加最大宽度设置，用于自动换行
        public TextElement setMaxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }
        
        // Getter方法
        public String getText() { return text; }
        public int getX() { return x; }
        public int getY() { return y; }
        public String getFontName() { return fontName; }
        public int getFontStyle() { return fontStyle; }
        public int getFontSize() { return fontSize; }
        public Color getColor() { return color; }
        public TextAlignment getTextAlignment() { return textAlignment; }
        public int getMaxWidth() { return maxWidth; }
    }
    
    /**
     * 便捷方法：创建默认样式的文字元素
     */
    public static TextElement createTextElement(String text, int x, int y) {
        return new TextElement(text, x, y);
    }
    
    /**
     * 便捷方法：创建带有自定义样式的文字元素
     */
    public static TextElement createTextElement(String text, int x, int y, String fontName, int fontSize, Color color) {
        return new TextElement(text, x, y)
                .setFontName(fontName)
                .setFontSize(fontSize)
                .setColor(color);
    }
    
    /**
     * 测试方法
     */
    public static void main(String[] args) {
        try {
            String templatePath = "D:\\data\\plan\\tpl.png";
            String outputPath = "D:\\data\\plan\\tpl-3.png";
            
            // 创建文字元素
            // 假设模板宽度约为400px，根据实际模板调整坐标
            int contentWidth = 320; // 内容区域宽度
            int centerX = 200; // 居中对齐的中心点X坐标
            int leftMargin = 50; // 左侧边距
            
            TextElement[] textElements = {
                // 用户信息
                createTextElement("小美用AI旅行搭子生成了一段超赞的行程攻略", leftMargin, 100)
                    .setFontSize(12)
                        .setFontStyle(Font.BOLD),
                createTextElement("从上海到西安三天两晚3人亲子游,总预算为3000元,", leftMargin, 150)
                    .setFontSize(12),
                createTextElement("想体验必玩景点与拍照出片", leftMargin, 170)
                    .setFontSize(12),
                
                // 标题
                createTextElement("西安三天旅行计划", centerX, 320)
                    .setFontName("微软雅黑")
                    .setFontSize(20)
                    .setFontStyle(Font.BOLD)
                    .setTextAlignment(TextElement.TextAlignment.CENTER),
                // 副标题
                createTextElement("(共3天 | 8个行程)", centerX, 360)
                    .setFontSize(12)
                    .setTextAlignment(TextElement.TextAlignment.CENTER),
                
                // DAY1行程
                createTextElement("DAY1", leftMargin, 400)
                    .setFontStyle(Font.BOLD),
                createTextElement("西安亚朵酒店 → 三根电杆映茶馆 → 西安城墙 → 西安钟楼 → 大唐芙蓉园 → 回民街", leftMargin, 420)
                    .setMaxWidth(contentWidth),
                
                // DAY2行程
                createTextElement("DAY2", leftMargin, 500)
                    .setFontStyle(Font.BOLD),
                createTextElement("西安亚朵酒店 → 三根电杆映茶馆 → 西安城墙 → 西安钟楼 → 大唐芙蓉园 → 回民街", leftMargin, 520)
                    .setMaxWidth(contentWidth),
                
                // DAY3行程
                createTextElement("DAY3", leftMargin, 600)
                    .setFontStyle(Font.BOLD),
                createTextElement("西安亚朵酒店 → 三根电杆映茶馆 → 西安城墙 → 西安钟楼 → 大唐芙蓉园 → 回民街", leftMargin, 620)
                    .setMaxWidth(contentWidth),
                
            };
            
            // 二维码位置和大小 (x, y, width, height) 
            int[] qrCodePosition = {200, 850, 103, 103}; // 调整二维码位置到底部框中
            
            // 生成图片
            generateImageWithTextAndQRCode(templatePath, outputPath, textElements, "https://example.com/travel", qrCodePosition);
            
            System.out.println("图片生成成功: " + outputPath);
        } catch (Exception e) {
            System.err.println("生成图片时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}