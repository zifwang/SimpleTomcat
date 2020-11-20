package SimpleTomcat.http;

import SimpleTomcat.catalina.Context;

import java.io.File;
import java.util.*;

public class ApplicationContext extends BaseServletContext {
    private Map<String, Object> attributesMap;  // attributesMap used to store data
    private Context context;                    // Context

    public ApplicationContext(Context context) {
        this.attributesMap = new HashMap<>();
        this.context = context;
    }

    @Override
    public void removeAttribute(String name) {
        attributesMap.remove(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributesMap.put(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return attributesMap.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        Set<String> keys = attributesMap.keySet();
        return Collections.enumeration(keys);
    }

    /**
     * Get Real Path in the file system
     * @param path
     * @return
     */
    @Override
    public String getRealPath(String path) {
        return new File(context.getDocBase(), path).getAbsolutePath();
    }
}
