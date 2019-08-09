package com.scoutzknifez.ranchocloverinventorymanager.Workers;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.ItemList;
import com.scoutzknifez.ranchocloverinventorymanager.Main;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Result;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Utils;

public class WorkerHandler {
    public static Result fetchInventory() {
        GetAllWorker allFetcher = new GetAllWorker();
        Thread thread = new Thread(allFetcher);
        thread.start();
        try {
            thread.join();
            Main.inventoryList = new ItemList(allFetcher.getItems());
            return Result.SUCCESS;
        } catch(Exception e) {
            Utils.log("Can not get inventory list from database.");
        }
        return Result.FAILURE;
    }
}