package com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Clover;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Item;
import com.scoutzknifez.ranchocloverinventorymanager.Interfaces.Filterable;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Constants;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@JsonPropertyOrder({
        "id",
        "hidden",
        "name",
        "code",
        "sku",
        "price",
        "priceType",
        "defaultTaxRates",
        "isRevenue",
        "tags",
        "itemStock",
        "modifiedTime"
})

@AllArgsConstructor @Getter @Setter
public class CloverItem implements Serializable, Filterable {
    private String id = "";
    private boolean hidden = false;
    private String name;
    private String code;
    private String sku;
    private long price;
    private String priceType = "FIXED";
    private boolean defaultTaxRates = true;
    @JsonProperty(value="isRevenue") private boolean isRevenue = true;
    private Object tags;
    private Object itemStock;
    private long modifiedTime = 0;

    public CloverItem() {}

    public CloverItem(String name, String sku, String code, long price) {
        setName(name);
        setSku(sku);
        setCode(code);
        setPrice(price);
    }

    public CloverItem(String id, String name, String sku, String code, long price, Object tags, Object itemStock) {
        setId(id);
        setName(name);
        setSku(sku);
        setCode(code);
        setPrice(price);
        setTags(tags);
        setItemStock(itemStock);
    }

    public boolean containsFilter(String searchTerm) {
        if(getId().equalsIgnoreCase(searchTerm) ||
                getName().equalsIgnoreCase(searchTerm) ||
                getCode().equalsIgnoreCase(searchTerm) ||
                getSku().equalsIgnoreCase(searchTerm) ||
                getPriceType().equalsIgnoreCase(searchTerm))
            return true;

        return false;
    }

    public boolean needsUpdate(Item item) {
        long itemPrice = Utils.makeLong(item.getPrice());
        return (!getName().equals(item.getName()) ||
                !getCode().equals(item.getProductCode()) ||
                getPrice() != itemPrice);
    }

    public boolean equalsItem(Item item) {
        return (item.getName().equals(getName()) && item.getUpc().equals(getSku()));
    }

    public boolean equalsSku(Item item) {
        return item.getUpc().equals(getSku());
    }

    public boolean equalsSku(CloverItem cloverItem) {
        return cloverItem.getSku().equals(getSku());
    }

    @Override
    public boolean equals(Object object) {
        if(object == this)
            return true;

        if(checkValues(object))
            return true;

        return false;
    }

    private boolean checkValues(Object object) {
        if(!(object instanceof CloverItem))
            return false;

        CloverItem cloverItem = (CloverItem) object;
        if(getId().equalsIgnoreCase(cloverItem.getId()) &&
            isHidden() == cloverItem.isHidden() &&
            getName().equalsIgnoreCase(cloverItem.getName()) &&
            getCode().equalsIgnoreCase(cloverItem.getCode()) &&
            getSku().equalsIgnoreCase(cloverItem.getSku()) &&
            getPrice() == cloverItem.getPrice() &&
            getPriceType().equalsIgnoreCase(cloverItem.getPriceType()) &&
            isDefaultTaxRates() == cloverItem.isDefaultTaxRates() &&
            isRevenue() == cloverItem.isRevenue() &&
            getModifiedTime() == cloverItem.getModifiedTime())
            return true;

        return  false;
    }

    @Override
    public String toString() {
        String returned = "";
        try {
            returned = Constants.OBJECT_MAPPER.writeValueAsString(this);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return returned;
    }
}