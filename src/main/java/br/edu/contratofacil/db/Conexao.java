package br.edu.contratofacil.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Conexao {
    private Conexao() { }

    public static Connection abrir() throws SQLException {
        String senha = System.getenv("DB_PASSWORD");
        if (senha == null || senha.isBlank()) {
            throw new SQLException("DB_PASSWORD nao definida no processo do Tomcat.", "08001");
        }

        String host = configuracao("DB_HOST", "localhost");
        String porta = configuracao("DB_PORT", "5432");
        String banco = configuracao("DB_NAME", "contratofacil");

        Properties dados = new Properties();
        dados.setProperty("user", configuracao("DB_USER", "contratofacil_app"));
        dados.setProperty("password", senha);
        dados.setProperty("connectTimeout", "5");
        dados.setProperty("socketTimeout", "15");

        String endereco = "jdbc:postgresql://" + host + ":" + porta + "/" + banco;
        return DriverManager.getConnection(endereco, dados);
    }

    private static String configuracao(String nome, String padrao) {
        String valor = System.getenv(nome);
        return valor == null || valor.isBlank() ? padrao : valor.trim();
    }
}
