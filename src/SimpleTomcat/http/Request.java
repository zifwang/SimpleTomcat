package SimpleTomcat.http;

import SimpleTomcat.catalina.Context;
import SimpleTomcat.catalina.Service;
import SimpleTomcat.util.MiniBrowser;
import cn.hutool.core.util.StrUtil;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * A simple HTTP Request Object
 */
public class Request extends BaseRequest{
    private String requestString;   // http request string
    private String uri;             // http uri
    private String method;          // http method;
    private Context context;
    private Service service;
    private Socket socket;

    /**
     * Constructor
     * @param socket: web socket
     * @throws IOException
     */
    public Request(Socket socket, Service service) throws IOException {
        this.socket = socket;
        this.service = service;
        parseHttpRequest();
        if (StrUtil.isEmpty(this.requestString)) return;
        parseUri();
        parseMethod();
        parseContext();
        if (!this.context.getPath().equals("/")) {
            this.uri = StrUtil.removePrefix(this.uri, this.context.getPath());
            if (StrUtil.isEmpty(this.uri)) this.uri = "/";
        }
    }

    public String getUri() {
        return this.uri;
    }

    public String getRequestString() {
        return this.requestString;
    }

    public Context getContext() {
        return this.context;
    }

    @Override
    public String getMethod() {
        return this.method;
    }

    @Override
    public ServletContext getServletContext() {
        return context.getServletContext();
    }

    @Override
    public String getRealPath(String path) {
        return getServletContext().getRealPath(path);
    }

    /**
     * parse http request
     * @throws IOException
     */
    private void parseHttpRequest() throws IOException {
        InputStream is = this.socket.getInputStream();
        //为什么这里不能用 true 呢？ 因为浏览器默认使用长连接，发出的连接不会主动关闭，那么 Request 读取数据的时候 就会卡在那里了
        byte[] bytes = MiniBrowser.readBytes(is, false);
        this.requestString = new String(bytes, "utf-8");
    }

    /**
     * parse uri
     * uri: e.g. http://127.0.0.1(address):8080(port number)/index.html(web page)?(get)name=green
     */
    private void parseUri() {
        if (this.requestString.length() <= 0) return;
        this.uri = StrUtil.subBetween(this.requestString, " ", " ");
        if (!StrUtil.contains(this.uri, '?')) return;   // check whether is post http
        this.uri = StrUtil.subBefore(this.uri, "?", false); // get http
    }

    /**
     * parse context
     */
    private void parseContext() {
        this.context = this.service.getEngine().getDefaultHost().getContext(this.uri);
        if (this.context != null) return;

        String path = StrUtil.subBetween(uri, "/", "/");
        if (path == null) {
            path = "/";
        } else {
            path = "/" + path;
        }

        this.context = this.service.getEngine().getDefaultHost().getContext(path);
        if (this.context == null)
            this.context = this.service.getEngine().getDefaultHost().getContext("/");
    }

    /**
     * parse http method
     */
    private void parseMethod() {
        this.method = StrUtil.subBefore(this.requestString, " ", false);
    }

}
