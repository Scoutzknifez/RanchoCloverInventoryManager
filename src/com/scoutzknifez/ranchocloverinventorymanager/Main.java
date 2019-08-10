package com.scoutzknifez.ranchocloverinventorymanager;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Clover.CloverItem;
import com.scoutzknifez.ranchocloverinventorymanager.Forms.Inventory;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Constants;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Main {
    public static Inventory inventoryPanel;

    public static void main(String[] args) {
        Utils.initializeApplication();
        for(int i = 0; i < Constants.cloverInventoryList.getObjectList().size(); i++) {
            CloverItem item = (CloverItem) Constants.cloverInventoryList.get(i);
            /*
            // LinkedHashMap <String, Object>
            if(item.getTags() != null)
                System.out.println(item.getTags().getClass());
            // Linked hashmap <String, Object>
            if(item.getItemStock() != null)
                System.out.println(item.getItemStock());*/
            if(item.getTags() != null && item.getItemStock() != null) {
                System.out.println(item.getTags());
                LinkedHashMap<String, Object> mapping = (LinkedHashMap<String, Object>) item.getTags();
                ArrayList<LinkedHashMap<String, Object>> tagList = (ArrayList<LinkedHashMap<String, Object>>) mapping.get("elements");
                LinkedHashMap<String, Object> currentTag = (LinkedHashMap<String, Object>) tagList.get(0);
                System.out.println(currentTag.get("name"));

                /*System.out.println(item.getItemStock());
                LinkedHashMap<String, Object> mapping = (LinkedHashMap<String, Object>) item.getItemStock();
                System.out.println(mapping.get("stockCount")); */
            }
        }
    }
}