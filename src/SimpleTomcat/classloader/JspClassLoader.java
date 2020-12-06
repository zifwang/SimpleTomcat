package SimpleTomcat.classloader;

import SimpleTomcat.catalina.Context;
import SimpleTomcat.util.Constant;
import cn.hutool.core.util.StrUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JspClassLoader is to load jsp files:
 *  1. one jsp file is corresponding to a JspClassLoader
 *  2. if this jsp file is modified, then need a new JspClassLoader
 *  3. JspClassLoader is based on the .class file which is got from compiling .jsp file
 */
public class JspClassLoader extends URLClassLoader {
    // map to store jsp file and JspClassLoader. key: jsp file, value: JspClassLoader
    // to prevent to create multiple JspClassLoader use ConcurrentHashMap
    private static Map<String, JspClassLoader> jspMap= new ConcurrentHashMap<>();

    /**
     * invalid jspClassLoader: remove jspClassLoader form jspMap
     * @param uri: jsp uri
     * @param context: context
     */
    public static void invalidJspClassLoader(String uri, Context context) {
        String key = context.getPath() + "/" + uri;
        jspMap.remove(key);
    }

    public static JspClassLoader getJspClassLoader(String uri, Context context) {
        String key = context.getPath() + "/" + uri;
        if (!jspMap.containsKey(key)) {
            jspMap.put(key, new JspClassLoader(context));
        }

        return jspMap.get(key);
    }

    /**
     * Constructor:
     *  JspClassLoader 会基于 WebClassLoader 来创建,因为jsp 里面可能会使用当前 web应用的 lib 目录里的 jar包。
     *  然后根据 context 的信息获取到 %TOMCAT_HOME%/work 目录下对应的目录，
     *  并且把这个目录作为 URL 加入到当前 ClassLoader 里，
     *  这样通过当前 JspClassLoader 加载jsp类的时候，就可以找到对应的类文件了。
     * @param context: context
     */
    private JspClassLoader(Context context) {
        super(new URL[] {}, context.getWebappClassLoader());
        try {
            String subDirectory;
            String path = context.getPath();
            if (path.equals("/")) {
                subDirectory = "_";
            } else {
                // not root
                subDirectory = StrUtil.subAfter(path, '/', false);
            }

            File classesFolder = new File(Constant.workFolder, subDirectory);
            URL url = new URL("file:" + classesFolder.getAbsolutePath() + "/");
            this.addURL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
