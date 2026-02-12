package com.felix;

import com.felix.file.DirectoryReader;

import java.io.IOException;
import java.util.List;

/**
 * 博观元数据标识
 */
public class BgAIGCMetadataExample {

    public static void main(String[] args) throws IOException {
        String rootPath = "D:\\data\\非拒答测试题回答图片和视频";
        List<DirectoryReader.FileInfo> files =
                DirectoryReader.readDirectoryWithVisitor(rootPath);
        for (DirectoryReader.FileInfo file : files) {
            String input = file.getPath();
            String output = input.replaceFirst("非拒答测试题回答图片和视频","非拒答测试题回答图片和视频(水印版)");
            System.out.println("input："+ input + " output：" + output + " type：" + file.getType());
        }
    }

}
