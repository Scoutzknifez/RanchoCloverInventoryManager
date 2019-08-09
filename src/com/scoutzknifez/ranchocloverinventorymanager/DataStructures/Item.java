package com.scoutzknifez.ranchocloverinventorymanager.DataStructures;

public class Item
{
    private String upc;
    private String productCode;
    private String name;
    private String brand;
    private double price;
    private int quantity;
    private String description;
    private String size;
    private String color;
    private boolean itemIsInPhysical;

    public Item(String upc, String productCode, String name, String brand, double price, int quantity, String description, String size, String color, boolean itemIsInPhysical)
    {
        this.upc = upc.trim();
        this.productCode = productCode.trim();
        this.name = name.trim();
        this.brand = brand.trim();
        this.price = price;
        this.quantity = quantity;
        this.description = description.trim();
        this.size = size.trim();
        this.color = color.trim();
        this.itemIsInPhysical = itemIsInPhysical;
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
    public String getName()
    {
        return name;
    }
    public String getUpc() {
        return upc;
    }
    public void setUpc(String barcode) {
        upc = barcode;
    }
    public String getProductCode()
    {
        return productCode;
    }
    public String getBrand() {
        return brand;
    }
    public double getPrice()
    {
        return price;
    }
    public int getQuantity()
    {
        return quantity;
    }
    public String getDescription() {
        return description;
    }
    public String getColor() {
        return color;
    }
    public boolean isItemIsInPhysical() {
        return itemIsInPhysical;
    }
    public void setItemIsInPhysical(boolean itemIsInPhysical) {
        this.itemIsInPhysical = itemIsInPhysical;
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