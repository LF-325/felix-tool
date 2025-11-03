package com.felix;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class TravelPlanGenerator {

    // 定义文本位置坐标（需要根据实际模板图片调整）
    private static final Map<String, Point> textPositions = new HashMap<>();
    static {
        // 标题位置
        textPositions.put("title", new Point(150, 100));
        // 行程内容位置
        textPositions.put("day1", new Point(50, 200));
        textPositions.put("day2", new Point(50, 350));
        textPositions.put("day3", new Point(50, 500));
        // 底部文字位置
        textPositions.put("footer", new Point(150, 650));
    }

    // 二维码位置和大小
    private static final Point qrCodePosition = new Point(400, 550);
    private static final int QR_CODE_SIZE = 150;

    public static void main(String[] args) {
        try {
            // 1. 加载模板图片
            BufferedImage templateImage = ImageIO.read(new File("D:\\data\\plan\\tpl.png"));

            // 2. 创建Graphics2D对象用于绘制
            BufferedImage outputImage = new BufferedImage(
                    templateImage.getWidth(),
                    templateImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );

            Graphics2D g2d = outputImage.createGraphics();
            g2d.drawImage(templateImage, 0, 0, null);

            // 3. 设置字体和颜色
            Font titleFont = new Font("微软雅黑", Font.BOLD, 24);
            Font contentFont = new Font("微软雅黑", Font.PLAIN, 14);
            Font footerFont = new Font("微软雅黑", Font.ITALIC, 12);
            g2d.setColor(Color.BLACK);

            // 4. 绘制标题
            g2d.setFont(titleFont);
            g2d.drawString("就用AI旅行搭了", textPositions.get("title").x, textPositions.get("title").y);

            // 5. 绘制行程内容
            g2d.setFont(contentFont);
            String[] day1Content = {
                    "DAY1",
                    "西安亚朵酒店 → 三根电杆跌落馆 → 西安城墙 →",
                    "西安钟楼 → 大唐芙蓉园 → 回民街"
            };

            String[] day2Content = {
                    "DAY2",
                    "西安亚朵酒店 → 三根电杆跌落馆 → 西安城墙 →",
                    "西安钟楼 → 大唐芙蓉园 → 回民街"
            };

            String[] day3Content = {
                    "DAY3",
                    "西安亚朵酒店 → 三根电杆跌落馆 → 西安城墙 →",
                    "新用AI旅行搭子"
            };

            drawMultiLineText(g2d, day1Content, textPositions.get("day1"));
            drawMultiLineText(g2d, day2Content, textPositions.get("day2"));
            drawMultiLineText(g2d, day3Content, textPositions.get("day3"));

            // 6. 绘制底部文字
            g2d.setFont(footerFont);
            g2d.drawString("小美用AI旅行搭子生成了一段超赞的行程攻略",
                    textPositions.get("footer").x, textPositions.get("footer").y);

            // 7. 生成并绘制二维码
            BufferedImage qrCode = generateQRCode("https://www.huilvyun.com", QR_CODE_SIZE);
            g2d.drawImage(qrCode, qrCodePosition.x, qrCodePosition.y, null);

            // 8. 保存输出图片
            ImageIO.write(outputImage, "png", new File("D:\\data\\plan\\tpl-1.png"));

            // 9. 释放资源
            g2d.dispose();

            System.out.println("旅行计划图片生成成功!");

        } catch (IOException | WriterException e) {
            e.printStackTrace();
        }
    }

    // 绘制多行文本
    private static void drawMultiLineText(Graphics2D g2d, String[] lines, Point startPoint) {
        int lineHeight = g2d.getFontMetrics().getHeight();
        for (int i = 0; i < lines.length; i++) {
            g2d.drawString(lines[i], startPoint.x, startPoint.y + (i * lineHeight));
        }
    }

    // 生成二维码
    private static BufferedImage generateQRCode(String content, int size) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

        BufferedImage qrImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                qrImage.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }

        return qrImage;
    }
}