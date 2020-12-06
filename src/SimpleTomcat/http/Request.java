package SimpleTomcat.http;

import SimpleTomcat.catalina.Connector;
import SimpleTomcat.catalina.Context;
import SimpleTomcat.catalina.Service;
import SimpleTomcat.util.MiniBrowser;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;

import javax.management.monitor.StringMonitor;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

/**
 * A simple HTTP Request Object
 */
public class Request extends BaseRequest{
    private String requestString;                   // http request string
    private String uri;                             // http uri
    private String method;                          // http method;
    private Context context;                        // Context object is used to store web services information
    private Service service;                        // Service object provided web services
    private Connector connector;                    // Connector object
    private Socket socket;                          // socket: client-server message transportation
    private String queryString;                     // data String from client. e.g. http://127.0.0.1:8080/hello?name=a, the queryString is name=a
    private Map<String, String[]> parameterMap;     // parameterMap: contains data from client to server which is parsed from queryString
    private Map<String, String> headerMap;          // headerMap: contains info about client's system
    private Cookie[] cookies;                       // cookie: request must be able to accept cookie from client's web browser
    private HttpSession session;                    // session


    /**
     * Constructor
     * @param socket: web socket
     * @throws IOException
     */
    public Request(Socket socket, Connector connector) throws IOException {
        this.socket = socket;
        this.connector = connector;
        this.service = connector.getService();
        this.parameterMap = new HashMap<>();
        this.headerMap = new HashMap<>();

        // parse http request
        parseHttpRequest();
        if (StrUtil.isEmpty(this.requestString)) {
            return;
        }
        // parse uri
        parseUri();
        // parse method
        parseMethod();
        // parse context
        parseContext();
        if (!this.context.getPath().equals("/")) {
            this.uri = StrUtil.removePrefix(this.uri, this.context.getPath());
            if (StrUtil.isEmpty(this.uri)) {
                this.uri = "/";
            }
        }
        // parse parameters
        parseParameters();
        // parse headers
        parseHeader();
        // parse cookie: cookie is contained in headerMap with key = cookie
        parseCookies();
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

    public Connector getConnector() {
        return this.connector;
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

    @Override
    public String getParameter(String name) {
        if (!parameterMap.containsKey(name)) {
            return null;
        }

        String values[] = parameterMap.get(name);
        return values[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameterMap;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }

    @Override
    public String getHeader(String name) {
        if (name == null) {
            return null;
        }

        name = name.toLowerCase();
        return headerMap.get(name);
    }

    @Override
    public Enumeration getHeaderNames() {
        return Collections.enumeration(headerMap.keySet());
    }

    @Override
    public int getIntHeader(String name) {
        return Convert.toInt(headerMap.get(name), 0);
    }

    @Override
    public Cookie[] getCookies() {
        return cookies;
    }

    @Override
    public String getLocalAddr() {
        return socket.getLocalAddress().getHostAddress();
    }

    @Override
    public String getLocalName() {
        return socket.getLocalAddress().getHostName();
    }

    @Override
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    @Override
    public String getProtocol() {
        return "HTTP:/1.1";
    }

    @Override
    public String getRemoteAddr() {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
        String remoteAddr = inetSocketAddress.getAddress().toString();
        return StrUtil.subAfter(remoteAddr, "/", false);
    }

    @Override
    public String getRemoteHost() {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
        return inetSocketAddress.getHostName();
    }

    @Override
    public int getRemotePort() {
        return socket.getPort();
    }

    @Override
    public String getScheme() {
        return "http";
    }

    @Override
    public String getServerName() {
        return getHeader("host").trim();
    }

    @Override
    public int getServerPort() {
        return getLocalPort();
    }

    @Override
    public String getContextPath() {
        String contextPath = this.context.getPath();
        if (contextPath.equals("/")) {
            return "";
        }
        return contextPath;
    }

    @Override
    public String getRequestURI() {
        return uri;
    }

    @Override
    public StringBuffer getRequestURL() {
        String scheme = getScheme();
        int port = getServerPort();
        if (port < 0) {
            port = 80;
        }

        // build request url;
        StringBuffer url = new StringBuffer();
        url.append(scheme);
        url.append("://");
        url.append(getServerName());
        if ((scheme.equals("http") && (port != 80)) || (scheme.equals("https") && (port != 443))) {
            url.append(":");
            url.append(port);
        }
        url.append(getRequestURI());

        return url;
    }

    @Override
    public String getServletPath() {
        return uri;
    }

    @Override
    public HttpSession getSession() {
        return session;
    }

    public void setSession(HttpSession session) {
        this.session = session;
    }

    /**
     * Get sessionId from cookie
     * @return
     */
    public String getJSessionIdFromCookie() {
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("JSESSIONID")) {
                return cookie.getValue();
            }
        }

        return null;
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

    /**
     * parse client's data (parameters) to server
     */
    private void parseParameters() {
        // GET Method
        if (this.method.equals("GET")) {
            String url = StrUtil.subBetween(this.requestString, " ", " ");
            if (StrUtil.contains(url, '?')) {
                this.queryString = StrUtil.subAfter(url, "?", false);
            }
        }

        // Post Method
        if (this.method.equals("POST")) {
            this.queryString = StrUtil.subAfter(requestString, "\r\n\r\n", false);
        }

        if (this.queryString == null) {
            return;
        }

        // QueryString -> parameterMap
        queryString = URLUtil.decode(queryString);
        String[] paramPairs = queryString.split("&");
        if (paramPairs != null) {
            for (String paramPair : paramPairs) {
                String[] param = paramPair.split("=");
                String key = param[0];
                String value = param[1];

                String[] values;
                if (parameterMap.containsKey(key)) {
                    values = parameterMap.get(key);
                    values = ArrayUtil.append(values, value);
                } else {
                    values = new String[]{value};
                }
                parameterMap.put(key, values);
            }
        }
    }

    /**
     * parse header info
     */
    private void parseHeader() {
        StringReader stringReader = new StringReader(requestString);
        List<String> lines = new ArrayList<>();
        IoUtil.readLines(stringReader, lines);

        // Start from the second line because the first line is http url
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);

            // End of request header
            if (line.length() == 0) {
                break;
            }
            String[] infos = line.split(":");
            String headerName = infos[0].toLowerCase();
            String headerVal = infos[1];
            headerMap.put(headerName, headerVal);
        }
    }

    /**
     * accept cookies from client's web browser and parse cookies to a cookie list
     */
    private void parseCookies() {
        List<Cookie> cookieList = new ArrayList<>();
        String cookies = headerMap.get("cookie");
        if (cookies != null) {
            String[] pairs = StrUtil.split(cookies, ";");
            for (String pair : pairs) {
                if (StrUtil.isBlank(pair)) {
                    continue;
                }
                String[] infos = StrUtil.split(pair, "=");
                Cookie cookie = new Cookie(infos[0].trim(), infos[1].trim());
                cookieList.add(cookie);
            }
        }
        this.cookies = ArrayUtil.toArray(cookieList, Cookie.class);
    }

}
