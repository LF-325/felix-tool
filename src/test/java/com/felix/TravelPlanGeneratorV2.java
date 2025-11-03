package com.felix;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
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

public class TravelPlanGeneratorV2 {

    // 调试模式 - 设置为true会显示参考线，方便定位
    private static final boolean DEBUG_MODE = true;

    // 模板图片尺寸
    private static final int TEMPLATE_WIDTH = 600;
    private static final int TEMPLATE_HEIGHT = 800;

    // 边距和间距设置
    private static final int MARGIN_LEFT = 40;
    private static final int MARGIN_TOP = 150;
    private static final int LINE_SPACING = 25;
    private static final int PARAGRAPH_SPACING = 15;

    // 字体设置
    private static final String FONT_NAME = "Microsoft YaHei"; // 尝试使用英文名称
    private static final int TITLE_FONT_SIZE = 28;
    private static final int DAY_TITLE_FONT_SIZE = 20;
    private static final int CONTENT_FONT_SIZE = 16;
    private static final int FOOTER_FONT_SIZE = 14;

    // 颜色设置
    private static final Color TITLE_COLOR = new Color(50, 50, 50);
    private static final Color DAY_TITLE_COLOR = new Color(60, 60, 60);
    private static final Color CONTENT_COLOR = new Color(80, 80, 80);
    private static final Color FOOTER_COLOR = new Color(100, 100, 100);
    private static final Color DEBUG_COLOR = new Color(255, 0, 0, 128);

    // 二维码位置和大小
    private static final int QR_CODE_SIZE = 120;
    private static final int QR_CODE_X = 400;
    private static final int QR_CODE_Y = 180;

    public static void main(String[] args) {
        try {
            // 1. 加载模板图片
            BufferedImage templateImage = ImageIO.read(new File("D:\\data\\plan\\tpl.png"));

            // 2. 创建Graphics2D对象用于绘制
            BufferedImage outputImage = new BufferedImage(
                    templateImage.getWidth(),
                    templateImage.getHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g2d = outputImage.createGraphics();

            // 设置高质量渲染
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // 绘制背景模板
            g2d.drawImage(templateImage, 0, 0, null);

            // 3. 创建字体
            Font titleFont = createFont(FONT_NAME, Font.BOLD, TITLE_FONT_SIZE);
            Font dayTitleFont = createFont(FONT_NAME, Font.BOLD, DAY_TITLE_FONT_SIZE);
            Font contentFont = createFont(FONT_NAME, Font.PLAIN, CONTENT_FONT_SIZE);
            Font footerFont = createFont(FONT_NAME, Font.ITALIC, FOOTER_FONT_SIZE);

            // 4. 绘制主标题
            g2d.setFont(titleFont);
            g2d.setColor(TITLE_COLOR);
            String mainTitle = "就用AI旅行搭了";
            int titleX = (TEMPLATE_WIDTH - getTextWidth(g2d, mainTitle)) / 2;
            g2d.drawString(mainTitle, titleX, MARGIN_TOP);

            // 5. 绘制行程内容
            int currentY = MARGIN_TOP + 60;

            // DAY1
            g2d.setFont(dayTitleFont);
            g2d.setColor(DAY_TITLE_COLOR);
            g2d.drawString("DAY1", MARGIN_LEFT, currentY);

            g2d.setFont(contentFont);
            g2d.setColor(CONTENT_COLOR);
            String[] day1Content = {
                    "西安亚朵酒店 → 三根电杆陕菜馆 → 西安城墙 →",
                    "西安钟楼 → 大唐芙蓉园 → 回民街"
            };
            currentY = drawMultiLineText(g2d, day1Content, MARGIN_LEFT + 20, currentY + LINE_SPACING);

            currentY += PARAGRAPH_SPACING;

            // DAY2
            g2d.setFont(dayTitleFont);
            g2d.setColor(DAY_TITLE_COLOR);
            g2d.drawString("DAY2", MARGIN_LEFT, currentY);

            g2d.setFont(contentFont);
            g2d.setColor(CONTENT_COLOR);
            String[] day2Content = {
                    "西安亚朵酒店 → 三根电杆陕菜馆 → 西安城墙 →",
                    "西安钟楼 → 大唐芙蓉园 → 回民街"
            };
            currentY = drawMultiLineText(g2d, day2Content, MARGIN_LEFT + 20, currentY + LINE_SPACING);

            currentY += PARAGRAPH_SPACING;

            // DAY3
            g2d.setFont(dayTitleFont);
            g2d.setColor(DAY_TITLE_COLOR);
            g2d.drawString("DAY3", MARGIN_LEFT, currentY);

            g2d.setFont(contentFont);
            g2d.setColor(CONTENT_COLOR);
            String[] day3Content = {
                    "西安亚朵酒店 → 三根电杆陕菜馆 → 西安城墙 →",
                    "新用AI旅行搭子"
            };
            currentY = drawMultiLineText(g2d, day3Content, MARGIN_LEFT + 20, currentY + LINE_SPACING);

            // 6. 绘制底部文字
            g2d.setFont(footerFont);
            g2d.setColor(FOOTER_COLOR);
            String footerText = "小美用AI旅行搭子生成了一段超赞的行程攻略";
            int footerX = (TEMPLATE_WIDTH - getTextWidth(g2d, footerText)) / 2;
            g2d.drawString(footerText, footerX, currentY + 50);

            // 7. 生成并绘制二维码
            BufferedImage qrCode = generateQRCode("https://www.huilvyun.com", QR_CODE_SIZE);
            g2d.drawImage(qrCode, QR_CODE_X, QR_CODE_Y, null);

            // 8. 调试模式 - 绘制参考线
            if (DEBUG_MODE) {
                drawDebugLines(g2d, TEMPLATE_WIDTH, TEMPLATE_HEIGHT);
            }

            // 9. 保存输出图片
            ImageIO.write(outputImage, "png", new File("D:\\data\\plan\\tpl-5.png"));

            // 10. 释放资源
            g2d.dispose();

            System.out.println("旅行计划图片生成成功!");
            if (DEBUG_MODE) {
                System.out.println("调试模式已启用，红色参考线将显示在输出图片中");
            }

        } catch (IOException | WriterException e) {
            e.printStackTrace();
        }
    }

    // 创建字体，如果首选字体不可用则使用备用字体
    private static Font createFont(String fontName, int style, int size) {
        Font font = new Font(fontName, style, size);
        if (!font.getFamily().equals(fontName)) {
            // 首选字体不可用，尝试使用备用字体
            String[] fallbackFonts = {"SimSun", "宋体", "Arial", "sans-serif"};
            for (String fallback : fallbackFonts) {
                font = new Font(fallback, style, size);
                if (!font.getFamily().equals(fallback)) {
                    break;
                }
            }
        }
        return font;
    }

    // 绘制多行文本并返回最后一行底部Y坐标
    private static int drawMultiLineText(Graphics2D g2d, String[] lines, int x, int y) {
        FontMetrics metrics = g2d.getFontMetrics();
        int lineHeight = metrics.getHeight();

        for (int i = 0; i < lines.length; i++) {
            g2d.drawString(lines[i], x, y + (i * lineHeight));
        }

        return y + (lines.length * lineHeight);
    }

    // 获取文本宽度
    private static int getTextWidth(Graphics2D g2d, String text) {
        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D bounds = g2d.getFont().getStringBounds(text, frc);
        return (int) bounds.getWidth();
    }

    // 生成二维码
    private static BufferedImage generateQRCode(String content, int size) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

        BufferedImage qrImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                qrImage.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0x00FFFFFF);
            }
        }

        return qrImage;
    }

    // 绘制调试参考线
    private static void drawDebugLines(Graphics2D g2d, int width, int height) {
        g2d.setColor(DEBUG_COLOR);

        // 绘制网格线
        for (int i = 0; i < width; i += 50) {
            g2d.drawLine(i, 0, i, height);
            g2d.drawString(String.valueOf(i), i + 2, 10);
        }

        for (int i = 0; i < height; i += 50) {
            g2d.drawLine(0, i, width, i);
            g2d.drawString(String.valueOf(i), 2, i - 2);
        }

        // 绘制关键区域标记
        g2d.drawRect(MARGIN_LEFT, MARGIN_TOP, width - 2 * MARGIN_LEFT, 500);
        g2d.drawRect(QR_CODE_X, QR_CODE_Y, QR_CODE_SIZE, QR_CODE_SIZE);
    }
}