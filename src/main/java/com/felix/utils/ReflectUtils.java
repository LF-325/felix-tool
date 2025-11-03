package com.felix.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ReflectUtils {

    /**
     * 判断字符串是否为类的属性并赋值
     * @param obj 目标对象
     * @param fieldName 属性名
     * @param value 要设置的值
     * @return 是否成功设置
     */
    public static boolean setFieldValue(Object obj, String fieldName, Object value) {
        try {
            Class<?> clazz = obj.getClass();
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
            return true;
        } catch (NoSuchFieldException e) {
            System.out.println("属性 '" + fieldName + "' 不存在");
            return false;
        } catch (IllegalAccessException e) {
            System.out.println("无法访问属性 '" + fieldName + "'");
            return false;
        }
    }

    /**
     * 获取属性值
     */
    public static Object getFieldValue(Object obj, String fieldName) {
        try {
            Class<?> clazz = obj.getClass();
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查类是否包含指定属性
     */
    public static boolean hasField(Class<?> clazz, String fieldName) {
        try {
            clazz.getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    // ========== 新增：获取所有属性名的方法 ==========

    /**
     * 获取类的所有属性名（不包含继承的父类属性）
     * @param clazz 目标类
     * @return 属性名列表
     */
    public static List<String> getDeclaredFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());
    }

    /**
     * 获取类的所有属性名（包含继承的父类属性）
     * @param clazz 目标类
     * @return 属性名列表
     */
    public static List<String> getAllFieldNames(Class<?> clazz) {
        List<String> fieldNames = new ArrayList<>();
        Class<?> currentClass = clazz;

        while (currentClass != null && currentClass != Object.class) {
            Field[] fields = currentClass.getDeclaredFields();
            for (Field field : fields) {
                fieldNames.add(field.getName());
            }
            currentClass = currentClass.getSuperclass();
        }

        return fieldNames;
    }

    /**
     * 获取类的所有属性名（可配置是否包含父类属性）
     * @param clazz 目标类
     * @param includeSuperClass 是否包含父类属性
     * @return 属性名列表
     */
    public static List<String> getFieldNames(Class<?> clazz, boolean includeSuperClass) {
        if (includeSuperClass) {
            return getAllFieldNames(clazz);
        } else {
            return getDeclaredFieldNames(clazz);
        }
    }

    /**
     * 获取类的所有属性（Field对象），包含继承的父类属性
     * @param clazz 目标类
     * @return Field对象列表
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = clazz;

        while (currentClass != null && currentClass != Object.class) {
            Field[] declaredFields = currentClass.getDeclaredFields();
            for (Field field : declaredFields) {
                field.setAccessible(true);
                fields.add(field);
            }
            currentClass = currentClass.getSuperclass();
        }

        return fields;
    }

    /**
     * 获取类的所有属性（Field对象），可配置是否包含父类属性
     * @param clazz 目标类
     * @param includeSuperClass 是否包含父类属性
     * @return Field对象列表
     */
    public static List<Field> getFields(Class<?> clazz, boolean includeSuperClass) {
        if (includeSuperClass) {
            return getAllFields(clazz);
        } else {
            return Arrays.asList(clazz.getDeclaredFields());
        }
    }

    /**
     * 获取所有属性名和类型的映射
     * @param clazz 目标类
     * @return 属性名-类型映射
     */
    public static java.util.Map<String, Class<?>> getFieldNameTypeMap(Class<?> clazz) {
        return getAllFields(clazz).stream()
                .collect(Collectors.toMap(
                        Field::getName,
                        Field::getType,
                        (existing, replacement) -> existing  // 如果有重复属性，保留第一个
                ));
    }

    /**
     * 获取所有属性名和值的映射
     * @param obj 目标对象
     * @return 属性名-值映射
     */
    public static java.util.Map<String, Object> getFieldValueMap(Object obj) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        try {
            List<Field> fields = getAllFields(obj.getClass());
            for (Field field : fields) {
                field.setAccessible(true);
                result.put(field.getName(), field.get(obj));
            }
        } catch (IllegalAccessException e) {
            System.out.println("获取属性值失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 打印类的所有属性信息
     * @param clazz 目标类
     */
    public static void printClassFields(Class<?> clazz) {
        System.out.println("=== " + clazz.getSimpleName() + " 属性信息 ===");

        List<Field> fields = getAllFields(clazz);
        for (Field field : fields) {
            System.out.printf("属性名: %-20s 类型: %-15s 修饰符: %s%n",
                    field.getName(),
                    field.getType().getSimpleName(),
                    java.lang.reflect.Modifier.toString(field.getModifiers()));
        }
        System.out.println("总计: " + fields.size() + " 个属性");
    }

}
