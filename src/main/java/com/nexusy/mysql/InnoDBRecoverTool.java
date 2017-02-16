package com.nexusy.mysql;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * @author lanhuidong
 * @since 2017-02-16
 */
public class InnoDBRecoverTool {

    private static String jdbcUrl;
    private static String dbUsername;
    private static String dbPassword;
    private static String oldDbDir;
    private static String newDbDir;
    private static Set<String> ignoredTables = new HashSet<>();

    public static void main(String[] args) {
        String cmd;
        if (args.length > 0) {
            cmd = args[0];
        } else {
            cmd = "help";
        }
        init();
        if ("help".equals(cmd)) {
            System.out.println("使用方法：");
            System.out.println("    --createTables    根据损坏的数据库目录中的表创建新表");
            System.out.println("    --replaceSpaceIds    将旧*.ibd文件中的space id替换为新建*.ibd文件中的space id");
            System.out.println("    --discardTablespaces    撤销表空间");
            System.out.println("    --importTablespaces    导入表空间");
        } else if ("--createTables".equals(cmd)) {
            createTables();
        } else if ("--replaceSpaceIds".equals(cmd)) {
            replaceSpaceIds();
        } else if ("--discardTablespaces".equals(cmd)) {
            alertTablespaces("DISCARD");
        } else if ("--importTablespaces".equals(cmd)) {
            alertTablespaces("IMPORT");

        }
    }

    private static void init() {
        Properties props = new Properties();
        try (InputStream in = InnoDBRecoverTool.class.getClassLoader().getResourceAsStream("config.properties")) {
            props.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        jdbcUrl = props.getProperty("jdbcUrl");
        dbUsername = props.getProperty("dbUsername");
        dbPassword = props.getProperty("dbPassword");
        oldDbDir = props.getProperty("oldDbDir");
        newDbDir = props.getProperty("newDbDir");
        String[] ignoredTableNames = props.getProperty("ignoredTables", "").split(",");
        for (String ignoredTableName : ignoredTableNames) {
            ignoredTables.add(ignoredTableName);
        }
    }

    private static void createTables() {
        File dir = new File(oldDbDir);
        List<String> tableNames = new ArrayList<>();
        String[] fileNames = dir.list();
        if (fileNames != null) {
            for (String fileName : fileNames) {
                if (fileName.endsWith("frm")) {
                    String tableName = fileName.substring(0, fileName.indexOf(".frm"));
                    tableNames.add(tableName);
                }
            }
        }
        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUsername, dbPassword);
             Statement stmt = conn.createStatement()) {
            for (String tableName : tableNames) {
                String sql = "CREATE TABLE " + tableName + " (id int(11) NOT NULL ) ENGINE=InnoDB";
                boolean result = stmt.execute(sql);
                if (result) {
                    System.out.println("创建表：" + tableName + "成功");
                } else {
                    System.out.println("创建表：" + tableName + "失败");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void replaceSpaceIds() {
        File dir = new File(newDbDir);
        Map<String, byte[]> map = new HashMap<>();
        for (File file : dir.listFiles()) {
            if (file.getName().endsWith("ibd")) {
                try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                    byte[] data = new byte[2];
                    raf.skipBytes(36);
                    raf.read(data);
                    map.put(file.getName(), data);
                    System.out.println(file.getName() + ":" + byte2Hex(data[0]) + byte2Hex(data[1]));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        File oldDir = new File(oldDbDir);
        for (File file : oldDir.listFiles()) {
            if (file.getName().endsWith("ibd")) {
                byte[] data = map.get(file.getName());
                try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                    raf.seek(36);
                    raf.write(data);
                    raf.seek(40);
                    raf.write(data);
                } catch (IOException e) {
                    e.printStackTrace();
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

    private static void alertTablespaces(String cmd) {
        File dir = new File(newDbDir);
        List<String> tableNames = new ArrayList<>();
        for (String fileName : dir.list()) {
            if (fileName.endsWith("frm")) {
                String tableName = fileName.substring(0, fileName.indexOf(".frm"));
                if (ignoredTables.contains(tableName)) {
                    tableNames.add(tableName);
                }
            }
        }
        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUsername, dbPassword);
             Statement stmt = conn.createStatement()) {
            for (String tableName : tableNames) {
                String sql = "ALTER TABLE " + tableName + " " + cmd + " TABLESPACE";
                boolean result = stmt.execute(sql);
                if (result) {
                    System.out.println("删除表空间：" + tableName + "成功");
                } else {
                    System.out.println("删除表空间：" + tableName + "失败");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
