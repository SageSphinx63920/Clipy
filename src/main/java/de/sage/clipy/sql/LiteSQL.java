package de.sage.clipy.sql;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class LiteSQL {

    private static Connection connection;
    private static Statement statement;

    public static void connect() {
        connection = null;
        try {
            File file = new File("database.db");
            if (!file.exists())
                file.createNewFile();

            String url = "jdbc:sqlite:" + file.getPath();
            connection = DriverManager.getConnection(url);
            statement = connection.createStatement();

            createTables();
        } catch (SQLException | java.io.IOException ex) {
            ex.printStackTrace();
        }
    }

    public static boolean isConnected() {
        try {
            return (connection != null && connection.prepareStatement("SELECT 1").executeQuery().next());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void disconnect() {
        try {
            if (connection != null)
                connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void onUpdate(final String statement, Object... preparedArgs) {
        if(isConnected()) {
            new FutureTask(new Runnable() {
                PreparedStatement preparedStatement;

                public void run() {
                    try {
                        this.preparedStatement = connection.prepareStatement(statement);

                        for(int i = 0; i < preparedArgs.length; i++) {
                            this.preparedStatement.setObject(i+1, preparedArgs[i]);
                        }

                        this.preparedStatement.executeUpdate();
                        this.preparedStatement.close();
                    } catch (SQLException throwable) {
                        throwable.printStackTrace();
                    }
                }
            }, 1).run();
        }else {
            connect();
            onUpdate(statement, preparedArgs);
        }
    }

    @Nullable
    public static ResultSet onQuery(final String query, Object... preparedArgs) {
        if (isConnected()) {
            try {
                FutureTask<ResultSet> task = new FutureTask<>(new Callable<ResultSet>() {
                    PreparedStatement ps;

                    public ResultSet call() throws Exception {
                        this.ps = connection.prepareStatement(query);

                        for(int i = 0; i < preparedArgs.length; i++) {
                            this.ps.setObject(i+1, preparedArgs[i]);
                        }

                        return this.ps.executeQuery();
                    }
                });
                task.run();
                return task.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            connect();
            return onQuery(query);
        }
        return null;
    }

    private static void createTables(){
        onUpdate("CREATE TABLE IF NOT EXISTS userData (userID INTEGER PRIMARY KEY, autoStart BOOLEAN, tos BOOLEAN, muted BOOLEAN DEFAULT false, premium BOOLEAN DEFAULT false)");
    }
}