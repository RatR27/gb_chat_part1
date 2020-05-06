package com.rr27.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Класс отвечащий за сетевую логику Клиента
 * - подключение к серверу
 * - аутентификация
 * - отправка и чтение сообщений
 * - через Callback прокидывает данные на фронтовую часть Controller
 */
public class Network {
    private static Socket socket;
    private static DataInputStream in;
    private static DataOutputStream out;

    //полноценные объекты с одним единственным методом, который переопределен для каждого в классе Controller
    //теперь вызывая их Network будет взаимодействовать с фронтом уже
    private static Callback callOnsMsgReceived;
    private static Callback callOnAuthenticated;
    private static Callback callOnException;
    private static Callback callOnCloseConnection;

    public static void setCallOnsMsgReceived(Callback callOnsMsgReceived) {
        Network.callOnsMsgReceived = callOnsMsgReceived;
    }

    public static void setCallOnAuthenticated(Callback callOnAuthenticated) {
        Network.callOnAuthenticated = callOnAuthenticated;
    }

    public static void setCallOnException(Callback callOnException) {
        Network.callOnException = callOnException;
    }

    public static void setCallOnCloseConnection(Callback callOnCloseConnection) {
        Network.callOnCloseConnection = callOnCloseConnection;
    }

    public static void connect(){
            try{
                socket = new Socket("localhost", 8189);
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                Thread t = new Thread(() -> {                                   //отдельный поток, который не будет блокировать основной и ждать сообщений от сервера
                    try {                                                       //без отдельного потока мы зависли бы на этой прослушке не увидев интерфейс
                        while(true){                                            //цикл авторизации, пока не будет правильной пары дальше не пройдем
                            String msg = in.readUTF();
                            if (msg.contains("/authok")){
                                callOnAuthenticated.callback(msg.split("\\s")[1], msg.split("\\s")[2]);          //fixme тут 1же не один параметр
                                break;
                            }
                        }
                        while(true) {
                            String msg = in.readUTF();
                            if (msg.equals("/end")) {                          //теперь никаких исключений в консоли, когда отключаемся
//                                closeConnection();
                                break;
                            }
                            callOnsMsgReceived.callback(msg);
                        }
                    } catch (IOException e) {
                        //когда придет именно обрыв связи - сервер пуал, будет аккуратное окно с предупреждением
                        callOnException.callback("Соединиение с сервером разорвано");
                    } finally {                                   //именно в потоке, который отвечает за прослушку новых сообщений
                        closeConnection();                        //мы добавляем закрытие потоков вводы и вывода, если соединение упало
                    }
                });
                t.setDaemon(true);                                //JavaFX работает в своем потоке, если окошко закроем, то без ДемонРежима поток на прослушку так и будет висеть
                t.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Здесь finally не пишем! Иначе, мы открыли сокет, потоки данных и тут же все закроем (закрытие идет выше)
    }

    public static boolean sendMsg(String msg) {
        try {
            out.writeUTF(msg);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void sendAuth(String login, String password) {
        try{
            if (socket == null || socket.isClosed()){
                connect();
            }
            out.writeUTF("/auth " + login + " " + password);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void closeConnection(){
        callOnCloseConnection.callback();
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

}
