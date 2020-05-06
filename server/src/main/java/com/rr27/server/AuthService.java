package com.rr27.server;

public interface AuthService {
    public String getNicknameByLoginAndPassword(String login, String pass);
    public boolean changeNickname(String oldNickname, String newNickname);
}
