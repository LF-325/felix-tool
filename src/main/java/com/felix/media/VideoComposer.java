package com.felix.media;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 通用视频合成工具类，支持添加文字、图片等覆盖层到视频上
 */
public class VideoComposer {

    // 通用覆盖层数据接口
    public interface OverlayData {
        int getDuration(); // 显示时长(秒)
    }

    // 天气数据模型 - 实现覆盖层数据接口
    public static class WeatherData implements OverlayData {
        public final String city;
        public final String weatherType;
        public final int temperature;
        public final String description;
        public final int duration; // 显示时长(秒)

        public WeatherData(String city, String weatherType, int temperature,
                           String description, int duration) {
            this.city = city;
            this.weatherType = weatherType;
            this.temperature = temperature;
            this.description = description;
            this.duration = duration;
        }

        @Override
        public int getDuration() {
            return duration;
        }
    }

    // 配置参数
    public static class GeneratorConfig {
        public File sourceVideo;       // 源视频文件
        public File outputVideo;       // 输出视频文件
        public List<? extends OverlayData> overlayData; // 覆盖层数据列表
        public File iconsDir;          // 图标目录
        public File tempDir;           // 临时工作目录
        public int resolutionWidth = 1280;  // 视频分辨率宽
        public int resolutionHeight = 720;  // 视频分辨率高
        public String fontName = "微软雅黑"; // 字体名称
        public int titleFontSize = 36;      // 标题字体大小
        public int valueFontSize = 48;      // 值字体大小
        public int descFontSize = 24;       // 描述字体大小
        public Color textColor = Color.WHITE; // 文字颜色
        public int textShadow = 2;           // 文字阴影大小
        public boolean keepTempFiles = false; // 是否保留临时文件
        public String ffmpegPath = "ffmpeg"; // FFmpeg可执行文件路径
        public OverlayGenerator overlayGenerator; // 覆盖层生成器
    }

    // 覆盖层生成器接口
    public interface OverlayGenerator {
        File generateOverlayImage(GeneratorConfig config, OverlayData data, Map<String, File> icons, int index) throws IOException;
    }

    // 天气覆盖层生成器
    public static class WeatherOverlayGenerator implements OverlayGenerator {
        @Override
        public File generateOverlayImage(GeneratorConfig config, OverlayData data, Map<String, File> icons, int index) throws IOException {
            if (!(data instanceof WeatherData)) {
                throw new IllegalArgumentException("数据必须是WeatherData类型");
            }

            WeatherData weatherData = (WeatherData) data;
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
            File iconFile = icons.get(weatherData.weatherType.toUpperCase());
            BufferedImage icon = null;
            int iconSize = 120;

            if (iconFile != null) {
                icon = ImageIO.read(iconFile);
                // 调整图标大小
                icon = resizeImage(icon, iconSize, iconSize);
            }

            // 绘制天气图标
            if (icon != null) {
                int iconX = (width - iconSize) / 2;
                int iconY = height / 4;
                g2d.drawImage(icon, iconX, iconY, null);
            }

            // 设置字体和颜色
            Font cityFont = new Font(config.fontName, Font.BOLD, config.titleFontSize);
            Font tempFont = new Font(config.fontName, Font.BOLD, config.valueFontSize);
            Font descFont = new Font(config.fontName, Font.PLAIN, config.descFontSize);

            g2d.setColor(config.textColor);

            // 绘制城市名称
            g2d.setFont(cityFont);
            FontMetrics cityMetrics = g2d.getFontMetrics(cityFont);
            int cityX = (width - cityMetrics.stringWidth(weatherData.city)) / 2;
            int cityY = height / 4 - iconSize - 20;
            drawTextWithShadow(g2d, weatherData.city, cityX, cityY, config.textShadow);

            // 绘制温度
            String tempText = weatherData.temperature + "°C";
            g2d.setFont(tempFont);
            FontMetrics tempMetrics = g2d.getFontMetrics(tempFont);
            int tempX = (width - tempMetrics.stringWidth(tempText)) / 2;
            int tempY = height / 4 + iconSize + 50;
            drawTextWithShadow(g2d, tempText, tempX, tempY, config.textShadow);

            // 绘制描述
            g2d.setFont(descFont);
            FontMetrics descMetrics = g2d.getFontMetrics(descFont);
            int descX = (width - descMetrics.stringWidth(weatherData.description)) / 2;
            int descY = tempY + 60;
            drawTextWithShadow(g2d, weatherData.description, descX, descY, config.textShadow);

            g2d.dispose();

            // 保存图片
            File overlayImage = new File(config.tempDir, "overlay_" + index + ".png");
            ImageIO.write(image, "PNG", overlayImage);

            return overlayImage;
        }
    }

    // 生成带覆盖层的视频
    public static void generateVideo(GeneratorConfig config) throws Exception {
        // 验证输入参数
        validateConfig(config);

        // 创建临时目录
        Files.createDirectories(config.tempDir.toPath());

        // 步骤1: 准备图标
        Map<String, File> icons = prepareIcons(config);

        // 步骤2: 生成分段时间轴文件
        File timelineFile = generateTimelineFile(config);

        // 步骤3: 为每个覆盖层数据生成覆盖层图片
        List<File> overlayImages = new ArrayList<>();
        for (int i = 0; i < config.overlayData.size(); i++) {
            OverlayData data = config.overlayData.get(i);
            File overlayImage = config.overlayGenerator.generateOverlayImage(config, data, icons, i);
            overlayImages.add(overlayImage);
        }

        // 步骤4: 生成FFmpeg命令文件
        File ffmpegScript = generateFFmpegScript(config, timelineFile, overlayImages);

        // 步骤5: 执行FFmpeg命令
        executeFFmpegCommand(ffmpegScript, config);

        // 步骤6: 清理临时文件
        if (!config.keepTempFiles) {
            cleanTempFiles(config, timelineFile, overlayImages, ffmpegScript);
        }

        System.out.println("视频生成成功: " + config.outputVideo.getAbsolutePath());
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

        if (config.overlayData == null || config.overlayData.isEmpty()) {
            throw new IllegalArgumentException("覆盖层数据不能为空");
        }

        if (config.iconsDir != null && !config.iconsDir.exists()) {
            throw new FileNotFoundException("图标目录不存在");
        }

        if (config.tempDir == null) {
            throw new IllegalArgumentException("临时目录未指定");
        }

        if (config.overlayGenerator == null) {
            throw new IllegalArgumentException("覆盖层生成器未指定");
        }

        // 验证FFmpeg路径
        File ffmpegFile = new File(config.ffmpegPath);
        if (!ffmpegFile.exists() && !config.ffmpegPath.equals("ffmpeg")) {
            throw new FileNotFoundException("FFmpeg可执行文件不存在: " + config.ffmpegPath);
        }
    }

    // 准备图标
    private static Map<String, File> prepareIcons(GeneratorConfig config) {
        Map<String, File> icons = new HashMap<>();
        if (config.iconsDir == null) {
            return icons;
        }

        File[] iconFiles = config.iconsDir.listFiles();
        if (iconFiles != null) {
            for (File file : iconFiles) {
                String fileName = file.getName().toLowerCase();
                String iconType = fileName.substring(0, fileName.lastIndexOf('.'));
                icons.put(iconType.toUpperCase(), file);
            }
        }

        return icons;
    }

    // 生成时间轴文件
    private static File generateTimelineFile(GeneratorConfig config) throws IOException {
        File timelineFile = new File(config.tempDir, "timeline.txt");

        try (PrintWriter writer = new PrintWriter(timelineFile)) {
            double startTime = 0.0;

            for (OverlayData data : config.overlayData) {
                writer.printf("file '%s'\n", config.sourceVideo.getAbsolutePath().replace("\\", "\\\\"));
                writer.printf("inpoint %.2f\n", startTime);
                writer.printf("outpoint %.2f\n", startTime + data.getDuration());
                startTime += data.getDuration();
            }
        }

        return timelineFile;
    }

    // 生成FFmpeg脚本
    private static File generateFFmpegScript(GeneratorConfig config, File timelineFile,
                                             List<File> overlayImages) throws IOException {
        File scriptFile = new File(config.tempDir, "ffmpeg_script.bat");
        String ffmpegPath = config.ffmpegPath;

        try (PrintWriter writer = new PrintWriter(scriptFile)) {
            writer.println("@echo off");
            writer.println("set FFMPEG=ffmpeg -y -hide_banner -loglevel warning");
            writer.println();

            // 步骤1: 创建输入文件列表
            writer.println("rem 步骤1: 连接源视频片段");
            writer.printf("\"%s\" -f concat -safe 0 -i \"%s\" -c copy no_overlay.mp4\n",
                    ffmpegPath, timelineFile.getAbsolutePath());
            writer.println();

            // 步骤2: 生成覆盖层视频
            writer.println("rem 步骤2: 生成覆盖层视频");
            writer.printf("\"%s\" -f lavfi -i color=color=black:size=%dx%d:duration=%d:rate=30 ",
                    ffmpegPath, config.resolutionWidth, config.resolutionHeight, getTotalDuration(config));
            writer.println("-vf \\\\");

            for (int i = 0; i < overlayImages.size(); i++) {
                double start = getStartTime(config, i);
                double duration = config.overlayData.get(i).getDuration();

                writer.printf("    [0:v]drawbox=color=black@0:replace=1:t=fill,");
                writer.printf("drawtext=text='':fontsize=0,");
                writer.printf("enable='between(t,%.2f,%.2f)'[base];\n", start, start + duration);

                writer.printf("    [base]");
                writer.printf("movie='%s'[overlay%d];\n",
                        overlayImages.get(i).getAbsolutePath().replace("\\", "\\\\"), i);

                writer.printf("    [overlay%d]format=rgba,", i);
                writer.printf("colorchannelmixer=aa=0.8[ovl%d];\n", i);

                writer.printf("    [base][ovl%d]overlay=eof_action=pass[base];\n", i);
            }

            writer.println("\" -c:v libx264 -pix_fmt yuv420p overlay.mp4");
            writer.println();

            // 步骤3: 合并视频和覆盖层
            writer.println("rem 步骤3: 合并视频和覆盖层");
            writer.printf("\"%s\" -i no_overlay.mp4 -i overlay.mp4 ", ffmpegPath);
            writer.println("-filter_complex \\");
            writer.println("    [0:v]setpts=PTS-STARTPTS[bg];");
            writer.println("    [1:v]setpts=PTS-STARTPTS,format=yuva420p,");
            writer.println("        colorchannelmixer=aa=0.7[fg];");
            writer.println("    [bg][fg]overlay=shortest=1\\");
            writer.printf(" -c:a copy \"%s\"\n", config.outputVideo.getAbsolutePath());
        }

        return scriptFile;
    }

    // 计算总时长
    private static int getTotalDuration(GeneratorConfig config) {
        return config.overlayData.stream().mapToInt(OverlayData::getDuration).sum();
    }

    // 获取开始时间
    private static double getStartTime(GeneratorConfig config, int index) {
        double start = 0.0;
        for (int i = 0; i < index; i++) {
            start += config.overlayData.get(i).getDuration();
        }
        return start;
    }

    // 执行FFmpeg命令
    private static void executeFFmpegCommand(File scriptFile, GeneratorConfig config) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", scriptFile.getAbsolutePath());
        builder.redirectErrorStream(true);
        builder.directory(scriptFile.getParentFile());

        // 设置环境变量，确保能找到FFmpeg
        Map<String, String> env = builder.environment();
        String path = env.get("PATH");
        File ffmpegDir = new File(config.ffmpegPath).getParentFile();
        if (ffmpegDir != null && ffmpegDir.exists()) {
            env.put("PATH", ffmpegDir.getAbsolutePath() + ";" + path);
        }

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
    private static void cleanTempFiles(GeneratorConfig config, File timelineFile,
                                       List<File> overlayImages, File ffmpegScript) {
        try {
            Files.deleteIfExists(timelineFile.toPath());
            for (File overlay : overlayImages) {
                Files.deleteIfExists(overlay.toPath());
            }
            Files.deleteIfExists(ffmpegScript.toPath());
            Files.deleteIfExists(new File(config.tempDir, "no_overlay.mp4").toPath());
            Files.deleteIfExists(new File(config.tempDir, "overlay.mp4").toPath());
        } catch (IOException e) {
            System.err.println("清理临时文件时出错: " + e.getMessage());
        }
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
        g2d.setColor(g2d.getBackground());
        g2d.drawString(text, x, y);
    }

    // 示例用法
    public static void main(String[] args) {
        try {
            // 准备配置
            GeneratorConfig config = new GeneratorConfig();
            config.sourceVideo = new File("D:\\data\\watermark\\fj.mp4");
            config.outputVideo = new File("D:\\data\\watermark\\weather_forecast.mp4");
            config.iconsDir = new File("D:\\data\\watermark\\weather");
            config.tempDir = new File("D:\\data\\watermark\\temp");
            config.textColor = new Color(255, 255, 255, 220); // 半透明白色
            config.ffmpegPath = "D:\\software\\ffmpeg-n7.1-latest-win64-gpl-7.1\\bin\\ffmpeg.exe"; // 设置FFmpeg路径
            config.overlayGenerator = new WeatherOverlayGenerator(); // 设置天气覆盖层生成器

            // 准备天气数据
            List<WeatherData> weatherData = new ArrayList<>();
            weatherData.add(new WeatherData("朝阳区", "SUNNY", 28, "晴朗炎热，注意防晒", 5));
            weatherData.add(new WeatherData("海淀区", "CLOUDY", 25, "多云转阴，适宜出行", 4));

            config.overlayData = weatherData;

            // 生成视频
            generateVideo(config);

            System.out.println("天气预报视频生成完成!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}