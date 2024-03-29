package com.scoutzknifez.ranchocloverinventorymanager.Forms;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Clover.CloverItem;
import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Item;
import com.scoutzknifez.ranchocloverinventorymanager.Main;
import com.scoutzknifez.ranchocloverinventorymanager.Utility.Constants;
import com.scoutzknifez.ranchocloverinventorymanager.Utility.Result;
import com.scoutzknifez.ranchocloverinventorymanager.Utility.Utils;
import com.scoutzknifez.ranchocloverinventorymanager.Workers.CloverWorkers.CloverDeleteWorker;
import com.scoutzknifez.ranchocloverinventorymanager.Workers.CloverWorkers.CloverUpdateWorker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class AddItem {
    private JButton create_item_button;
    private JButton create_item_cancel_button;
    private JLabel create_item_name_label;
    private JTextField create_item_name_field;
    private JLabel create_item_upc_label;
    private JTextField create_item_upc_field;
    private JLabel create_item_product_code_label;
    private JTextField create_item_product_code_field;
    private JLabel create_item_brand_label;
    private JTextField create_item_brand_field;
    private JLabel create_item_price_label;
    private JTextField create_item_price_field;
    private JLabel create_item_quantity_label;
    private JTextField create_item_quantity_field;
    private JLabel create_item_label;
    private JPanel add_item_panel;
    private boolean isAddItem;

    public AddItem(boolean isAddItem) {
        this.isAddItem = isAddItem;
        setCreateEditButtonFunction(isAddItem);
        create_item_cancel_button.addActionListener(getCancelButtonListener());
    }

    public void close() {
        Component component = getAddItemPanel();
        while (!(component instanceof JFrame)) {
            component = component.getParent();
        }
        ((JFrame) component).dispose();

        Inventory.itemFrame = null;
        Inventory.itemFrameIsOpen = false;
        Main.inventoryPanel.updateDisplayList();
    }

    private void setCreateEditButtonFunction(boolean isAddItem) {
        if(isAddItem) {
            create_item_button.addActionListener(getCreateButtonListener());
        } else {
            create_item_button.addActionListener(getEditButtonListener());
            create_item_label.setText(Constants.EDIT_ITEM_TITLE);
            create_item_upc_field.setEnabled(false);
            create_item_upc_field.addMouseListener(getMouseListener());
        }
    }

    private MouseListener getMouseListener() {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int action = JOptionPane.showConfirmDialog(null, "Do you want to edit this item's upc", "Edit UPC Confirmation", JOptionPane.YES_NO_OPTION);

                Item created = createItem();
                boolean isEditing = false;

                if(action == JOptionPane.OK_OPTION) {
                    close();
                    CloverItem cloverItem = Constants.cloverInventoryList.getCloverItem(created.getUpc());
                    Thread thread = new Thread(new CloverDeleteWorker(cloverItem));
                    thread.start();
                    try {
                        thread.join();
                        ArrayList<Item> list = (ArrayList<Item>) Constants.inventoryList.filterList(created.getUpc());
                        if(list.size() > 0) {
                            Constants.cloverInventoryList.remove(list.get(0));
                        }
                        Main.inventoryPanel.updateDisplayList();
                        isEditing = true;
                    } catch (Exception ex) {
                        Utils.log("Could not edit the UPC: " + created.getUpc());
                    }
                }

                if(isEditing) {
                    AddItem editUPC = new AddItem(true);
                    Utils.makeNewFrame(Constants.ADD_ITEM_TITLE, editUPC, created);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        };
    }

    public void doSubmission() {
        if(isAddItem)
            create();
        else
            edit();

        close();
    }

    private ActionListener getCreateButtonListener() {
        return e -> {
            create();
            close();
        };
    }

    private void create() {
        if(attemptCreation() == Result.SUCCESS) {
            Item created = createItem();
            Utils.postItem(created);
        }
    }

    private ActionListener getEditButtonListener() {
        return e -> {
            edit();
            close();
        };
    }

    private void edit() {
        // Delete the old item, and reupload the item that was created like above
        if (attemptCreation() == Result.SUCCESS) {
            Item updated = createItem();
            CloverUpdateWorker cloverUpdateWorker = new CloverUpdateWorker(updated);
            Thread updateThread = new Thread(cloverUpdateWorker);
            updateThread.start();

            try {
                updateThread.join();

                if(Main.inventoryPanel.getSelectedItemList().size() > 0) {
                    Item currentItem = Main.inventoryPanel.getSelectedItemList().get(0);
                    int index = Constants.inventoryList.getItemList().indexOf(currentItem);
                    if(index >= 0)
                        Main.inventoryPanel.setItemToInventory(updated, index);
                    Main.inventoryPanel.getInventoryTable().getSelectionModel().clearSelection();
                    Main.inventoryPanel.getSelectedItemList().clear();
                }

                Main.inventoryPanel.getDeleteButton().setEnabled(false);
                Main.inventoryPanel.getEditButton().setEnabled(false);
            } catch (Exception e) {
                Utils.log("Could not delete the item '" + updated.getName() + "' from the clover database.");
                e.printStackTrace();
            }
        }
    }

    private ActionListener getCancelButtonListener() {
        return e -> close();
    }

    public JPanel getAddItemPanel() {
        return add_item_panel;
    }

    public String getUPC() {
        return create_item_upc_field.getText().trim();
    }

    public String getProductCode() {
        return create_item_product_code_field.getText().trim();
    }

    public String getName() {
        return create_item_name_field.getText().trim();
    }

    public String getBrand() {
        return create_item_brand_field.getText().trim();
    }

    public double getPrice() {
        double returnPrice = 0;

        String priceString = create_item_price_field.getText().trim();
        if(priceString.contains("$")) {
            priceString = priceString.split("\\$")[1].trim();
        }

        try {
            returnPrice = Double.parseDouble(priceString);
        } catch(Exception e) {
            returnPrice = 0;
        }

        return returnPrice;
    }

    public int getQuantity() {
        int returnQuantity;

        String quantityString = create_item_quantity_field.getText().trim();

        try {
            returnQuantity = Integer.parseInt(quantityString);
        } catch(Exception e) {
            returnQuantity = 0;
        }

        return returnQuantity;
    }

    public Result attemptCreation() {
       if(getUPC().isEmpty())
           return doDialogAndFailure("UPC");
       if(getName().isEmpty())
           return doDialogAndFailure("Item Name");
       return Result.SUCCESS;
    }

    private Result doDialogAndFailure(String type) {
        JOptionPane.showMessageDialog(null, type + " field can not be empty.", "Error: " + type + " field empty", JOptionPane.ERROR_MESSAGE);
        return Result.FAILURE;
    }

    public Item createItem() {
        return new Item(getUPC(), getProductCode(), getName(), getBrand(), getPrice(), getQuantity(), "", "", "", (!isAddItem));
    }

    public JTextField getUpcField() {
        return create_item_upc_field;
    }

    public JTextField getItemNameField() {
        return create_item_name_field;
    }

    public JTextField getProductCodeField() {
        return create_item_product_code_field;
    }

    public JTextField getBrandField() {
        return create_item_brand_field;
    }

    public JTextField getPriceField() {
        return create_item_price_field;
    }

    public JTextField getQuantityField() {
        return create_item_quantity_field;
    }

    public JButton getCreateItemButton() {
        return create_item_button;
    }

    public static WindowAdapter getWindowCloser(AddItem addItem) {
        return new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                resetFrame(addItem);
            }
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                resetFrame(addItem);
            }
        };
    }
    private static void resetFrame(AddItem addItem) {
        if(addItem == Inventory.itemFrame) {
            Inventory.itemFrameIsOpen = false;
            Inventory.itemFrame = null;
        }
    }
}
