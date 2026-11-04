package br.edu.contratofacil.model;

import java.io.Serializable;

public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long id;
    private final String login;
    private final PerfilUsuario perfil;

    public Usuario(long id, String login, PerfilUsuario perfil) {
        this.id = id;
        this.login = login;
        this.perfil = perfil;
    }

    public long getId() { return id; }
    public String getLogin() { return login; }
    public PerfilUsuario getPerfil() { return perfil; }
}
