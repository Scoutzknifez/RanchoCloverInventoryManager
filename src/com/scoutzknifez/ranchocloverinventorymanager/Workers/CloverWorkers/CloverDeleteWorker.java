package com.scoutzknifez.ranchocloverinventorymanager.Workers.CloverWorkers;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Clover.CloverItem;
import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Item;
import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.RequestType;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Constants;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Utils;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class CloverDeleteWorker extends CloverWorkerParent{
    private CloverItem cloverItem;

    public CloverDeleteWorker(CloverItem cloverItem) {
        setCloverItem(cloverItem);
    }

    @Override
    public void run() {
        deleteItem(cloverItem);
    }

    private void deleteItem(CloverItem cloverItem) {
        if(Constants.TEST_MODE)
            return;

        if(cloverItem.getId().equalsIgnoreCase(""))
            throw new RuntimeException("Can not delete an item with not clover ID!");

        String[] args = new String[1];
        args[0] = "items/" + cloverItem.getId();
        Request request = Utils.buildRequest(RequestType.DELETE, args);
        Response response = Utils.runRequest(request);

        if(response != null) {
            Constants.cloverInventoryList.remove(cloverItem);
            Item toDelete = Constants.inventoryList.getItem(getCloverItem().getSku());
            Constants.inventoryList.remove(toDelete);
        }

        Utils.closeResponseBody(response);
    }
}