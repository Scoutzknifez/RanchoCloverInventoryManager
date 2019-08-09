package com.scoutzknifez.ranchocloverinventorymanager.DataStructures;

import com.scoutzknifez.ranchocloverinventorymanager.Forms.Inventory;

import java.util.ArrayList;
import java.util.List;

public class ItemList
{
    private List<Item> itemList = new ArrayList<>();

    public ItemList() {

    }

    public ItemList(List<Item> inList) {
        itemList = inList;
    }

    public Item get(int index) {
        return itemList.get(index);
    }

    public Item getItem(String filter) {
        return filterList(filter).get(0);
    }

    public List<Item> getItemList() {
        return itemList;
    }

    public void add(Item item) {
        itemList.add(item);
    }

    public void set(Item item, int index) {
        itemList.set(index, item);
    }

    public void remove(Item item) {
        itemList.remove(item);
    }

    public List<Item> filterList(String filter) {
        if(filter.equalsIgnoreCase(Inventory.FILTER_BOX_HINT) || filter.equalsIgnoreCase(""))
            return itemList;

        List<Item> subset = new ArrayList<>();
        for(Item item : itemList) {
            if(item.containsFilter(filter))
                subset.add(item);
        }
        return subset;
    }
}