package SimpleTomcat.exception;

/**
 * WebConfigException is the exception that happens when load web.xml fail
 */
public class WebConfigException extends Exception {
    /**
     * WebConfig exception
     *  Some common exceptions:
     *      name duplication, not found
     *      class duplication, not found
     *      url duplication, not found
     * @param msg: error msg
     */
    public WebConfigException(String msg) {
        super(msg);
    }
}
