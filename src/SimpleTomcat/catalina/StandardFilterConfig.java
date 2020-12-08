package SimpleTomcat.catalina;

import SimpleTomcat.http.StandardSession;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.*;

/**
 * StandardFilterConfig implements FilterConfig which is used to provide service of storing Filter's init params.
 */
public class StandardFilterConfig implements FilterConfig {
    private ServletContext servletContext;          // servlet context
    private Map<String, String> initParams;         // filter init. params
    private String filterName;                      // filter name

    public StandardFilterConfig(ServletContext servletContext, Map<String, String> initParams, String filterName ) {
        this.servletContext = servletContext;
        this.initParams = initParams;
        if (this.initParams == null) {
            this.initParams = new HashMap<>();
        }
        this.filterName = filterName;
    }

    @Override
    public String getFilterName() {
        return filterName;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getInitParameter(String s) {
        return initParams.get(s);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParams.keySet());
    }
}
