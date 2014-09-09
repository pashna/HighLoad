import java.util.HashMap;
import java.util.Map;

/**
 * Created by popka on 09.09.14.
 */
public class HttpStatusHelper {
    private static final Map<Integer, String> httpStatusMap;

    static  {
        httpStatusMap = new HashMap<Integer, String>();
        httpStatusMap.put(200, "200 OK");
        httpStatusMap.put(404, "404 Not Found");
        httpStatusMap.put(405, "405 Method Not Allowed");
    }

    public static String get(Integer s) {
        return httpStatusMap.get(s);
    }
}
