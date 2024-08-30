package dbfix;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.RootDoc;
import com.sun.tools.javadoc.Main;

import java.util.HashMap;
import java.util.Map;

/**
 * Description:
 *
 * @author jack
 * @date 2021/7/13 5:10 下午
 */
public class DocUtil {

    /**
     * 会自动注入
     */
    private static RootDoc rootDoc;

    /**
     * 会自动调用这个方法
     *
     * @param root root
     * @return true
     */
    public static boolean start(RootDoc root) {
        rootDoc = root;
        return true;
    }

    /**
     * 生成文档
     *
     * @param beanFilePath 注意这里是.java文件绝对路径
     * @return 文档注释
     */
    public static Map<String, String> execute(String beanFilePath) {
        Main.execute(new String[]{"-doclet", DocUtil.class.getName(), "-docletpath",
                DocUtil.class.getResource("/").getPath(), "-encoding", "utf-8", beanFilePath});

        ClassDoc[] classes = rootDoc.classes();

        if (classes == null || classes.length == 0) {
            return null;
        }
        ClassDoc classDoc = classes[0];
        // 获取属性名称和注释
        FieldDoc[] fields = classDoc.fields(false);

        Map<String, String> resultMap = new HashMap<>();

        for (FieldDoc field : fields) {
            resultMap.put(field.name(), field.commentText());
        }

        resultMap.put(classDoc.name(), classDoc.commentText());
        return resultMap;
    }

    public static String getClassDoc(String beanFilePath) {
        Main.execute(new String[]{"-doclet", DocUtil.class.getName(), "-docletpath",
                DocUtil.class.getResource("/").getPath(), "-encoding", "utf-8", beanFilePath});

        ClassDoc[] classes = rootDoc.classes();

        if (classes == null || classes.length == 0) {
            return null;
        }
        ClassDoc classDoc = classes[0];
        return classDoc.commentText();
    }

    public static void main(String[] args) {
        String beanFilePath = "D:\\WorkspacesSelf\\ruoyi-vue-pro\\yudao-module-system\\yudao-module-system-biz\\src\\main\\java\\cn\\iocoder\\yudao\\module\\system\\dal\\dataobject\\tenant\\TenantDO.java";
//        Map<String, String> docVO = DocUtil.execute(beanFilePath);
//        System.out.println(JSONObject.toJSONString(docVO));
        String docVO = DocUtil.getClassDoc(beanFilePath);
        System.out.println(docVO);
    }

}
