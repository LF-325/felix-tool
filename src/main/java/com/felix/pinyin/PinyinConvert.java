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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PinyinConvert {

    // 多音字词典：存储词组到拼音的映射（带数字声调）
    private static final Map<String, String[]> PHRASE_DICT = new HashMap<>();
    // 多音字集合：记录所有多音字
    private static final Set<Character> POLYPHONE_CHARS = new HashSet<>();

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
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // 解析格式：汉字=拼音（空格分隔）
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String phrase = parts[0].trim();
                    String[] pinyinArray = parts[1].trim().split("\\s+");

                    // 保存词组拼音映射
                    PHRASE_DICT.put(phrase, pinyinArray);

                    // 将词组中的多音字添加到集合
                    for (char c : phrase.toCharArray()) {
                        if (isChinese(c)) {
                            POLYPHONE_CHARS.add(c);
                        }
                    }
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
        POLYPHONE_CHARS.clear();

        // 添加常见多音字词组映射
        addPhrase("重庆", "chong2", "qing4");
        addPhrase("重要", "zhong4", "yao4");
        addPhrase("重复", "chong2", "fu4");
        addPhrase("重量", "zhong4", "liang4");
        addPhrase("银行", "yin2", "hang2");
        addPhrase("行动", "xing2", "dong4");
        addPhrase("行业", "hang2", "ye4");
        addPhrase("长度", "chang2", "du4");
        addPhrase("长大", "zhang3", "da4");
        addPhrase("行长", "hang2", "zhang3");
        addPhrase("重阳", "chong2", "yang2");
        addPhrase("重心", "zhong4", "xin1");
        addPhrase("行李", "xing2", "li");
        addPhrase("长久", "chang2", "jiu3");
        addPhrase("长辈", "zhang3", "bei4");
        addPhrase("中国", "zhong1", "guo2");
        addPhrase("音乐", "yin1", "yue4");
        addPhrase("快乐", "kuai4", "le4");
        addPhrase("和平", "he2", "ping2");
        addPhrase("暖和", "nuan3", "huo");
        addPhrase("了结", "liao3", "jie2");
        addPhrase("了解", "liao3", "jie3");
        addPhrase("了得", "liao3", "de");
        addPhrase("好了", "hao3", "le");
        addPhrase("行了", "xing2", "le");
    }

    private static void addPhrase(String phrase, String... pinyins) {
        PHRASE_DICT.put(phrase, pinyins);
        for (char c : phrase.toCharArray()) {
            if (isChinese(c)) {
                POLYPHONE_CHARS.add(c);
            }
        }
    }

    /**
     * 汉字转拼音（多音字标注为"字(拼音)"格式）
     * @param text 输入文本
     * @return 转换后的字符串（如"重(zhong4)量"）
     */
    public static String toMarkedPinyin(String text) {
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

            // 1. 处理非汉字字符（原样输出）
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
                    String[] pinyinArray = PHRASE_DICT.get(phrase);
                    if (pinyinArray != null && pinyinArray.length == phrase.length()) {
                        // 匹配到词组，处理词组中的每个字符
                        for (int j = 0; j < phrase.length(); j++) {
                            char phraseChar = phrase.charAt(j);
                            // 多音字标注拼音，非多音字原样输出
                            if (POLYPHONE_CHARS.contains(phraseChar)) {
                                result.append(phraseChar)
                                        .append('(')
                                        .append(pinyinArray[j])
                                        .append(')');
                            } else {
                                result.append(phraseChar);
                            }
                        }
                        i += phrase.length();
                        phraseMatched = true;
                        break;
                    }
                }
            }
            if (phraseMatched) continue;

            // 3. 处理单个字符
            if (POLYPHONE_CHARS.contains(c)) {
                // 多音字：标注拼音
                try {
                    String[] pinyins = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyins != null && pinyins.length > 0) {
                        result.append(c).append('(').append(pinyins[0]).append(')');
                    } else {
                        result.append(c);
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    result.append(c);
                }
            } else {
                // 非多音字：原样输出
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
                "重量", "重庆银行", "重要长度",
                "abc长大123", "音乐会长", "快乐中国",
                "暖和天气", "和平发展", "银行行长",
                "了解情况", "了结此事", "好了好了","我是测试话术，该说什么"
        };

        for (String text : testCases) {
            System.out.println(text + " => " + toMarkedPinyin(text));
        }
    }

}
