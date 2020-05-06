package com.rr27.server;

public class DBAuthService implements AuthService {

    @Override
    public String getNicknameByLoginAndPassword(String login, String pass) {
        return UsersRepository.getNicknameByLoginAndPassword(login, pass);
    }
    @Override
    public boolean changeNickname(String oldNickname, String newNickname) {
        return UsersRepository.updateNickname(oldNickname, newNickname);
    }
}
