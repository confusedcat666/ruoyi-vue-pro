package dbfix;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtil {

    public static void main(String[] args) throws IOException {
        Map<String, String> tableNameAndClass = new HashMap<>();

        //要遍历的路径
        func(new File("D:\\WorkspacesSelf\\ruoyi-vue-pro"), tableNameAndClass);

        tableNameAndClass.remove("");

        Map<String, String> missingTables = new HashMap<>();

        tableNameAndClass.forEach((k, v) -> {
            if (!tableInDBs.contains(k)) {
                missingTables.put(k, v);
            }
        });

        System.out.println(JSONObject.toJSONString(missingTables));
        System.out.println(missingTables.size());

        Map<String, String> errorTables = new HashMap<>();

        Map<String, Integer> typeAndNumMap = new HashMap<>();

        System.out.println();
        StringBuffer ddlAll = new StringBuffer();
        missingTables.values().forEach(a -> {
            try {
                ddlAll.append(JavaObject2SqlDDLGenerator.ddlSqlGenerate(a, typeAndNumMap));
            } catch (Exception e) {
                e.printStackTrace();
                errorTables.put(a, e.getMessage());
            }
        });

        sqlSave2File(ddlAll.toString(), "D:\\ddlAll_2.sql");

        System.out.println(JSONObject.toJSONString(errorTables));

//        missingTables.forEach((k, v) -> {
//            System.out.println("ALTER TABLE `" + k + "` modify `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除';");
//        });

        System.out.println(JSONObject.toJSONString(typeAndNumMap));
    }

    private static void func(File file, Map<String, String> tableNameAndClass) {
        Pattern patternForPackage = Pattern.compile("(package (.*);)");
        Pattern patternForTableName = Pattern.compile("(@TableName(.*))");

        File[] fs = file.listFiles();
        for (File f : fs) {
            //若是目录，则递归打印该目录下的文件
            if (f.isDirectory()) {
                func(f, tableNameAndClass);
            } else if (f.isFile()) {     //若是文件，直接打印
                if (f.getName().endsWith("DO.java")) {
                    try {
                        String javaFileContent = getContent(f.getPath());
                        Matcher matcherForPackage = patternForPackage.matcher(javaFileContent);
                        Matcher matcherForTableName = patternForTableName.matcher(javaFileContent);

                        String className = matcherForPackage.find() ? matcherForPackage.group(2) + "." + f.getName().split("\\.")[0] : "";
                        String tableName = matcherForTableName.find() ? matcherForTableName.group(2).split("\"")[1] : "";
                        tableNameAndClass.put(tableName, className);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private static String getContent(String fileName) throws Exception {
        FileInputStream fis = new FileInputStream(fileName);
        byte[] buffer = new byte[10];
        StringBuilder sb = new StringBuilder();
        while (fis.read(buffer) != -1) {
            sb.append(new String(buffer));
            buffer = new byte[10];
        }
        fis.close();
        return sb.toString();
    }

    /**
     * 生成的SQL保存指定文件
     * @param str
     * @param path
     */
    private static void sqlSave2File(String str, String path) throws IOException {
        byte[] sourceByte = str.getBytes();

        File file = new File(path);
        if (!file.exists()) {
            File dir = new File(file.getParent());
            dir.mkdirs();
            file.createNewFile();
        }
        FileOutputStream outStream = new FileOutputStream(file, true);
        outStream.write(sourceByte);
        outStream.flush();
        outStream.close();
        System.out.println("生成成功");

    }

    //    static Set<String> tableInDBs =
    //            Sets.newHashSet("Tables_in_ruoyi-vue-pro", "infra_api_access_log", "infra_api_error_log", "infra_codegen_column", "infra_codegen_table",
    //                    "infra_config", "infra_data_source_config", "infra_file", "infra_file_config", "infra_file_content", "infra_job", "infra_job_log",
    //                    "market_activity", "member_address", "member_brokerage_record", "member_group", "member_level", "member_tag", "member_user", "pay_app",
    //                    "pay_channel", "pay_notify_log", "pay_notify_task", "pay_order", "pay_order_extension", "pay_refund", "pay_wallet_recharge",
    //                    "product_brand", "product_category", "product_comment", "product_property", "product_property_value", "product_sku", "product_spu",
    //                    "product_statistics", "promotion_article", "promotion_article_category", "promotion_combination_activity", "promotion_coupon",
    //                    "promotion_coupon_template", "promotion_discount_activity", "promotion_diy_page", "promotion_diy_template", "promotion_reward_activity",
    //                    "promotion_seckill_activity", "promotion_seckill_config", "system_dept", "system_dict_data", "system_dict_type", "system_login_log",
    //                    "system_mail_account", "system_mail_log", "system_mail_template", "system_menu", "system_notice", "system_notify_message",
    //                    "system_notify_template", "system_oauth2_access_token", "system_oauth2_approve", "system_oauth2_client", "system_oauth2_code",
    //                    "system_oauth2_refresh_token", "system_operate_log", "system_post", "system_role", "system_role_menu", "system_sms_channel",
    //                    "system_sms_code", "system_sms_log", "system_sms_template", "system_social_client", "system_social_user", "system_social_user_bind",
    //                    "system_tenant", "system_tenant_package", "system_user_post", "system_user_role", "system_users", "trade_after_sale",
    //                    "trade_after_sale_log", "trade_brokerage_record", "trade_brokerage_user", "trade_brokerage_withdraw", "trade_config",
    //                    "trade_delivery_express", "trade_delivery_express_template", "trade_delivery_pick_up_store", "trade_order", "trade_order_item",
    //                    "trade_statistics", "yudao_demo01_contact", "yudao_demo02_category", "yudao_demo03_course", "yudao_demo03_grade", "yudao_demo03_student");
    static Set<String> tableInDBs =
            Sets.newHashSet("infra_api_access_log", "infra_api_error_log", "infra_codegen_column", "infra_codegen_table", "infra_config",
                    "infra_data_source_config", "infra_file", "infra_file_config", "infra_file_content", "infra_job", "infra_job_log", "system_dept",
                    "system_dict_data", "system_dict_type", "system_login_log", "system_mail_account", "system_mail_log", "system_mail_template", "system_menu",
                    "system_notice", "system_notify_message", "system_notify_template", "system_oauth2_access_token", "system_oauth2_approve",
                    "system_oauth2_client", "system_oauth2_code", "system_oauth2_refresh_token", "system_operate_log", "system_post", "system_role",
                    "system_role_menu", "system_sms_channel", "system_sms_code", "system_sms_log", "system_sms_template", "system_social_client",
                    "system_social_user", "system_social_user_bind", "system_tenant", "system_tenant_package", "system_user_post", "system_user_role",
                    "system_users", "yudao_demo01_contact", "yudao_demo02_category", "yudao_demo03_course", "yudao_demo03_grade", "yudao_demo03_student");

}
