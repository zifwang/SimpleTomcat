package SimpleTomcat.catalina;

import SimpleTomcat.http.Request;
import SimpleTomcat.http.Response;
import SimpleTomcat.servlet.DefaultServlet;
import SimpleTomcat.servlet.InvokeServlet;
import SimpleTomcat.util.Constant;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * HttpProcessor object is used to execute web request
 */
public class HttpProcessor {
    /**
     * execute http request and give response to web app based on this request
     * @param socket: web socket
     * @param service: service provided by server
     */
    public static void execute(Socket socket, Service service) {
        try {
            Request request = new Request(socket, service);
            // create response
            Response response = new Response();
            String uri = request.getUri();
            if (uri == null) return;
            String servletClassName = request.getContext().getServletClassByUrl(uri);

            if (servletClassName != null) InvokeServlet.getInstance().service(request, response);
            else DefaultServlet.getInstance().service(request, response);

            if (response.getStatus() == Constant.CODE_200) {
                handle200(socket, response);
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
     * handle 200 ok response
     * @param s: socket
     * @param response: response
     * @throws IOException: IOException
     */
    private static void handle200(Socket s, Response response) throws IOException {
        String contentType = response.getContentType();
        String cookiesHeader = response.getCookiesHeader();
        String headText = Constant.response_head_202;
        headText = StrUtil.format(headText, contentType, cookiesHeader);
        byte[] head = headText.getBytes();
        byte[] body = response.getBody();
        byte[] responseBytes = new byte[head.length + body.length];
        ArrayUtil.copy(head, 0, responseBytes, 0, head.length);
        ArrayUtil.copy(body, 0, responseBytes, head.length, body.length);

        OutputStream outputStream = s.getOutputStream();
        outputStream.write(responseBytes);
        s.close();
    }

    /**
     * Handle file not found response
     * @param s: socket
     * @param uri: uri
     * @throws IOException: IOException
     */
    private static void handle404(Socket s, String uri) throws IOException {
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
     * @throws IOException: IOException
     */
    private static void handle500(Socket s, Exception exception) {
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

}
