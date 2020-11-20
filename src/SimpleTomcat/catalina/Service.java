package SimpleTomcat.catalina;

import SimpleTomcat.util.XMLParser;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;

import java.util.List;

/**
 * Service Object: 用于代表 tomcat 提供的服务。 它里面会有很多 Connector 对象
 */
public class Service {
    private String serviceName;
    private List<Connector> connectors;     // connectors are list of Connector which listen on ports defined in server.xml
    private Engine engine;
    private Server server;

    /**
     * Constructor
     */
    public Service(Server server){
        this.serviceName = XMLParser.getServiceName();
        this.engine = new Engine(this);
        this.server = server;
        this.connectors = XMLParser.getConnectors(this);
    }

    /**
     * get engine
     * @return engine
     */
    public Engine getEngine() {
        return engine;
    }

    /**
     * start service
     */
    public void start() {
        init();
    }

    /**
     * init service
     */
    private void init() {
        TimeInterval timeInterval = DateUtil.timer();
        for (Connector c : connectors)
            c.init();
        LogFactory.get().info("Initialization processed in {} ms", timeInterval.intervalMs());
        for (Connector c : connectors)
            c.start();
    }
}
