package SimpleTomcat.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server 就代表最外层的 Server 元素，即服务器本身。
 */
public class Server {
    private Service service;

    public Server() {
        this.service = new Service(this);
    }

    /**
     * Start the server
     */
    public void start() {
        TimeInterval timeInterval = DateUtil.timer();
        logJVM();
        init();
        LogFactory.get().info("Server startup in {} ms", timeInterval.intervalMs());
    }

    /**
     * init the server
     */
    private void init() {
        service.start();;
    }

    /**
     * logJVM function print out system information
     */
    private void logJVM() {
        Map<String, String> infos = new LinkedHashMap<String, String>();
        infos.put("Server versions", "SimpleTomcat/1.0.1");
        infos.put("Server built", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        infos.put("Server number", "1.0.1");
        infos.put("OS Name\t", SystemUtil.get("os.name"));
        infos.put("OS Version", SystemUtil.get("os.version"));
        infos.put("Architecture", SystemUtil.get("os.arch"));
        infos.put("Java Home", SystemUtil.get("java.home"));
        infos.put("JVM Version", SystemUtil.get("java.runtime.version"));
        infos.put("JVM Vendor", SystemUtil.get("java.vm.specification.vendor"));

        for (Map.Entry<String, String> entry : infos.entrySet()) {
            LogFactory.get().info(entry.getKey() + ":\t\t" + entry.getValue());
        }
    }
}
