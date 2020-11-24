package SimpleTomcat.util;

import cn.hutool.system.SystemUtil;

import java.io.File;

/**
 * Constant Object contains important constants of Simply Tomcat: e.g., response header
 */
public class Constant {
    // 202 http response header: stands for transmission ok
    public final static String response_head_202 = "HTTP/1.1 200 OK\r\n" + "Content-Type: {}\r\n\r\n";

    // 404 http response header: file not found
    public final static String response_head_404 = "HTTP/1.1 404 Not Found\r\n" + "Content-Type: text/html\r\n\r\n";
    // 404 html format
    public final static String html_404 =
            "<html><head><title>Simple Tomcat/1.0.1 - Error report</title><style>" +
            "<!--H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} " +
            "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} " +
            "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} " +
            "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} " +
            "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} " +
            "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}" +
            "A {color : black;}A.name {color : black;}HR {color : #525D76;}--></style> " +
            "</head><body><h1>HTTP Status 404 - {}</h1>" +
            "<HR size='1' noshade='noshade'><p><b>type</b> Status report</p><p><b>message</b> <u>{}</u></p><p><b>description</b> " +
            "<u>The requested resource is not available.</u></p><HR size='1' noshade='noshade'><h3>SimpleTocmat 1.0.1</h3>" +
            "</body></html>";

    // 500 http response header: Internal Server Error
    public final static String response_head_500 = "HTTP/1.1 500 Internal Server Error\r\n" + "Content-Type: text/html\r\n\r\n";
    // 500 html format
    public final static String html_500 =
            "<html><head><title>Simple Tomcat/1.0.1 - Error report</title><style>"
            + "<!--H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} "
            + "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} "
            + "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} "
            + "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} "
            + "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} "
            + "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}"
            + "A {color : black;}A.name {color : black;}HR {color : #525D76;}--></style> "
            + "</head><body><h1>HTTP Status 500 - An exception occurred processing {}</h1>"
            + "<HR size='1' noshade='noshade'><p><b>type</b> exception report</p><p><b>message</b> <u>An exception occurred processing {}</u></p><p><b>description</b> "
            + "<u>The server encountered an internal error that prevented it from fulfilling this request.</u></p>"
            + "<p>Stacktrace:</p>" + "<pre>{}</pre>" + "<HR size='1' noshade='noshade'><h3>SimpleTomcat 1.0.1</h3>"
            + "</body></html>";

    // WEBAPPS Directory
    public final static File webappsFolder = new File(SystemUtil.get("user.dir"),"webapps");
    public final static File rootFolder = new File(webappsFolder,"ROOT");

    // Configuration Directory
    public final static File confFolder = new File(SystemUtil.get("user.dir"), "conf");
    public final static File serverXmlFile = new File(confFolder, "server.xml");        // server config.xml
    public final static File webXmlFile = new File(confFolder, "web.xml");              // web.xml
    public final static File contextXMLFile = new File(confFolder, "context.xml");      // context.xml

    // Http Code
    public static final int CODE_200 = 200;
    public static final int CODE_302 = 302;
    public static final int CODE_404 = 404;
    public static final int CODE_500 = 500;

    // Http Request Method
    public static final String HttpGet = "GET";
    public static final String HttpPost = "POST";
}
