package SimpleTomcat.catalina;

import SimpleTomcat.util.XMLParser;

import java.io.Serializable;
import java.util.List;

/**
 * Engine Object is used to parse servlet request
 */
public class Engine {
    private String defaultHost;         // default host
    private List<Host> hosts;
    private Service service;

    public Engine(Service service) {
        this.service = service;
        this.defaultHost = XMLParser.getEngineDefaultHost();
        this.hosts = XMLParser.getHosts(this);
        getDefaultHost();
    }

    public Service getService() {
        return this.service;
    }

    public String getDefaultHostName() {
        return this.defaultHost;
    }

    public List<Host> getHosts() {
        return this.hosts;
    }

    private void isDefaultHostExist() {
        if (getDefaultHost() == null) throw new RuntimeException("The defaultHost " + this.defaultHost + " does not exist!");
    }

    public Host getDefaultHost() {
        for (Host host : hosts) {
            if (host.getHostName().equals(defaultHost)) return host;
        }
        return null;
    }
}
