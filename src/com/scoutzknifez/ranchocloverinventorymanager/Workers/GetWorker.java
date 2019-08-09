package com.scoutzknifez.ranchocloverinventorymanager.Workers;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Item;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Utils;

import java.sql.ResultSet;

public class GetWorker extends WorkerParent implements Runnable {
    private Item item;
    private String upc;
    private boolean isPhysical;

    public GetWorker(String upc, boolean isPhysical) {
        super();
        this.upc = upc;
        this.isPhysical = isPhysical;
    }

    @Override
    public void run() {
        if(statement == null)
            return;

        item = get();
        if(item != null)
            item.setUpc(upc);

        if(item == null) {
            System.out.println("Item that was fetched is null.");
        }

        closeConnection();
    }
    private Item get() {
        String whereFrom = "inventory";
        if(isPhysical)
            whereFrom = "physical_inventory";

        String sqlArg = "SELECT * FROM " + whereFrom + " WHERE UPC = '" + upc + "'";
        try {
            return putResultIntoItem(statement.executeQuery(sqlArg));
        } catch (Exception e) {
            Utils.log("Failed to fetch item from database.");
            return null;
        }
    }
    private Item putResultIntoItem(ResultSet set) {
        Item fetchedItem = null;
        try {
            while(set.next()) {
                String upc = set.getString("UPC");
                String itemName = set.getString("Name");
                double price = set.getDouble("Price");
                int quantity = set.getInt("Quantity");
                String brand = set.getString("Brand");
                String productCode = set.getString("product_code");
                fetchedItem = new Item(upc, productCode, itemName, brand, price, quantity, "", "", "", isPhysical);
            }
            return fetchedItem;
        } catch(Exception e) {
            Utils.log("Could not put set into item.");
            return fetchedItem;
        }
    }
    public Item getItem() {
        return item;
    }
}
