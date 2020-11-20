package SimpleTomcat.webappservlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HelloServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            response.getWriter().println("Hello Simple Tomcat from HelloServlet");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
