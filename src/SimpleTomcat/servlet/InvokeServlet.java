package SimpleTomcat.servlet;

import SimpleTomcat.catalina.Context;
import SimpleTomcat.http.Request;
import SimpleTomcat.http.Response;
import SimpleTomcat.util.Constant;
import cn.hutool.core.util.ReflectUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet, Static Html, and Dynamic html (JSP file) are three things a web-service have to process.
 * InvokeServlet Object is used to process Servlet request.
 * This object is singleton.
 */
public class InvokeServlet extends HttpServlet {
    private static class classHolder {
        private static final InvokeServlet INSTANCE = new InvokeServlet();
    }

    private InvokeServlet() {
    }

    public static final InvokeServlet getInstance() {
        return classHolder.INSTANCE;
    }

    @Override
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException  {
        Request request = (Request) httpServletRequest;
        Response response = (Response) httpServletResponse;
        String uri = request.getUri();
        Context context = request.getContext();
        String servletClassName = context.getServletClassByUrl(uri);

        try {
            Class servletClass = context.getWebappClassLoader().loadClass(servletClassName);
            System.out.println("ServletClass: " + servletClass);
            System.out.println("ServletClass's classLoader: " + servletClass.getClassLoader());
            Object servletObject = context.getHttpServlet(servletClass);
            ReflectUtil.invoke(servletObject, "service", request, response);

            // set response status
            response.setStatus(Constant.CODE_200);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
