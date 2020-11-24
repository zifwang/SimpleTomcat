package SimpleTomcat.exception;

/**
 * WebConfigException is the exception that happens during the loading of web.xml from webapp
 */
public class WebConfigException extends Exception {
    /**
     * WebConfig exception
     *  Some common exceptions:
     *      name: duplication or not found
     *      class: duplication or not found
     *      url: duplication or not found
     * @param msg: error msg
     */
    public WebConfigException(String msg) {
        super(msg);
    }
}
