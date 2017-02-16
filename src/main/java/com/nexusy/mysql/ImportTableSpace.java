package com.nexusy.mysql;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lan
 * @since 2017-02-16
 */
public class ImportTableSpace {

    private static String dbDir = "D:\\zenointel";

    public static void main(String[] args) {
        File dir = new File(dbDir);
        List<String> tableNames = new ArrayList<>();
        for (String fileName : dir.list()) {
            if (fileName.endsWith("frm") && !fileName.startsWith("view")) {
                String tableName = fileName.substring(0, fileName.indexOf(".frm"));
                tableNames.add(tableName);
            }
        }
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://10.12.13.111/zenointel", "mysql", "password");
            stmt = conn.createStatement();
            for (String tableName : tableNames) {
                String sql = "ALTER TABLE " + tableName + " IMPORT TABLESPACE";
                boolean result = stmt.execute(sql);
                if (result) {
                    System.out.println("删除表空间：" + tableName + "成功");
                } else {
                    System.out.println("删除表空间：" + tableName + "失败");
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
