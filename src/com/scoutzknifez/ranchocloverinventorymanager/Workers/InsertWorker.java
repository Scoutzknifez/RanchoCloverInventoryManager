package com.scoutzknifez.ranchocloverinventorymanager.Workers;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Item;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Utils;

public class InsertWorker extends WorkerParent implements Runnable {
    private Item pushedItem;

    public InsertWorker(Item item) {
        pushedItem = item;
        pushedItem.checkSyntax();
    }

    @Override
    public void run() {
        if(statement == null)
            return;

        int rowsEffected = insert();
        if(rowsEffected < 0) {
            Utils.log("Could not insert item.");
        } else {
            Utils.log("Inserted " + rowsEffected + " items.");
        }

        closeConnection();
    }

    private int insert() {
        // upc, pc, name, brand, price, quantity
        String sqlArg = "INSERT INTO physical_inventory VALUES('"
                + pushedItem.getUpc() + "', '" + pushedItem.getProductCode() + "', '"
                + pushedItem.getName() + "', '" + pushedItem.getBrand() + "', '"
                + pushedItem.getPrice() + "', '" + pushedItem.getQuantity() + "')";

        try {
            return statement.executeUpdate(sqlArg);
        } catch (Exception e) {
            Utils.log("Could not insert item into database.");
            return -1;
        }
    }
}
