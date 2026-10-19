package br.edu.contratofacil.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Conexao {
    private Conexao() {
    }

    public static Connection abrir() throws SQLException {
        String host = configurar("DB_HOST", "localhost");
        String porta = configurar("DB_PORT", "5432");
        String banco = configurar("DB_NAME", "contratofacil");
        String usuario = configurar("DB_USER", "contratofacil_app");
        String senha = System.getenv("DB_PASSWORD");

        if (senha == null || senha.isBlank()) {
            throw new SQLException("Configure a variavel de ambiente DB_PASSWORD antes de iniciar o sistema.");
        }

        String url = "jdbc:postgresql://" + host + ":" + porta + "/" + banco;
        return DriverManager.getConnection(url, usuario, senha);
    }

    private static String configurar(String nome, String padrao) {
        String valor = System.getenv(nome);
        return valor == null || valor.isBlank() ? padrao : valor;
    }
}
