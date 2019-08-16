package com.scoutzknifez.ranchocloverinventorymanager.Workers.CloverWorkers;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Clover.CloverItem;
import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Item;
import com.scoutzknifez.ranchocloverinventorymanager.Utility.Constants;
import com.scoutzknifez.ranchocloverinventorymanager.Utility.Result;
import com.scoutzknifez.ranchocloverinventorymanager.Utility.Utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class CloverWorkerHandler {
    public static Result fetchInventory() {
        CloverGetAllWorker cloverGetAllWorker = new CloverGetAllWorker();
        Thread thread = new Thread(cloverGetAllWorker);
        Constants.cloverInventoryList.getObjectList().clear();
        thread.start();
        try {
            thread.join();
            // Utils.saveData();
            makeItemsFromCloverList();
            return Result.SUCCESS;
        } catch(Exception e) {
            Utils.log("Can not get inventory list from database.");
            e.printStackTrace();
        }
        return Result.FAILURE;
    }

    private static void makeItemsFromCloverList() {
        Constants.inventoryList.getItemList().clear();
        for(Object object : Constants.cloverInventoryList.getObjectList()) {
            if(object instanceof CloverItem) {
                CloverItem cloverItem = (CloverItem) object;
                Constants.inventoryList.add(makeItemFromCloverItem(cloverItem));
            }
        }
    }

    public static Item makeItemFromCloverItem(CloverItem cloverItem) {
        if(cloverItem == null)
            return null;

        Item item = new Item(cloverItem.getSku(), cloverItem.getCode(), cloverItem.getName(), cloverItem.getPrice());

        item.setBrand(getBrand(cloverItem));
        item.setQuantity(getQuantity(cloverItem));

        return item;
    }

    private static String getBrand(CloverItem item) {
        String brand = "NEEDS BRAND";
        if(item.getTags() != null) {
            try {
                LinkedHashMap<String, Object> mapping = (LinkedHashMap<String, Object>) item.getTags();
                ArrayList<LinkedHashMap<String, Object>> tagList = (ArrayList<LinkedHashMap<String, Object>>) mapping.get("elements");
                LinkedHashMap<String, Object> currentTag = tagList.get(0);
                brand = (String) currentTag.get("name");
            } catch (Exception e) {
                System.out.println("Item does not have brand: " + item.getName());
            }
        }
        return brand;
    }

    private static int getQuantity(CloverItem item) {
        int quantity = 0;
        if(item.getItemStock() != null) {
            try {
                LinkedHashMap<String, Object> mapping = (LinkedHashMap<String, Object>) item.getItemStock();
                Object quantityObject = mapping.get("quantity");
                if(quantityObject instanceof Float) {
                    quantity = Math.round((Float) quantityObject);
                }
                else if(quantityObject instanceof Double) {
                    quantity = Math.round((int) (((double) quantityObject) * 1.000005));
                }
                else if(quantityObject instanceof Integer) {
                    quantity = (Integer) quantityObject;
                }
                else if(quantityObject instanceof String) {
                    quantity = Integer.parseInt((String) quantityObject);
                }
            } catch (Exception e) {
                System.out.println("Could not make the quantity for item: " + item.getName());
                e.printStackTrace();
            }
        }
        return quantity;
    }
}