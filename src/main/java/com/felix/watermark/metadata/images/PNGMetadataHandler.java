package com.felix.watermark.metadata.images;


import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.*;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

/**
 * PNG格式AIGC标识添加器
 */
public class PNGMetadataHandler {

    /**
     * 在PNG图片中添加符合GB 45438-2025标准的AIGC隐式标识
     * @param inputFile 输入图片文件
     * @param outputFile 输出图片文件
     * @param aigcMetadata AIGC元数据
     * @return 是否成功
     */
    public static boolean addAIGCMetadata(File inputFile, File outputFile, AIGCMetadata aigcMetadata) {
        try {
            // 读取原始图片
            BufferedImage image = ImageIO.read(inputFile);
            if (image == null) {
                throw new IOException("无法读取图片文件");
            }

            // 获取PNG writer
            ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();

            // 创建包含AIGC元数据的metadata
            IIOMetadata metadata = createAIGCMetadata(writer, writeParam, aigcMetadata);

            // 写入带元数据的图片
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
                writer.setOutput(ios);
                writer.write(new IIOImage(image, null, metadata));
            }

            writer.dispose();
            System.out.println("成功添加AIGC元数据到PNG文件");
            return true;

        } catch (Exception e) {
            System.err.println("添加AIGC元数据失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 创建包含AIGC元数据的IIOMetadata
     * 符合GB 45438-2025标准：元数据位于tEXt chunk，块类型标识符为"AIGC"
     */
    private static IIOMetadata createAIGCMetadata(ImageWriter writer,
                                                  ImageWriteParam writeParam,
                                                  AIGCMetadata aigcMetadata) throws IIOInvalidTreeException {

        IIOMetadata metadata = writer.getDefaultImageMetadata(new ImageTypeSpecifier(
                new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)), writeParam);

        // PNG格式的元数据格式名称
        String metaFormatName = metadata.getNativeMetadataFormatName();

        // 创建元数据树
        IIOMetadataNode root = new IIOMetadataNode(metaFormatName);

        // 生成符合标准的JSON字符串
        String aigcJson = aigcMetadata.toJsonString();

        // 添加AIGC文本数据块 - 符合标准要求：块类型标识符为"AIGC"
        addAIGCTextChunk(root, aigcJson);

        metadata.mergeTree(metaFormatName, root);
        return metadata;
    }

    /**
     * 添加AIGC文本数据块到元数据
     * 符合GB 45438-2025标准：块类型标识符为"AIGC"
     */
    private static void addAIGCTextChunk(IIOMetadataNode root, String aigcJson) {
        if (aigcJson == null || aigcJson.isEmpty()) return;

        IIOMetadataNode textEntry = new IIOMetadataNode("tEXtEntry");
        textEntry.setAttribute("keyword", "AIGC");  // 标准要求的块类型标识符
        textEntry.setAttribute("value", aigcJson);

        // 查找或创建tEXt节点
        IIOMetadataNode textNode = null;
        NodeList nodes = root.getElementsByTagName("tEXt");
        if (nodes.getLength() > 0) {
            textNode = (IIOMetadataNode) nodes.item(0);
        } else {
            textNode = new IIOMetadataNode("tEXt");
            root.appendChild(textNode);
        }

        textNode.appendChild(textEntry);

        System.out.println("添加AIGC tEXt chunk: " + aigcJson);
    }

    /**
     * 从PNG文件读取AIGC元数据
     * @param pngFile PNG文件
     * @return AIGC元数据对象，如果不存在则返回null
     */
    public static AIGCMetadata readAIGCMetadata(File pngFile) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(pngFile)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

            if (!readers.hasNext()) {
                throw new IOException("不支持的图片格式");
            }

            ImageReader reader = readers.next();
            reader.setInput(iis);

            IIOMetadata metadata = reader.getImageMetadata(0);
            AIGCMetadata aigcMetadata = null;

            if (metadata != null) {
                aigcMetadata = parseAIGCMetadata(metadata);
            }

            reader.dispose();
            return aigcMetadata;
        }
    }

    /**
     * 解析AIGC元数据
     */
    private static AIGCMetadata parseAIGCMetadata(IIOMetadata metadata) {
        try {
            String[] formatNames = metadata.getMetadataFormatNames();
            for (String formatName : formatNames) {
                Node root = metadata.getAsTree(formatName);
                AIGCMetadata result = findAIGCTextNode(root);
                if (result != null) {
                    return result;
                }
            }
        } catch (Exception e) {
            System.err.println("解析AIGC元数据时出错: " + e.getMessage());
        }
        return null;
    }

    /**
     * 查找AIGC文本节点并解析
     */
    private static AIGCMetadata findAIGCTextNode(Node node) {
        if (node instanceof IIOMetadataNode) {
            IIOMetadataNode metadataNode = (IIOMetadataNode) node;

            // 处理tEXt条目
            NodeList textEntries = metadataNode.getElementsByTagName("tEXtEntry");

            for (int i = 0; i < textEntries.getLength(); i++) {
                IIOMetadataNode textEntry = (IIOMetadataNode) textEntries.item(i);
                String keyword = textEntry.getAttribute("keyword");
                String value = textEntry.getAttribute("value");

                // 查找关键字为"AIGC"的文本块
                if ("AIGC".equals(keyword)) {
                    return AIGCMetadata.fromJsonString(value);
                }
            }
        }

        // 递归处理子节点
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            AIGCMetadata result = findAIGCTextNode(children.item(i));
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * 检查PNG文件是否包含AIGC元数据
     */
    public static boolean hasAIGCMetadata(File pngFile) {
        try {
            AIGCMetadata metadata = readAIGCMetadata(pngFile);
            return metadata != null;
        } catch (Exception e) {
            return false;
        }
    }

}
