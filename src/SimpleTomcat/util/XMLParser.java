package SimpleTomcat.util;

import SimpleTomcat.catalina.*;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.log.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * XMLParser Object
 */
public class XMLParser {
    /**
     * Get a list of Context Object from server.xml file
     * @return
     */
    public static List<Context> getContexts(Host host) {
        List<Context> contexts = new ArrayList<Context>();

        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document document = Jsoup.parse(xml);

        Elements elements = document.select("Context");
        for (Element e : elements) {
            String path = e.attr("path");
            String docBase = e.attr("docBase");
            boolean reloadable = false;
            if (e.hasAttr("reloadable")) {
                reloadable = Convert.toBool(e.attr("reloadable"), true);
            }
            Context context = new Context(path, docBase, reloadable, host);
            contexts.add(context);
        }

        return contexts;
    }

    /**
     * Get first host from server.xml file
     * @return
     */
    public static String getHostName() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document document = Jsoup.parse(xml);
        Element host = document.select("Host").first();

        return host.attr("name");
    }

    /**
     * Get first Engine Default-host from server.xml file
     * @return
     */
    public static String getEngineDefaultHost() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document document = Jsoup.parse(xml);
        Element engine = document.selectFirst("Engine");

        return  engine.attr("defaultHost");
    }

    /**
     * Get Hosts
     * @param engine
     * @return
     */
    public static List<Host> getHosts(Engine engine) {
        List<Host> hosts =  new ArrayList<Host>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document document = Jsoup.parse(xml);

        Elements elements = document.select("Host");
        for (Element element : elements) {
            String name = element.attr("name");
            Host host = new Host(name, engine);
            hosts.add(host);
        }

        return hosts;
    }

    /**
     * Get Service name
     * @return
     */
    public static String getServiceName() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document document = Jsoup.parse(xml);
        Element engine = document.selectFirst("Service");

        return  engine.attr("name");
    }

    /**
     * Get welcome file name from web.xml
     * @param context
     * @return
     */
    public static String getWelcomeFile(Context context) {
        String xml = FileUtil.readUtf8String(Constant.webXmlFile);
        Document document = Jsoup.parse(xml);
        Elements elements = document.select("welcome-file");
        for (Element element : elements) {
            String welcomeFileName = element.text();
            File f = new File(context.getDocBase(), welcomeFileName);
            if (f.exists()) {
                return f.getName();
            }
        }
        // if no file found, return index.html
        return "index.html";
    }

    /**
     * Mine-type parser
     */
    private static Map<String, String> mineTypeMap = new HashMap<String, String>();

    /**
     * read mine-type from web.xml file in the conf directory
     */
    private static void initMineTypeMap() {
        String xml = FileUtil.readUtf8String(Constant.webXmlFile);
        Document document = Jsoup.parse(xml);
        Elements elements = document.select("mime-mapping");
        for (Element element : elements) {
            // element child 0: extension, element child 1: mine-type
            if (!mineTypeMap.containsKey(element.child(0).text())) {
                mineTypeMap.put(element.child(0).text(), element.child(1).text());
            }
        }
    }

    /**
     * get Mine-type from web.xml
     * The synchronized key word is used to make sure initMineTypeMap function do not execute multiple time
     * when multiple request send to server.
     * @param extensionName: extension name
     * @return mine-type
     */
    public static synchronized String getMineType(String extensionName) {
        if (mineTypeMap.isEmpty()) initMineTypeMap();

        if (mineTypeMap.containsKey(extensionName)) return mineTypeMap.get(extensionName);

        return "text.html";
    }

    /**
     * get connectors
     *  1. parse connectors' port from server.xml
     *  2. init. Connector Object
     *  3. add to connectors list
     * @param service: service provided by simple tomcat
     * @return connector
     */
    public static List<Connector> getConnectors(Service service) {
        List<Connector> connectors = new ArrayList<>();

        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document document = Jsoup.parse(xml);
        Elements elements = document.select("Connector");
        for (Element element : elements) {
            int port = Convert.toInt(element.attr("port"));
            String compression = element.attr("compression");
            int compressionMinSize = Convert.toInt(element.attr("compressionMinSize"), 0);
            String noCompressionUserAgents = element.attr("noCompressionUserAgents");
            String compressionMimeType = element.attr("compressionMimeType");

            Connector connector = new Connector(port, service, compression, compressionMinSize, noCompressionUserAgents, compressionMimeType);
            connectors.add(connector);
        }

        return connectors;
    }

    /**
     * Get pwd of WEB-INF/web.xml from context.xml under conf directory.
     * @return pwd of WEB-INF/web.xml
     */
    public static String getWatchedResource() {
        try {
            String xml = FileUtil.readUtf8String(Constant.contextXMLFile);
            Document document = Jsoup.parse(xml);
            Element watchedResource = document.selectFirst("WatchedResource");

            return watchedResource.text();
        } catch (Exception e) {
            e.getStackTrace();
            return "WEB-INF/web.xml";
        }
    }
}
