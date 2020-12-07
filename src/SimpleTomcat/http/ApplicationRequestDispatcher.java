package SimpleTomcat.http;

import SimpleTomcat.catalina.HttpProcessor;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * ApplicationRequestDispatcher implements RequestDispatcher to provide http redirect in the server side.
 *      Steps:
 *          1. modify request's uri
 *          2. HttpProcessor execute again
 *      This can be view as visiting a client's web site in server
 */
public class ApplicationRequestDispatcher implements RequestDispatcher {
    private String uri;

    public ApplicationRequestDispatcher(String uri) {
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        this.uri = uri;
    }

    @Override
    public void forward(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        Request request = (Request) servletRequest;
        Response response = (Response) servletResponse;

        request.setUri(uri);

        HttpProcessor httpProcessor = new HttpProcessor();
        response.resetBuffer();
        httpProcessor.execute(request.getSocket(), request, response);

        request.setForwarded(true);
    }

    @Override
    public void include(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {

    }
}
