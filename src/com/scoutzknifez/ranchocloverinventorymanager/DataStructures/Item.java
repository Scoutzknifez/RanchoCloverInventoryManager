package com.scoutzknifez.ranchocloverinventorymanager.DataStructures;

import com.scoutzknifez.ranchocloverinventorymanager.Interfaces.Filterable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor @Getter @Setter
public class Item implements Filterable
{
    private String upc;
    private String productCode;
    private String name;
    private String brand;
    private double price;
    private int quantity;
    private String description = "";
    private String size = "";
    private String color = "";
    private boolean itemIsInPhysical;

    public Item(String upc, String productCode, String name, long price) {
        setUpc(upc);
        setProductCode(productCode);
        setName(name);
        setPrice(price / 100.0);
    }

    public boolean containsFilter(String searchTerm) {
        if(getUpc().toLowerCase().contains(searchTerm.toLowerCase())
                || getName().toLowerCase().contains(searchTerm.toLowerCase())
                || getBrand().toLowerCase().contains(searchTerm.toLowerCase())
                || getProductCode().toLowerCase().contains(searchTerm.toLowerCase())
                || getDescription().toLowerCase().contains(searchTerm.toLowerCase())
                || getColor().toLowerCase().contains(searchTerm.toLowerCase())
                || (getPrice() + "").toLowerCase().contains(searchTerm))
            return true;
        return false;
    }
    public void checkSyntax()
    {
        if(upc.contains("\'"))
        {
            upc = upc.replaceAll("'", "\\\\\'");
        }
        if(name.contains("\'"))
        {
            name = name.replaceAll("'", "\\\\\'");
        }
        if(brand.contains("\'"))
        {
            brand = brand.replaceAll("'", "\\\\\'");
        }
        if(description.contains("\'"))
        {
            description = description.replaceAll("'","\\\\\'");
        }
        if(size.contains("\'"))
        {
            size = size.replaceAll("'", "\\\\\'");
        }
        if(productCode.contains("\'"))
        {
            productCode = productCode.replaceAll("'", "\\\\\'");
        }
        if(color.contains("\'"))
        {
            color = color.replaceAll("'", "\\\\\'");
        }

    }

    public String toString()
    {
        String returned = "";

        returned += "UPC: " + getUpc() + "\n";
        returned += "Product Code: " + getProductCode() + "\n";
        returned += "Item name: " + getName() + "\n";
        returned += "Brand: " + getBrand() + "\n";
        returned += "Price: " + getPrice() + "\n";
        returned += "Quantity: " + getQuantity() + "\n";
        returned += "In Physical: " + isItemIsInPhysical() + "\n";

        return returned;
    }
}