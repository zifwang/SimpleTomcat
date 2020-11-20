package SimpleTomcat.monitor;

import SimpleTomcat.catalina.Context;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.log.LogFactory;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 * ContextFileChangeMonitor is to monitor on the classes and jars change in the WEB-INF directory of webapp.
 */
public class ContextFileChangeMonitor {
    private WatchMonitor monitor;           // file change monitor
    private boolean stopFlag = false;       // stop flag

    /**
     * constructor
     * @param context context: web service info
     */
    public ContextFileChangeMonitor(Context context) {
        /**
         * the monitor is created by WatchUtil.createAll method
         *  context.getDocBase() is the directory the file monitor to monitor on.
         *  Integer.MAX_VALUE is the depth of monitor
         *  Watcher: when files change, watcher will execute corresponding action
         */
        this.monitor = WatchUtil.createAll(context.getDocBase(), Integer.MAX_VALUE, new Watcher() {
            @Override
            public void onCreate(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent);
            }

            @Override
            public void onModify(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent);
            }

            @Override
            public void onDelete(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent);
            }

            @Override
            public void onOverflow(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent);
            }

            private void dealWith(WatchEvent<?> watchEvent) {
                synchronized (ContextFileChangeMonitor.class) {
                    String fileName = watchEvent.context().toString();

                    if (stopFlag) {
                        return;
                    }

                    if (fileName.endsWith(".jar") || fileName.endsWith(".class") || fileName.endsWith(".xml")) {
                        stopFlag = true;
                        LogFactory.get().info(ContextFileChangeMonitor.this + " detects import file changes in Webapp {}", fileName);
                        context.reload();
                    }
                }
            }
        });
        this.monitor.setDaemon(true);
    }

    /**
     * Start ContextFileChangeMonitor
     */
    public void start() {
        this.monitor.start();
    }

    /**
     * Stop ContextFileChangeMonitor
     *  When ContextFileChangeMonitor find there are jars and classes change in the WEB-INF,
     *  the ContextFileChangeMonitor will be stopped and send signal to Host to reload context.
     */
    public void stop() {
        this.monitor.close();
    }
}
