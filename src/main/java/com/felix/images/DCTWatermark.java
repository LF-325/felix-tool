package com.felix.images;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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
     * 将文本转换为二进制字符串
     *
     * @param text 文本
     * @return 二进制字符串
     */
    private static String toBinaryString(String text) {
        StringBuilder binary = new StringBuilder();
        for (char c : text.toCharArray()) {
            binary.append(String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0'));
        }
        return binary.toString();
    }

    /**
     * 将二进制字符串转换为文本
     *
     * @param binary 二进制字符串
     * @return 文本
     */
    private static String binaryToString(String binary) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < binary.length(); i += 8) {
            String byteStr = binary.substring(i, Math.min(i + 8, binary.length()));
            text.append((char) Integer.parseInt(byteStr, 2));
        }
        return text.toString();
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
            embed(dctInput, dctOutput, watermark);
            System.out.println("嵌入水印完成！");
            String extractedDCT = extract(dctOutput, watermark.length());
            System.out.println("DCT提取结果: " + extractedDCT);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
