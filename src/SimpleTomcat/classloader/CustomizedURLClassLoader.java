package SimpleTomcat.classloader;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * CustomizedClassLoader is to learn how to use URLClassLoader to import .jar files to program
 */
public class CustomizedURLClassLoader extends URLClassLoader  {

    public CustomizedURLClassLoader(URL[] urls) {
        super(urls);
    }

    public static void main(String[] args) throws Exception {
        URL url = new URL("file:/Users/zifwang/Desktop/SimpleTomcat/jar_4_test/test.jar");
        URL[] urls = new URL[] {url};

        CustomizedURLClassLoader loader = new CustomizedURLClassLoader(urls);
        Class<?> how2jClass = loader.loadClass("cn.how2j.diytomcat.test.HOW2J");
        Object o = how2jClass.newInstance();
        Method m = how2jClass.getMethod("hello");
        m.invoke(o);
        System.out.println(how2jClass.getClassLoader());

        /**
         * 在学习类加载器之前，我们会认为在一个虚拟机里，一个类只有一个对应的类对象。
         * 现在学习了类加载器之后，对类的唯一性的理解，应该更为准确地描述为： 在一个加载器下，一个类只有一个对应的类对象。
         * 如果有多个类加载器都各自加载了同一个类，那么他们将得到不同的类对象。
         */
        CustomizedURLClassLoader loader1 = new CustomizedURLClassLoader(urls);
        Class<?> how2jClass1 = loader1.loadClass("cn.how2j.diytomcat.test.HOW2J");

        CustomizedURLClassLoader loader2 = new CustomizedURLClassLoader(urls);
        Class<?> how2jClass2 = loader2.loadClass("cn.how2j.diytomcat.test.HOW2J");

        System.out.println(how2jClass1==how2jClass2);
    }
}
