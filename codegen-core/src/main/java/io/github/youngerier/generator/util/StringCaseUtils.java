package io.github.youngerier.generator.util;

/**
 * 字符串命名转换工具类
 */
public final class StringCaseUtils {

    private StringCaseUtils() {
    }

    /**
     * 将字符串首字母转为小写
     *
     * @param str 输入字符串
     * @return 首字母小写的字符串
     */
    public static String lowerFirstChar(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 将字符串首字母转为大写
     *
     * @param str 输入字符串
     * @return 首字母大写的字符串
     */
    public static String upperFirstChar(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
