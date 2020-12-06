package SimpleTomcat.servlet;

import SimpleTomcat.http.Request;
import SimpleTomcat.http.Response;
import SimpleTomcat.util.Constant;
import SimpleTomcat.util.XMLParser;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * JspServlet is designed to deal with jsp file
 *  The basic logic behind JspServlet is: change jsp file into java file
 *  and the build the java file into .class file. Last step is load and
 *  run this .class file.
 * This object is singleton
 */
public class JspServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static class classHolder {
        private static final JspServlet INSTANCE = new JspServlet();
    }

    private JspServlet() {
    }

    public static final JspServlet getInstance() {
        return classHolder.INSTANCE;
    }

    @Override
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        try {
            Request request = (Request) httpServletRequest;
            Response response = (Response) httpServletResponse;
            String uri = request.getRequestURI();

            if (uri.equals("/")) {
                uri = XMLParser.getWelcomeFile(request.getContext());
            }

            String fileName = StrUtil.removePrefix(uri, "/");
            File file = FileUtil.file(request.getRealPath(fileName));

            if (file.exists()) {
                String extName = FileUtil.extName(file);
                String mimeType = XMLParser.getMineType(extName);
                response.setContentType(mimeType);

                byte[] body = FileUtil.readBytes(file);
                response.setBody(body);
                response.setStatus(Constant.CODE_200);
            } else {
                response.setStatus(Constant.CODE_404);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
