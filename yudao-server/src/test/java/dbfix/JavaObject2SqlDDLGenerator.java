package dbfix;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import liquibase.pro.packaged.I;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** * @Title SqlGenerator * @Description 根据JAVA实体生成SQL建表语句工具 * @Copyright: 版权所有 (c) 2018 - 2019 * @Company: wt * @Author root * @Version 1.0.0 * @Create 19-4-1 下午4:22 */
@Slf4j
public class JavaObject2SqlDDLGenerator {

    /**
     * 常见MYSQL字段类型 VS java对象属性类型 映射集合
     */
    public static Map<String, String> javaFiled2TableColumnMappingMap = new HashMap<>();

    // {"localtime":2,"payclientconfig":1,"set":2,"string":715,"bigdecimal":146,"double":11,"localdate":1,"integer":351,"list":37,"long":270,"boolean":182,"localdatetime":371,"map":2}
    static {
        javaFiled2TableColumnMappingMap.put("integer", "int");
//        javaFiled2TableColumnMappingMap.put("short", "tinyint");
        javaFiled2TableColumnMappingMap.put("long", "bigint");
        javaFiled2TableColumnMappingMap.put("bigdecimal", "decimal(13,2)");
        javaFiled2TableColumnMappingMap.put("double", "double(10,2)");
//        javaFiled2TableColumnMappingMap.put("float", "float");
        javaFiled2TableColumnMappingMap.put("boolean", "bit");
        javaFiled2TableColumnMappingMap.put("timestamp", "datetime");
//        javaFiled2TableColumnMappingMap.put("date", "datetime");
        javaFiled2TableColumnMappingMap.put("localdate", "datetime");
        javaFiled2TableColumnMappingMap.put("string", "varchar(255)");
        javaFiled2TableColumnMappingMap.put("localdatetime", "datetime");
        javaFiled2TableColumnMappingMap.put("list", "varchar(255)");
        javaFiled2TableColumnMappingMap.put("set", "varchar(255)");
        javaFiled2TableColumnMappingMap.put("map", "varchar(255)");
        javaFiled2TableColumnMappingMap.put("payclientconfig", "varchar(255)");
        javaFiled2TableColumnMappingMap.put("localtime", "time");
    }

    public static Map<String, String> specialName = new HashMap<>();
    static {
        specialName.put("create_time", "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', \n");
        specialName.put("updater", "`updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者', \n");
        specialName.put("update_time", "`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间', \n");
        specialName.put("deleted", "`deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除', \n");
        specialName.put("tenant_id", "`tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号', \n");
    }

    /**
     * 生成SQL
     * @param className
     * @return
     */
    public static String ddlSqlGenerate(String className, Map<String, Integer> typeNumMap) throws ClassNotFoundException {

        Class<?> clz = Class.forName(className);
        String classPath =
                clz.getResource("").getPath().substring(1).replace("/", "\\").replace("target\\classes", "src\\main\\java") + clz.getSimpleName() + ".java";
        System.out.println("class 路径 " + classPath);

        List<Field> fieldList = new ArrayList<>();
        Map<String, String> fieldDes = new HashMap<>();

        Collections.addAll(fieldList, clz.getDeclaredFields());
        fieldDes.putAll(DocUtil.execute(classPath));

        // 获取该类的父类下的字段
        Class<?> superclass = clz.getSuperclass();
        while (!superclass.getSimpleName().equals("Object")) {
            String superClassPath = superclass.getResource("").getPath().substring(1).replace("/", "\\").replace("target\\classes", "src\\main\\java") +
                    superclass.getSimpleName() + ".java";
            System.out.println("superclass 路径 " + superClassPath);

            fieldDes.putAll(DocUtil.execute(superClassPath));

            Collections.addAll(fieldList, superclass.getDeclaredFields());

            superclass = superclass.getSuperclass();
        }

        // 查询主键字段
        String sqlPrimaryKey = null;

        StringBuilder column = new StringBuilder();

        for (Field f : fieldList) {
            TableId annotation = f.getAnnotation(TableId.class);
            if (null != annotation) {
                sqlPrimaryKey = f.getName();
            }
        }

        if (StringUtils.isBlank(sqlPrimaryKey)) {
            sqlPrimaryKey = "id";
        }

        for (Field f : fieldList) {
            if (Modifier.isFinal(f.getModifiers())) {
                continue;
            }

            // 剔除序列化自动生成的字段
            if (f.getName().equalsIgnoreCase("serialVersionUID")) {
                continue;
            }

            String comment = fieldDes.getOrDefault(f.getName(), "").split("\n")[0];

            if (f.getName().equals(sqlPrimaryKey)) {
                if (f.getName().equals("id")) {
                    column.append("`id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',\n");
                } else {
                    column.append("`").append(humpToLine(f.getName())).append("` bigint NOT NULL AUTO_INCREMENT COMMENT '").append(comment).append("',\n");
                }
            } else {
                column.append(wrapperTableColumnSql(f, comment, typeNumMap));
            }
        }

        String tableName = clz.getAnnotation(TableName.class).value();
        String tableNameCN = fieldDes.getOrDefault(clz.getSimpleName(), tableName).split("\n")[0];

        StringBuffer sql = new StringBuffer();
        sql.append("\nDROP TABLE IF EXISTS `").append(tableName).append("`; \n");
        sql.append("CREATE TABLE `").append(tableName).append("` (");
        sql.append(" \n").append(column, 0, column.lastIndexOf(","));
        if (StringUtils.isNotBlank(sqlPrimaryKey)) {
            sql.append(", \n PRIMARY KEY (`").append(sqlPrimaryKey).append("`) USING BTREE");
        }
        sql.append(" \n ) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 ").append(StringUtils.isNotBlank(tableNameCN) ? " COMMENT='" + tableNameCN + "'" : "")
                .append(";\n");
        return sql.toString();

    }

    /**
     * 构建字段部分
     * @param field
     * @return
     */
    private static String wrapperTableColumnSql(Field field, String comment, Map<String, Integer> typeNumMap) {
        String tpl = "`%s` %s NOT NULL COMMENT '%s', \n";
        String typeName = field.getType().getSimpleName().toLowerCase();
        String sqlType = javaFiled2TableColumnMappingMap.get(typeName);
        if (sqlType == null || sqlType.isEmpty()) {
            log.info(field.getName() + ":" + field.getType().getName() + " 需要单独创建表");
            return "";
        }

        typeNumMap.put(typeName, typeNumMap.getOrDefault(typeName, 0) + 1);

        String humpToLineName = humpToLine(field.getName());

        if (specialName.containsKey(humpToLineName)) {
            return specialName.get(humpToLineName);
        } else {
            // 将java对象属性值的驼峰写法转换为下划线模式
            return String.format(tpl, humpToLineName, sqlType.toLowerCase(), comment);
        }
    }

    private static final Pattern HUMP_PATTERN = Pattern.compile("[A-Z0-9]");

    private static String humpToLine(String str) {
        Matcher matcher = HUMP_PATTERN.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        String str = ddlSqlGenerate("cn.iocoder.yudao.module.statistics.dal.dataobject.product.ProductStatisticsDO", new HashMap<>());
        System.out.println(str);
    }


}