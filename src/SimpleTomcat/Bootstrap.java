package SimpleTomcat;

import SimpleTomcat.classloader.CommonClassLoader;

import java.lang.reflect.Method;

/**;
 * Bootstrap class is the main entrance of SimplyTomcat
 */
public class Bootstrap {
    /**
     * main function: start program
     *  The CommonClassLoader is used to ensure all libs are loaded into program by .bat or .sh files.
     *  Starting program by IDE, all external libs can be loaded. However, when the program is started by .bat or .sh file,
     *  external libs cannot be loaded. Same to Tomcat, a CommonClassLoader is developed to solve it.
     * @param args: args
     * @throws Exception: Exception
     */
    public static void main(String[] args) throws Exception {
        CommonClassLoader commonClassLoader = new CommonClassLoader();
        // current thread will use commonClassLoader as its class loader
        Thread.currentThread().setContextClassLoader(commonClassLoader);

        String serverClassName = "SimpleTomcat.catalina.Server";
        Class<?> serverClass = commonClassLoader.loadClass(serverClassName);
        Object server = serverClass.newInstance();
        Method method = serverClass.getMethod("start");
        method.invoke(server);
    }

}
