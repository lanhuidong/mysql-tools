package com.nexusy.mysql;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lan
 * @since 2017-02-16
 */
public class TableSpaceId {

    private static String dbNewDir = "D:\\zenointel";
    private static String dbOldDir = "D:\\DB\\data\\zenointel";

    public static void main(String[] args) throws Exception {
        File dir = new File(dbNewDir);
        Map<String, byte[]> map = new HashMap<>();
        for (File file : dir.listFiles()) {
            if (file.getName().endsWith("ibd")) {
                try(RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                    byte[] data = new byte[2];
                    raf.skipBytes(36);
                    raf.read(data);
                    map.put(file.getName(), data);
                    System.out.println(file.getName() + ":" + byte2Hex(data[0]) + byte2Hex(data[1]));
                }
            }
        }
        File oldDir = new File(dbOldDir);
        for (File file : oldDir.listFiles()) {
            if (file.getName().endsWith("ibd")) {
                byte[] data = map.get(file.getName());
               try(RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                   raf.seek(36);
                   raf.write(data);
                   raf.seek(40);
                   raf.write(data);
               }
            }
        }
    }

    private static String byte2Hex(byte data) {
        int i = 0;
        i = i | data;
        String s = Integer.toHexString(i);
        if (s.length() > 2) {
            s = s.substring(s.length() - 2, s.length());
        }
        return s;
    }
}
