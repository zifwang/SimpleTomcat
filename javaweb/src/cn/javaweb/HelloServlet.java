package cn.javaweb;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HelloServlet extends HttpServlet {
    public HelloServlet() {
        System.out.println(this + "'s construction method");
    }

    public void init(ServletConfig config) {
        String author = config.getInitParameter("author");
        String site = config.getInitParameter("site");

        System.out.println(this + "'s init. method");
        System.out.println("Get Parameter author:" + author);
        System.out.println("Get Parameter site:" + site);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            System.out.println(this + "'s doGet() method");
            Class jdbc = Class.forName("com.mysql.jdbc.Driver");
            System.out.println(jdbc);
            System.out.println(jdbc.getClassLoader());
//            response.getWriter().println("Test reload context@javaweb.");
            response.getWriter().println("Hello Simply Tomcat from HelloServlet@javaweb" + this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void destroy(){
        System.out.println(this + " is destroy");
    }
}
