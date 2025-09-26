package net.jdr2021.utils;

import java.text.DecimalFormat;

public class OtherUtils {

    /**
     * 将字节数格式化为友好字符串。
     *
     * @param bytes 原始字节数（允许为负）
     * @param si    true 使用 SI 单位 (kB = 1000 B)，false 使用 二进制 单位 (KiB = 1024 B)
     * @param decimals 小数位数（>=0）
     * @return 例如 "532 B", "1.23 KiB", "4.56 MB"
     */
    public static String formatSize(long bytes, boolean si, int decimals) {
        if (decimals < 0) throw new IllegalArgumentException("decimals must be >= 0");

        final int unit = si ? 1000 : 1024;
        if (Math.abs(bytes) < unit) {
            return bytes + " B";
        }

        String[] units = si
                ? new String[] { "kB", "MB", "GB", "TB", "PB", "EB" }
                : new String[] { "KiB", "MiB", "GiB", "TiB", "PiB", "EiB" };

        double value = bytes;
        int idx = -1;
        while (Math.abs(value) >= unit && idx < units.length - 1) {
            value /= unit;
            idx++;
        }

        // 构造格式，例如 decimals=2 -> "#,##0.00"
        StringBuilder pattern = new StringBuilder("#,##0");
        if (decimals > 0) {
            pattern.append(".");
            for (int i = 0; i < decimals; i++) pattern.append("0");
        }
        DecimalFormat df = new DecimalFormat(pattern.toString());
        return df.format(value) + " " + units[Math.max(0, idx)];
    }

    // 便捷重载：默认二进制（KiB），保留2位小数
    public static String formatSize(long bytes) {
        return formatSize(bytes, false, 2);
    }

    // 便捷重载：可以选择 SI 或 二进制，默认 2 位小数
    public static String formatSize(long bytes, boolean si) {
        return formatSize(bytes, si, 2);
    }
}
