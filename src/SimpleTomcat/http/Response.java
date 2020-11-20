package SimpleTomcat.http;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

/**
 * A Simple Http Response Object
 */
public class Response extends BaseResponse{
    private StringWriter stringWriter;          // to store responded html text
    private PrintWriter printWriter;            // to write data into stringWriter
    private String contentType;                 // header: content-type
    private byte[] body;                        // response body
    private int status;                      // status

    /**
     * Constructor. Set contentType to "text/html"
     */
    public Response() {
        this.stringWriter = new StringWriter();
        this.printWriter = new PrintWriter(this.stringWriter, true);    // open the autoFlush to automatically write printWriter to stringWriter
        this.contentType = "text/html";
    }

    public void setContentType(String mineType) {
        this.contentType = mineType;
    }

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
    public PrintWriter getWriter() {
        return this.printWriter;
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

}
