本文描述的方法用于MySQL的InnoDB数据存储引擎从\*.frm和\*.ibd文件中恢复数据。

1. 在一个新的MySQL实例中新建一个和损坏数据库同名的数据库

2. 根据损坏数据库中的\*.frm文件在一个新的MySQL实例中创建同名的表，新表的结构可以尽量简单，如：

   ```mysql
   CREATE TABLE tableName (id int(11) NOT NULL ) ENGINE=InnoDB
   ```
   由于需要恢复的数据库可能包含很多表，因此本项目中使用如下命令自动创建同名表：

   ```shell
   java -cp *.jar InnoDBRecoverTool --createTables
   ```

3. 停止MySQL服务器，在配置文件[mysqld]段中加入

   ```mysql
   innodb_force_recovery=6
   ```

4. 使用损坏数据库的\*.frm文件替换新建的的\*.frm文件，然后重新启动MySQL服务器

5. 导出所有表结构，然后停止MySQL服务器，并注释掉第三步的配置

   ```mysql
   #innodb_force_recovery=6
   ```

6. 删除第1步中的数据库，然后新建一个同名数据库，并使用第5步导出的表结构新建所有表

7. 替换space id，每个\*.ibd文件的第37-38和41-42字节表示space id，必须使用第6步的产生的\*.ibd中的space id替换掉损坏数据库同名\*.ibd文件的space id，可使用vim或[xvi32](http://www.chmaas.handshake.de/delphi/freeware/xvi32/xvi32.htm)等工具编辑，本项目中可使用如下命令

   ```shell
   java -cp *.jar InnoDBRecoverTool --replaceSpaceIds
   ```

8. 删除第6步创建的所有表空间文件

   ```mysql
   ALTER TABLE tableName DISCARD TABLESPACE
   ```
   可使用本项目的如下命令

   ```shell
   java -cp *.jar InnoDBRecoverTool --discardTablespaces
   ```

9. 将损坏数据库的\*.ibd文件考入新建的数据库目录

10. 导入表空间文件

```mysql
   ALTER TABLE tableName IMPORT TABLESPACE
```

   本项目可使用如下命令

   ```
   java -cp *.jar InnoDBRecoverTool --importTablespaces
   ```

11. 设置MySQL配置文件

```mysql
   innodb_force_recovery=6
```

12. 重新启动MySQL并导出数据
