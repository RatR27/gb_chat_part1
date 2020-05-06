package com.rr27.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Класс отвечающий только за графический интерфейс Клиента
 * - всю логику мы пробрасываем отсюда в Network
 * - чтобы их связать, но без жесктой привязки, мы используем interface Callback
 */
public class Controller implements Initializable {

    @FXML
    TextArea textArea;
    @FXML
    TextField msgField;
    @FXML
    HBox msgPanel, authPanel;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passField;
    @FXML
    ListView<String> clientsList;

    private boolean authenticated;
    private String nickname;
    private String login;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthenticated(false);
        //добавление возможности по двойному клику по никнейму в списке отправлять личное сообщение
        clientsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2){
                String nick = clientsList.getSelectionModel().getSelectedItem();        //вытаскиваем String (ник) из поля
                msgField.appendText("/w " + nick + " ");
                msgField.requestFocus();
                msgField.selectEnd();                                                   //ставим курсор в конец сообщения
            }
        });
        linkCallback();
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);                             //managed - оставлять или нет под наши поля место, если они НЕВИДНЫ
        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);
        clientsList.setVisible(authenticated);
        clientsList.setManaged(authenticated);
        if (!authenticated){
            nickname = "";
            History.stop();
            textArea.clear();
        }
    }

    public void sendAuth() {
        Network.sendAuth(loginField.getText(), passField.getText());
        loginField.clear();
        passField.clear();
    }

    public void sendMsg() {
        if (Network.sendMsg(msgField.getText())) {
            msgField.clear();
            msgField.requestFocus();
        }
    }

    public void showAlert(String msg){
        Platform.runLater(() -> {                                           //опять прокидываем в JavaFX поток, чтобы он смог его принять и вывести, а не консоль
            Alert alert = new Alert(Alert.AlertType.WARNING, msg);
            alert.showAndWait();                                            //блокирует приложуху, пока не ответим на окно
//            alert.show();                                                 //без блокировки основного потока
        });
    }

    //мы связываем через интерфейс Network и Controller для обмена данными
    public void linkCallback(){
        //теперь через переменные прокидываются ссылки на методы
        Network.setCallOnException(args -> showAlert(args[0].toString()));

        Network.setCallOnCloseConnection(args -> setAuthenticated(false));

        Network.setCallOnAuthenticated(args -> {
            setAuthenticated(true);
            nickname = args[0].toString();
            login = args[1].toString();
            textArea.clear();
            textArea.appendText(History.get100LastMSg(login));
            History.start(login);
        });

        Network.setCallOnsMsgReceived(args -> {
            String msg = args[0].toString();
            if (msg.startsWith("/")){
                if (msg.startsWith("/clients ")){
                    String[] tokens = msg.split("\\s");
                    //JavaFX (gui) работает в своем потоке и никакие другие потоки не должны лезть в отрисовку интерфейса
                    //поэтому ту логику обновления списка мы через Platform прокидываем обратно в поток JFX
                    Platform.runLater(() -> {
                        clientsList.getItems().clear();
                        for (int i = 1; i < tokens.length; i++) {
                            clientsList.getItems().add(tokens[i]);
                        }
                    });
                }
            }
            else {
                textArea.appendText(msg + "\n");
                History.writeHistory(msg);
            }
        });

    }
}
