package SimpleTomcat.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolUtil {
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(20, 100,
            60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(10));

    public static void run(Runnable runnable) {
        threadPoolExecutor.execute(runnable);
    }
}
