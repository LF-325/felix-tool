package com.felix;

import com.felix.media.WeatherVideoGenerator;

import java.awt.*;
import java.io.File;

public class WeatherVideoGenExample {

    public static void main(String[] args) {
        try {
            // 1. 创建配置对象
            WeatherVideoGenerator.GeneratorConfig config =
                    new WeatherVideoGenerator.GeneratorConfig();

            config.ffmpeg = "D:\\software\\ffmpeg-n7.1-latest-win64-gpl-7.1\\bin\\ffmpeg.exe";

            // 2. 设置基本参数
            config.sourceVideo = new File("D:\\data\\watermark\\fj.mp4");
            config.outputVideo = new File("D:\\data\\watermark\\weather_forecast.mp4");
            config.weatherIconsDir = new File("D:\\data\\watermark\\weather");
            config.tempDir = new File("D:\\data\\watermark\\temp");

            // 3. 设置文字样式
            config.textColor = new Color(255, 255, 255, 220); // 半透明白色
            config.cityFontSize = 42;
            config.tempFontSize = 56;
            config.overlayOpacity = 0.9f; // 90%透明度

            config.resolutionWidth = 400;
            config.resolutionHeight = 500;

            config.keepTempFiles = true;

            // 4. 准备天气数据
            WeatherVideoGenerator.WeatherData weatherData =
                    new WeatherVideoGenerator.WeatherData(
                            "西安",
                            "RAINY",
                            32,
                            "中到大雨，请携带雨具"
                    );

            config.weatherData = weatherData;

            // 5. 生成视频
            WeatherVideoGenerator.generateWeatherVideo(config);

            System.out.println("天气预报视频已生成");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
