package com.scoutzknifez.ranchocloverinventorymanager;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.ItemList;
import com.scoutzknifez.ranchocloverinventorymanager.Forms.Inventory;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Utils;

public class Main
{
    public static ItemList inventoryList;
    public static Inventory inventoryPanel;

    public static void main(String[] args)
    {
        Utils.initializeApplication();
    }
}