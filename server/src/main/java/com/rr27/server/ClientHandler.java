package com.rr27.server;

import java.io.*;
import java.net.Socket;

//Класс обработчик сообщений от клиентов
//для каждого клиента он свой
public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;


    public String getNickname() {
        return nickname;
    }

    //получаем ссылку на клиента (его сокет) и ждем в отдельном потоке от него сообщение
    public ClientHandler(Server server, Socket socket)throws IOException{
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try{
                while (!checkAuth());
                while (readMessage());
            }catch (IOException ex){
                ex.printStackTrace();
            }finally {                                                                    //если клиент отваливается
                disconnect();
            }
        }).start();
    }

    private boolean checkAuth() throws IOException{
        String msg = in.readUTF();
        if (msg.startsWith("/auth")) {
            String[] tokens = msg.split("\\s");
            nickname = server.getAuthService().getNicknameByLoginAndPassword(tokens[1], tokens[2]);
            if (nickname != null && !server.isNicknameBusy(nickname)) {
                sendMsg("/authok " + nickname + " " + tokens[1]);
                server.subscribe(this);                 //подключаем на общую рассылку
                return true;
            }
        }
        return false;
    }

    /**
     * @return true - продолжаем слушать сокет на наличие новых сообщений, false - выходим из цикла
     * @throws IOException
     */
    private boolean readMessage() throws IOException{
        String msg = in.readUTF();
        if (msg.startsWith("/")){                                                               //особые команды
            if (msg.equals("/end")) {
                System.out.println("Клиент " + nickname + " отключился");
                sendMsg("/end");
                return false;
            }
            if (msg.startsWith("/w ")){
                String nick = msg.split("\\s")[1];
                String newMsg = msg.substring(nick.length() + 4);
                server.privateMsg(this, nick, newMsg);
            }
            if (msg.startsWith("/change_nickname")){
                String newNick = msg.split("\\s")[1];                                   //оставили ток новое имя
                if (server.getAuthService().changeNickname(this.nickname, newNick)){
                    sendMsg("Ваш никнейм успешно изменен на " + newNick);
                    nickname = newNick;
                    server.broadcastClients();
                }
                else {
                    sendMsg("Такой никнейм скорее всего существует");
                }
            }
        }
        else{
            server.broadcastMs(nickname + ": " + msg);                     //идет рассылка всем клиентам
        }
        return true;
    }

    private void disconnect() {
        server.unsubscribe(this);                             //удаляем клиента из списка рассылок
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg){
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
