package cn.javaweb;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GetCookieServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    response.getWriter().println(cookie.getName() + ":" + cookie.getValue() + "<br>");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
