package SimpleTomcat.util;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * MiniBrowser Object is created for better understanding the SimpleTomcat.http.http protocol
 */
public class MiniBrowser {
    /**
     * get SimpleTomcat.http.http response's content in byte type
     * @param url: url
     * @return
     */
    public static byte[] getContentBytes(String url) {
        return getContentBytes(url, false);
    }

    /**
     * get SimpleTomcat.http.http response's content in byte type with data been zipped flag
     * @param url: url
     * @param is_gzip: data is zipped ?
     * @return
     */
    public static byte[] getContentBytes(String url, boolean is_gzip) {
        byte[] response = getHttpBytes(url, is_gzip);
        byte[] doubleReturn = "\r\n\r\n".getBytes();

        int pos = -1;
        for (int i = 0; i < response.length - doubleReturn.length; i++) {
            byte[] temp = Arrays.copyOfRange(response, i,i + doubleReturn.length);
            if (Arrays.equals(temp, doubleReturn)) {
                pos = i;
                break;
            }
        }

        if (pos == -1) return null;

        pos += doubleReturn.length;

        return Arrays.copyOfRange(response, pos, response.length);
    }

    /**
     * get SimpleTomcat.http.http response's content in string type
     * @param url: url
     * @return
     */
    public static String getContentString(String url) {
        return getContentString(url, false);
    }

    /**
     * get SimpleTomcat.http.http response's content in string type with data been zipped flag
     * @param url: url
     * @param is_gzip: data is zipped ?
     * @return
     */
    public static String getContentString(String url, boolean is_gzip) {
        byte[] content = getContentBytes(url, is_gzip);
        if (content == null) return null;

        try {
            return new String(content, "utf-8").trim();
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    /**
     * get SimpleTomcat.http.http response in string type
     * @param url: url
     * @return
     */
    public static String getHttpString(String url) {
        return getHttpString(url, false);
    }

    /**
     * get SimpleTomcat.http.http response in string type with data been zipped flag
     * @param url: url
     * @param is_gzip: data is zipped ?
     * @return
     */
    public static String getHttpString(String url, boolean is_gzip) {
        return new String(getHttpBytes(url, is_gzip)).trim();
    }

    /**
     * get SimpleTomcat.http response in byte type with data been zipped flag
     * @param url
     * @return
     */
    public static byte[] getHttpBytes(String url, boolean is_gzip) {
        byte[] httpResponse = null;

        try {
            URL u = new URL(url);           // create url by given url address
            Socket client = new Socket();   // define socket client
            int port = u.getPort();         // define port number of connection

            if (port == -1) port = 80;      // if port is not set, set port number to 80
            // Create a socket address from a hostname and a port number.
            InetSocketAddress inetSocketAddress = new InetSocketAddress(u.getHost(), port);
            // Create sock connection with socket address and timeout
            client.connect(inetSocketAddress, 1000);
            // Create SimpleTomcat.http.http request header in HashMap type. key - header name, value - value of header name
            // e.g. Host: 127.0.0.1, Accept: text/html, Connection: close, User-Agent: chrome
            Map<String, String> requestHeaders = new HashMap<String, String>();
            requestHeaders.put("Host", u.getHost() + ":" + port);
            requestHeaders.put("Accept", "text/html");
            requestHeaders.put("Connection", "close");
            requestHeaders.put("User-Agent", "my mini browser / java1.8");
            // whether gzip data is used
            if (is_gzip) requestHeaders.put("Accept-Encoding", "gzip");

            // Define path
            String path = u.getPath();
            if (path.length() == 0) path = "/";

            // Create Http Response String
            StringBuffer httpRequestString = new StringBuffer();
            httpRequestString.append("GET " + path + " HTTP/1.1\r\n");
            Set<String> headers = requestHeaders.keySet();
            for (String header : headers) {
                String headerLine = header + ":" + requestHeaders.get(header)+ "\r\n";
                httpRequestString.append(headerLine);
            }

            PrintWriter printWriter = new PrintWriter(client.getOutputStream(), true);
            printWriter.println(httpRequestString);
            InputStream is = client.getInputStream();
            httpResponse = readBytes(is, true);
            client.close();

        } catch (Exception e) {
            e.printStackTrace();
            try {
                httpResponse = e.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException uee) {
                uee.printStackTrace();
            }
        }

        return httpResponse;
    }

    /**
     * Read data getting from socket
     * @param is: input stream
     * @param fullFlag: the flag indicate whether fully read file
     * 当 fullFlag 等于 true 的时候，即便读取到的数据没有 buffer_size 那么长，也会继续读取。
     * @return
     * @throws IOException
     */
    public static byte[] readBytes(InputStream is, boolean fullFlag) throws IOException {
        int buffer_size = 1024;
        byte buffer[] = new byte[buffer_size];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (true) {
            int readSize = is.read(buffer);
            if (readSize == -1) break;
            baos.write(buffer, 0, readSize);
            if(!fullFlag && readSize > 0 && readSize < buffer_size) break;
        }
        return baos.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        String url = "https://www.baidu.com/";
        String contentString = getContentString(url);
        System.out.println(contentString);
        String httpString = getHttpString(url);
        System.out.println(httpString);
    }
}
