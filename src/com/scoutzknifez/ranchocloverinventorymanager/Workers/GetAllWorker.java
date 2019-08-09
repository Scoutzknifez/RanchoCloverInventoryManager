package com.scoutzknifez.ranchocloverinventorymanager.Workers;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Item;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Utils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class GetAllWorker extends WorkerParent implements Runnable{
    private List<Item> items;

    public GetAllWorker() {
        super();
    }
    @Override
    public void run() {
        if(statement == null)
            return;

        items = getAll();
        if(items == null) {
            System.out.println("Item list that was fetched is null.");
        }

        closeConnection();
    }
    private List<Item> getAll() {
        String sqlArg = "SELECT * FROM physical_inventory";
        try {
            return putResultIntoList(statement.executeQuery(sqlArg));
        } catch(Exception e) {
            System.out.println("Failed to fetch item list from database.");
            return null;
        }
    }
    private List<Item> putResultIntoList(ResultSet set) {
        List<Item> fetchedList = new ArrayList<>();
        try {
            while (set.next()) {
                String upc = set.getString("UPC");
                String itemName = set.getString("Name");
                double price = set.getDouble("Price");
                int quantity = set.getInt("Quantity");
                String brand = set.getString("Brand");
                String productCode = set.getString("product_code");
                Item thisItem = new Item(upc, productCode, itemName, brand, price, quantity, "", "", "", true);
                fetchedList.add(thisItem);
            }
            return fetchedList;
        } catch(Exception e) {
            Utils.log("Could not put result set into item list.");
            return null;
        }
    }
    public List<Item> getItems() {
        return items;
    }
}