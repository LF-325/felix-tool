package com.felix.images;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class DWTWatermark {

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

    // 提取水印
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

    // 修复Base64解码问题
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

    // 应用一级DWT（Haar小波）
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

    // 应用一级逆DWT
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

    // RGB转YUV
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

    // YUV转RGB
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

    // 工具方法
    private static String toBinaryString(String text) {
        StringBuilder binary = new StringBuilder();
        for (char c : text.toCharArray()) {
            // 确保每个字符转换为8位二进制
            String bin = String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0');
            binary.append(bin);
        }
        return binary.toString();
    }

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

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    public static void main(String[] args) {
        try {
            File original = new File("D:\\data\\watermark\\wm.jpg");
            String watermark = "安全水印@2023";

            // 计算原始水印的Base64编码
            String base64Original = Base64.getEncoder().encodeToString(
                    watermark.getBytes(StandardCharsets.UTF_8));
            System.out.println("原始水印文本: " + watermark);
            System.out.println("Base64编码后: " + base64Original);

            // 计算水印二进制长度
            int binaryLength = toBinaryString(base64Original).length();
            System.out.println("Base64二进制长度: " + binaryLength + " 位");

            // DWT水印
            File dwtOutput = new File("D:\\data\\watermark\\wm-dwt.png");
            embed(original, dwtOutput, watermark);
            System.out.println("水印嵌入完成");

            // 提取水印
            String extractedText = extract(dwtOutput, binaryLength);

            // 输出结果
            System.out.println("提取的水印文本: " + extractedText);

            // 对比结果
            System.out.println("\n水印验证结果:");
            System.out.println("原始水印: " + watermark);
            System.out.println("提取水印: " + extractedText);
            System.out.println("匹配状态: " + watermark.equals(extractedText));

        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("错误: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("解码错误: " + e.getMessage());
        }
    }

}
