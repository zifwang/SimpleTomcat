package SimpleTomcat.catalina;


import SimpleTomcat.util.ThreadPoolUtil;
import cn.hutool.log.LogFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Connector Object is used for listening on multiple ports
 */
public class Connector implements Runnable {
    private int port;           // port number
    private Service service;    // service provided by server

    public Connector(int port, Service service) {
        this.port = port;
        this.service = service;
    }

    public void setPort(int port) {
        this.port = port;
    }
    public int getPort() {
        return this.port;
    }
    public void setService(Service service) {
        this.service = service;
    }
    public Service getService() {
        return this.service;
    }

    /**
     * init connector
     */
    public void init() {
        LogFactory.get().info("Initializing ProtocolHandler [http-bio-{}]", this.port);
    }

    /**
     * start process
     * init. a new thread and start it
     */
    public void start() {
        LogFactory.get().info("Starting ProtocolHandler [http-bio-{}]", this.port);
        new Thread(this).start();
    }

    /**
     * run connector
     */
    @Override
    public void run() {
        try {
            // Define a server socket
            ServerSocket serverSocket = new ServerSocket(port);

            while (true) {
                Socket socket = serverSocket.accept();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        HttpProcessor.execute(socket, service);
                    }
                };
                ThreadPoolUtil.run(runnable);
            }
        } catch (IOException ioe) {
            LogFactory.get().error(ioe);
            ioe.printStackTrace();
        }
    }

}
