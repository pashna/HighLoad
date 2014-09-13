import com.sun.org.apache.xml.internal.serializer.utils.SystemIDResolver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
* Created by popka on 10.09.14.
*/
public class SocketHandler implements Runnable {

    private Socket s;
    private InputStream is;
    private OutputStream os;
    private Map<String, String> requestMap = new HashMap<String, String>();

    SocketHandler(Socket s) throws Throwable {
        this.s = s;
        this.is = s.getInputStream();
        this.os = s.getOutputStream();
    }

    public void run() {
        System.err.println("Client accepted");
        try {
            readRequest();
            writeResponse();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            try {
                s.close();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        System.err.println("Client processing finished");
    }

    private void readRequest() throws Throwable {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String s="";
        String buffer;

        while(true) {
            buffer = br.readLine();
            s += buffer + "\r\n";
            if(buffer == null || buffer.trim().length() == 0) {
                break;
            }
        }
        System.out.println(s);
        parseRequestToMap(s);
    }

    private void writeResponse() throws Throwable {
        byte [] byteArray=null;
        if (isMethodSupported(requestMap.get("METHOD"))) {
            Path path = Paths.get(Config.dir, requestMap.get("FILE"));
            try {
                if ("GET".equals(requestMap.get("METHOD"))) byteArray = Files.readAllBytes(path); // Для GET - считываем данные,
                                                                                    // Для HEAD - останется null
                writeHeader(200, new File(Config.dir + requestMap.get("FILE")).length());

            } catch (IOException e) { // exception NoFile
                if (!path.toString().contains("index.html")) {
                    writeHeader(404, -1L);
                    byteArray = "PAGE NOT FOUND".getBytes();
                } else { // В папке отсутствует index.html
                    writeHeader(403, -1L);
                    byteArray = "Forbidden =(".getBytes();
                }
            }
        }
        else { // Unknown method
            writeHeader(405, -1L);
        }
        if (byteArray != null) os.write(byteArray); //Если что-то записали
        os.flush();
    }

    private void parseRequestToMap(String requestString) {
        String method = requestString.substring(0, requestString.indexOf(" "));
        if (isMethodSupported(method)) {
            requestMap.put("METHOD", method);
            String filePath = requestString.substring(method.length()+1, requestString.indexOf("\r\n")); // Первая строка запроса без метода
            filePath = filePath.substring(0, filePath.lastIndexOf(" ")); // Все остальное в этой строке до последнего пробела
            try {
                filePath = java.net.URLDecoder.decode(filePath, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (filePath.contains("../")) {
                try{
                    if (!new File(filePath).getCanonicalPath().contains(Config.dir)) {
                        filePath = "randomUnnownFile123412"; // Если выходим за пределы Config.dir - ставим заранее несуществующий файл
                    }
                } catch (Exception r) {
                }

            }
            if (filePath.contains("?")) filePath = filePath.substring(0, filePath.indexOf("?")); // Если есть строка параметров удаляем ее
            requestMap.put("FILE", filePath);
            if (filePath.endsWith("/")) requestMap.put("FILE", filePath+"index.html");
        }
        else {
            requestMap.put("METHOD", "UNNOWN"); // Пока без необходимости
        }
    }

    private String getContentType(String file) {
        String type = ContentTypeHelper.get(file.substring(file.lastIndexOf(".") + 1));
        if (type == null) type = ContentTypeHelper.get("");
        return "Content-Type: " + type;
    }

    private void writeHeader(int status, long length) {
        String response = "HTTP/1.1 " + HttpStatusHelper.get(status) +
                "\r\nDate: " + getTime() +
                "\r\nServer: "+Config.SERVER ;
        switch (status) {
        case 200:
            response += "\r\n" + getContentType(requestMap.get("FILE")) +
            "\r\nContent-Length: " + length +
            "\r\nConnection: close";
            break;
        case 403:
            break;
        case 404:
            break;
        case 405:
            response += "\r\nAllow: GET,HEAD";
            break;
        }
        response += "\r\n\r\n";
        System.out.println(response);
        try {
            os.write(response.getBytes());
        } catch (IOException e) {

        };
    }

    private static String getTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }

    private boolean isMethodSupported(String method) {
        return "GET".equals(method)||"HEAD".equals(method);
    }

}
