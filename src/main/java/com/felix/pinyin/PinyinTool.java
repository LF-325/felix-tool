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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PinyinTool {

    // 多音字词典：存储词组到拼音的映射
    private static final Map<String, String> PHRASE_DICT = new HashMap<>();
    // 单字多音字词典（当词组未匹配时使用）
    private static final Map<String, List<String>> CHAR_DICT = new HashMap<>();

    static {
        // 从资源文件加载词典
        loadDictFromResource("phrase_dict.txt", "char_dict.txt");
    }

    /**
     * 从资源文件加载词典
     */
    private static void loadDictFromResource(String phraseFile, String charFile) {
        // 加载词组词典
        try (InputStream is = PinyinTool.class.getClassLoader().getResourceAsStream(phraseFile);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    PHRASE_DICT.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException | NullPointerException e) {
            System.err.println("加载词组词典失败: " + e.getMessage());
        }

        // 加载单字多音字词典
        try (InputStream is = PinyinTool.class.getClassLoader().getResourceAsStream(charFile);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String word = parts[0].trim();
                    String[] pinyins = parts[1].split(",");
                    for (String pinyin : pinyins) {
                        CHAR_DICT.computeIfAbsent(word, k -> new ArrayList<>()).add(pinyin.trim());
                    }
                }
            }
        } catch (IOException | NullPointerException e) {
            System.err.println("加载单字词典失败: " + e.getMessage());
        }
    }

    /**
     * 带声调转换（优化多音字处理）
     */
    public static String toPinyinWithTone(String text) {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setToneType(HanyuPinyinToneType.WITH_TONE_MARK); // 带声调符号
        format.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);

        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);

            // 1. 处理非汉字字符（连续非汉字作为整体）
            if (!isChinese(c)) {
                StringBuilder nonChinese = new StringBuilder();
                while (i < text.length() && !isChinese(text.charAt(i))) {
                    nonChinese.append(text.charAt(i));
                    i++;
                }
                result.append(nonChinese).append(" ");
                continue;
            }

            // 2. 优先尝试匹配词组（从4字到2字）
            boolean phraseMatched = false;
            for (int len = 4; len >= 2; len--) {
                if (i + len <= text.length()) {
                    String phrase = text.substring(i, i + len);
                    if (PHRASE_DICT.containsKey(phrase)) {
                        result.append(PHRASE_DICT.get(phrase)).append(" ");
                        i += len;
                        phraseMatched = true;
                        break;
                    }
                }
            }
            if (phraseMatched) continue;

            // 3. 单字多音字处理
            String charStr = String.valueOf(c);
            if (CHAR_DICT.containsKey(charStr)) {
                // 使用上下文匹配多音字
                String pinyin = resolvePolyphone(text, i, format);
                result.append(pinyin).append(" ");
            }
            // 4. 普通汉字处理
            else {
                try {
                    String[] pinyins = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyins != null && pinyins.length > 0) {
                        result.append(pinyins[0]).append(" ");
                    } else {
                        result.append(c).append(" ");
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    result.append(c).append(" ");
                }
            }
            i++;
        }
        return result.toString().trim();
    }

    /**
     * 改进的多音字消歧算法（基于上下文词组匹配）
     */
    private static String resolvePolyphone(String text, int index, HanyuPinyinOutputFormat format) {
        String currentChar = String.valueOf(text.charAt(index));
        List<String> possiblePinyins = CHAR_DICT.get(currentChar);

        // 1. 尝试向后匹配词组（2-4字）
        for (int len = 2; len <= 4; len++) {
            if (index + len > text.length()) continue;

            String phrase = text.substring(index, index + len);
            if (PHRASE_DICT.containsKey(phrase)) {
                // 从词组拼音中提取当前字的拼音
                String phrasePinyin = PHRASE_DICT.get(phrase);
                String[] pinyinParts = phrasePinyin.split(" ");
                if (pinyinParts.length > 0) {
                    return pinyinParts[0]; // 返回词组的第一个拼音
                }
            }
        }

        // 2. 尝试向前匹配词组（2-4字）
        for (int len = 2; len <= 4; len++) {
            if (index - len + 1 < 0) continue;

            String phrase = text.substring(index - len + 1, index + 1);
            if (PHRASE_DICT.containsKey(phrase)) {
                // 从词组拼音中提取当前字的拼音
                String phrasePinyin = PHRASE_DICT.get(phrase);
                String[] pinyinParts = phrasePinyin.split(" ");
                if (pinyinParts.length >= len) {
                    return pinyinParts[len - 1]; // 返回词组的最后一个拼音
                }
            }
        }

        // 3. 无匹配则返回第一个读音
        return possiblePinyins.get(0);
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
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION;
    }

    public static void main(String[] args) {
        String[] tests = {"重庆银行", "重要长度", "行动重复", "abc长大123", "音乐会长"};
        for (String test : tests) {
            System.out.println(test + " => " + toPinyinWithTone(test));
        }
    }

}
