import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by popka 08.08.2014
 */
public class HttpServer {

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