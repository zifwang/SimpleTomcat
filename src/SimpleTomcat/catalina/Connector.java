package SimpleTomcat.catalina;

import SimpleTomcat.http.Request;
import SimpleTomcat.http.Response;
import SimpleTomcat.util.ThreadPoolUtil;
import cn.hutool.log.LogFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Connector Object is used for listening on multiple ports
 *  Connector implements gzip. Usually, the data transform between clients and server are text which
 *  is able to zip efficiently. After gzip files, the transformation rate will increase.
 *  Gzip is one way to zip files and it is also the way that tomcat does.
 */
public class Connector implements Runnable {
    private int port;                       // port number
    private Service service;                // service provided by server
    private String compression;             // compression: indicate whether zip file is used
    private int compressionMinSize;         // minimal compression size
    private String noCompressionUserAgents; // browser user can not use compression
    private String compressionMimeType;     // MimeType stands for file type. This is indicate which file can be compressed

    public Connector() {
    }

    public Connector(int port, Service service) {
        this.port = port;
        this.service = service;
    }

    public Connector(int port, Service service, String compression, int compressionMinSize, String noCompressionUserAgents, String compressionMimeType) {
        this.port = port;
        this.service = service;
        this.compression = compression;
        this.compressionMinSize = compressionMinSize;
        this.noCompressionUserAgents = noCompressionUserAgents;
        this.compressionMimeType = compressionMimeType;
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

    public String getCompression() {
        return this.compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public int getCompressionMinSize() {
        return compressionMinSize;
    }

    public void setCompressionMinSize(int compressionMinSize) {
        this.compressionMinSize = compressionMinSize;
    }

    public String getNoCompressionUserAgents() {
        return noCompressionUserAgents;
    }

    public void setNoCompressionUserAgents(String noCompressionUserAgents) {
        this.noCompressionUserAgents = noCompressionUserAgents;
    }

    public String getCompressionMimeType() {
        return compressionMimeType;
    }

    public void setCompressionMimeType(String compressionMimeType) {
        this.compressionMimeType = compressionMimeType;
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
                        try {
                            Request request = new Request(socket, Connector.this);
                            Response response = new Response();
                            HttpProcessor processor = new HttpProcessor();
                            processor.execute(socket, request, response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (!socket.isClosed()) {
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
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
