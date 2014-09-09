import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        private Map<String, String> requestMap;

        private SocketProcessor(Socket s) throws Throwable {
            this.s = s;
            this.is = s.getInputStream();
            this.os = s.getOutputStream();
        }

        public void run() {
            try {
                readInputHeaders();
                writeResponse();
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

        private void writeResponse() throws Throwable {
            String response = "";
            String fileText = "";
            byte [] byteArray = new byte[Config.byteLength];
            if (requestMap.get("METHOD").equals("GET")) {
                Path path = Paths.get(Config.dir, requestMap.get("FILE"));
                try {
                    if (getContentType(requestMap.get("FILE")).contains("text")) {
                        byteArray = Files.readAllBytes(path);
                        //fileText = new String(fileArray, "UTF-8");
                        /*response = "HTTP/1.1 200 OK\r\n" +
                                "Date: " + getTime() + "\r\n" +
                                "Server: myBeautyServer v.1.0\r\n";
                        response += getContentType(requestMap.get("FILE"));
                        response+="Content-Length: " + (new File(path.toString()).length()) + "\r\n" +
                                "Connection: close\r\n\r\n";
                        //response += fileText;*/

                    } else {
                        if (!(new File(path.toString()).isFile())) {throw new IOException();}
                        //BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path.toString()));
                        //while ((bis.read(byteArray)) != -1){
                        //}
                        //bis.close();
                        byteArray = Files.readAllBytes(path);
                        response = "HTTP/1.1 200 OK\r\n" +
                                "Date: " + getTime() + "\r\n" +
                                "Server: myBeautyServer v.1.0\r\n";
                        response += getContentType(requestMap.get("FILE"));
                        response+="Content-Length: " + (new File(path.toString()).length()) + "\r\n" +
                                "Connection: close\r\n\r\n";
                    }
                } catch (IOException e) {
                    response = "HTTP/1.1 404 NOT FOUND\r\n\r\nNOT FOUND PAGE";
                }
            }
            else {
                response = "405 Method Not Allowed\r\n\r\nAllow: GET\r\n";
            }
            System.out.println(response);
            os.write(response.getBytes());
            os.write(byteArray);
            os.flush();
        }

        private void readInputHeaders() throws Throwable {
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
            parseRequestToMap(s);
        }


        private void parseRequestToMap(String requestString) {
            if (requestString.substring(0,3).equals("GET")) {
                requestMap.put("METHOD", "GET");
                String filePath = requestString.substring(4, requestString.indexOf(" ", 4));
                try {
                    filePath = java.net.URLDecoder.decode(filePath, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                requestMap.put("FILE", filePath);
                if (filePath.endsWith("/")) requestMap.put("FILE", filePath+"index.html");

            }
            else {
                requestMap.put("METHOD", "UNNOWN");
            }
        }

        private String getContentType(String file) {
            String type = ContentTypeHelper.get(file.substring(file.lastIndexOf(".") + 1));
            if (type == null) type = ContentTypeHelper.get("");
            return "Content-Type: " + type + "\r\n";
        }

        private void writeHeader(int status, String content, int length) {
            String response = "HTTP/1.1 " + HttpStatusHelper.get(status) + "\r\n";
            if (status == 200) {
            response += "HTTP/1.1 " + HttpStatusHelper.get(status) + "\r\n" +
                    "Date: " + getTime() + "\r\n" +
                    "Server: myBeautyServer v.1.0\r\n";
            response += getContentType(requestMap.get("FILE"));
            response +="Content-Length: " + length
                    + "\r\nConnection: close\r\n\r\n";
            } else if (status == 404) {

            } else {// Статус 403

            }
        }


    }

}