package com.scoutzknifez.ranchocloverinventorymanager.Workers;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Clover.CloverItem;
import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Clover.CloverItemListResponseBody;
import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.RequestType;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Constants;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Utils;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class CloverGetAllWorker extends WorkerParent implements Runnable{

    public CloverGetAllWorker() {
        super();
    }
    @Override
    public void run() {
        doGetAll();
    }

    private void doGetAll() {
        try {
            boolean stillMore = true;
            int i = 0;
            while(stillMore) {
                String[] args = new String[3];
                args[0] = "items/";
                args[1] = "limit=1000";
                args[2] = "offset=" + (i * 1000);
                Request request = Utils.buildRequest(RequestType.GET, args);
                Response response = Utils.runRequest(request);
                if(response != null) {
                    CloverItemListResponseBody cloverItemListResponseBody = Constants.OBJECT_MAPPER.readValue(response.body().string(), CloverItemListResponseBody.class);
                    ArrayList<LinkedHashMap<String, String>> unparsedItemList = cloverItemListResponseBody.getElements();
                    ArrayList<CloverItem> items = Utils.parseList(unparsedItemList);
                    for(CloverItem cloverItem : items) {
                        if(cloverItem.getSku() != null)
                            Constants.cloverInventoryList.add(cloverItem);
                    }
                    if(items.size() < 1000)
                        stillMore = false;
                }
                i++;
            }
        } catch (Exception e) {
            System.out.println("Could not make the object list.");
            e.printStackTrace();
        }
        Utils.sortCloverItemList();
    }
}