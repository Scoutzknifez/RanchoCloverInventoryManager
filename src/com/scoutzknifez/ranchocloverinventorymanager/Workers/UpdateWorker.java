package com.scoutzknifez.ranchocloverinventorymanager.Workers;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Item;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Utils;

public class UpdateWorker extends WorkerParent implements Runnable {
    private Item updatedItem;

    public UpdateWorker(Item item) {
        super();
        updatedItem = item;
        updatedItem.checkSyntax();
    }

    @Override
    public void run() {
        if(statement == null)
            return;

        int rowsEffected = update();
        if(rowsEffected < 0) {
            Utils.log("No items were effected by update.");
        } else {
            Utils.log("Updated " + rowsEffected + " from the inventory.");
        }

        closeConnection();
    }

    private int update() {
        String sqlArg = "UPDATE physical_inventory SET Name='" + updatedItem.getName() + "', Price=" + updatedItem.getPrice() + ", Quantity=" + updatedItem.getQuantity() + ", Brand='" + updatedItem.getBrand() + "', product_code='" + updatedItem.getProductCode() + "' WHERE UPC='" + updatedItem.getUpc() + "'";
        try {
            return statement.executeUpdate(sqlArg);
        } catch(Exception e) {
            Utils.log("Could not update item.");
            return -1;
        }
    }
}