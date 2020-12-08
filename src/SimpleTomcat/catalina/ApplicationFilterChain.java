package SimpleTomcat.catalina;

import cn.hutool.core.util.ArrayUtil;

import javax.servlet.*;
import java.io.IOException;
import java.util.List;

public class ApplicationFilterChain implements FilterChain {
    private Filter[] filters;       // filters
    private Servlet servlet;        // servlet after filters
    int pos;

    public ApplicationFilterChain(List<Filter> filterList, Servlet servlet) {
        this.filters = ArrayUtil.toArray(filterList, Filter.class);
        this.servlet = servlet;
        pos = 0;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
        if (pos < filters.length) {
            Filter filter = filters[pos++];
            filter.doFilter(servletRequest, servletResponse, this);
        } else {
            servlet.service(servletRequest, servletResponse);
        }
    }
}
