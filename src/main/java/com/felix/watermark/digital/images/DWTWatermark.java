package com.felix.watermark.digital.images;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 基于离散小波变换(DWT)的数字水印工具类
 * @author 刘飞
 * 实现了在图像中嵌入和提取文本水印的功能
 * 支持文件和流两种操作方式
 */
public class DWTWatermark {

    /**
     * 在图像文件中嵌入水印
     * 
     * @param imageFile 原始图像文件
     * @param outputFile 嵌入水印后的输出文件
     * @param watermark 要嵌入的水印文本
     * @throws IOException 如果文件读写过程中发生错误
     */
    public static void embed(File imageFile, File outputFile, String watermark) throws IOException {
        BufferedImage image = ImageIO.read(imageFile);
        int width = image.getWidth();
        int height = image.getHeight();

        // 使用Base64编码水印文本
        String encodedWatermark = Base64.getEncoder().encodeToString(
                watermark.getBytes(StandardCharsets.UTF_8));
        String watermarkBinary = toBinaryString(encodedWatermark);

        int watermarkLength = watermarkBinary.length();
        System.out.println("Base64编码后水印: " + encodedWatermark);
        System.out.println("水印二进制长度: " + watermarkLength + " 位");

        // 计算最大容量
        int subbandWidth = width / 2;
        int subbandHeight = height / 2;
        int capacity = subbandWidth * subbandHeight;
        System.out.println("图像容量: " + capacity + " 位");

        if (watermarkLength > capacity) {
            throw new IllegalArgumentException("水印信息过长，最大容量: " + capacity + " 位");
        }

        // 将图像转换为YUV颜色空间
        double[][][] yuvImage = convertRGBtoYUV(image);
        double[][] yChannel = yuvImage[0];

        // 应用DWT到Y通道
        double[][][] subbands = applyDWT(yChannel);

        // 在HL子带嵌入水印
        double[][] hl = subbands[1];
        int bitIndex = 0;
        for (int y = 0; y < hl.length; y++) {
            for (int x = 0; x < hl[0].length; x++) {
                if (bitIndex >= watermarkLength) break;

                char bit = watermarkBinary.charAt(bitIndex);
                double delta = 12.0; // 增加嵌入强度

                // 使用绝对值+符号法确保提取可靠性
                double absValue = Math.abs(hl[y][x]);
                if (bit == '1') {
                    hl[y][x] = absValue + delta; // 正数表示1
                } else {
                    hl[y][x] = -(absValue + delta); // 负数表示0
                }
                bitIndex++;
            }
        }
        System.out.println("实际嵌入位数: " + bitIndex);

        // 应用逆DWT
        double[][] reconstructedY = applyIDWT(subbands);

        // 更新Y通道
        yuvImage[0] = reconstructedY;

        // 转换回RGB
        BufferedImage watermarkedImage = convertYUVtoRGB(yuvImage, width, height);

        // 保存结果
        ImageIO.write(watermarkedImage, "png", outputFile);
    }

    /**
     * 从图像文件中提取水印
     * 
     * @param watermarkedImage 嵌入水印的图像文件
     * @param binaryLength 水印二进制长度
     * @return 提取的水印文本
     * @throws IOException 如果文件读取过程中发生错误
     */
    public static String extract(File watermarkedImage, int binaryLength) throws IOException {
        BufferedImage image = ImageIO.read(watermarkedImage);
        int width = image.getWidth();
        int height = image.getHeight();

        // 将图像转换为YUV
        double[][][] yuvImage = convertRGBtoYUV(image);

        // 应用DWT到Y通道
        double[][][] subbands = applyDWT(yuvImage[0]);

        // 从HL子带提取水印
        double[][] hl = subbands[1];
        StringBuilder extractedBinary = new StringBuilder();

        // 提取水印位
        for (int y = 0; y < hl.length; y++) {
            for (int x = 0; x < hl[0].length; x++) {
                // 使用符号检测水印位（正数=1，负数=0）
                char bit = hl[y][x] > 0 ? '1' : '0';
                extractedBinary.append(bit);
            }
        }

        System.out.println("提取的总位数: " + extractedBinary.length());

        // 截取有效长度
        String validBinary = extractedBinary.substring(0, Math.min(binaryLength, extractedBinary.length()));
        System.out.println("截取的有效位数: " + validBinary.length());

        // 转换为Base64字符串
        String base64Str = binaryToString(validBinary);
        System.out.println("Base64字符串: " + base64Str);

        try {
            // 解码Base64
            byte[] decodedBytes = Base64.getDecoder().decode(base64Str);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            System.err.println("Base64解码错误: " + e.getMessage());
            // 尝试修复填充问题
            return repairBase64Decoding(base64Str);
        }
    }

    /**
     * 修复Base64解码时的填充问题
     * 
     * @param base64Str 需要修复的Base64字符串
     * @return 修复后的解码结果或错误信息
     */
    private static String repairBase64Decoding(String base64Str) {
        // 尝试添加填充字符
        while (base64Str.length() % 4 != 0) {
            base64Str += "=";
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64Str);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            System.err.println("修复Base64解码失败: " + e.getMessage());
            return "提取失败: " + e.getMessage();
        }
    }

    /**
     * 应用一级离散小波变换(Haar小波)
     * 
     * @param data 输入数据矩阵
     * @return 四个子带系数数组[LL, HL, LH, HH]
     */
    private static double[][][] applyDWT(double[][] data) {
        int width = data[0].length;
        int height = data.length;
        int newWidth = width / 2;
        int newHeight = height / 2;

        double[][] ll = new double[newHeight][newWidth];
        double[][] hl = new double[newHeight][newWidth];
        double[][] lh = new double[newHeight][newWidth];
        double[][] hh = new double[newHeight][newWidth];

        // 水平变换
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < newWidth; x++) {
                double left = data[y][2*x];
                double right = data[y][2*x+1];
                double avg = (left + right) / Math.sqrt(2);
                double diff = (left - right) / Math.sqrt(2);

                if (y < newHeight) {
                    ll[y][x] = avg;
                    hl[y][x] = diff;
                } else {
                    lh[y - newHeight][x] = avg;
                    hh[y - newHeight][x] = diff;
                }
            }
        }

        return new double[][][]{ll, hl, lh, hh};
    }

    /**
     * 应用一级逆离散小波变换
     * 
     * @param subbands 四个子带系数数组[LL, HL, LH, HH]
     * @return 重构后的数据矩阵
     */
    private static double[][] applyIDWT(double[][][] subbands) {
        double[][] ll = subbands[0];
        double[][] hl = subbands[1];
        double[][] lh = subbands[2];
        double[][] hh = subbands[3];

        int width = ll[0].length * 2;
        int height = ll.length * 2;
        double[][] data = new double[height][width];

        // 水平反变换
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width/2; x++) {
                if (y < height/2) {
                    double avg = ll[y][x];
                    double diff = hl[y][x];
                    data[y][2*x] = (avg + diff) / Math.sqrt(2);
                    data[y][2*x+1] = (avg - diff) / Math.sqrt(2);
                } else {
                    int yy = y - height/2;
                    double avg = lh[yy][x];
                    double diff = hh[yy][x];
                    data[y][2*x] = (avg + diff) / Math.sqrt(2);
                    data[y][2*x+1] = (avg - diff) / Math.sqrt(2);
                }
            }
        }

        return data;
    }

    /**
     * 将RGB图像转换为YUV颜色空间
     * 
     * @param image 输入的RGB图像
     * @return YUV三个通道的数据数组[Y, U, V]
     */
    private static double[][][] convertRGBtoYUV(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] y = new double[height][width];
        double[][] u = new double[height][width];
        double[][] v = new double[height][width];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int rgb = image.getRGB(col, row);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // 转换公式
                y[row][col] = 0.299 * r + 0.587 * g + 0.114 * b;
                u[row][col] = -0.147 * r - 0.289 * g + 0.436 * b;
                v[row][col] = 0.615 * r - 0.515 * g - 0.100 * b;
            }
        }

        return new double[][][]{y, u, v};
    }

    /**
     * 将YUV颜色空间转换为RGB图像
     * 
     * @param yuv YUV三个通道的数据数组[Y, U, V]
     * @param width 图像宽度
     * @param height 图像高度
     * @return 转换后的RGB图像
     */
    private static BufferedImage convertYUVtoRGB(double[][][] yuv, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        double[][] y = yuv[0];
        double[][] u = yuv[1];
        double[][] v = yuv[2];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                double yVal = y[row][col];
                double uVal = u[row][col];
                double vVal = v[row][col];

                // 转换公式
                int r = clamp((int)(yVal + 1.140 * vVal));
                int g = clamp((int)(yVal - 0.395 * uVal - 0.581 * vVal));
                int b = clamp((int)(yVal + 2.032 * uVal));

                int rgb = (r << 16) | (g << 8) | b;
                image.setRGB(col, row, rgb);
            }
        }

        return image;
    }

    /**
     * 将文本转换为二进制字符串
     * 
     * @param text 输入文本
     * @return 二进制字符串表示
     */
    public static String toBinaryString(String text) {
        StringBuilder binary = new StringBuilder();
        for (char c : text.toCharArray()) {
            // 确保每个字符转换为8位二进制
            String bin = String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0');
            binary.append(bin);
        }
        return binary.toString();
    }

    /**
     * 将二进制字符串转换为文本
     * 
     * @param binary 二进制字符串
     * @return 转换后的文本
     */
    private static String binaryToString(String binary) {
        StringBuilder text = new StringBuilder();
        // 每8位一组转换为字符
        for (int i = 0; i < binary.length(); i += 8) {
            if (i + 8 > binary.length()) break;
            String byteStr = binary.substring(i, i + 8);
            try {
                text.append((char) Integer.parseInt(byteStr, 2));
            } catch (NumberFormatException e) {
                // 忽略无效字节
            }
        }
        return text.toString();
    }

    /**
     * 将数值限制在0-255范围内
     * 
     * @param value 输入数值
     * @return 限制后的数值
     */
    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /**
     * 基于流的水印嵌入方法
     * 
     * @param imageInputStream 原始图像输入流
     * @param watermark 水印文本
     * @return 嵌入水印后的图像输出流
     * @throws IOException 如果图像流处理发生错误
     */
    public static OutputStream embed(InputStream imageInputStream, String watermark) throws IOException {
        BufferedImage image = ImageIO.read(imageInputStream);
        int width = image.getWidth();
        int height = image.getHeight();

        // 使用Base64编码水印文本
        String encodedWatermark = Base64.getEncoder().encodeToString(
                watermark.getBytes(StandardCharsets.UTF_8));
        String watermarkBinary = toBinaryString(encodedWatermark);

        int watermarkLength = watermarkBinary.length();
        System.out.println("Base64编码后水印: " + encodedWatermark);
        System.out.println("水印二进制长度: " + watermarkLength + " 位");

        // 计算最大容量
        int subbandWidth = width / 2;
        int subbandHeight = height / 2;
        int capacity = subbandWidth * subbandHeight;
        System.out.println("图像容量: " + capacity + " 位");

        if (watermarkLength > capacity) {
            throw new IllegalArgumentException("水印信息过长，最大容量: " + capacity + " 位");
        }

        // 将图像转换为YUV颜色空间
        double[][][] yuvImage = convertRGBtoYUV(image);
        double[][] yChannel = yuvImage[0];

        // 应用DWT到Y通道
        double[][][] subbands = applyDWT(yChannel);

        // 在HL子带嵌入水印
        double[][] hl = subbands[1];
        int bitIndex = 0;
        for (int y = 0; y < hl.length; y++) {
            for (int x = 0; x < hl[0].length; x++) {
                if (bitIndex >= watermarkLength) break;

                char bit = watermarkBinary.charAt(bitIndex);
                double delta = 12.0; // 增加嵌入强度

                // 使用绝对值+符号法确保提取可靠性
                double absValue = Math.abs(hl[y][x]);
                if (bit == '1') {
                    hl[y][x] = absValue + delta; // 正数表示1
                } else {
                    hl[y][x] = -(absValue + delta); // 负数表示0
                }
                bitIndex++;
            }
        }
        System.out.println("实际嵌入位数: " + bitIndex);

        // 应用逆DWT
        double[][] reconstructedY = applyIDWT(subbands);

        // 更新Y通道
        yuvImage[0] = reconstructedY;

        // 转换回RGB
        BufferedImage watermarkedImage = convertYUVtoRGB(yuvImage, width, height);

        // 将结果写入输出流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(watermarkedImage, "png", outputStream);
        return outputStream;
    }
    
    /**
     * 从流中提取水印
     * 
     * @param watermarkedImageStream 嵌入水印的图像输入流
     * @param binaryLength 水印二进制长度
     * @return 提取的水印文本
     * @throws IOException 如果图像流处理发生错误
     */
    public static String extract(InputStream watermarkedImageStream, int binaryLength) throws IOException {
        BufferedImage image = ImageIO.read(watermarkedImageStream);
        int width = image.getWidth();
        int height = image.getHeight();

        // 将图像转换为YUV
        double[][][] yuvImage = convertRGBtoYUV(image);

        // 应用DWT到Y通道
        double[][][] subbands = applyDWT(yuvImage[0]);

        // 从HL子带提取水印
        double[][] hl = subbands[1];
        StringBuilder extractedBinary = new StringBuilder();

        // 提取水印位
        for (int y = 0; y < hl.length; y++) {
            for (int x = 0; x < hl[0].length; x++) {
                // 使用符号检测水印位（正数=1，负数=0）
                char bit = hl[y][x] > 0 ? '1' : '0';
                extractedBinary.append(bit);
            }
        }

        System.out.println("提取的总位数: " + extractedBinary.length());

        // 截取有效长度
        String validBinary = extractedBinary.substring(0, Math.min(binaryLength, extractedBinary.length()));
        System.out.println("截取的有效位数: " + validBinary.length());

        // 转换为Base64字符串
        String base64Str = binaryToString(validBinary);
        System.out.println("Base64字符串: " + base64Str);

        try {
            // 解码Base64
            byte[] decodedBytes = Base64.getDecoder().decode(base64Str);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            System.err.println("Base64解码错误: " + e.getMessage());
            // 尝试修复填充问题
            return repairBase64Decoding(base64Str);
        }
    }
    
    /**
     * 主函数，用于测试水印嵌入和提取功能
     * 包含文件版本和流版本的完整测试流程
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        try {
            File original = new File("D:\\data\\watermark\\wm.jpg");
            String watermark = "安全水印@2023@我要看看能够嵌入多大内容水印";

            // 计算原始水印的Base64编码
            String base64Original = Base64.getEncoder().encodeToString(
                    watermark.getBytes(StandardCharsets.UTF_8));
            System.out.println("原始水印文本: " + watermark);
            System.out.println("Base64编码后: " + base64Original);

            // 计算水印二进制长度
            int binaryLength = toBinaryString(base64Original).length();
            System.out.println("Base64二进制长度: " + binaryLength + " 位");

            // 文件版本 - DWT水印
            File dwtOutput = new File("D:\\data\\watermark\\wm-dwt.png");
            embed(original, dwtOutput, watermark);
            System.out.println("文件版本水印嵌入完成");

            // 提取水印
            String extractedText = extract(dwtOutput, binaryLength);

            // 输出结果
            System.out.println("提取的水印文本: " + extractedText);

            // 对比结果
            System.out.println("\n水印验证结果:");
            System.out.println("原始水印: " + watermark);
            System.out.println("提取水印: " + extractedText);
            System.out.println("匹配状态: " + watermark.equals(extractedText));
            
            // 流版本测试
            System.out.println("\n开始测试基于流的水印嵌入和提取...");
            java.io.FileInputStream fis = new java.io.FileInputStream(original);
            OutputStream outputStream = embed(fis, watermark);
            System.out.println("流版本水印嵌入完成");
            
            // 从流中提取水印
            java.io.ByteArrayInputStream watermarkedStream = new java.io.ByteArrayInputStream(
                ((ByteArrayOutputStream)outputStream).toByteArray());
            String extractedFromStream = extract(watermarkedStream, binaryLength);
            System.out.println("从流中提取的水印文本: " + extractedFromStream);
            System.out.println("流版本匹配状态: " + watermark.equals(extractedFromStream));

        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("错误: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("解码错误: " + e.getMessage());
        }
    }

}
