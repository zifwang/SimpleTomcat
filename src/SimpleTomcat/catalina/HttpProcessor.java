package SimpleTomcat.catalina;

import SimpleTomcat.http.Request;
import SimpleTomcat.http.Response;
import SimpleTomcat.servlet.DefaultServlet;
import SimpleTomcat.servlet.InvokeServlet;
import SimpleTomcat.servlet.JspServlet;
import SimpleTomcat.util.Constant;
import SimpleTomcat.util.SessionManager;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.log.LogFactory;
import org.apache.tomcat.util.bcel.Const;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * HttpProcessor object is used to execute web request
 */
public class HttpProcessor {
    /**
     * execute http request and give response to web app based on this request
     * @param socket: web socket
     * @param request: http request
     * @param response: http response
     */
    public void execute(Socket socket, Request request, Response response) {
        try {
            String uri = request.getUri();
            if (uri == null) {
                return;
            }

            // create session or get session
            prepareSession(request, response);

            String servletClassName = request.getContext().getServletClassByUrl(uri);

            HttpServlet workingServlet;
            // Choose the way to provide service based on servlet (use one of methods: InvokeServlet, JspServlet, and DefaultServlet)
            if (servletClassName != null) {
                workingServlet = InvokeServlet.getInstance();
            }
            else if (uri.endsWith(".jsp")) {
                workingServlet = JspServlet.getInstance();
            }
            else {
                workingServlet = DefaultServlet.getInstance();
            }

            List<Filter> filterList = request.getContext().getMatchedFilters(request.getRequestURI());
            ApplicationFilterChain filterChain = new ApplicationFilterChain(filterList, workingServlet);
            filterChain.doFilter(request, response);

            if (request.isForwarded()) {
                // check server side jump
                return;
            }

            if (response.getStatus() == Constant.CODE_200) {
                handle200(socket, request, response);
                return;
            }
            else if (response.getStatus() == Constant.CODE_302) {
                handle302(socket, response);
                return;
            }
            else if (response.getStatus() == Constant.CODE_404) {
                handle404(socket, uri);
                return;
            }

        } catch (Exception e) {
            LogFactory.get().error(e);
            handle500(socket, e);
        }
        finally {
            try {
                if(!socket.isClosed())
                    socket.close();
            } catch (IOException e) {
                LogFactory.get().error(e);
                e.printStackTrace();
            }
        }
    }

    /**
     * prepare session.
     *  First, get sessionId from cookie.
     *  Second, create session or get session from SessionManager.
     *  Third, set this session to request.
     * @param request: http request
     * @param response: http response
     */
    public void prepareSession(Request request, Response response) {
        String sessionId = request.getJSessionIdFromCookie();
        HttpSession session = SessionManager.getSession(sessionId, request, response);
        request.setSession(session);
    }

    /**
     * handle 200 ok response
     * @param s: socket
     * @param request: request
     * @param response: response
     * @throws IOException: IOException
     */
    private void handle200(Socket s, Request request, Response response) throws IOException {
        // get content's mimeType
        String contentType = response.getContentType();
        // get cookieHeader
        String cookiesHeader = response.getCookiesHeader();
        // get body
        byte[] body = response.getBody();
        // check whether need gzip
        boolean gzipFlag = isGzip(request, body, contentType);
        // set headText and gzip body based on gzipFlag
        String headText;
        if (gzipFlag) {
            headText = Constant.response_head_200_gzip;
            body = ZipUtil.gzip(body);
        } else {
            headText = Constant.response_head_200;
        }
        headText = StrUtil.format(headText, contentType, cookiesHeader);
        byte[] head = headText.getBytes();
        byte[] responseBytes = new byte[head.length + body.length];
        ArrayUtil.copy(head, 0, responseBytes, 0, head.length);
        ArrayUtil.copy(body, 0, responseBytes, head.length, body.length);

        OutputStream outputStream = s.getOutputStream();
        outputStream.write(responseBytes, 0, responseBytes.length);
        outputStream.flush();
        outputStream.close();
    }

    /**
     * Handle client jump
     * @param socket: socket
     * @param response: response
     */
    private void handle302(Socket socket, Response response) throws IOException {
        String redirectPath = response.getRedirectPath();
        String head_text = Constant.response_head_302;
        String header = StrUtil.format(head_text, redirectPath);
        byte[] responseBytes = header.getBytes("utf-8");
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(responseBytes);
    }

    /**
     * Handle file not found response
     * @param s: socket
     * @param uri: uri
     * @throws IOException: IOException
     */
    private void handle404(Socket s, String uri) throws IOException {
        String headText = Constant.response_head_404;
        String responseText = StrUtil.format(Constant.html_404, uri, uri);
        responseText = headText + responseText;
        OutputStream outputStream = s.getOutputStream();
        byte[] responseByte = responseText.getBytes(StandardCharsets.UTF_8);
        outputStream.write(responseByte);
    }

    /**
     * Handle Internet Server Error
     * @param s: socket
     * @param exception: exception
     */
    private void handle500(Socket s, Exception exception) {
        try {
            StackTraceElement[] stackTraceElements = exception.getStackTrace();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(exception.toString());
            stringBuilder.append("\r\n");
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                stringBuilder.append("\t");
                stringBuilder.append(stackTraceElement);
                stringBuilder.append("\r\n");
            }

            String msg = exception.getMessage();
            if (msg != null && msg.length() > 20) msg = msg.substring(0, 19);

            String text = StrUtil.format(Constant.html_500, msg, exception.toString(), stringBuilder.toString());
            text = Constant.response_head_500 + text;
            byte[] responseBytes = text.getBytes(StandardCharsets.UTF_8);
            OutputStream outputStream = s.getOutputStream();
            outputStream.write(responseBytes);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /**
     * check whether the transferred file needs to be zipped.
     * @param request: request
     * @param body: transferred html body
     * @param mimeType: file type
     * @return true or false: follow the steps in the method to check out whether can turn gzip on
     */
    private boolean isGzip(Request request, byte[] body, String mimeType) {
        // Check whether Accept-Encoding contains gzip in header
        String acceptEncodings = request.getHeader("Accept-Encoding");
        if (!StrUtil.containsAny(acceptEncodings, "gzip")) {
            return false;
        }

        Connector connector = request.getConnector();
        if (mimeType.contains(";")) {
            mimeType = StrUtil.subBefore(mimeType, ";", false);
        }
        // check whether compression model is on this server
        if (!connector.getCompression().equals("on")) {
            return false;
        }
        // check whether the transfer file size is more than compression min size
        if (connector.getCompressionMinSize() > body.length) {
            return false;
        }
        // check whether user agent accept gzip
        String userAgents = connector.getNoCompressionUserAgents();
        String[] userAgentList = userAgents.split(",");
        for (String userAgent : userAgentList) {
            userAgent = userAgent.trim();
            if (StrUtil.containsAny(request.getHeader("User-Agent"), userAgent)) {
                return false;
            }
        }
        // check whether requested mime type is one of mime types provided by server
        String serverMimeTypes = connector.getCompressionMimeType();
        String[] serverMimeTypeList = serverMimeTypes.split(",");
        for (String serverMimeType : serverMimeTypeList) {
            if (mimeType.equals(serverMimeType)) {
                return true;
            }
        }

        return false;
    }

}
