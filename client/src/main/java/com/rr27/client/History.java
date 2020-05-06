package com.rr27.client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class History {

    private static PrintWriter out;

    private static String getFileName(String login){
        return "client/history/history_" + login + ".txt";
    }

    public static void start(String login){
        try {
            out = new PrintWriter(new FileOutputStream(getFileName(login), true), true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void stop(){
        if (out != null) {
            out.close();
        }
    }

    public static void writeHistory(String msg){
        out.println(msg);
    }

    public static String get100LastMSg(String login){
        if (!Files.exists(Paths.get(getFileName(login)))){                        //проверка на наличие файла (возможно это наше первый вход и мы ничего не писали)
            return "";
        }
        StringBuilder last100msg = new StringBuilder();
        try {
            List<String> allLines = Files.readAllLines(Paths.get(getFileName(login)));
            int startPosition = 0;
            if (allLines.size() > 100){
                startPosition = allLines.size()-100;
            }
            for (int i = startPosition; i < allLines.size(); i++) {
                last100msg.append(allLines.get(i)).append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return last100msg.toString();
    }

//    public String getHistory(){
//        StringBuilder history = new StringBuilder(100);
//        try {
//            int x;
//            while((x=in.read())!=-1){
//                history.append((char) x);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return history.toString();
//    }
}
