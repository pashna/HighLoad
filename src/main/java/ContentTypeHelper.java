import java.util.HashMap;
import java.util.Map;

/**
 * Created by popka on 08.09.14.
 */
public class ContentTypeHelper {
    private static final Map<String, String> contentMap;

    static  {
        contentMap = new HashMap<String, String>();
        contentMap.put("", "content/unknown");
        contentMap.put("gif", "image/gif");
        contentMap.put("jpg", "image/jpeg");
        contentMap.put("jpeg", "image/jpeg");
        contentMap.put("png", "image/png");
        contentMap.put("html", "text/html");
        contentMap.put("css", "text/css");
        contentMap.put("js", "text/javascript");
        contentMap.put("swf", "application/x-shockwave-flash");
    }

    public static String get(String s) {
        return contentMap.get(s);
    }

}
