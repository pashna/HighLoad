import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by popka 08.09.2014
 */
public class HttpServer {

    public static String getTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }

    public static void main(String[] args) throws Throwable {
        ServerSocket ss = new ServerSocket(8001);
        ContentTypeHelper contentTypeHelper = new ContentTypeHelper();
        while (true) {
            Socket s = ss.accept();
            System.err.println("Client accepted");
            new Thread(new SocketProcessor(s)).start();
        }
    }

    private static class SocketProcessor implements Runnable {

        private Socket s;
        private InputStream is;
        private OutputStream os;

        private SocketProcessor(Socket s) throws Throwable {
            this.s = s;
            this.is = s.getInputStream();
            this.os = s.getOutputStream();
        }

        public void run() {
            try {
                Map requestMap = readInputHeaders();
                writeResponse(requestMap);
            } catch (Throwable t) {
                /*do nothing*/
            } finally {
                try {
                    s.close();
                } catch (Throwable t) {
                    t.printStackTrace();
                    /*do nothing*/
                }
            }
            System.err.println("Client processing finished");
        }

        private void writeResponse(Map<String, String> requestMap) throws Throwable {
            String response;
            String fileText = "";
            if (requestMap.get("METHOD").equals("GET")) {
                Path path = Paths.get(Config.dir, requestMap.get("FILE"));
                try {
                    byte[] fileArray = Files.readAllBytes(path);
                    fileText = new String(fileArray, "UTF-8");
                    response = "HTTP/1.1 200 OK\r\n" +
                            "Date: " + getTime() + "\r\n" +
                            "Server: myBeautyServer v.1.0\r\n";
                    response += getContentType(requestMap.get("FILE"));
                    response+="Content-Length: " + fileText.length() + "\r\n" +
                            "Connection: close\r\n\r\n";
                } catch (IOException e) {
                    response = "HTTP/1.1 404 NOT FOUND\r\n\r\nNOT FOUND PAGE";
                }
            }
            else {
                response = "405 Method Not Allowed\r\n\r\nAllow: GET\r\n";
            }
            String result = response + fileText;
            System.out.println(response);
            os.write(result.getBytes());
            os.flush();
        }

        private Map<String, String> readInputHeaders() throws Throwable {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String s="";
            String buffer;

            while(true) {
                buffer = br.readLine();
                s += buffer;
                if(buffer == null || buffer.trim().length() == 0) {
                    break;
                }
            }
            System.out.println(s);
            return parseRequestToMap(s);
        }

        private Map<String, String> parseRequestToMap(String requestString) {
            Map requestMap = new HashMap<String, String>();
            if (requestString.substring(0,3).equals("GET")) {
                requestMap.put("METHOD", "GET");
                String filePath = requestString.substring(4, requestString.indexOf(" ", 4));
                try {
                    filePath = java.net.URLDecoder.decode(filePath, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                requestMap.put("FILE", filePath);
                if (requestMap.get("FILE").equals("/")) requestMap.put("FILE", "index.html");

            }
            else {
                requestMap.put("METHOD", "UNNOWN");
            }
            return requestMap;
        }

        private String getContentType(String file) {
            String type = ContentTypeHelper.getContentType(file.substring(file.lastIndexOf(".") + 1));
            if (type == null) type = ContentTypeHelper.getContentType("");
            return "Content-Type: " + type + "\r\n";
        }
    }

}