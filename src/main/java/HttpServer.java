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
        private Map<String, String> requestMap = new HashMap<String, String>();

        private SocketProcessor(Socket s) throws Throwable {
            this.s = s;
            this.is = s.getInputStream();
            this.os = s.getOutputStream();
        }

        public void run() {
            try {
                readRequest();
                writeResponse();
            } catch (Throwable t) {

            } finally {
                try {
                    s.close();
                } catch (Throwable t) {
                    t.printStackTrace();

                }
            }
            System.err.println("Client processing finished");
        }

        private void writeResponse() throws Throwable {
            byte [] byteArray=null;
            if (requestMap.get("METHOD").equals("GET")) {
                Path path = Paths.get(Config.dir, requestMap.get("FILE"));
                try {
                    byteArray = Files.readAllBytes(path);
                    writeHeader(200, new File(Config.dir + requestMap.get("FILE")).length());

                } catch (IOException e) { // exception NoFile
                    writeHeader(404, -1L);
                    byteArray = "PAGE NOT FOUND".getBytes();
                }
            }
            else {
                writeHeader(405, -1L);
            }
            os.write(byteArray);
            os.flush();
        }

        private void readRequest() throws Throwable {
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
                String filePath = requestString.substring(4, requestString.indexOf(" ", 4)); // Подстрока, начиная с пробела после Get
                                                                                            // И до второго пробела
                try {
                    filePath = java.net.URLDecoder.decode(filePath, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (filePath.contains("../")) filePath = "randomUnnownFile123412"; // Если идет вверх по дереву - ставим заранее неизвестный файл
                if (filePath.contains("?")) filePath = filePath.substring(0, filePath.indexOf("?")); // Если есть строка подзапроса удаляем ее
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
            return "Content-Type: " + type;
        }

        private void writeHeader(int status, long length) {
            String response = "HTTP/1.1 " + HttpStatusHelper.get(status);
            if (status == 200) {
                response += "\r\nDate: " + getTime() +
                        "\r\nServer: myBeautyServer v.1.0" +
                        "\r\n" + getContentType(requestMap.get("FILE")) +
                        "\r\nContent-Length: " + length +
                        "\r\nConnection: close";

            } else if (status == 404) {

            } else {

            }
            response += "\r\n\r\n";
            System.out.println(response);
            try {
                os.write(response.getBytes());
            } catch (IOException e) {

            };
        }


    }

}