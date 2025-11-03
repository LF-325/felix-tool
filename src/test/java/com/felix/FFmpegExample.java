package com.felix;

import com.felix.media.FFmpegUtil;

import java.util.Arrays;
import java.util.List;

public class FFmpegExample {

    public static void main(String[] args) {
        // 构建FFmpeg命令
        List<String> command = Arrays.asList(
            "ffmpeg",
            "-i", "input.mp4",
            "-c:v", "libx264",
            "-crf", "23",
            "-c:a", "aac",
            "-b:a", "192k",
            "output.mp4"
        );

        try {
            // 同步执行（带60秒超时）
            boolean success = FFmpegUtil.executeCommand(command, 60);
            System.out.println("转换结果: " + (success ? "成功" : "失败"));
        } catch (Exception e) {
            System.err.println("执行出错: " + e.getMessage());
        }

        // 异步执行
        FFmpegUtil.executeAsync(command, (success, exception) -> {
            if (success) {
                System.out.println("异步转换完成");
            } else {
                System.err.println("异步转换失败: " + exception.getMessage());
            }
        });
    }

}
