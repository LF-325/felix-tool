package com.felix.media;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import java.awt.*;
import javax.imageio.ImageIO;

public class WeatherVideoGenerator {

    // 天气数据模型
    public static class WeatherData {
        public final String city;
        public final String weatherType;
        public final int temperature;
        public final String description;

        public WeatherData(String city, String weatherType, int temperature,
                           String description) {
            this.city = city;
            this.weatherType = weatherType;
            this.temperature = temperature;
            this.description = description;
        }
    }

    // 配置参数
    public static class GeneratorConfig {
        public String ffmpeg = "ffmpeg"; // 默认使用系统路径中的ffmpeg
        public String fontName = "微软雅黑"; // 字体
        public File sourceVideo;       // 源视频文件
        public File outputVideo;       // 输出视频文件
        public WeatherData weatherData; // 天气数据
        public File weatherIconsDir;   // 天气图标目录
        public File tempDir;           // 临时工作目录
        public int resolutionWidth = 1280;  // 视频分辨率宽
        public int resolutionHeight = 720;  // 视频分辨率高
        public int cityFontSize = 36;       // 城市名称字体大小
        public int tempFontSize = 48;       // 温度字体大小
        public int descFontSize = 24;       // 描述字体大小
        public Color textColor = Color.WHITE; // 文字颜色
        public int textShadow = 2;           // 文字阴影大小
        public float overlayOpacity = 0.8f;  // 覆盖层透明度

        public int overlayX = 0;           // 覆盖图水平位置（默认左上角）
        public int overlayY = 0;           // 覆盖图垂直位置（默认左上角）
        public boolean keepTempFiles = false; // 是否保留临时文件
    }

    // 生成天气预报视频
    public static void generateWeatherVideo(GeneratorConfig config) throws Exception {
        File overlayImage = null;
        try {
            // 验证输入参数
            validateConfig(config);

            // 创建临时目录
            Files.createDirectories(config.tempDir.toPath());

            // 准备天气图标
            Map<String, File> weatherIcons = prepareWeatherIcons(config);

            // 生成覆盖层图片
            overlayImage = generateOverlayImage(config, weatherIcons);

            // 生成并执行FFmpeg命令
            String ffmpegCommand = generateFFmpegCommand(config, overlayImage);
            executeFFmpegCommand(ffmpegCommand);
//            boolean success = FFmpegUtil.executeCommand(Arrays.asList(ffmpegCommand.split(" ")), 60000);
//            System.out.println("转换结果: " + (success ? "成功" : "失败"));
            System.out.println("视频生成成功: " + config.outputVideo.getAbsolutePath());
        }catch (Exception e){
            System.out.println("生成视频出错: " + e.getMessage());
        }finally {
            // 清理临时文件
            if (!config.keepTempFiles) {
                cleanTempFiles(config, overlayImage);
            }
        }
    }

    // 验证配置参数
    private static void validateConfig(GeneratorConfig config) throws Exception {
        if (config.sourceVideo == null || !config.sourceVideo.exists()) {
            throw new FileNotFoundException("源视频文件不存在: " +
                    (config.sourceVideo != null ? config.sourceVideo.getPath() : "null"));
        }

        if (config.outputVideo == null) {
            throw new IllegalArgumentException("输出视频文件未指定");
        }

        if (config.weatherData == null) {
            throw new IllegalArgumentException("天气数据不能为空");
        }

        if (config.weatherIconsDir == null || !config.weatherIconsDir.exists()) {
            throw new FileNotFoundException("天气图标目录不存在");
        }

        if (config.tempDir == null) {
            throw new IllegalArgumentException("临时目录未指定");
        }
    }

    // 准备天气图标
    private static Map<String, File> prepareWeatherIcons(GeneratorConfig config) {
        Map<String, File> icons = new HashMap<>();
        File[] iconFiles = config.weatherIconsDir.listFiles();

        if (iconFiles != null) {
            for (File file : iconFiles) {
                String fileName = file.getName().toLowerCase();
                if (fileName.contains(".")) {
                    String weatherType = fileName.substring(0, fileName.lastIndexOf('.'));
                    icons.put(weatherType.toUpperCase(), file);
                }
            }
        }

        // 检查所需图标是否存在
        if (!icons.containsKey(config.weatherData.weatherType.toUpperCase())) {
            System.err.println("警告: 缺少" + config.weatherData.weatherType + "天气图标");
        }

        return icons;
    }

    // 生成覆盖层图片
    private static File generateOverlayImage(GeneratorConfig config,
                                             Map<String, File> weatherIcons) throws IOException {
        // 创建图片
        int width = config.resolutionWidth;
        int height = config.resolutionHeight;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 设置背景透明
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, width, height);
        g2d.setComposite(AlphaComposite.SrcOver);

        // 获取天气图标
        File iconFile = weatherIcons.get(config.weatherData.weatherType.toUpperCase());
        BufferedImage icon = null;
        int iconSize = 120;

        if (iconFile != null) {
            icon = ImageIO.read(iconFile);
            // 调整图标大小
            icon = resizeImage(icon, iconSize, iconSize);
        } else {
            // 生成默认图标
            icon = generateDefaultIcon(config.weatherData.weatherType, iconSize);
        }

        // 绘制天气图标
        if (icon != null) {
            int iconX = (width - iconSize) / 2;
            int iconY = height / 4-20;
            g2d.drawImage(icon, iconX, iconY, null);
        }

        // 设置字体和颜色
        Font cityFont = new Font(config.fontName, Font.BOLD, config.cityFontSize);
        Font tempFont = new Font(config.fontName, Font.BOLD, config.tempFontSize);
        Font descFont = new Font(config.fontName, Font.PLAIN, config.descFontSize);

        g2d.setColor(config.textColor);

        // 绘制城市名称
        g2d.setFont(cityFont);
        FontMetrics cityMetrics = g2d.getFontMetrics(cityFont);
        int cityX = (width - cityMetrics.stringWidth(config.weatherData.city)) / 2;
        int cityY = height / 4 - iconSize + 40;
        drawTextWithShadow(g2d, config.weatherData.city, cityX, cityY, config.textShadow);

        // 绘制温度
        String tempText = config.weatherData.temperature + "°C";
        g2d.setFont(tempFont);
        FontMetrics tempMetrics = g2d.getFontMetrics(tempFont);
        int tempX = (width - tempMetrics.stringWidth(tempText)) / 2;
        int tempY = height / 4 + iconSize + 50;
        drawTextWithShadow(g2d, tempText, tempX, tempY, config.textShadow);

        // 绘制描述
        g2d.setFont(descFont);
        FontMetrics descMetrics = g2d.getFontMetrics(descFont);
        int descX = (width - descMetrics.stringWidth(config.weatherData.description)) / 2;
        int descY = tempY + 60;
        drawTextWithShadow(g2d, config.weatherData.description, descX, descY, config.textShadow);

        g2d.dispose();

        // 保存图片
        File overlayImage = new File(config.tempDir, "weather_overlay.png");
        ImageIO.write(image, "PNG", overlayImage);

        return overlayImage;
    }

    // 生成默认天气图标
    private static BufferedImage generateDefaultIcon(String weatherType, int size) {
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();

        // 设置背景透明
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, size, size);
        g2d.setComposite(AlphaComposite.SrcOver);

        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 根据天气类型绘制不同图标
        Color color;
        String symbol;

        switch (weatherType.toUpperCase()) {
            case "SUNNY":
                color = Color.YELLOW;
                symbol = "☀";
                break;
            case "RAINY":
                color = Color.BLUE;
                symbol = "☔";
                break;
            case "CLOUDY":
                color = Color.GRAY;
                symbol = "☁";
                break;
            case "SNOW":
                color = Color.WHITE;
                symbol = "❄";
                break;
            case "THUNDER":
                color = Color.ORANGE;
                symbol = "⚡";
                break;
            default:
                color = Color.WHITE;
                symbol = "?";
        }

        // 绘制图标背景
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 180));
        g2d.fillOval(0, 0, size, size);

        // 绘制符号
        g2d.setColor(Color.BLACK);
        Font font = new Font("Segoe UI Emoji", Font.PLAIN, size * 2 / 3);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics(font);
        int x = (size - fm.stringWidth(symbol)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(symbol, x, y);

        g2d.dispose();
        return icon;
    }

    // 调整图片大小
    private static BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    // 绘制带阴影的文字
    private static void drawTextWithShadow(Graphics2D g2d, String text, int x, int y, int shadowSize) {
        // 绘制阴影
        if (shadowSize > 0) {
            g2d.setColor(Color.BLACK);
            for (int i = 0; i < shadowSize; i++) {
                g2d.drawString(text, x + i, y + i);
            }
        }

        // 绘制文字
        g2d.setColor(g2d.getColor());
        g2d.drawString(text, x, y);
    }

    // 生成完整的FFmpeg命令
    private static String generateFFmpegCommand(GeneratorConfig config, File overlayImage) {
        StringBuilder cmd = new StringBuilder(config.ffmpeg);

        cmd.append(" -y -hide_banner -loglevel warning ");

        // 添加源视频输入
        cmd.append("-i \"").append(config.sourceVideo.getAbsolutePath()).append("\" ");

        // 添加覆盖层图片输入
        cmd.append("-i \"").append(overlayImage.getAbsolutePath()).append("\" ");

        // 构建过滤器图
        cmd.append("-filter_complex \"");

        // 处理覆盖层图片
        cmd.append("[1]loop=loop=-1:size=1,format=rgba,")
                .append("colorchannelmixer=aa=").append(config.overlayOpacity)
                .append("[overlay];");

        // 叠加覆盖层到视频
//        cmd.append("[0][overlay]overlay=shortest=1")
//                .append(":format=auto:format=rgb");
        cmd.append("[0][overlay]overlay=x=")
                .append(config.overlayX)  // 使用配置的X坐标
                .append(":y=")
                .append(config.overlayY)  // 使用配置的Y坐标
                .append(":shortest=1:format=auto:format=rgb");

        // 添加音频流
        cmd.append("\" -map 0:a -c:a copy ");

        // 视频编码设置
        cmd.append("-c:v libx264 -preset fast -crf 22 -pix_fmt yuv420p ");

        // 输出文件
        cmd.append("\"").append(config.outputVideo.getAbsolutePath()).append("\"");

        return cmd.toString();
    }

    // 执行FFmpeg命令
    private static void executeFFmpegCommand(String command) throws IOException, InterruptedException {
        System.out.println("执行FFmpeg命令:");
        System.out.println(command);

        // 根据操作系统确定命令执行方式
        String[] cmdArray;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            cmdArray = new String[]{"cmd.exe", "/c", command};
        } else {
            cmdArray = new String[]{"/bin/sh", "-c", command};
        }

        ProcessBuilder builder = new ProcessBuilder(cmdArray);
        builder.redirectErrorStream(true);

        Process process = builder.start();

        // 读取输出
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg执行失败，退出码: " + exitCode);
        }
    }

    // 清理临时文件
    private static void cleanTempFiles(GeneratorConfig config, File overlayImage) {
        try {
            Files.deleteIfExists(overlayImage.toPath());
        } catch (IOException e) {
            System.err.println("清理临时文件时出错: " + e.getMessage());
        }
    }

}
