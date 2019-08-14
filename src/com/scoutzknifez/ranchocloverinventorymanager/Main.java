package com.scoutzknifez.ranchocloverinventorymanager;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Clover.CloverItem;
import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.RequestType;
import com.scoutzknifez.ranchocloverinventorymanager.Forms.Inventory;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Constants;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Utils;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class Main {
    public static Inventory inventoryPanel;

    public static void main(String[] args) {
        Utils.initializeApplication();
    }

    public static void debug() {
        CloverItem cloverItem = (CloverItem) (Constants.cloverInventoryList.get(0));
        System.out.println(cloverItem);
        Request request = Utils.buildRequest(RequestType.POST, makeBody(), "items/" + cloverItem.getId());
        Response response = Utils.runRequest(request);
        Utils.printResponseBody(response);
    }

    private static Object makeBody() {
        return "{\"tags\": {\"elements\":[ null] }}";
    }
}