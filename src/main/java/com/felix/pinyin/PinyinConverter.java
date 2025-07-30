package com.felix.pinyin;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
public class PinyinConverter {

    // 多音字词典：存储词组到拼音的映射（带数字声调）
    private static final Map<String, String> PHRASE_DICT = new HashMap<>();

    static {
        // 从txt文件加载多音词典
        loadPolyphoneDict("polyphone_dict.txt");
    }

    /**
     * 从资源文件加载多音词典
     * @param fileName 词典文件名
     */
    private static void loadPolyphoneDict(String fileName) {
        try (InputStream is = PinyinConverter.class.getClassLoader().getResourceAsStream(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // 跳过空行和注释行
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // 解析格式：汉字=拼音（无空格）
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String phrase = parts[0].trim();
                    String pinyin = parts[1].trim();
                    PHRASE_DICT.put(phrase, pinyin);
                }
            }
        } catch (IOException | NullPointerException e) {
            // 文件不存在时使用内置默认词典
            initDefaultDict();
            System.err.println("WARN: 多音词典文件 '" + fileName + "' 未找到，使用内置默认词典");
        }
    }

    /**
     * 初始化内置默认多音词典
     */
    private static void initDefaultDict() {
        // 清除可能存在的旧数据
        PHRASE_DICT.clear();

        // 添加常见多音字词组映射
        PHRASE_DICT.put("重庆", "chong2qing4");
        PHRASE_DICT.put("重要", "zhong4yao4");
        PHRASE_DICT.put("重复", "chong2fu4");
        PHRASE_DICT.put("重量", "zhong4liang4");
        PHRASE_DICT.put("银行", "yin2hang2");
        PHRASE_DICT.put("行动", "xing2dong4");
        PHRASE_DICT.put("行业", "hang2ye4");
        PHRASE_DICT.put("长度", "chang2du4");
        PHRASE_DICT.put("长大", "zhang3da4");
        PHRASE_DICT.put("行长", "hang2zhang3");
        PHRASE_DICT.put("重阳", "chong2yang2");
        PHRASE_DICT.put("重心", "zhong4xin1");
        PHRASE_DICT.put("行李", "xing2li");
        PHRASE_DICT.put("长久", "chang2jiu3");
        PHRASE_DICT.put("长辈", "zhang3bei4");
        PHRASE_DICT.put("中国", "zhong1guo2");
        PHRASE_DICT.put("音乐", "yin1yue4");
        PHRASE_DICT.put("快乐", "kuai4le4");
        PHRASE_DICT.put("和平", "he2ping2");
        PHRASE_DICT.put("暖和", "nuan3huo");
        PHRASE_DICT.put("了结", "liao3jie2");
        PHRASE_DICT.put("了解", "liao3jie3");
        PHRASE_DICT.put("了得", "liao3de");
        PHRASE_DICT.put("好了", "hao3le");
        PHRASE_DICT.put("行了", "xing2le");
    }

    /**
     * 汉字转拼音（带数字声调，无空格分隔）
     * @param text 输入文本
     * @return 拼音字符串（如"zhong1guo2"）
     */
    public static String toPinyin(String text) {
        // 设置拼音输出格式：小写、数字声调、v表示ü
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setToneType(HanyuPinyinToneType.WITH_TONE_NUMBER);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);

        StringBuilder result = new StringBuilder();
        int i = 0;
        final int length = text.length();

        while (i < length) {
            char c = text.charAt(i);

            // 1. 非汉字直接追加
            if (!isChinese(c)) {
                result.append(c);
                i++;
                continue;
            }

            // 2. 优先尝试匹配词组（从4字到2字）
            boolean phraseMatched = false;
            for (int phraseLen = 4; phraseLen >= 2; phraseLen--) {
                if (i + phraseLen <= length) {
                    String phrase = text.substring(i, i + phraseLen);
                    String pinyin = PHRASE_DICT.get(phrase);
                    if (pinyin != null) {
                        result.append(pinyin);
                        i += phraseLen;
                        phraseMatched = true;
                        break;
                    }
                }
            }
            if (phraseMatched) continue;

            // 3. 处理单字
            try {
                String[] pinyins = PinyinHelper.toHanyuPinyinStringArray(c, format);
                if (pinyins != null && pinyins.length > 0) {
                    // 多音字选择：优先第一个读音
                    result.append(pinyins[0]);
                } else {
                    result.append(c); // 非汉字字符
                }
            } catch (BadHanyuPinyinOutputFormatCombination e) {
                result.append(c);
            }
            i++;
        }

        return result.toString();
    }

    /**
     * 判断字符是否为汉字
     */
    private static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION;
    }

    public static void main(String[] args) {
        String[] testCases = {
                "中国", "重庆银行", "重要长度",
                "abc长大123", "音乐会长", "快乐中国",
                "暖和天气", "和平发展", "银行行长",
                "了解情况", "了结此事", "好了好了"
        };

        for (String text : testCases) {
            System.out.println(text + " => " + toPinyin(text));
        }
    }

}
