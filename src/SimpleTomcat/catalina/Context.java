package SimpleTomcat.catalina;

import SimpleTomcat.classloader.WebappClassLoader;
import SimpleTomcat.exception.WebConfigException;
import SimpleTomcat.http.ApplicationContext;
import SimpleTomcat.monitor.ContextFileChangeMonitor;
import SimpleTomcat.util.XMLParser;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.servlet.ServletContext;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Context object is used to store web services information:
 *     Commonly, there are three types of web services: Servlet, Static Html, and Dynamic html (JSP file) based on Tomcat
 *     A web service is usual contains information about: http path, location in the local file system, special file types mapping,
 *     servlet mapping info., webappClass, and context file change monitor.
 * -path: shows how to access this web app from url (static and dynamic html)
 * -docBase: location of this web app in the system (static and dynamic html)
 * -webXmlFile: the file that shows servlet services (often are get and post requests from html ) are provided
 */
public class Context {
    private String path;                                        // the path used to access
    private String docBase;                                     // the location of web application in the system
    private File webXmlFile;                                    // web.xml under WEB-INF directory
    private Map<String, String> servletNameToClassMap;          // map servlet name to class. key: name, value: class
    private Map<String, String> servletClassToNameMap;          // map servlet class to name. key: class, value: name
    private Map<String, String> servletUrlToNameMap;            // map servlet url to name. key: url, value: name
    private Map<String, String> servletUrlToClassMap;           // map servlet url to class. key: url, value: name
    private WebappClassLoader webappClassLoader;                // webappClassLoader
    private ContextFileChangeMonitor contextFileChangeMonitor;  // contextFileChangeMonitor: monitor on files change
    private Boolean reloadable;                                 // reloadable: whether context is reloadable
    private Host host;                                          // Host of this context
    private ServletContext servletContext;                      //

    /**
     * constructor
     * @param path
     * @param docBase
     */
    public Context(String path, String docBase, Boolean reloadable, Host host) {
        this.path = path;
        this.docBase = docBase;
        this.webXmlFile = new File(docBase, XMLParser.getWatchedResource());
        this.servletNameToClassMap = new HashMap<String, String>();
        this.servletClassToNameMap = new HashMap<String, String>();
        this.servletUrlToNameMap = new HashMap<String, String>();
        this.servletUrlToClassMap = new HashMap<String, String>();
        this.reloadable = reloadable;
        this.host = host;
        this.servletContext = new ApplicationContext(this);

        ClassLoader commonClassLoader = Thread.currentThread().getContextClassLoader();
        this.webappClassLoader = new WebappClassLoader(docBase, commonClassLoader);

        deploy();
    }

    /**
     * Deploy web-app
     */
    private void deploy() {
        TimeInterval timeInterval = DateUtil.timer();
        LogFactory.get().info("Deploying web application directory {}", this.docBase);
        boolean successFlag = init();
        if (successFlag) LogFactory.get().info("Deployment of web application directory {} succeed in {} ms",
                this.docBase, timeInterval.intervalMs());
        else LogFactory.get().info("Deployment of web application directory {} failed in {} ms",
                this.docBase, timeInterval.intervalMs());

        // Monitor classes and jars change in webapp
        if (reloadable) {
            contextFileChangeMonitor = new ContextFileChangeMonitor(this);
            contextFileChangeMonitor.start();
        }
    }

    /**
     * init web-app: by reading web.xml file
     * @return success or not
     */
    private boolean init() {
        if (!webXmlFile.exists()) {
            LogFactory.get().info("Web application directory {} doesn't contains web.xml file", docBase);
            return true;
        }

        try {
            parseWebXmlFile();
        } catch (WebConfigException webConfigException) {
            webConfigException.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Parse web.xml file to servlets' map
     */
    private void parseWebXmlFile() throws WebConfigException {
        // read web.xml file
        String webXml = FileUtil.readUtf8String(webXmlFile);
        Document document = Jsoup.parse(webXml);

        // parse context in <servlet></servlet>
        Elements servletElements = document.select("servlet");
        for (Element servlet : servletElements) {
            String servletName = servlet.selectFirst("servlet-name").text();
            String servletClass = servlet.selectFirst("servlet-class").text();

            if (!servletNameToClassMap.containsKey(servletName)) servletNameToClassMap.put(servletName, servletClass);
            else throw new WebConfigException(
                    StrUtil.format("servlet-name: {} duplicated. The servlet-name has to be unique.",
                            servletName));

            if (!servletClassToNameMap.containsKey(servletClass)) servletClassToNameMap.put(servletClass, servletName);
            else throw new WebConfigException(
                    StrUtil.format("servlet-class: {} duplicated. The servlet-class has to be unique.",
                            servletClass));
        }

        // parse context in <servlet-mapping></servlet-mapping>
        Elements servletMappingElements = document.select("servlet-mapping");
        for (Element servlet : servletMappingElements) {
            String servletName = servlet.selectFirst("servlet-name").text();
            String urlPattern = servlet.selectFirst("url-pattern").text();
            String servletClass = "";

            if (servletNameToClassMap.containsKey(servletName)) servletClass = servletNameToClassMap.get(servletName);
            else throw new WebConfigException(
                    StrUtil.format("{}'s servlet-name and servlet-class mapping not found. Require to define it in configuration file",
                            servletName));

            if (servletClass.equals("")) {
                continue;
            }
            if (!servletUrlToNameMap.containsKey(urlPattern)) servletUrlToNameMap.put(urlPattern, servletName);
            else throw new WebConfigException(
                    StrUtil.format("servlet-url: {} duplicated. The servlet-url has to be unique.",
                            servletName));
            if (!servletUrlToClassMap.containsKey(urlPattern)) servletUrlToClassMap.put(urlPattern, servletClass);
            else throw new WebConfigException(
                    StrUtil.format("servlet-url: {} duplicated. The servlet-url has to be unique.",
                            servletName));
        }
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }

    public String getDocBase() {
        return docBase;
    }

    public Map<String, String> getServletNameToClassMap() {
        return servletNameToClassMap;
    }

    public Map<String, String> getServletClassToNameMap() {
        return servletClassToNameMap;
    }

    public Map<String, String> getServletUrlToClassMap() {
        return servletUrlToClassMap;
    }

    public Map<String, String> getServletUrlToNameMap() {
        return servletUrlToNameMap;
    }

    public WebappClassLoader getWebappClassLoader() {
        return webappClassLoader;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * Get servlet class by servlet name
     * @param name: servlet name
     * @return servlet class or null
     */
    public String getServletClassByName(String name) {
        if (this.servletNameToClassMap.containsKey(name)) return servletNameToClassMap.get(name);
        return null;
    }

    /**
     * Get servlet name by servlet class
     * @param servletClass: servlet class
     * @return servlet name or null
     */
    public String getServletNameByClass(String servletClass) {
        if (this.servletClassToNameMap.containsKey(servletClass)) return servletClassToNameMap.get(servletClass);
        return null;
    }

    /**
     * Get servlet class by servlet url
     * @param url: servlet url
     * @return servlet class or null
     */
    public String getServletClassByUrl(String url) {
        if (this.servletUrlToClassMap.containsKey(url)) return servletUrlToClassMap.get(url);
        return null;
    }

    /**
     * Get servlet name by servlet url
     * @param url: servlet url
     * @return servlet name or null
     */
    public String getServletNameByUrl(String url) {
        if (this.servletUrlToNameMap.containsKey(url)) return servletUrlToNameMap.get(url);
        return null;
    }

    /**
     * Check whether context is reloadable
     * @return
     */
    public boolean isReloadable() {
        return reloadable;
    }

    /**
     * Set reloadable property of webapp
     * @param reloadable
     */
    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    /**
     * Stop context by stop webappClassLoader and contextFileChangeMonitor
     */
    public void stop() {
        webappClassLoader.stop();
        contextFileChangeMonitor.stop();
    }

    /**
     * Reload context by reload it in Host
     */
    public void reload() {
        host.reload(this);
    }
}
