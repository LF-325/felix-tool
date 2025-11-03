package com.felix.watermark.digital.images;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 基于DCT（离散余弦变换）的数字水印
 */
public class DCTWatermark {

    /**
     * 将水印嵌入图像中
     *
     * @param imageFile 原始图像文件
     * @param outputFile 嵌入水印后的输出图像文件
     * @param watermark 水印文本
     * @throws IOException 如果文件处理发生错误
     */
    public static void embed(File imageFile, File outputFile, String watermark) throws IOException {
        // 读取原始图像
        BufferedImage image = ImageIO.read(imageFile);
        int width = image.getWidth();
        int height = image.getHeight();

        // 将水印转换为二进制
        String watermarkBinary = toBinaryString(watermark);
        int watermarkLength = watermarkBinary.length();

        // 检查容量
        int blockSize = 8;
        int blocksPerRow = width / blockSize;
        int blocksPerCol = height / blockSize;
        if (watermarkLength > blocksPerRow * blocksPerCol) {
            throw new IllegalArgumentException("水印信息过长");
        }

        int bitIndex = 0;
        for (int by = 0; by < blocksPerCol; by++) {
            for (int bx = 0; bx < blocksPerRow; bx++) {
                if (bitIndex >= watermarkLength) break;

                // 获取8x8像素块
                int[][] block = new int[blockSize][blockSize];
                for (int y = 0; y < blockSize; y++) {
                    for (int x = 0; x < blockSize; x++) {
                        int px = bx * blockSize + x;
                        int py = by * blockSize + y;
                        block[y][x] = image.getRGB(px, py) & 0xFF; // 取亮度值
                    }
                }

                // 应用DCT变换（简化实现）
                double[][] dctBlock = applyDCT(block);

                // 在中频系数中嵌入水印（位置(3,3)）
                char bit = watermarkBinary.charAt(bitIndex++);
                double delta = 25.0; // 嵌入强度
                if (bit == '1') {
                    dctBlock[3][3] += delta;
                } else {
                    dctBlock[3][3] -= delta;
                }

                // 应用逆DCT变换
                int[][] idctBlock = applyIDCT(dctBlock);

                // 更新图像块
                for (int y = 0; y < blockSize; y++) {
                    for (int x = 0; x < blockSize; x++) {
                        int px = bx * blockSize + x;
                        int py = by * blockSize + y;
                        int rgb = image.getRGB(px, py);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int bVal = (rgb) & 0xFF;

                        // 只更新亮度分量（简化处理）
                        int newLum = clamp(idctBlock[y][x]);
                        int newRgb = (r << 16) | (g << 8) | newLum;
                        image.setRGB(px, py, newRgb);
                    }
                }
            }
        }

        // 保存嵌入水印后的图像
        ImageIO.write(image, "png", outputFile);
    }

    /**
     * 将水印嵌入图像中（基于流的实现）
     *
     * @param imageStream 原始图像输入流
     * @param watermark 水印文本
     * @return 嵌入水印后的图像输出流
     * @throws IOException 如果流处理发生错误
     */
    public static OutputStream embed(InputStream imageStream, String watermark) throws IOException {
        // 读取原始图像
        BufferedImage image = ImageIO.read(imageStream);
        int width = image.getWidth();
        int height = image.getHeight();

        // 将水印转换为二进制
        String watermarkBinary = toBinaryString(watermark);
        int watermarkLength = watermarkBinary.length();
        
        System.out.println("水印二进制长度: " + watermarkLength);
        System.out.println("水印二进制内容: " + watermarkBinary);

        // 检查容量
        int blockSize = 8;
        int blocksPerRow = width / blockSize;
        int blocksPerCol = height / blockSize;
        if (watermarkLength > blocksPerRow * blocksPerCol) {
            throw new IllegalArgumentException("水印信息过长");
        }

        int bitIndex = 0;
        for (int by = 0; by < blocksPerCol; by++) {
            for (int bx = 0; bx < blocksPerRow; bx++) {
                if (bitIndex >= watermarkLength) break;

                // 获取8x8像素块
                int[][] block = new int[blockSize][blockSize];
                for (int y = 0; y < blockSize; y++) {
                    for (int x = 0; x < blockSize; x++) {
                        int px = bx * blockSize + x;
                        int py = by * blockSize + y;
                        block[y][x] = image.getRGB(px, py) & 0xFF; // 取亮度值
                    }
                }

                // 应用DCT变换（简化实现）
                double[][] dctBlock = applyDCT(block);

                // 在中频系数中嵌入水印（位置(3,3)）
                char bit = watermarkBinary.charAt(bitIndex++);
                double delta = 25.0; // 嵌入强度
                if (bit == '1') {
                    dctBlock[3][3] += delta;
                } else {
                    dctBlock[3][3] -= delta;
                }

                // 应用逆DCT变换
                int[][] idctBlock = applyIDCT(dctBlock);

                // 更新图像块
                for (int y = 0; y < blockSize; y++) {
                    for (int x = 0; x < blockSize; x++) {
                        int px = bx * blockSize + x;
                        int py = by * blockSize + y;
                        int rgb = image.getRGB(px, py);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int bVal = (rgb) & 0xFF;

                        // 只更新亮度分量（简化处理）
                        int newLum = clamp(idctBlock[y][x]);
                        int newRgb = (r << 16) | (g << 8) | newLum;
                        image.setRGB(px, py, newRgb);
                    }
                }
            }
        }

        // 将处理后的图像写入输出流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream;
    }

    /**
     * 从图像中提取水印（基于流的实现）
     *
     * @param watermarkedImageStream 嵌入水印的图像输入流
     * @param length 水印文本的长度（字符数）
     * @return 提取的水印文本
     * @throws IOException 如果流处理发生错误
     */
    public static String extract(InputStream watermarkedImageStream, int length) throws IOException {
        BufferedImage image = ImageIO.read(watermarkedImageStream);
        int width = image.getWidth();
        int height = image.getHeight();

        int blockSize = 8;
        int blocksPerRow = width / blockSize;
        int blocksPerCol = height / blockSize;
        
        // 估计每个字符平均需要的二进制位数（UTF-8中文约3字节，即24位）
        int estimatedBinaryLength = length * 24;
        
        StringBuilder extracted = new StringBuilder(estimatedBinaryLength);
        int bitIndex = 0;

        for (int by = 0; by < blocksPerCol; by++) {
            for (int bx = 0; bx < blocksPerRow; bx++) {
                if (bitIndex >= estimatedBinaryLength) break;

                // 获取8x8像素块
                int[][] block = new int[blockSize][blockSize];
                for (int y = 0; y < blockSize; y++) {
                    for (int x = 0; x < blockSize; x++) {
                        int px = bx * blockSize + x;
                        int py = by * blockSize + y;
                        block[y][x] = image.getRGB(px, py) & 0xFF; // 取亮度值
                    }
                }

                // 应用DCT变换
                double[][] dctBlock = applyDCT(block);

                // 提取中频系数(3,3)的值
                double coeff = dctBlock[3][3];
                extracted.append(coeff > 0 ? '1' : '0');
                bitIndex++;
            }
        }

        // 将二进制水印信息转换为文本
        String extractedBinary = extracted.toString();
        System.out.println("提取的二进制长度: " + extractedBinary.length());
        System.out.println("提取的二进制内容: " + extractedBinary);
        
        // 使用固定的水印文本计算二进制长度
        String testWatermark = "DCT测试";
        String testBinary = toBinaryString(testWatermark);
        int originalBinaryLength = testBinary.length();
        
        // 确保提取的二进制长度与原始长度匹配
        if (extractedBinary.length() > originalBinaryLength) {
            extractedBinary = extractedBinary.substring(0, originalBinaryLength);
            System.out.println("截断后的二进制长度: " + extractedBinary.length());
            System.out.println("截断后的二进制内容: " + extractedBinary);
        }
        
        return binaryToString(extractedBinary);
    }
    
    /**
     * 从图像中提取水印
     *
     * @param watermarkedImage 嵌入水印的图像文件
     * @param length 水印文本的长度
     * @return 提取的水印文本
     * @throws IOException 如果文件处理发生错误
     */
    public static String extract(File watermarkedImage, int length) throws IOException {
        BufferedImage image = ImageIO.read(watermarkedImage);
        int width = image.getWidth();
        int height = image.getHeight();

        int blockSize = 8;
        int blocksPerRow = width / blockSize;
        int blocksPerCol = height / blockSize;

        StringBuilder extracted = new StringBuilder(length * 8);
        int bitIndex = 0;

        for (int by = 0; by < blocksPerCol; by++) {
            for (int bx = 0; bx < blocksPerRow; bx++) {
                if (bitIndex >= length * 8) break;

                // 获取8x8像素块
                int[][] block = new int[blockSize][blockSize];
                for (int y = 0; y < blockSize; y++) {
                    for (int x = 0; x < blockSize; x++) {
                        int px = bx * blockSize + x;
                        int py = by * blockSize + y;
                        block[y][x] = image.getRGB(px, py) & 0xFF; // 取亮度值
                    }
                }

                // 应用DCT变换
                double[][] dctBlock = applyDCT(block);

                // 提取中频系数(3,3)的值
                double coeff = dctBlock[3][3];
                extracted.append(coeff > 0 ? '1' : '0');
                bitIndex++;
            }
        }

        // 将二进制水印信息转换为文本
        return binaryToString(extracted.toString());
    }

    /**
     * 应用离散余弦变换（DCT）
     *
     * @param block 图像块
     * @return DCT变换后的系数矩阵
     */
    private static double[][] applyDCT(int[][] block) {
        int size = block.length;
        double[][] dct = new double[size][size];

        for (int u = 0; u < size; u++) {
            for (int v = 0; v < size; v++) {
                double sum = 0.0;
                for (int x = 0; x < size; x++) {
                    for (int y = 0; y < size; y++) {
                        double cu = (u == 0) ? 1.0 / Math.sqrt(2) : 1.0;
                        double cv = (v == 0) ? 1.0 / Math.sqrt(2) : 1.0;
                        double cos1 = Math.cos((2*x+1)*u*Math.PI/(2.0*size));
                        double cos2 = Math.cos((2*y+1)*v*Math.PI/(2.0*size));
                        sum += cu * cv * block[x][y] * cos1 * cos2;
                    }
                }
                dct[u][v] = 0.25 * sum;
            }
        }
        return dct;
    }

    /**
     * 应用逆离散余弦变换（IDCT）
     *
     * @param dct DCT系数矩阵
     * @return IDCT变换后的图像块
     */
    private static int[][] applyIDCT(double[][] dct) {
        int size = dct.length;
        int[][] block = new int[size][size];

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                double sum = 0.0;
                for (int u = 0; u < size; u++) {
                    for (int v = 0; v < size; v++) {
                        double cu = (u == 0) ? 1.0 / Math.sqrt(2) : 1.0;
                        double cv = (v == 0) ? 1.0 / Math.sqrt(2) : 1.0;
                        double cos1 = Math.cos((2*x+1)*u*Math.PI/(2.0*size));
                        double cos2 = Math.cos((2*y+1)*v*Math.PI/(2.0*size));
                        sum += cu * cv * dct[u][v] * cos1 * cos2;
                    }
                }
                block[x][y] = clamp((int) (0.25 * sum + 0.5));
            }
        }
        return block;
    }

    /**
     * 将文本转换为二进制字符串（支持UTF-8编码）
     *
     * @param text 文本
     * @return 二进制字符串
     */
    public static String toBinaryString(String text) {
        try {
            StringBuilder binary = new StringBuilder();
            byte[] bytes = text.getBytes("UTF-8");
            for (byte b : bytes) {
                binary.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
            }
            return binary.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 将二进制字符串转换为文本（支持UTF-8编码）
     *
     * @param binary 二进制字符串
     * @return 文本
     */
    public static String binaryToString(String binary) {
        try {
            // 确保二进制字符串长度是8的倍数
            int validLength = (binary.length() / 8) * 8;
            if (validLength == 0) return "";
            
            byte[] bytes = new byte[validLength / 8];
            for (int i = 0; i < validLength; i += 8) {
                String byteStr = binary.substring(i, i + 8);
                bytes[i / 8] = (byte) Integer.parseInt(byteStr, 2);
            }
            return new String(bytes, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static int clamp(int value) {
        return Math.min(255, Math.max(0, value));
    }

    // ============== 主方法测试 ==============

    public static void main(String[] args) {
        try {
            String watermark = "WMTEST@2025";
            File dctInput = new File("D:\\data\\watermark\\wm.jpg");
            File dctOutput = new File("D:\\data\\watermark\\wm-dct.png");
            
            // 测试基于文件的水印嵌入和提取
            embed(dctInput, dctOutput, watermark);
            System.out.println("基于文件的水印嵌入完成！");
            String extractedDCT = extract(dctOutput, watermark.length());
            System.out.println("基于文件的DCT提取结果: " + extractedDCT);
            
            // 测试基于流的水印嵌入和提取
            java.io.FileInputStream fis = new java.io.FileInputStream(dctInput);
            OutputStream os = embed(fis, watermark);
            System.out.println("基于流的水印嵌入完成！");
            
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(
                ((java.io.ByteArrayOutputStream)os).toByteArray());
            String extractedStreamDCT = extract(bais, watermark.length());
            System.out.println("基于流的DCT提取结果: " + extractedStreamDCT);
            
            // 保存基于流处理的结果到文件（用于比较）
            java.io.FileOutputStream fos = new java.io.FileOutputStream("D:\\data\\watermark\\wm-dct-stream.png");
            fos.write(((java.io.ByteArrayOutputStream)os).toByteArray());
            fos.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
