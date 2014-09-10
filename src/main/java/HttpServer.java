import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by popka 08.08.2014
 */
public class HttpServer {

    public static String getTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }

    public static void main(String[] args) throws Throwable {
        ServerSocket ss = new ServerSocket(Config.port);
        WorkQueue workQueue = new WorkQueue(Config.threadCount);
        while (true) {
            Socket s = ss.accept();
            System.err.println("Client accepted");
            workQueue.execute(new SocketHandler(s));
        }
    }

}