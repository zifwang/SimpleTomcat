package SimpleTomcat.http;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * A Simple Http Response Object
 */
public class Response extends BaseResponse{
    private StringWriter stringWriter;          // to store responded html text
    private PrintWriter printWriter;            // to write data into stringWriter
    private String contentType;                 // header: content-type
    private byte[] body;                        // response body
    private int status;                         // status
    private List<Cookie> cookies;               // cookies
    private String redirectPath;                // redirect path

    /**
     * Constructor. Set contentType to "text/html"
     */
    public Response() {
        this.stringWriter = new StringWriter();
        this.printWriter = new PrintWriter(this.stringWriter, true);    // open the autoFlush to automatically write printWriter to stringWriter
        this.contentType = "text/html";
        this.cookies = new ArrayList<>();
    }

    // remove cache to make sure resetting body when server jump happens. This aims to prevent last page impacting current web page
    @Override
    public void resetBuffer() {
        this.stringWriter.getBuffer().setLength(0);
    }

    @Override
    public void sendRedirect(String redirectPath) throws IOException {
        this.redirectPath = redirectPath;
    }

    public String getRedirectPath() {
        return this.redirectPath;
    }

    @Override
    public void setContentType(String mineType) {
        this.contentType = mineType;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public int getStatus() {
        return this.status;
    }

    /**
     * This getter function provided here is used to put data into printWriter by Response.getWriter.println(String).
     * Use the autoFlush provided from PrintWriter to put data into stringWriter
     * @return
     */
    @Override
    public PrintWriter getWriter() {
        return this.printWriter;
    }

    @Override
    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public List<Cookie> getCookies() {
        return this.cookies;
    }

    /**
     * get byte array from html
     * @return
     * @throws UnsupportedEncodingException
     */
    public byte[] getBody() throws UnsupportedEncodingException {
        if (body == null) body = stringWriter.toString().getBytes("utf-8");

        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    /**
     * GetCookiesHeader() is used to create cookie from client's info
     *  Cookie is a way to transfer information or data between client(web browser) and server.
     *  Cookie is created by server, but will not store in the server. After finishing creation,
     *  server will send cookie to client's web browser. And client's web browser will store these cookies locally.
     *  Web browser will resend cookies to server when user browse web site.
     *  It is HttpServletResponse's responsibility to create the cookie when user first in the web site.
     *  So, getCookiesHeader() method aims to do this job.
     * @return
     */
    public String getCookiesHeader() {
        if (cookies == null) {
            return "";
        }

        String pattern = "EEE, d MMM yyyy HH:mm:ss 'GMT'";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, Locale.ENGLISH);
        // build cookies header
        StringBuffer cookiesHeader = new StringBuffer();
        for (Cookie cookie : cookies) {
            cookiesHeader.append("\r\n");
            cookiesHeader.append("Set-Cookie: ");
            cookiesHeader.append(cookie.getName() + "=" + cookie.getValue() + "; ");
            if (cookie.getMaxAge() != -1) {
                cookiesHeader.append("Expires=");
                Date now = new Date();
                Date expire = DateUtil.offset(now, DateField.MINUTE, cookie.getMaxAge());
                cookiesHeader.append(simpleDateFormat.format(expire));
                cookiesHeader.append(";");
            }
            if (cookie.getPath() != null) {
                cookiesHeader.append("Path=" + cookie.getPath());
            }
        }

        return cookiesHeader.toString();
    }
}
