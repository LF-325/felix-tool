package com.felix.utils;

public class OperatingSystem {

    public enum OSType {
        WINDOWS, LINUX, MAC, SOLARIS, OTHER
    }

    private static final OSType detectedOS;

    static {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            detectedOS = OSType.WINDOWS;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            detectedOS = OSType.LINUX;
        } else if (osName.contains("mac")) {
            detectedOS = OSType.MAC;
        } else if (osName.contains("sunos") || osName.contains("solaris")) {
            detectedOS = OSType.SOLARIS;
        } else {
            detectedOS = OSType.OTHER;
        }
    }

    public static OSType getOperatingSystem() {
        return detectedOS;
    }

    public static boolean isWindows() {
        return detectedOS == OSType.WINDOWS;
    }

    public static boolean isLinux() {
        return detectedOS == OSType.LINUX;
    }

    public static boolean isMac() {
        return detectedOS == OSType.MAC;
    }

    public static boolean isUnix() {
        return isLinux() || isMac() || detectedOS == OSType.SOLARIS;
    }

    /**
     * 获取文件路径分隔符
     */
    public static String getFileSeparator() {
        return System.getProperty("file.separator");
    }

    /**
     * 获取路径分隔符（用于PATH等环境变量）
     */
    public static String getPathSeparator() {
        return System.getProperty("path.separator");
    }

    /**
     * 获取行分隔符
     */
    public static String getLineSeparator() {
        return System.getProperty("line.separator");
    }

}
