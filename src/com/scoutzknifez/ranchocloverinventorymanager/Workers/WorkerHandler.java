package com.scoutzknifez.ranchocloverinventorymanager.Workers;

import com.scoutzknifez.ranchocloverinventorymanager.Utils.Result;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Utils;

public class WorkerHandler {
    public static Result fetchInventory() {
        CloverGetAllWorker cloverGetAllWorker = new CloverGetAllWorker();
        Thread thread = new Thread(cloverGetAllWorker);
        thread.start();
        try {
            thread.join();
            return Result.SUCCESS;
        } catch(Exception e) {
            Utils.log("Can not get inventory list from database.");
        }
        return Result.FAILURE;
    }
}