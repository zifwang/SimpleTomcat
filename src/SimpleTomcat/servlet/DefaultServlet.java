package SimpleTomcat.servlet;

import SimpleTomcat.catalina.Context;
import SimpleTomcat.http.Request;
import SimpleTomcat.http.Response;
import SimpleTomcat.util.Constant;
import SimpleTomcat.util.XMLParser;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * Servlet, Static Html, and Dynamic html (JSP file) are three things a web-service have to process.
 * DefaultServlet Object is used to process Static html file.
 * This object is singleton.
 */
public class DefaultServlet extends HttpServlet {
    private static class classHolder {
        private static final DefaultServlet INSTANCE = new DefaultServlet();
    }

    private DefaultServlet() {
    }

    public static final DefaultServlet getInstance() {
        return classHolder.INSTANCE;
    }

    @Override
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        Request request = (Request) httpServletRequest;
        Response response = (Response) httpServletResponse;
        String uri = request.getUri();
        Context context = request.getContext();
        if("/500.html".equals(uri))
            throw new RuntimeException("this is a deliberately created exception");
        // uri in root -> open welcome file: index.html/jsp
        if (uri.equals("/")) uri = XMLParser.getWelcomeFile(request.getContext());
        String fileName = StrUtil.removePrefix(uri, "/");
        File file = FileUtil.file(request.getRealPath(fileName));

        if (file.exists()) {
            String extensionName = FileUtil.extName(file);
            response.setContentType(XMLParser.getMineType(extensionName));
            byte[] body = FileUtil.readBytes(file);
            response.setBody(body);
            if (fileName.equals("timeConsume.html")) ThreadUtil.sleep(1000);
            response.setStatus(Constant.CODE_200);
        } else {
            response.setStatus(Constant.CODE_404);
        }
    }
}
