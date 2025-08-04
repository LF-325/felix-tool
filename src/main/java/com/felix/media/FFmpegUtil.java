package com.felix.media;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FFmpegUtil {

    /**
     * 执行FFmpeg命令
     * @param command FFmpeg命令列表
     * @param timeout 超时时间（秒），0表示无超时
     * @return 执行结果（true表示成功）
     * @throws IOException 输入输出异常
     * @throws InterruptedException 进程中断异常
     */
    public static boolean executeCommand(List<String> command, long timeout) 
        throws IOException, InterruptedException {
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        // 合并错误流到标准输出流
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream());
        outputGobbler.start();
        
        boolean success;
        if (timeout > 0) {
            success = process.waitFor(timeout, TimeUnit.SECONDS);
        } else {
            process.waitFor();
            success = true;
        }
        
        if (!success) {
            process.destroy();
            throw new RuntimeException("FFmpeg command timed out");
        }
        
        int exitCode = process.exitValue();
        outputGobbler.join(); // 确保输出内容被完整读取
        
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg execution failed with code: " + exitCode + 
                                     "\nOutput: " + outputGobbler.getOutput());
        }
        
        return true;
    }

    /**
     * 异步执行FFmpeg命令（不阻塞当前线程）
     * @param command FFmpeg命令列表
     * @param callback 执行结果回调
     */
    public static void executeAsync(List<String> command, FFmpegCallback callback) {
        new Thread(() -> {
            try {
                boolean success = executeCommand(command, 0);
                callback.onComplete(success, null);
            } catch (Exception e) {
                callback.onComplete(false, e);
            }
        }).start();
    }

    /**
     * 内部类：流内容处理器
     */
    private static class StreamGobbler extends Thread {
        private InputStream inputStream;
        private StringBuilder output = new StringBuilder();

        StreamGobbler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // 可选：打印实时日志
                    System.out.println("[FFmpeg] " + line);
                }
            } catch (IOException e) {
                System.err.println("Error reading FFmpeg output: " + e.getMessage());
            }
        }

        public String getOutput() {
            return output.toString();
        }
    }

    /**
     * 回调接口
     */
    public interface FFmpegCallback {
        void onComplete(boolean success, Throwable exception);
    }

}
