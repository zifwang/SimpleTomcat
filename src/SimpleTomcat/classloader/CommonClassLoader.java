package SimpleTomcat.classloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * CommonClassLoader Object is used to scan lib directory to get all jar files and add these files to URL[] array.
 * The reason to have the CommonClassLoader Object is that when use .bat file to start the program, the libs will not be loaded.
 * The CommonClassLoader would help to include all external jar files.
 * When other object uses loadClass Method from CommonClassLoader, jar files will be found from the URL[] array.
 */
public class CommonClassLoader extends URLClassLoader {

    public CommonClassLoader() {
        super(new URL[] {});

        try {
            File workingDirectory = new File(System.getProperty("user.dir"));
            File libDirectory = new File(workingDirectory, "lib");
            File[] jarFiles = libDirectory.listFiles();

            if (jarFiles == null || jarFiles.length <= 0 ) {
                throw new NullPointerException();
            }

            for (File file : jarFiles) {
                if (file.getName().endsWith("jar")) {
                    URL url = new URL("file:" + file.getAbsolutePath());
                    this.addURL(url);
                }
            }
        } catch (NullPointerException | MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
