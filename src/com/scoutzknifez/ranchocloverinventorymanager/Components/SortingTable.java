package com.scoutzknifez.ranchocloverinventorymanager.Components;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Item;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class SortingTable extends AbstractTableModel {
    private List<Item> displayedItemList;
    private  String[] columnNames = {"UPC", "Name", "PC", "Brand", "Price", "Quantity"};
    private static final int COLUMN_UPC = 0;
    private static final int COLUMN_NAME = 1;
    private static final int COLUMN_PC = 2;
    private static final int COLUMN_BRAND = 3;
    private static final int COLUMN_PRICE = 4;
    private static final int COLUMN_QUANTITY = 5;

    public SortingTable(List<Item> itemList) {
        displayedItemList = itemList;
    }

    @Override
    public int getRowCount() {
        return displayedItemList != null ? displayedItemList.size() : 0;
    }

    /**
     * This is to be used in conjunction with the RowSorter for JTable
     * @param columnIndex index to be sorted
     * @return sorting Class
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if(columnIndex == COLUMN_PRICE)
            return Double.class;
        else if(columnIndex == COLUMN_QUANTITY)
            return Integer.class;
        else
            return String.class;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        /*if(!(rowIndex < displayedItemList.size()))
            return null;*/

        Item item = displayedItemList.get(rowIndex);
        Object returned;
        switch(columnIndex) {
            case COLUMN_UPC:
                returned = item.getUpc();
                break;
            case COLUMN_NAME:
                returned = item.getName();
                break;
            case COLUMN_PC:
                returned = item.getProductCode();
                break;
            case COLUMN_BRAND:
                returned = item.getBrand();
                break;
            case COLUMN_PRICE:
                returned = item.getPrice();
                break;
            case COLUMN_QUANTITY:
                returned = item.getQuantity();
                break;
            default:
                throw new IllegalArgumentException("Invalid column index, index: " + columnIndex + ", size: " + getColumnCount());
        }
        return returned;
    }

    public void setDisplayedItemList(List<Item> displayedItemList) {
        this.displayedItemList = displayedItemList;
    }

    public List<Item> getDisplayedItemList() {
        return this.displayedItemList;
    }
}