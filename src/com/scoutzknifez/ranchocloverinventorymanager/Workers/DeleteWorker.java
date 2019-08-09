package com.scoutzknifez.ranchocloverinventorymanager.Workers;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Item;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Utils;

public class DeleteWorker extends WorkerParent implements Runnable {
    private String upc;

    public DeleteWorker(String toDelete) {
        super();
        upc = toDelete;
    }

    public DeleteWorker(Item item) {
        super();
        upc = item.getUpc();
    }

    @Override
    public void run() {
        if(statement == null)
            return;

        int rowsEffected = delete();
        if(rowsEffected < 0) {
            // we had an issue and most likely an error.
            Utils.log("No items in database were effected.");
        } else {
            Utils.log("Deleted " + rowsEffected + " from the inventory. (UPC: " + upc + ")");
        }

        closeConnection();
    }

    private int delete() {
        String sqlArg = "DELETE FROM physical_inventory WHERE UPC = '" + upc + "'";
        try {
            return statement.executeUpdate(sqlArg);
        } catch(Exception e) {
            Utils.log("Could not delete item.");
            return -1;
        }
    }
}