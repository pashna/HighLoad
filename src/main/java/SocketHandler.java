import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

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

    private void writeResponse() throws Throwable {
        byte [] byteArray=null;
        if (requestMap.get("METHOD").equals("GET")) {
            Path path = Paths.get(Config.dir, requestMap.get("FILE"));
            //System.out.println(new File(path.toString()).exists());
            try {
                byteArray = Files.readAllBytes(path);
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
        System.out.println(os);
        os.flush();
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


    private void parseRequestToMap(String requestString) {
        if (requestString.substring(0,3).equals("GET")) {
            requestMap.put("METHOD", "GET");
            String filePath = requestString.substring(4, requestString.indexOf("\r\n", 4)); // Первая строка запроса без Get
            filePath = filePath.substring(0, filePath.lastIndexOf(" ")); // Все остальное в этой строке до последнего пробела
            try {
                filePath = java.net.URLDecoder.decode(filePath, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (filePath.contains("../")) filePath = "randomUnnownFile123412"; // Если идет вверх по дереву - ставим заранее неизвестный файл
            if (filePath.contains("?")) filePath = filePath.substring(0, filePath.indexOf("?")); // Если есть строка подзапроса удаляем ее
            if (filePath.contains("?")) filePath = filePath.substring(0, filePath.indexOf("?")); // С пробелом
            requestMap.put("FILE", filePath);
            if (filePath.endsWith("/")) requestMap.put("FILE", filePath+"index.html");
            System.out.println(filePath);
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
                "\r\nDate: " + HttpServer.getTime() +
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
            response += "\r\nAllow: GET";
            break;
        }
        response += "\r\n\r\n";
        System.out.println(response);
        try {
            os.write(response.getBytes());
        } catch (IOException e) {

        };
    }


}
