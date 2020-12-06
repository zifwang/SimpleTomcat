package SimpleTomcat.servlet;

import SimpleTomcat.catalina.Context;
import SimpleTomcat.classloader.JspClassLoader;
import SimpleTomcat.http.Request;
import SimpleTomcat.http.Response;
import SimpleTomcat.util.Constant;
import SimpleTomcat.util.JspTranslateUtil;
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
            File jspFile = file;

            if (jspFile.exists()) {
                Context context = request.getContext();
                String path = context.getPath();
                String subDirectory;
                // root
                if (path.equals("/")) {
                    subDirectory = "_";
                } else {
                    // not root
                    subDirectory = StrUtil.subAfter(path, '/', false);
                }

                String servletClassPath = JspTranslateUtil.getServletClassPath(uri, subDirectory);
                File jspServletClassFile = new File(servletClassPath);
                // determine whether .jsp file need recompile
                if (!jspServletClassFile.exists()) {
                    JspTranslateUtil.compileJsp(context, jspFile);
                } else if (jspFile.lastModified() > jspServletClassFile.lastModified()) {
                    JspTranslateUtil.compileJsp(context, jspFile);
                    JspClassLoader.invalidJspClassLoader(uri, context);
                }

                String extName = FileUtil.extName(file);
                String mimeType = XMLParser.getMineType(extName);
                response.setContentType(mimeType);

                JspClassLoader jspClassLoader = JspClassLoader.getJspClassLoader(uri, context);
                String jspServletClassName = JspTranslateUtil.getJspServletClassName(uri, subDirectory);
                Class jspServletClass = jspClassLoader.loadClass(jspServletClassName);
                HttpServlet servlet = context.getHttpServlet(jspServletClass);
                servlet.service(request, response);

                response.setStatus(Constant.CODE_200);
            } else {
                response.setStatus(Constant.CODE_404);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
