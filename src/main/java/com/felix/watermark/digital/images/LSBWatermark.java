package com.felix.watermark.digital.images;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * 基于LSB（最低有效位）的隐写术
 * @author 刘飞
 * @Description:
 * 修改像素最低位
 * 对图像质量影响极小
 * 抗攻击能力弱（压缩、滤波会破坏水印）
 */
public class LSBWatermark {

    // 嵌入水印
    /**
     * 在图像中嵌入水印
     * 该方法通过修改图像的RGB值来隐藏水印信息，每个颜色通道的最低位用于存储水印的二进制数据
     *
     * @param imageFile 原始图像文件
     * @param outputFile 嵌入水印后的图像文件
     * @param watermark 水印文本
     * @throws IOException 如果图像文件读写发生错误
     */
    public static void embedWatermark(File imageFile, File outputFile, String watermark) throws IOException {
        BufferedImage image = ImageIO.read(imageFile);
        int width = image.getWidth();
        int height = image.getHeight();

        // 将水印文本转换为二进制字符串
        String watermarkBinary = toBinaryString(watermark);
        int watermarkLength = watermarkBinary.length();

        // 检查图像容量是否足够
        if (watermarkLength > width * height * 3) {
            throw new IllegalArgumentException("水印信息过长，超出图像承载能力");
        }

        int bitIndex = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitIndex >= watermarkLength) break;

                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // 修改RGB通道的最低位
                if (bitIndex < watermarkLength) {
                    r = setLSB(r, watermarkBinary.charAt(bitIndex++));
                }
                if (bitIndex < watermarkLength) {
                    g = setLSB(g, watermarkBinary.charAt(bitIndex++));
                }
                if (bitIndex < watermarkLength) {
                    b = setLSB(b, watermarkBinary.charAt(bitIndex++));
                }

                int newRgb = (r << 16) | (g << 8) | b;
                image.setRGB(x, y, newRgb);
            }
            if (bitIndex >= watermarkLength) break;
        }

        ImageIO.write(image, "png", outputFile); // PNG格式保留无损数据
    }

    /**
     * 从带有水印的图像中提取水印信息
     *
     * @param watermarkedImage 嵌入水印的图像文件
     * @param length 水印文本的长度（字符数）
     * @return 提取的水印信息
     * @throws IOException 如果图像文件读取发生错误
     */
    public static String extractWatermark(File watermarkedImage, int length) throws IOException {
        BufferedImage image = ImageIO.read(watermarkedImage);
        int width = image.getWidth();
        int height = image.getHeight();
        
        // 估计UTF-8编码的字符平均需要3个字节
        int estimatedBytes = length * 3;
        int totalBits = estimatedBytes * 8;

        StringBuilder binaryBuilder = new StringBuilder(totalBits);
        int extractedBits = 0;

        outerLoop:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // 提取RGB通道的最低位
                binaryBuilder.append(getLSB(r));
                if (++extractedBits >= totalBits) break outerLoop;

                binaryBuilder.append(getLSB(g));
                if (++extractedBits >= totalBits) break outerLoop;

                binaryBuilder.append(getLSB(b));
                if (++extractedBits >= totalBits) break outerLoop;
            }
        }

        String result = binaryToString(binaryBuilder.toString());
        // 截取到指定长度，避免多余字符
        return result.length() > length ? result.substring(0, length) : result;
    }

    /**
     * 设置颜色通道的最低有效位(LSB)
     *
     * @param color 颜色值
     * @param bit 要设置的位，'1' 或 '0'
     * @return 修改后的颜色值
     */
    private static int setLSB(int color, char bit) {
        return (bit == '1')
                ? (color | 1)   // 设置最低位为1
                : (color & ~1); // 设置最低位为0
    }

    /**
     * 获取颜色通道的最低有效位(LSB)
     *
     * @param color 颜色值
     * @return 最低位的值，'1' 或 '0'
     */
    private static char getLSB(int color) {
        return (color & 1) == 1 ? '1' : '0';
    }

    /**
     * 将字符串转换为二进制表示
     * 使用UTF-8编码确保正确处理中文和其他Unicode字符
     *
     * @param text 输入字符串
     * @return 字符串的二进制表示
     */
    private static String toBinaryString(String text) {
        StringBuilder binary = new StringBuilder();
        try {
            byte[] bytes = text.getBytes("UTF-8");
            for (byte b : bytes) {
                binary.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return binary.toString();
    }

    /**
     * 将二进制字符串转换回文本字符串
     * 使用UTF-8编码确保正确处理中文和其他Unicode字符
     *
     * @param binary 二进制字符串
     * @return 转换后的文本字符串
     */
    private static String binaryToString(String binary) {
        StringBuilder text = new StringBuilder();
        try {
            byte[] bytes = new byte[binary.length() / 8];
            for (int i = 0; i < bytes.length; i++) {
                String byteStr = binary.substring(i * 8, (i + 1) * 8);
                bytes[i] = (byte) Integer.parseInt(byteStr, 2);
            }
            text.append(new String(bytes, "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text.toString();
    }
    /**
     * 在图像中嵌入水印（基于流的版本）
     * 该方法通过修改图像的RGB值来隐藏水印信息，每个颜色通道的最低位用于存储水印的二进制数据
     *
     * @param imageInputStream 原始图像输入流
     * @param watermark 水印文本
     * @return 嵌入水印后的图像输出流
     * @throws IOException 如果图像流处理发生错误
     */
    public static OutputStream embedWatermark(InputStream imageInputStream, String watermark) throws IOException {
        BufferedImage image = ImageIO.read(imageInputStream);
        int width = image.getWidth();
        int height = image.getHeight();

        // 将水印文本转换为二进制字符串
        String watermarkBinary = toBinaryString(watermark);
        int watermarkLength = watermarkBinary.length();

        // 检查图像容量是否足够
        if (watermarkLength > width * height * 3) {
            throw new IllegalArgumentException("水印信息过长，超出图像承载能力");
        }

        int bitIndex = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitIndex >= watermarkLength) break;

                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // 修改RGB通道的最低位
                if (bitIndex < watermarkLength) {
                    r = setLSB(r, watermarkBinary.charAt(bitIndex++));
                }
                if (bitIndex < watermarkLength) {
                    g = setLSB(g, watermarkBinary.charAt(bitIndex++));
                }
                if (bitIndex < watermarkLength) {
                    b = setLSB(b, watermarkBinary.charAt(bitIndex++));
                }

                int newRgb = (r << 16) | (g << 8) | b;
                image.setRGB(x, y, newRgb);
            }
            if (bitIndex >= watermarkLength) break;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream); // PNG格式保留无损数据
        return outputStream;
    }
    
    /**
     * 从带有水印的图像输入流中提取水印信息
     *
     * @param watermarkedImageStream 嵌入水印的图像输入流
     * @param length 水印文本的长度（字符数）
     * @return 提取的水印信息
     * @throws IOException 如果图像流处理发生错误
     */
    public static String extractWatermark(InputStream watermarkedImageStream, int length) throws IOException {
        BufferedImage image = ImageIO.read(watermarkedImageStream);
        int width = image.getWidth();
        int height = image.getHeight();
        
        // 估计UTF-8编码的字符平均需要3个字节
        int estimatedBytes = length * 3;
        int totalBits = estimatedBytes * 8;

        StringBuilder binaryBuilder = new StringBuilder(totalBits);
        int extractedBits = 0;

        outerLoop:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // 提取RGB通道的最低位
                binaryBuilder.append(getLSB(r));
                if (++extractedBits >= totalBits) break outerLoop;

                binaryBuilder.append(getLSB(g));
                if (++extractedBits >= totalBits) break outerLoop;

                binaryBuilder.append(getLSB(b));
                if (++extractedBits >= totalBits) break outerLoop;
            }
        }

        String result = binaryToString(binaryBuilder.toString());
        // 截取到指定长度，避免多余字符
        return result.length() > length ? result.substring(0, length) : result;
    }
    
    public static void main(String[] args) {
        try {
            // 文件版本示例
            File input = new File("D:\\data\\watermark\\wm.jpg");
            File output = new File("D:\\data\\watermark\\wm-lsb.png");
            String watermark = "WMTEST@2025";
            embedWatermark(input, output, watermark);
            System.out.println("水印嵌入完成！");

            // 提取示例（需提前知道水印长度）
            String extracted = extractWatermark(output, watermark.length());
            System.out.println("提取的水印: " + extracted);
            
            // 流版本示例
            InputStream inputStream = new ByteArrayInputStream(java.nio.file.Files.readAllBytes(input.toPath()));
            OutputStream outputStream = embedWatermark(inputStream, watermark);
            System.out.println("流版本水印嵌入完成！");
            
            // 从流中提取水印
            ByteArrayInputStream watermarkedStream = new ByteArrayInputStream(((ByteArrayOutputStream)outputStream).toByteArray());
            String extractedFromStream = extractWatermark(watermarkedStream, watermark.length());
            System.out.println("从流中提取的水印: " + extractedFromStream);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
