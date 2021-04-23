package mcmaster.ilos.datacollectionapp.Utils;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;

/* This class contains many methods used to read the availability of disk space on the devices local storage and external SD cards,
 * as well as some storage formatting methods. */
public class Storage {

    private static boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }

    public static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }

    public static long getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return totalBlocks * blockSize;
    }

    public static long getAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long availableBlocks = stat.getAvailableBlocksLong();
            return availableBlocks * blockSize;
        } else {
            return 0;
        }
    }

    public static long getTotalExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            return totalBlocks * blockSize;
        } else {
            return 0;
        }
    }

    public static String formatSize(long size) {
        String suffix = "Bytes";

        if (size >= 1024) {
            suffix = " Kb";
            size /= 1024;
            if (size >= 1024) {
                suffix = " Mb";
                size /= 1024;
                if (size >= 1024) {
                    suffix = " Gb";
                    size /= 1024;
                }
            }
        }

        StringBuilder resultBuffer = new StringBuilder(Long.toString(size));

        int commaOffset = resultBuffer.length() - 3;
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',');
            commaOffset -= 3;
        }

        if (suffix != null) resultBuffer.append(suffix);
        return resultBuffer.toString();
    }

    public static Double unformatSize(String size) {

        String suffix = size.split(" ")[1];
        String number = size.split(" ")[0];

        double amount = Double.parseDouble(number.trim());

        if (suffix.equals("Kb")) {
            amount = amount*1024;
        }

        if (suffix.equals("Mb")) {
            amount = amount*1024*1024;
        }

        if (suffix.equals("Gb")) {
            amount = amount*1024*1024*1024;
        }

        return amount;
    }

    public static long getFreeDiskSpace() {
        long aInternal = getAvailableInternalMemorySize();
        long aExternal = getAvailableExternalMemorySize();
        long tExternal = getTotalExternalMemorySize();

        long availableSize;
        if (tExternal == 0) {
            availableSize = aInternal;
        } else {
            availableSize = aExternal;
        }

        return availableSize;
    }

    public static boolean isDiskSpaceEmpty() {
        long aInternal = getAvailableInternalMemorySize();
        long aExternal = getAvailableExternalMemorySize();
        long tExternal = getTotalExternalMemorySize();

        long availableSize;
        if (tExternal == 0) {
            availableSize = aInternal;
        } else {
            availableSize = aExternal;
        }

        // 50Mb - must have at least 50 to collect data!
        return availableSize < 1024 * 1024 * 50;
    }
}
