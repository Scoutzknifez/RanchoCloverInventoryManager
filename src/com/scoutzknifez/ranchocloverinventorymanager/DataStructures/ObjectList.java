package com.scoutzknifez.ranchocloverinventorymanager.DataStructures;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Clover.CloverItem;
import com.scoutzknifez.ranchocloverinventorymanager.Interfaces.Filterable;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class ObjectList
{
    @Setter
    private List<Object> ObjectList = new ArrayList<>();

    public ObjectList() {

    }

    public ObjectList(List<Object> inList) {
        ObjectList = inList;
    }

    public Object get(int index) {
        return ObjectList.get(index);
    }

    public Object getObject(String filter) {
        return filterList(filter).get(0);
    }

    public List<Object> getObjectList() {
        return ObjectList;
    }

    public void add(Object item) {
        ObjectList.add(item);
    }

    public void set(Object item, int index) {
        ObjectList.set(index, item);
    }

    public void remove(Object item) {
        ObjectList.remove(item);
    }

    public List<Object> filterList(String filter) {
        List<Object> subset = new ArrayList<>();
        for(Object item : ObjectList) {
            if(item instanceof Filterable) {
                if(((Filterable) item).containsFilter(filter))
                    subset.add(item);
            }
        }
        return subset;
    }

    public boolean contains(String string) {
        for(Object item : ObjectList) {
            if(item instanceof Filterable) {
                if(((Filterable) item).containsFilter(string))
                    return true;
            }
        }
        return false;
    }

    public boolean contains(CloverItem cloverItem) {
        for(Object object : ObjectList) {
            if(object instanceof CloverItem) {
                CloverItem inListItem = (CloverItem) object;
                if(inListItem.getSku().equalsIgnoreCase(cloverItem.getSku()))
                    return true;
            }
        }

        return false;
    }

    /**
     * Get the item that is associated with the parameter
     * @param object Either a string or another clover item
     * @return The clover item
     */
    public CloverItem getCloverItem(Object object) {
        if(object instanceof String) {
            String sku = (String) object;
            for(Object objectItem : ObjectList) {
                if(objectItem instanceof CloverItem) {
                    CloverItem inListItem = (CloverItem) objectItem;
                    if(inListItem.getSku().equalsIgnoreCase(sku))
                        return inListItem;
                }
            }
        }

        if(object instanceof CloverItem) {
            CloverItem cloverItem = (CloverItem) object;
            for(Object objectItem : ObjectList) {
                if(objectItem instanceof CloverItem) {
                    CloverItem inListItem = (CloverItem) objectItem;
                    if(inListItem.getSku().equalsIgnoreCase(cloverItem.getSku()))
                        return inListItem;
                }
            }
        }
        return null;
    }
}