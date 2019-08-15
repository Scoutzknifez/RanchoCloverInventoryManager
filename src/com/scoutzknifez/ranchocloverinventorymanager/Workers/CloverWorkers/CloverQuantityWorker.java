package com.scoutzknifez.ranchocloverinventorymanager.Workers.CloverWorkers;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Clover.CloverItem;
import com.scoutzknifez.ranchocloverinventorymanager.Utility.Utils;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CloverQuantityWorker extends CloverWorkerParent {
    private CloverItem cloverItem;
    private int quantity;

    public CloverQuantityWorker(CloverItem cloverItem, int quantity) {
        setCloverItem(cloverItem);
        setQuantity(quantity);
    }

    @Override
    public void run() {
        Utils.setItemQuantity(getCloverItem(), getQuantity());
    }
}