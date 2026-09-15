package org.hanxingfeng.blog.UtilAndOther;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZIP 压缩/解压工具类
 * 适用于文本类内容（文章正文、JSON 等）
 */
public final class ZipUtil {

    /** 压缩阈值：小于该字节数的内容不压缩，避免压缩后反而变大 */
    private static final int COMPRESS_THRESHOLD = 1024; // 1KB

    /** 缓冲区大小，避免频繁扩容 */
    private static final int BUFFER_SIZE = 8192;

    private ZipUtil() {
    }

    /**
     * 压缩字符串为字节数组
     *
     * @param content 原始文本
     * @return 压缩后的字节数组；若内容过小则返回原始 UTF-8 字节
     */
    public static byte[] compress(String content) {
        if (content == null || content.isEmpty()) {
            return new byte[0];
        }
        byte[] raw = content.getBytes(StandardCharsets.UTF_8);
        // 内容太小，压缩收益低，直接返回原字节
        if (raw.length < COMPRESS_THRESHOLD) {
            return raw;
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(raw.length);
             GZIPOutputStream gzip = new GZIPOutputStream(bos, BUFFER_SIZE)) {
            gzip.write(raw);
            gzip.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("GZIP compress failed", e);
        }
    }

    /**
     * 解压字节数组为字符串
     * 自动识别是否为 GZIP 格式（通过魔数 0x1f 0x8b 判断），兼容未压缩的小文本
     *
     * @param data 压缩后的字节数组
     * @return 原始文本
     */
    public static String decompress(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        // 非 GZIP 格式（未压缩的小内容），直接按 UTF-8 解码
        if (!isGzip(data)) {
            return new String(data, StandardCharsets.UTF_8);
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             GZIPInputStream gzip = new GZIPInputStream(bis, BUFFER_SIZE);
             ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length * 4)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = gzip.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new IllegalStateException("GZIP decompress failed", e);
        }
    }

    /**
     * 判断字节数组是否为 GZIP 格式
     * GZIP 文件头固定为 0x1f 0x8b
     */
    private static boolean isGzip(byte[] data) {
        return data.length >= 2
                && (data[0] & 0xFF) == 0x1f
                && (data[1] & 0xFF) == 0x8b;
    }
}