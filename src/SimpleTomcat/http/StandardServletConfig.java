package SimpleTomcat.http;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * When Servlet is in the init. process, ServletConfig Object will pass configurations to the Servlet.
 * StandardServletConfig implements ServletConfig and provides corresponding methods.
 */
public class StandardServletConfig implements ServletConfig {
    private ServletContext servletContext;          // servlet context
    private Map<String, String> initParameters;     // initialized parameters
    private String servletName;                     // servlet name

    /**
     * Constructor
     * @param servletContext
     * @param servletName
     * @param initParameters
     */
    public StandardServletConfig(ServletContext servletContext, String servletName, Map<String, String> initParameters) {
        this.servletContext = servletContext;
        this.servletName = servletName;
        this.initParameters = initParameters;
        if (this.initParameters == null)
            this.initParameters = new HashMap<>();
    }

    @Override
    public String getServletName() {
        return servletName;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getInitParameter(String s) {
        return initParameters.get(s);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }
}
