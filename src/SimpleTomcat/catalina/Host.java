package SimpleTomcat.catalina;

import SimpleTomcat.monitor.WarFileMonitor;
import SimpleTomcat.util.Constant;
import SimpleTomcat.util.XMLParser;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Host Object: Host 的意思是虚拟主机。 通常都是 localhost, 即表示本机。
 */
public class Host {
    private String hostName;            // host name
    private Map<String, Context> contextMap; // map path to Context
    private Engine engine;              // Engine: servlet request parser

    public Host(String hostName, Engine engine) {
        this.hostName = hostName;
        this.engine = engine;
        this.contextMap = new HashMap<String, Context>();

        scanContextsOnWebAppsFolder();
        loadContextsInServerXML();
        scanWarOnWebAppsFolder();

        new WarFileMonitor(this).start();
    }

    public String getHostName() {
        return this.hostName;
    }

    public Map<String, Context> getContextMap() {
        return this.contextMap;
    }

    public Context getContext(String path) {
        return contextMap.get(path);
    }

    /**
     * Reload context. Find context in the contextMap and reload it
     * @param context
     */
    public void reload(Context context) {
        LogFactory.get().info("Reloading Context with name [{}] has started", context.getPath());

        String path = context.getPath();
        String docBase = context.getDocBase();
        boolean reloadable = context.isReloadable();
        // stop
        context.stop();
        // remove
        contextMap.remove(path);
        // allocate new context
        Context newContext = new Context(path, docBase, reloadable, this);
        // assign it to map
        contextMap.put(newContext.getPath(), newContext);
        LogFactory.get().info("Reloading Context with name [{}] has completed", context.getPath());
    }

    /**
     * Scan folders in webapps directory to get path-context map
     */
    private void scanContextsOnWebAppsFolder() {
        File[] folders = Constant.webappsFolder.listFiles();
        for (File folder : folders) {
            if (folder.isDirectory()) {
                loadContext(folder);
            }
        }
    }

    /**
     * Load Html files to context map
     * @param folder
     */
    private void loadContext(File folder) {
        String path = folder.getName();
        if (path.equals("ROOT")) {
            path = "/";
        } else {
            path = "/" + path;
        }
        String docBase = folder.getAbsolutePath();
        Context context = new Context(path, docBase, true, this);
        this.contextMap.put(context.getPath(), context);
    }

    /**
     * Load contexts from server.xml file
     */
    private void loadContextsInServerXML() {
        List<Context> contexts = XMLParser.getContexts(this);
        for (Context context : contexts) {
            this.contextMap.put(context.getPath(), context);
        }
    }

    /**
     * load directory
     * @param folder: directory
     */
    private void load(File folder) {
        String path = folder.getName();
        if (path.equals("ROOT")) {
            path = "/";
        } else {
            path = "/" + path;
        }
        String docBase = folder.getAbsolutePath();
        Context context = new Context(path, docBase, false, this);
    }

    /**
     * load war file:
     * @param warFile: war file
     */
    public void loadWar(File warFile) {
        String fileName = warFile.getName();
        String folderName = StrUtil.subBefore(fileName, ".", true);
        Context context = getContext("/" + folderName);
        // if context existed, then return
        if (context != null) {
            return;
        }
        // if not exist
        File folder = new File(Constant.webappsFolder, folderName);
        // if folder exists -> return
        if (folder.exists()) {
            return;
        }
        File tempWarFile = FileUtil.file(Constant.webappsFolder, folderName, fileName);
        File contextFolder = tempWarFile.getParentFile();
        contextFolder.mkdir();
        FileUtil.copyFile(warFile, tempWarFile);
        //解压
        String command = "jar xvf " + fileName;

        Process p = RuntimeUtil.exec(null, contextFolder, command);
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        tempWarFile.delete();

        load(contextFolder);
    }

    private void scanWarOnWebAppsFolder() {
        File folder = FileUtil.file(Constant.webappsFolder);
        File[] files = folder.listFiles();
        for (File file : files) {
            if(file.getName().toLowerCase().endsWith(".war"))
                loadWar(file);
        }
    }
}
