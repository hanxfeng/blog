package org.hanxingfeng.blog.other;

public class FileNameUtil {

    /**
     * 获取文件名去掉前 11 个字符后的剩余部分。
     * 如果文件名长度小于等于 11，则返回空字符串。
     *
     * @param fileName 完整的文件名（可包含扩展名）
     * @return 去除前 11 个字符后的子串，或空字符串
     */
    public static String trimPrefix11(String fileName) {
        if (fileName == null) {
            return null;
        }
        return fileName.length() > 11 ? fileName.substring(11) : "";
    }

    /**
     * 检查两个 .md 文件的名称在去除前 11 个字符后是否相同。
     *
     * @param name1 第一个文件名（例如 "abc123_def_xyz.md"）
     * @param name2 第二个文件名
     * @return 去除前缀后相同返回 true，否则 false
     */
    public static boolean isSameAfterTrim11(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return false;
        }
        String trimmed1 = trimPrefix11(name1);
        String trimmed2 = trimPrefix11(name2);
        return trimmed1.equals(trimmed2);
    }
}