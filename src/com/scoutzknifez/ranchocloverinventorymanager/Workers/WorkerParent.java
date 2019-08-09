package com.scoutzknifez.ranchocloverinventorymanager.Workers;

import com.scoutzknifez.ranchocloverinventorymanager.Utils.Constants;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Result;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public abstract class WorkerParent implements Runnable {
    Statement statement;
    Connection connection;

    public WorkerParent() {
        ready();
    }
    public Statement ready() {
        if (connectToDriver() == Result.FAILURE) {
            System.out.println("Could not connect driver for MySQL.");
            return null;
        }

        connection = connectToMySQLDatabase();
        if (connection == null) {
            Utils.log("Could not connect to MySQL Database.");
            return null;
        }

        statement = getSQLStatement(connection);
        if (statement == null) {
            System.out.println("Could not get MySQL statement.");
            return null;
        }

        return statement;
    }

    public void closeConnection() {
        try {
            statement.close();
        } catch (Exception e) {
            System.out.println("Could not close statement for connection.");
        }
        try {
            connection.close();
        } catch (Exception e) {
            System.out.println("Could not close connection.");
        }
    }

    private Result connectToDriver() {
        System.out.println("Connecting driver! ");
        try {
            Class.forName(Constants.JDBC_DRIVER);
            return Result.SUCCESS;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return Result.FAILURE;
    }

    private Connection connectToMySQLDatabase() {
        try {
            Connection connection = DriverManager.getConnection(Constants.DATABASE_URL, Constants.USERNAME, Constants.PASSWORD);
            return connection;
        } catch (Exception e) {
            Utils.log("Could not connect to MySQL database at " + Constants.DATABASE_URL);
            return null;
        }
    }

    private Statement getSQLStatement(Connection connection) {
        try {
            return connection.createStatement();
        } catch (Exception e) {
            System.out.println("Could not create MySQL statement.");
            return null;
        }
    }
}