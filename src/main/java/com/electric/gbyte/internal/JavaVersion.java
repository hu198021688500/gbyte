package com.electric.gbyte.internal;

/**
 * @author bingo
 */
public final class JavaVersion {

    private static final int MAJOR_JAVA_VERSION = determineMajorJavaVersion();

    private static int determineMajorJavaVersion() {
        String javaVersion = System.getProperty("java.version");
        return getMajorJavaVersion(javaVersion);
    }

    private static int getMajorJavaVersion(String javaVersion) {
        int version = parseDotted(javaVersion);
        if (version == -1) {
            version = extractBeginningInt(javaVersion);
        }
        if (version == -1) {
            return 6;
        }
        return version;
    }

    private static int parseDotted(String javaVersion) {
        try {
            String[] parts = javaVersion.split("[._]");
            int firstVer = Integer.parseInt(parts[0]);
            if (firstVer == 1 && parts.length > 1) {
                return Integer.parseInt(parts[1]);
            } else {
                return firstVer;
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int extractBeginningInt(String javaVersion) {
        try {
            StringBuilder num = new StringBuilder();
            for (int i = 0; i < javaVersion.length(); ++i) {
                char c = javaVersion.charAt(i);
                if (Character.isDigit(c)) {
                    num.append(c);
                } else {
                    break;
                }
            }
            return Integer.parseInt(num.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static int getMajorJavaVersion() {
        return MAJOR_JAVA_VERSION;
    }

    private JavaVersion() {

    }
}
