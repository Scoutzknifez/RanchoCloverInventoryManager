package com.scoutzknifez.ranchocloverinventorymanager.Workers.CloverWorkers;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Clover.CloverItem;
import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.RequestType;
import com.scoutzknifez.ranchocloverinventorymanager.Utility.Constants;
import com.scoutzknifez.ranchocloverinventorymanager.Utility.Utils;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class CloverInsertWorker extends CloverWorkerParent {
    private CloverItem cloverItem;

    public CloverInsertWorker(CloverItem cloverItem) {
        setCloverItem(cloverItem);
    }

    @Override
    public void run() {
        postItem(cloverItem);
        Utils.sortCloverItemList();
    }

    private void postItem(CloverItem cloverItem) {
        Request request = Utils.buildRequest(RequestType.POST, cloverItem, "items");
        Response response = Utils.runRequest(request);
        try {
            String body = response.body().string();
            setCloverItem(Constants.OBJECT_MAPPER.readValue(body , CloverItem.class));
            Constants.cloverInventoryList.add(getCloverItem());
            Utils.closeResponseBody(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}