package SimpleTomcat.http;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * StandardSession object is the session in the Simple Tomcat.
 *  When a user browser the web page provided by Simple Tomcat, the Simple Tomcat will create a session which lasts unit
 *  user exits or existed time reaches the default timeout.
 *  Session can be viewed as a chat between client and server. The data will store in the attributesMap shared by server
 *  and client.
 */
public class StandardSession implements HttpSession {
    private Map<String, Object> attributesMap;  // where session store data
    private String sessionId;                   // current session's id (unique)
    private long creationTime;                  // create time
    private long lastAccessedTime;              // last access time. default time 30 second. if user don't login, session will automatically become invalid
    private ServletContext servletContext;      // servlet context
    private int maxInactiveInterval;            // max inactive interval

    /**
     * Constructor
     * @param sessionId: session id
     * @param servletContext: servlet context
     */
    public StandardSession(String sessionId, ServletContext servletContext) {
        this.attributesMap = new HashMap<>();
        this.sessionId = sessionId;
        this.creationTime = System.currentTimeMillis();
        this.servletContext = servletContext;
    }

    @Override
    public long getCreationTime() {
        return this.creationTime;
    }

    @Override
    public String getId() {
        return this.sessionId;
    }

    @Override
    public long getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    public void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public void setMaxInactiveInterval(int i) {
        this.maxInactiveInterval = i;
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }

    /**
     * @deprecated
     */
    @Override
    public HttpSessionContext getSessionContext() {
        return null;
    }

    @Override
    public Object getAttribute(String s) {
        return attributesMap.get(s);
    }

    /**
     * @param s
     * @deprecated
     */
    @Override
    public Object getValue(String s) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributesMap.keySet());
    }

    /**
     * @deprecated
     */
    @Override
    public String[] getValueNames() {
        return null;
    }

    @Override
    public void setAttribute(String s, Object o) {
        attributesMap.put(s, o);
    }

    /**
     * @param s
     * @param o
     * @deprecated
     */
    @Override
    public void putValue(String s, Object o) {

    }

    @Override
    public void removeAttribute(String s) {
        attributesMap.remove(s);
    }

    /**
     * @param s
     * @deprecated
     */
    @Override
    public void removeValue(String s) {

    }

    @Override
    public void invalidate() {
        attributesMap.clear();
    }

    @Override
    public boolean isNew() {
        return creationTime == lastAccessedTime;
    }
}
