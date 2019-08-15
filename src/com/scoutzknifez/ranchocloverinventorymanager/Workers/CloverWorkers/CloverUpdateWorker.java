package com.scoutzknifez.ranchocloverinventorymanager.Workers.CloverWorkers;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Clover.CloverItem;
import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Item;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Constants;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Utils;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class CloverUpdateWorker extends CloverWorkerParent {
    private CloverItem cloverItem;
    private Item item;

    public CloverUpdateWorker(Item item) {
        // Sudo updater
        // Done by deleting old while at the same time adding the new and linking it.
        setItem(item);
        setCloverItem(Utils.getCloverItemFromItem(item));
    }

    @Override
    public void run() {
        CloverDeleteWorker cloverDeleteWorker = new CloverDeleteWorker(getCloverItem());
        Thread deleterThread = new Thread(cloverDeleteWorker);
        deleterThread.start();

        CloverItem newItem = new CloverItem(getItem().getName(), getItem().getUpc(), getItem().getProductCode(), Utils.makeLong(getItem().getPrice()));
        CloverInsertWorker cloverInsertWorker = new CloverInsertWorker(newItem);
        Thread insertWorker = new Thread(cloverInsertWorker);
        insertWorker.start();

        try {
            deleterThread.join();
        } catch (Exception e) {
            Utils.log("Updater's deletion failed.");
            e.printStackTrace();
        }

        try {
            insertWorker.join();
            setCloverItem(cloverInsertWorker.getCloverItem());
            Constants.inventoryList.add(getItem());
        } catch (Exception e) {
            Utils.log("Updater's insertion failed.");
            e.printStackTrace();
        }

        // We have deleted the old item, now we have to update the new item to fit the quantity and label

        CloverItemTagWorker cloverItemTagWorker = new CloverItemTagWorker(getCloverItem(), Utils.getBrandTag(getItem()));
        CloverQuantityWorker cloverQuantityWorker = new CloverQuantityWorker(getCloverItem(), getItem().getQuantity());
        Thread tagThread = new Thread(cloverItemTagWorker);
        Thread quantityThread = new Thread(cloverQuantityWorker);
        tagThread.start();
        quantityThread.start();

        try {
            tagThread.join();
        } catch (Exception e) {
            Utils.log("Updater's tagger failed.");
            e.printStackTrace();
        }

        try {
            quantityThread.join();
        } catch (Exception e) {
            Utils.log("Updater's quantity setter failed.");
            e.printStackTrace();
        }
    }
}