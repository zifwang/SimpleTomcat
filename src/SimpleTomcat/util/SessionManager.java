package SimpleTomcat.util;

import SimpleTomcat.http.Request;
import SimpleTomcat.http.Response;
import SimpleTomcat.http.StandardSession;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SessionManager Class is used to manager sessions in the Simple Tomcat
 * <p>
 *     When multiple users visit web site provided by the Simple Tomcat, the Simple Tomcat will create a session
 *     for each users. To manage sessions in the Simple Tomcat, the sessionManager is introduced.
 *     sessionMap is used to store sessions with a sessionId. -- key: sessionId, value: session
 *     defaultTimeout is used to define the default time out for each session.
 *
 * </p>
 */
public class SessionManager {
    private static Map<String, StandardSession> sessionMap = new ConcurrentHashMap<>();     // store sessions.
    private static int defaultTimeout = getTimeout();                                       // session default timeout

    // set SessionInactiveCheckThread start in default.
    static {
        startSessionInactiveCheckThread();
    }

    /**
     * randomly generate sessionId and encrypt by md5
     * set to synchronized to prevent a session is set to multiple sessionId when multiple threads in working
     * @return
     */
    public static synchronized String generateSessionId() {
        String sessionId = null;
        byte[] bytes = RandomUtil.randomBytes(16);
        sessionId = new String(bytes);
        sessionId = SecureUtil.md5(sessionId);                                      // md5 encrypt
        sessionId = sessionId.toUpperCase();                                        // upper case
        return sessionId;
    }

    /**
     * get session
     *  if web browser send sessionId and it is invalid, create new session
     *  if web browser send sessionId and it is valid, update lastAccessedTime and create corresponding cookie
     *  if web browser does not send sessionId, creat new session
     * @param sessionId: session id
     * @param request: http request
     * @param response: http response
     * @return
     */
    public static HttpSession getSession(String sessionId, Request request, Response response) {
        if (sessionId == null) {
            return newSession(request, response);
        }

        StandardSession currentSession = sessionMap.get(sessionId);
        if (currentSession == null) {
            return newSession(request, response);
        }
        currentSession.setLastAccessedTime(System.currentTimeMillis());
        createCookieBySession(currentSession, request, response);
        return currentSession;
    }

    /**
     * Create a new HttpSession
     * @param request: http request
     * @param response: http response
     * @return
     */
    private static HttpSession newSession(Request request, Response response) {
        ServletContext servletContext = request.getServletContext();
        String sessionId = generateSessionId();
        StandardSession standardSession = new StandardSession(sessionId, servletContext);
        standardSession.setMaxInactiveInterval(defaultTimeout);
        sessionMap.put(sessionId, standardSession);
        createCookieBySession(standardSession, request, response);

        return standardSession;
    }

    /**
     * create a new cookie
     * @param session: http session
     * @param request: http request
     * @param response: http response
     */
    private static void createCookieBySession(HttpSession session, Request request, Response response) {
        Cookie cookie = new Cookie("JSESSIONID", session.getId());
        cookie.setMaxAge(session.getMaxInactiveInterval());
        cookie.setPath(request.getContext().getPath());
        response.addCookie(cookie);
    }

    /**
     * Start a thread for SessionInactiveCheck in each 30 seconds
     */
    private static void startSessionInactiveCheckThread() {
        new Thread() {
            @Override
            public void run() {
                sessionInactiveCheck();
                ThreadUtil.sleep(1000*30);
            }
        }.start();
    }

    /**
     * sessionInactiveCheck is used to check whether session is still active.
     * If it is inactive, remove it from sessionMap.
     * It basically compare maxInactiveInterval with (currentTime - lastAccessedTime) to know inactive sessionId.
     */
    private static void sessionInactiveCheck() {
        List<String> inactiveSessionIds = new ArrayList<>();

        for (Map.Entry<String, StandardSession> entry : sessionMap.entrySet()) {
            String sessionId = entry.getKey();
            StandardSession standardSession = entry.getValue();
            long interval = System.currentTimeMillis() - standardSession.getLastAccessedTime();

            if (interval > standardSession.getMaxInactiveInterval() * 1000) {
                inactiveSessionIds.add(sessionId);
            }
        }

        for (String sessionId : inactiveSessionIds) {
            sessionMap.remove(sessionId);
        }
    }

    /**
     * Read default timeout from web.xml under the config directory where file types and welcome page is defined.
     * @return default timeout
     */
    private static int getTimeout() {
        int defaultTimeout = 30;        // set 30 seconds default time out
        try {
            Document document = Jsoup.parse(Constant.webXmlFile, "utf-8");
            Elements elements = document.select("session-config session-timeout");
            if (!elements.isEmpty()) {
                defaultTimeout = Convert.toInt(elements.get(0).text());
            }
        } catch (IOException e) {
            return defaultTimeout;
        }

        return defaultTimeout;
    }

}


