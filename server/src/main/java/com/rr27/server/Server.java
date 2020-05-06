package com.rr27.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {

    //ClientHandler - по сути это сущность Клиента, ток как она выглядит со стороны Сервера
    private Vector<ClientHandler> clients;
    private AuthService authService;

    public AuthService getAuthService() {
        return authService;
    }

    public Server(){
        clients = new Vector<>();
        if (!UsersRepository.connect()) {
            throw new RuntimeException("Не удалось подключиться к БД!");
        }
        authService = new DBAuthService();
        try (ServerSocket serverSocket = new ServerSocket(8189)){
            System.out.println("Сервер запущен на порту 8189");
            while(true){
                Socket socket = serverSocket.accept();                                      //ждем клиентов, как подключился создалось соединение
                try {
                    new ClientHandler(this, socket);                                 //для работы с ним создаем новый Обработчик и подвязываем через сокет
                    System.out.println("Подключился новый клиент");
                }catch (IOException ex){
                    System.out.println("По какой-то причине клиент не смог подключиться");
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            UsersRepository.disconnect();
            System.out.println("Сервер завершил свою работу");
        }
    }

    public void broadcastMs(String msg){
        for (ClientHandler client: clients) {
            client.sendMsg(msg);
        }
    }

    //старая версия
//    public void privateMsgSend(String nickname, String msg){
//        for (ClientHandler client: clients) {
//            if (client.getNickname().equals(nickname)){
//                client.sendMsg(msg);
//            }
//        }
//    }

    public void privateMsg(ClientHandler sender, String nickname, String msg){
        if (sender.getNickname().equals(nickname)){
            sender.sendMsg("заметка для себе: " + msg);
            return;
        }
        for (ClientHandler client: clients) {
            if (client.getNickname().equals(nickname)){
                client.sendMsg("от " + sender.getNickname() + ": " + msg);
                sender.sendMsg("для " + nickname + ": " + msg);
                return;
            }
        }
        sender.sendMsg("Клиент: " + nickname + " не найден");
    }

    public void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
        broadcastClients();
    }

    public void unsubscribe(ClientHandler clientHandler){
        clients.remove(clientHandler);
        broadcastClients();
    }

    public boolean isNicknameBusy(String nickname){
        for (ClientHandler client: clients) {
            if (client.getNickname().equals(nickname)){
                return true;
            }
        }
        return false;
    }

    //вывод списка подключенных клиентов
    public void broadcastClients(){
        StringBuilder stringBuilder = new StringBuilder(15 * clients.size());
        stringBuilder.append("/clients ");
        for (ClientHandler c: clients) {
            stringBuilder.append(c.getNickname()).append(" ");
        }
        stringBuilder.setLength(stringBuilder.length() -1);                 //-1 для того чтобы убрать " " после последнего ника
        String out = stringBuilder.toString();
        for (ClientHandler c: clients) {
            c.sendMsg(out);
        }
    }
}
