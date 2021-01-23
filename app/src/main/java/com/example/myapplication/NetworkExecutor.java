package com.example.myapplication;

import android.os.Message;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class NetworkExecutor extends Thread {

    final public int CODE_OK = 200;
    final public int CODE_BADREQUEST = 400;
    final public int CODE_FORBIDDEN = 403;
    final public int CODE_NOTFOUND = 404;
    final public int CODE_INTERNALSERVERERROR = 500;
    final public int CODE_NOTIMPLEMENTED = 501;

    private static final int HTTP_SERVER_PORT = 8080;

    public void run() {
        Socket scliente;
        ServerSocket unSocket = null;
        try {

            unSocket = new ServerSocket(HTTP_SERVER_PORT); //Creamos el puerto
            Log.d("Socket", String.valueOf(HTTP_SERVER_PORT));
        } catch (IOException e) {
            e.printStackTrace();
            try {
                unSocket.close();
                unSocket=new ServerSocket(HTTP_SERVER_PORT);
            } catch (IOException ex) {
                ex.printStackTrace();
            }


        }
        while (true) {
            try {
                scliente = unSocket.accept(); //Aceptando conexiones del navegador Web
                Log.d("SCLIENTE", "Conexión aceptada por el cliente.");

                System.setProperty("line.separator", "\r\n");

                //Creamos los objetos para leer y escribir en el socket
                BufferedReader in = new BufferedReader(new InputStreamReader(scliente.getInputStream()));
                PrintStream out=new PrintStream(new BufferedOutputStream(scliente.getOutputStream()));

                //Leemos el comando que ha sido enviado por el servidor web
                // Ejemplo de comando: GET /index.html HTTP\1.0
                String cadena = in.readLine();
                StringTokenizer st = new StringTokenizer(cadena);
                String commandString = st.nextToken().toUpperCase();

                if (commandString.equals("GET")) {
                    String urlObjectString = st.nextToken();
                    Log.v("urlObjectString", urlObjectString);

                    String fileStr = MainActivity.indexhtml;

                    if (urlObjectString.toUpperCase().startsWith("/INDEX.HTML") ||
                            urlObjectString.toUpperCase().equals("/INDEX.HTM") ||
                            urlObjectString.equals("/")) {

                        String headerStr = getHTTP_Header(CODE_OK, "text/html", fileStr.length());
                        out.print(headerStr);
                        out.println(fileStr);
                        out.flush();
                    }

                    if (urlObjectString.toUpperCase().startsWith("/FORWARD")) {
                        MainActivity.showDisplayMessage("FORWARD");
                        String headerStr = getHTTP_Header(CODE_OK,
                                "text/html",
                                fileStr.length());

                        int cont=MainActivity.getCont();
                        System.out.println("Contador: "+cont);

                       // byte[] buffer = new byte[4096];
                       // out.write(buffer, 0, cont);
                        //out.write(cont);



                        out.print(headerStr);
                        out.println(fileStr);
                        out.flush();
                    }

                    if (urlObjectString.toUpperCase().startsWith("/CONTADOR")) {
                        MainActivity.showDisplayMessage("CONTADOR");
                        String headerStr = getHTTP_Header(CODE_OK,
                                "CONTADOR",
                                fileStr.length());

                        int cont=MainActivity.getCont();
                        out.write(cont);
                        out.flush();
                        out.close();
                      /*  File cont=MainActivity.getContador();
                        out.print(headerStr);
                        out.println(fileStr);
                        out.flush();
                        MainActivity.showDisplayMessage("CONTADOR");
                        File cameraFile = MainActivity.getOutputMediaFile();
                        FileInputStream fis = null;
                        boolean exist = true;
                        try {
                            fis = new FileInputStream(cameraFile);
                        }catch (FileNotFoundException e) {
                            exist = false;
                        }

                        if (exist) {
                           // String headerStr = getHTTP_Header(CODE_OK,
                                  //  "CONTADOR",
                                  //  (int)cameraFile.length());
                            out.print(headerStr);

                            byte[] buffer = new byte[4096];
                            int n;
                            while ( (n = fis.read(buffer)) > 0 ) { // enviar archivo
                                out.write(buffer, 0, n);
                            }
                            out.flush();out.close();
                        }*/
                    }










                   /* if (urlObjectString.toUpperCase().startsWith("/CAMERA.JPG") ||
                            urlObjectString.toUpperCase().startsWith("/CAMERA.")) {

                        MainActivity.showDisplayMessage("CAMERA");
                        File cameraFile = MainActivity.getOutputMediaFile();
                        FileInputStream fis = null;
                        boolean exist = true;
                        try {
                            fis = new FileInputStream(cameraFile);
                        }catch (FileNotFoundException e) {
                            exist = false;
                        }

                        if (exist) {
                            String headerStr = getHTTP_Header(CODE_OK,
                                    "image/jpeg",
                                    (int)cameraFile.length());
                            out.print(headerStr);

                            byte[] buffer = new byte[4096];
                            int n;
                            while ( (n = fis.read(buffer)) > 0 ) { // enviar archivo
                                out.write(buffer, 0, n);
                            }
                            out.flush();out.close();
                        }
                    }*/
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    private String getHTTP_Header(int headerStatusCode,
                                  String headerContentType,
                                  int headerFileLength) {
        String result = getHTTP_HeaderStatus(headerStatusCode) +
                "\r\n" +
                getHTTP_HeaderContentLength(headerFileLength)+
                getHTTP_HeaderContentType(headerContentType)+
                "\r\n";

        return result;
    }



    private String getHTTP_HeaderStatus(int headerStatusCode){
        String result = "";
        switch (headerStatusCode) {
            case CODE_OK:
                result = "200 OK"; break;
            case CODE_BADREQUEST:
                result = "400 Bad Request"; break;
            case CODE_FORBIDDEN:
                result = "403 Forbidden"; break;
            case CODE_NOTFOUND:
                result = "404 Not Found"; break;
            case CODE_INTERNALSERVERERROR:
                result = "500 Internal Server Error"; break;
            case CODE_NOTIMPLEMENTED:
                result = "501 Not Implemented"; break;
        }
        return ("HTTP/1.0 "+result);
    }

    private String getHTTP_HeaderContentLength(int headerFileLength){
        return "Content-Length: " + headerFileLength + "\r\n";
    }

    private String getHTTP_HeaderContentType(String headerContentType){
        return "Content-Type: "+headerContentType+"\r\n";
    }






}
