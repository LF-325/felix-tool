package com.felix;

import com.felix.images.metadata.ImageMetadataUtil;

public class ImageMetadataExample {

    public static void main(String[] args) {
//        jpgTest();

        pngTest();
    }

    public static void jpgTest() {
        String sourceFilePath = "D:\\data\\watermark\\wm.jpg";
        String destFilePath = "D:\\data\\watermark\\wm_out.jpg";
        try {
            ImageMetadataUtil.writeExifUserComment(sourceFilePath, destFilePath, "{\"AIGC\":\"true\",\"PropagateID\":\"123456\"}");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void pngTest() {
        String sourceFilePath = "D:\\data\\watermark\\sy.png";
        String destFilePath = "D:\\data\\watermark\\sy_out.png";
        try {
            ImageMetadataUtil.writePngCustomData(sourceFilePath, destFilePath, "AIGC", "{\"AIGC\":\"true\",\"PropagateID\":\"123456\"}");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
