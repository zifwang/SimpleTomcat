package SimpleTomcat.catalina;

import SimpleTomcat.classloader.WebappClassLoader;
import SimpleTomcat.exception.WebConfigException;
import SimpleTomcat.http.ApplicationContext;
import SimpleTomcat.http.StandardServletConfig;
import SimpleTomcat.monitor.ContextFileChangeMonitor;
import SimpleTomcat.util.XMLParser;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import org.apache.jasper.JspC;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.*;

/**
 * Context object is used to store web services information:
 *     Commonly, there are three types of web services: Servlet, Static Html, and Dynamic html (JSP file) based on Tomcat
 *     A web service is usual contains information about: http path, location in the local file system, special file types mapping,
 *     servlet mapping info., webappClass, and context file change monitor.
 *     To make sure the servlet is singleton, a servlet pool is added.
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
    private Map<String, Map<String, String>> servletInitParams; // servlet initialization configuration parameters. key: name, value: (Map) initParam (key: param-name, value: param-value)
    private List<String> loadOnStartupServletClassName;         // load on startup property

    private WebappClassLoader webappClassLoader;                // webappClassLoader
    private ContextFileChangeMonitor contextFileChangeMonitor;  // contextFileChangeMonitor: monitor on files change
    private Boolean reloadable;                                 // reloadable: whether context is reloadable
    private Host host;                                          // Host of this context
    private ServletContext servletContext;                      // servlet context
    private Map<Class<?>, HttpServlet> servletPool;             // servlet pool

    private Map<String, List<String>> url_filterClassNames;     // map url to filter classes: key: url, value: filterClasses' name (classes url need to travel)
    private Map<String, List<String>> url_filterNames;          // map url to filter names: key: url, value: filters' names (classes url need to travel)
    private Map<String, String> filterName_filterClassName;     // map filter name to filter class name: key: filter name, value: filter class name
    private Map<String, String> filterClassName_filterName;     // map filter class name to filter name: key: filter class name, value: filter name
    private Map<String, Map<String, String>> filterClassName_initParams; // map filter_name to class name and init params;
    private Map<String, Filter> filterPool;                     // filter pool

    private List<ServletContextListener> listeners;             // context listeners

    /**
     * constructor
     * @param path: url path
     * @param docBase: system file directory
     */
    public Context(String path, String docBase, Boolean reloadable, Host host) {
        this.path = path;
        this.docBase = docBase;
        this.webXmlFile = new File(docBase, XMLParser.getWatchedResource());
        this.servletNameToClassMap = new HashMap<>();
        this.servletClassToNameMap = new HashMap<>();
        this.servletUrlToNameMap = new HashMap<>();
        this.servletUrlToClassMap = new HashMap<>();
        this.servletInitParams = new HashMap<>();
        this.reloadable = reloadable;
        this.host = host;
        this.servletContext = new ApplicationContext(this);
        this.servletPool = new HashMap<>();
        this.loadOnStartupServletClassName = new ArrayList<>();
        this.url_filterClassNames = new HashMap<>();
        this.url_filterNames = new HashMap<>();
        this.filterName_filterClassName = new HashMap<>();
        this.filterClassName_filterName = new HashMap<>();
        this.filterClassName_initParams = new HashMap<>();
        this.filterPool = new HashMap<>();
        this.listeners=new ArrayList<ServletContextListener>();

        ClassLoader commonClassLoader = Thread.currentThread().getContextClassLoader();
        this.webappClassLoader = new WebappClassLoader(docBase, commonClassLoader);


        deploy();
    }

    /**
     * getHttpServlet(Class) method will return the corresponding servlet of input class.
     * If the class is not existed, it will add this mapping to the servlet pool.
     * To prevent creating the same class-servlet mapping, the key word synchronized is used.
     * @param clazz: corresponding servlet class
     * @return HttpServlet
     * @throws InstantiationException: InstantiationException
     * @throws IllegalAccessException: IllegalAccessException
     * @throws ServletException: ServletException
     */
    public synchronized HttpServlet getHttpServlet(Class<?> clazz) throws InstantiationException, IllegalAccessException, ServletException {
        if (this.servletPool.containsKey(clazz)) {
            return this.servletPool.get(clazz);
        }

        HttpServlet servlet = (HttpServlet) clazz.newInstance();
        ServletContext servletContext = this.getServletContext();
        String servletName = servletClassToNameMap.get(clazz.getName());        // servlet name
        Map<String, String> initParams = servletInitParams.get(servletName);    // servlet init params

        ServletConfig servletConfig = new StandardServletConfig(servletContext, servletName, initParams);
        servlet.init(servletConfig);            // init servlet using servletConfig

        this.servletPool.put(clazz, servlet);

        return servlet;
    }

    /**
     * Stop context by stop webappClassLoader and contextFileChangeMonitor
     */
    public void stop() {
        webappClassLoader.stop();
        contextFileChangeMonitor.stop();
        destroyServlets();
        fireEvent("destory");
    }

    /**
     * Reload context by reload it in Host
     */
    public void reload() {
        host.reload(this);
    }

    /**
     * Deploy web-app
     */
    private void deploy() {
        loadListeners();
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

        JspC jspC = new JspC();
        // init JspRunTimeContext: 为了能够在jsp所转换的 java 文件里的 javax.servlet.jsp.JspFactory.getDefaultFactory() 这行能够有返回值
        new JspRuntimeContext(servletContext, jspC);
    }

    /**
     * handle the load on startup property
     *  Create servlet after reading web.xml
     */
    public void handleLoadOnStartup() {
        for (String servletClassName : loadOnStartupServletClassName) {
            try {
                Class<?> clazz = webappClassLoader.loadClass(servletClassName);
                getHttpServlet(clazz);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ServletException e) {
                e.printStackTrace();
            }
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
            // read web.xml file
            String webXml = FileUtil.readUtf8String(webXmlFile);
            Document document = Jsoup.parse(webXml);

            // parse servlet mapping
            parseServletMapping(document);
            // parse filter mapping
            parseFilterMapping(document);
            // parse servlet init. config.
            parseServletInitConfig(document);
            // parse filter init. config.
            parseFilterInitParams(document);
            // parse servlet load on startup property
            parseServletLoadOnStartup(document);
            // handle load on startup
            handleLoadOnStartup();
            // init filter
            initFilter();

            // fire listener
            fireEvent("init");
        } catch (WebConfigException webConfigException) {
            webConfigException.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Parse web.xml file to servlets' map:
     *  This method aims to parse the servlet definition and the servlet-mapping definition.
     *  <servlet></servlet>: contains servlet-name and corresponding java class in the webapp
     *  <servlet-mapping></servlet-mapping>: contains servlet-name and corresponding url of the webapp
     *  Parsing these two properties makes Context build relationships between servlet-name, servlet class, and servlet url
     *
     * @param document: web.xml file
     * @throws WebConfigException: WebConfigException is the error happens during web configuration building
     * @see SimpleTomcat.exception.WebConfigException
     */
    private void parseServletMapping(Document document) throws WebConfigException {

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
            String servletClass;

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

    /**
     * Parse Servlet Initialization Configurations from web.xml in the webapp
     *  The init. configurations will inside <servlet></servlet> block with <init-param></init-param> tag.
     *  There are two properties: name and corresponding value. This method will parse these configs to servletInitParams.
     *
     * @param document: document: web.xml
     */
    private void parseServletInitConfig(Document document) {

        // parse context in <servlet></servlet>
        Elements servletElements = document.select("servlet");
        for (Element servletElement : servletElements) {
            String servletName = servletElement.selectFirst("servlet-name").text();
            if (servletName == null || servletName.length() == 0) {
                continue;
            }

            Map<String, String> initConfig = new HashMap<>(); // init config map. key: init-param name, value: init-param value
            // parse init. configurations from <servlet></servlet>
            Elements initParams = servletElement.select("init-param");
            for (Element initParam : initParams) {
                String paramName = initParam.selectFirst("param-name").text();
                String paramValue = initParam.selectFirst("param-value").text();
                if (paramName == null || paramValue == null || paramName.length() == 0 || paramValue.length() == 0) {
                    continue;
                }

                initConfig.put(paramName, paramValue);
            }
            if (!initConfig.isEmpty()) {
                servletInitParams.put(servletName, initConfig);
            }
        }
    }

    /**
     * Servlet can be configured to start up while loading in web.xml
     *  <load-on-startup></load-on-startup> tag aims to define this property
     * @param document: web.xml
     */
    private void parseServletLoadOnStartup(Document document) {
        Elements elements = document.select("load-on-startup");
        for (Element element : elements) {
            String servletClassName = element.parent().select("servlet-class").text();
            if (servletClassName == null || servletClassName.length() == 0) {
                continue;
            }
            this.loadOnStartupServletClassName.add(servletClassName);
        }
    }

    /**
     * Filter is configured in the web.xml of a web app.
     *  Get all info in tag <filter></filter> in web.xml and parse into filter maps
     * @param document: web.xml
     * @throws WebConfigException: WebConfigException
     */
    private void parseFilterMapping(Document document) throws WebConfigException {
        Elements filters = document.select("filter");

        // parse filterName_filterClassName & filterClassName_filterName
        for (Element filter : filters) {
            String filterName = filter.selectFirst("filter-name").text();
            String filterClass = filter.selectFirst("filter-class").text();

            if (!filterName_filterClassName.containsKey(filterName)) {
                filterName_filterClassName.put(filterName, filterClass);
            }
            else {
                throw new WebConfigException(
                        StrUtil.format("filter-name: {} duplicated. The filter-name has to be unique.",
                                filterName));
            }

            if (!filterClassName_filterName.containsKey(filterClass)) {
                filterClassName_filterName.put(filterClass, filterName);
            }
            else {
                throw new WebConfigException(
                        StrUtil.format("filter-class-name: {} duplicated. The filter-class-name has to be unique.",
                                filterClass));
            }
        }

        // parse url_filterClassNames & url_filterNames
        Elements filterMappings = document.select("filter-mapping");
        for (Element filterMapping : filterMappings) {
            String filterName = filterMapping.selectFirst("filter-name").text();
            String url = filterMapping.selectFirst("url-pattern").text();

            if (!filterName_filterClassName.containsKey(filterName)) {
                throw new WebConfigException(
                        StrUtil.format("Url: {}'s corresponding filter name: {} not found.",
                                url, filterName));
            }
            String filterClass = filterName_filterClassName.get(filterName);
            if (!filterClassName_filterName.containsKey(filterClass)) {
                throw new WebConfigException(
                        StrUtil.format("Url: {}'s corresponding filter class: {} not found.",
                                url, filterClass));
            }
            if (!filterClassName_filterName.get(filterClass).equals(filterName)) {
                throw new WebConfigException(
                        StrUtil.format("Url: {}'s corresponding filter name: {} and filter class: {} are not corresponding to each other",
                                url, filterName, filterClass)
                );
            }

            if (url_filterNames.containsKey(url)) {
                url_filterNames.get(url).add(filterName);
            } else {
                List<String> filterNames = new ArrayList<>();
                filterNames.add(filterName);
                url_filterNames.put(url, filterNames);
            }

            if (url_filterClassNames.containsKey(url)) {
                url_filterClassNames.get(url).add(filterClass);
            } else {
                List<String> classNames = new ArrayList<>();
                classNames.add(filterClass);
                url_filterClassNames.put(url, classNames);
            }

        }
    }

    /**
     * Filter is configured in the web.xml of a web app. This method is to parse init params mapping
     * @param document: web.xml
     * @throws WebConfigException: WebConfigException
     * filterClassName_initParams
     */
    private void parseFilterInitParams(Document document) throws WebConfigException {
        Elements filterClassNameElements = document.select("filter-class");
        for (Element filterClassNameElement : filterClassNameElements) {
            String filterClassName = filterClassNameElement.text();

            Elements initElements = filterClassNameElement.parent().select("init-param");
            if (initElements.isEmpty())
                continue;

            Map<String, String> initParams = new HashMap<>();

            for (Element element : initElements) {
                String name = element.select("param-name").get(0).text();
                String value = element.select("param-value").get(0).text();
                initParams.put(name, value);
            }

            filterClassName_initParams.put(filterClassName, initParams);
        }
    }

    /**
     * init filter
     */
    private void initFilter() {
        Set<String> filterClassNames = filterClassName_filterName.keySet();
        for (String filterClassName : filterClassNames) {
            try {
                Class clazz = this.getWebappClassLoader().loadClass(filterClassName);
                Map<String, String> initParams = filterClassName_initParams.get(filterClassName);
                String filterName = filterClassName_filterName.get(filterClassName);
                FilterConfig filterConfig = new StandardFilterConfig(servletContext, initParams, filterName);

                Filter filter = filterPool.get(clazz);
                if(filter == null) {
                    filter = (Filter) ReflectUtil.newInstance(clazz);
                    filter.init(filterConfig);
                    filterPool.put(filterClassName, filter);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * destroy servlets in the servlet pool
     */
    private void destroyServlets() {
        Collection<HttpServlet> servlets = servletPool.values();
        for (HttpServlet servlet : servlets) {
            servlet.destroy();
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
     * @return reloadable
     */
    public boolean isReloadable() {
        return reloadable;
    }

    /**
     * Set reloadable property of webapp
     * @param reloadable: whether context is reloadable by definition
     */
    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    /**
     * uriMatchFilterPattern: compare pattern with uri
     * @param pattern: filter url-pattern
     * @param uri: request url
     * @return true or false
     */
    private boolean uriMatchFilterPattern(String pattern, String uri) {
        // completely match
        if (StrUtil.equals(pattern, uri)) {
            return true;
        }

        // /* pattern
        if (StrUtil.equals(pattern, "/*")) {
            return true;
        }

        // suffix match
        if (StrUtil.startWith(pattern, "/*.")) {
            String patternExtName = StrUtil.subAfter(pattern, '.', false);
            String uriExtName = StrUtil.subAfter(uri, '.', false);
            if (StrUtil.equals(patternExtName, uriExtName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * get matched filters
     * @param uri: request uri
     * @return
     */
    public List<Filter> getMatchedFilters(String uri) {
        List<Filter> filters = new ArrayList<>();
        Set<String> patterns = url_filterClassNames.keySet();
        Set<String> matchedPatterns = new HashSet<>();
        for (String pattern : patterns) {
            if (uriMatchFilterPattern(pattern, uri)) {
                matchedPatterns.add(pattern);
            }
        }

        Set<String> matchedFilterClassNames = new HashSet<>();
        for (String pattern : matchedPatterns) {
            List<String> filterClassName = url_filterClassNames.get(pattern);
            matchedFilterClassNames.addAll(filterClassName);
        }
        for (String filterClassName : matchedFilterClassNames) {
            Filter filter = filterPool.get(filterClassName);
            filters.add(filter);
        }

        return filters;
    }

    /**
     * add listener
     * @param listener: listener
     */
    public void addListener(ServletContextListener listener) {
        this.listeners.add(listener);
    }

    /**
     * load listeners from web.xml
     */
    private void loadListeners()  {
        try {
            if(!webXmlFile.exists())
                return;
            String webXml = FileUtil.readUtf8String(webXmlFile);
            Document d = Jsoup.parse(webXml);

            Elements es = d.select("listener listener-class");
            for (Element e : es) {
                String listenerClassName = e.text();

                Class<?> clazz= this.getWebappClassLoader().loadClass(listenerClassName);
                ServletContextListener listener = (ServletContextListener) clazz.newInstance();
                addListener(listener);

            }
        } catch (IORuntimeException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * start Event
     * @param type
     */
    private void fireEvent(String type) {
        ServletContextEvent event = new ServletContextEvent(servletContext);
        for (ServletContextListener servletContextListener : listeners) {
            if("init".equals(type))
                servletContextListener.contextInitialized(event);
            if("destroy".equals(type))
                servletContextListener.contextDestroyed(event);
        }
    }
}
