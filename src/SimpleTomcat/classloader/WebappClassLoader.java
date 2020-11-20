package SimpleTomcat.classloader;

import cn.hutool.core.io.FileUtil;
import cn.hutool.log.LogFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * WebappClassLoader object is to scan WEB-INF directory in the web-app project.
 *  The WEB-INF directory of a web-app contains classes and all external libs used by web-app.
 *  Classes and libs will be added to URL[]. SimpleTomcat can find these files from URL[].
 */
public class WebappClassLoader extends URLClassLoader  {

    /**
     * Constructor
     * @param docBase: context will pass this information
     */
    public WebappClassLoader(String docBase, ClassLoader commonClassLoader) {
        super(new URL[] {}, commonClassLoader);

        if (!isWebInfExist(docBase)) {
            LogFactory.get().info("Web application {} does not contains WEB-INF directory", docBase.substring(docBase.lastIndexOf("/")));
            return;
        }

        if (isClassesExist(docBase)) {
            try {
                loadClasses(docBase);
            } catch (MalformedURLException e) {
                LogFactory.get().info("Web application {} fails to load classes", docBase.substring(docBase.lastIndexOf("/")));
            }
        }

        if (isLibExist(docBase)) {
            try {
                loadLib(docBase);
            } catch (NullPointerException e) {
                LogFactory.get().info("Web application {} does not contain jar files", docBase.substring(docBase.lastIndexOf("/")));
            } catch (MalformedURLException e) {
                LogFactory.get().info("Web application {} fails to load jar files", docBase.substring(docBase.lastIndexOf("/")));
            }
        }
    }

    /**
     * Stop webappClassLoader
     */
    public void stop() {
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check whether WEB-INF directory exists in the web app
     * @param docBase
     * @return true or false
     */
    private boolean isWebInfExist(String docBase) {
        String webInfDirectory = docBase + "/" + "WEB-INF";
        if (FileUtil.exist(webInfDirectory)) return true;
        return false;
    }

    /**
     * Check whether classes directory exists in the web app
     * @param docBase
     * @return true or false
     */
    private boolean isClassesExist(String docBase) {
        String classesDirectory = docBase + "/" + "WEB-INF" + "/" + "classes";
        if (FileUtil.exist(classesDirectory)) return true;
        return false;
    }

    /**
     * Check whether lib directory exists in the web app
     * @param docBase
     * @return true or false
     */
    private boolean isLibExist(String docBase) {
        String libDirectory = docBase + "/" + "WEB-INF" + "/" + "lib";
        if (FileUtil.exist(libDirectory)) return true;
        return false;
    }

    /**
     * Load Classes from WEB-INF
     */
    private void loadClasses(String docBase) throws MalformedURLException {
        File webInfDirectory = new File(docBase, "WEB-INF");
        File classDirectory = new File(webInfDirectory, "classes");
        this.addURL(new URL("file:" + classDirectory.getAbsolutePath() + "/"));
    }

    /**
     * Load libs from WEB-INF
     */
    private void loadLib(String docBase) throws NullPointerException, MalformedURLException {
        File webINFDirectory = new File(docBase, "WEB-INF");
        File libDirectory = new File(webINFDirectory, "lib");
        File[] jarFiles = libDirectory.listFiles();
        if (jarFiles == null || jarFiles.length <= 0) {
            throw new NullPointerException();
        }

        for (File file : jarFiles) {
            if (file.getName().endsWith("jar")) {
                URL url = new URL("file:" + file.getAbsolutePath());
                this.addURL(url);
            }
        }
    }

}
