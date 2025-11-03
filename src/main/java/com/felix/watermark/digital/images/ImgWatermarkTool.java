package com.felix.watermark.digital.images;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Position;
import net.coobird.thumbnailator.geometry.Positions;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 图片水印工具类，支持添加文本水印和图片水印
 */
public class ImgWatermarkTool {

    /**
     * 添加文本水印
     * @param inputImagePath 输入图片路径
     * @param outputImagePath 输出图片路径
     * @param watermarkText 水印文本
     * @param font 字体
     * @param color 颜色
     * @param position 位置
     * @param alpha 透明度(0.0-1.0)
     * @throws IOException 图片处理异常
     */
    public static void addTextWatermark(String inputImagePath, String outputImagePath, String watermarkText,
                                       Font font, Color color, Position position, float alpha) throws IOException {
        BufferedImage watermarkImage = createTextImage(watermarkText, font, color);
        Thumbnails.of(new File(inputImagePath))
                .scale(1.0)
                .watermark(position, watermarkImage, alpha)
                .outputQuality(0.95)
                .toFile(new File(outputImagePath));
    }

    /**
     * 添加图片水印
     * @param inputImagePath 输入图片路径
     * @param outputImagePath 输出图片路径
     * @param watermarkImagePath 水印图片路径
     * @param position 位置
     * @param alpha 透明度(0.0-1.0)
     * @throws IOException 图片处理异常
     */
    public static void addImageWatermark(String inputImagePath, String outputImagePath, String watermarkImagePath,
                                        Position position, float alpha) throws IOException {
        BufferedImage watermarkImage = ImageIO.read(new File(watermarkImagePath));
        Thumbnails.of(new File(inputImagePath))
                .scale(1.0)
                .watermark(position, watermarkImage, alpha)
                .outputQuality(0.95)
                .toFile(new File(outputImagePath));
    }

    /**
     * 创建文本水印图片
     */
    private static BufferedImage createTextImage(String text, Font font, Color color) {
        // 创建临时图像以获取FontMetrics
        BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dTemp = tempImage.createGraphics();
        FontMetrics metrics = g2dTemp.getFontMetrics(font);
        g2dTemp.dispose();

        // 计算文本宽度和高度
        int width = metrics.stringWidth(text);
        int height = metrics.getHeight();

        // 创建带透明背景的图片
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // 设置透明度
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.setColor(color);
        g2d.setFont(font);
        g2d.drawString(text, 0, metrics.getAscent());
        g2d.dispose();

        return image;
    }

    /**
     * 默认文本水印（右下角红色Arial字体）
     */
    public static void addDefaultTextWatermark(String inputImagePath, String outputImagePath, String watermarkText) throws IOException {
        addTextWatermark(inputImagePath, outputImagePath, watermarkText,
                new Font("Arial", Font.BOLD, 36), Color.RED, Positions.BOTTOM_RIGHT, 0.5f);
    }

    /**
     * 默认图片水印（右下角50%透明度）
     */
    public static void addDefaultImageWatermark(String inputImagePath, String outputImagePath, String watermarkImagePath) throws IOException {
        addImageWatermark(inputImagePath, outputImagePath, watermarkImagePath, Positions.BOTTOM_RIGHT, 0.5f);
    }

    public static void main(String[] args) {
        try {
            // 测试文本水印
            String inputImage = "D:\\data\\watermark\\wm.jpg";
            String textOutputImage = "D:\\data\\watermark\\text_watermark_output.jpg";
            // 使用居中位置和0.4透明度
            addTextWatermark(inputImage, textOutputImage, "Felix Tool 2023",
                    new Font("Arial", Font.BOLD, 36), Color.RED, Positions.CENTER, 0.4f);
            System.out.println("文本水印添加成功: " + textOutputImage);

            // 测试图片水印
            String watermarkImage = "D:\\data\\watermark\\sy.png";
            String imageOutputImage = "D:\\data\\watermark\\img_watermark_output.jpg";
            // 使用居中位置和0.4透明度
            addImageWatermark(inputImage, imageOutputImage, watermarkImage, Positions.CENTER, 0.4f);
            System.out.println("图片水印添加成功: " + imageOutputImage);
        } catch (IOException e) {
            System.err.println("水印添加失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}