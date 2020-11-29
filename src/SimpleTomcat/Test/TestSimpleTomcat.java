package SimpleTomcat.Test;

import SimpleTomcat.util.MiniBrowser;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestSimpleTomcat {
    private static int port = 8081;
    private static String ip = "127.0.0.1";

    @BeforeClass
    public static void beforeClass() {
        if (NetUtil.isUsableLocalPort(port)) {
            System.err.println("Unit Test Fail to Start. Please check whether " + port + " port has been used");
            System.exit(1);
        } else {
            System.out.println("Simple Tomcat is started. Unit Test start:");
        }
    }

    /**
     * Test root
     */
    @Test
    public void testHelloTomcat() {
        String html = getContentString("/");
        Assert.assertEquals(html,"Hello Simple Tomcat from ROOT's index.html");
    }

    /**
     * Test include html file
     */
    @Test
    public void testHtml() {
        String html = getContentString("/a.html");
        String actual = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Simple Tomcat</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>Hello Simple Tomcat from ROOT</h1>\n" +
                "</body>\n" +
                "</html>";
        Assert.assertEquals(html, actual);
    }

    /**
     * Test multiple threads functionality
     */
    @Test
    public void testTimeConsume() throws InterruptedException {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(20, 20, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(10));
        TimeInterval timeInterval = DateUtil.timer();

        for (int i = 0; i < 3; i++) {
            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    getContentString("/timeConsume.html");
                }
            });
        }

        threadPoolExecutor.shutdown();
        threadPoolExecutor.awaitTermination(1, TimeUnit.HOURS);

        long duration = timeInterval.intervalMs();

        Assert.assertTrue(duration < 3000);
    }

    /**
     * test context
     */
    @Test
    public void testaIndex() {
        String html = getContentString("/a");
        String actual = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Hello Simple Tomcat from a Directory</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>Hello Simple Tomcat from a Directory</h1>\n" +
                "</body>\n" +
                "</html>";
        Assert.assertEquals(html,actual);
    }

    /**
     * test parse server.xml
     */
    @Test
    public void testbIndex() {
        String html = getContentString("/b");
        Assert.assertEquals(html,"Hello Simple Tomcat from index.html@b");
    }

    @Test
    public void test404() {
        String response = getHttpString("/not_exist.html");
        containAssert(response, "HTTP/1.1 404 Not Found");
    }

    @Test
    public void test500() {
        String response  = getHttpString("/500.html");
        containAssert(response, "HTTP/1.1 500 Internal Server Error");
    }

    @Test
    public void testaTxt() {
        String response  = getHttpString("/a.txt");
        containAssert(response, "Content-Type: text/plain");
    }

    @Test
    public void testPNG() {
        byte[] bytes = getContentBytes("/logo.png");
        int pngFileLength = 1672;
        Assert.assertEquals(pngFileLength, bytes.length);
    }

    @Test
    public void testPDF() {
        byte[] bytes = getContentBytes("/etf.pdf");
        int pngFileLength = 3590775;
        Assert.assertEquals(pngFileLength, bytes.length);
    }

    @Test
    public void testhello() {
        String html = getContentString("/j2ee/hello");
        Assert.assertEquals(html,"Hello Simple Tomcat from HelloServlet");
    }

    @Test
    public void testJavawebHelloSingleton() {
        String html1 = getContentString("/javaweb/hello");
        String html2 = getContentString("/javaweb/hello");
        Assert.assertEquals(html1,html2);
    }

    @Test
    public void testgetParam() {
        String uri = "/javaweb/param";
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        Map<String,Object> params = new HashMap<>();
        params.put("name","meepo");
        String html = MiniBrowser.getContentString(url, params, true);
        Assert.assertEquals(html,"get name: meepo");
    }

    @Test
    public void testpostParam() {
        String uri = "/javaweb/param";
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        Map<String,Object> params = new HashMap<>();
        params.put("name","meepo");
        String html = MiniBrowser.getContentString(url, params, false);
        Assert.assertEquals(html,"post name: meepo");
    }

    @Test
    public void testHeader() {
        String html = getContentString("/javaweb/header");
        Assert.assertEquals(html, "my mini browser / java1.8");
    }

    private byte[] getContentBytes(String uri) {
        return getContentBytes(uri,false);
    }
    private byte[] getContentBytes(String uri,boolean gzip) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        return MiniBrowser.getContentBytes(url,false);
    }
    private String getContentString(String uri) {
        String url = StrUtil.format("http://{}:{}{}", this.ip, this.port, uri);
        String content = MiniBrowser.getContentString(url);
        return content;
    }
    private String getHttpString(String uri) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        String http = MiniBrowser.getHttpString(url);
        return http;
    }
    private void containAssert(String html, String string) {
        boolean match = StrUtil.containsAny(html, string);
        Assert.assertTrue(match);
    }
}
