package net.jdr2021.utils;

import java.io.File;
import java.io.IOException;

public class FileUtils {

    /**
     * 打开文件所在目录，并尽量选中该文件
     *
     * @param filePath 文件路径
     * @throws IOException 如果打开失败
     */
    public static void openExplorerAndSelectFile(String filePath) throws IOException {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new IOException("文件不存在: " + filePath);
        }

        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            openWindowsExplorer(file);
        } else if (osName.contains("mac")) {
            openMacFinder(file);
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            openLinuxFileManager(file);
        } else {
            throw new UnsupportedOperationException("不支持的操作系统: " + osName);
        }
    }

    private static void openWindowsExplorer(File file) throws IOException {
        String absolutePath = file.getAbsolutePath();
        String cmd = "explorer /select,\"" + absolutePath + "\"";
        Runtime.getRuntime().exec(cmd);
    }

    private static void openMacFinder(File file) throws IOException {
        String absolutePath = file.getAbsolutePath();
        String[] cmd = {"open", "-R", absolutePath}; // macOS 支持 -R 选中文件
        Runtime.getRuntime().exec(cmd);
    }

    private static void openLinuxFileManager(File file) throws IOException {
        String absolutePath = file.getAbsolutePath();
        String dir = file.getParent();

        // 常见 Linux 文件管理器
        String[] managers = {"nautilus", "xdg-open", "dolphin", "thunar", "pcmanfm"};

        boolean opened = false;
        for (String manager : managers) {
            try {
                Process process = new ProcessBuilder(manager, dir).start();
                if (process.isAlive()) {
                    opened = true;
                    break;
                }
            } catch (IOException ignored) {}
        }

        if (!opened) {
            throw new IOException("无法打开 Linux 文件管理器");
        }
    }

}
