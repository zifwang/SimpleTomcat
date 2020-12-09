package cn.javaweb;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SetCookieServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            Cookie cookie = new Cookie("name", "Gareen(cookie)");
            cookie.setMaxAge(60 * 24 * 60);
            cookie.setPath("/");
            response.addCookie(cookie);
            response.getWriter().println("Successfully set cookie");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
