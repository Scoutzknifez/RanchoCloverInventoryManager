package com.scoutzknifez.ranchocloverinventorymanager.Forms;

import com.scoutzknifez.ranchocloverinventorymanager.Components.SortingTable;
import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Item;
import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.TimeAtMoment;
import com.scoutzknifez.ranchocloverinventorymanager.Main;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Constants;
import com.scoutzknifez.ranchocloverinventorymanager.Utils.Utils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class Inventory {
    // GUI elements
    private JPanel inventory_panel;
    private JTable inventory_table;
    private JButton edit_button;
    private JButton add_button;
    private JTextField item_name_field;
    private JLabel item_name_label;
    private JLabel item_upc_label;
    private JLabel item_product_code_label;
    private JLabel item_brand_label;
    private JLabel item_price_label;
    private JLabel item_quantity_label;
    private JTextField item_upc_field;
    private JTextField item_product_code_field;
    private JTextField item_brand_field;
    private JTextField item_price_field;
    private JTextField item_quantity_field;
    private JButton delete_button;
    private JScrollPane customJTable;
    private JTextField filter_box;
    private JButton more_info_button;
    private JLabel program_running_info;

    // JTable data
    private List<Item> selectedItemList = new ArrayList<>();
    private List<Item> displayList = new ArrayList<>();
    public static final String FILTER_BOX_HINT = "Filter by";

    // Scanner Variables
    public static boolean itemFrameIsOpen = false;
    public static AddItem itemFrame = null;

    public Inventory() {
        if(inventory_table.getModel() instanceof SortingTable) {
            updateDisplayList();
        }

        inventory_table.getSelectionModel().addListSelectionListener(e -> handleSelectionEvent(e, inventory_table.getSelectionModel()));

        edit_button.addActionListener(getEditListener());
        add_button.addActionListener(getAddListener());
        delete_button.addActionListener(getDeleteListener());

        filter_box.addFocusListener(getFocusListener());
        filter_box.addActionListener(getEnterListener());

        more_info_button.addActionListener(getInfoButtonListener());
        setInfo("Program started");
    }

    public JPanel getInventoryPanel() {
        return inventory_panel;
    }

    private void $$$setupUI$$$() {
        createUIComponents();
    }

    private void createUIComponents() {
        createTable();
    }

    public void createTable() {
        inventory_table = new JTable(new SortingTable(((Constants.inventoryList != null) ? Constants.inventoryList.getItemList() : null)));
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(inventory_table.getModel());
        inventory_table.setRowSorter(sorter);
        inventory_table.addMouseListener(getClickListener());
    }

    public JTextField getFilterBox() {
        return filter_box;
    }

    public List<Item> getSelectedItemList() {
        return selectedItemList;
    }

    public JTable getInventoryTable() {
        return inventory_table;
    }

    public JButton getEditButton() {
        return edit_button;
    }

    public JButton getDeleteButton() {
        return delete_button;
    }

    public void updateDisplayList() {
        getInventoryTable().clearSelection();
        displayList = Constants.inventoryList.filterList(filter_box.getText());
        ((SortingTable) inventory_table.getModel()).setDisplayedItemList(displayList);
        inventory_table.revalidate();
    }

    private void setSidebarInfo(Item item) {
        item_upc_field.setText(item.getUpc());
        item_name_field.setText(item.getName());
        item_product_code_field.setText(item.getProductCode());
        item_brand_field.setText(item.getBrand());
        item_price_field.setText("$" + item.getPrice());
        item_quantity_field.setText("" + item.getQuantity());
    }

    public void setInfo(String text) {
        TimeAtMoment timeAtMoment = new TimeAtMoment(System.currentTimeMillis());
        setProgramRunningInfoLabel("[" + timeAtMoment + "] " + text);
    }

    private void setProgramRunningInfoLabel(String text) {
        System.out.println(text);
        getProgramRunningInfoLabel().setText(text);
    }

    private JLabel getProgramRunningInfoLabel() {
        return program_running_info;
    }

    protected void handleSelectionEvent(ListSelectionEvent e, ListSelectionModel lsm) {
        if(e.getValueIsAdjusting())
            return;

        if(lsm.isSelectionEmpty())
            return;

        selectedItemList.clear();

        for(int i = lsm.getMinSelectionIndex(); i <= lsm.getMaxSelectionIndex(); i++) {
            if(lsm.isSelectedIndex(i) && (i < displayList.size())) {
                String upc = inventory_table.getValueAt(i, 0).toString();
                selectedItemList.add(Constants.inventoryList.filterList(upc).get(0));
            }
        }

        Main.inventoryPanel.setSidebarInfo(selectedItemList.get(selectedItemList.size() - 1));

        if(!selectedItemList.isEmpty()) {
            edit_button.setEnabled(true);
            delete_button.setEnabled(true);
        }
    }

    private ActionListener getInfoButtonListener() {
        return e -> {
            String out = Constants.inventoryList.getItemList().size() + " items in the database.";
            Utils.log(out);
        };
    }

    private MouseListener getClickListener() {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = inventory_table.rowAtPoint(e.getPoint());
                String upc = inventory_table.getValueAt(row, 0).toString();
                setSidebarInfo(Constants.inventoryList.getItem(upc));
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

    private ActionListener getEditListener() {
        return e -> Utils.makeEditItemFrame();
    }

    private ActionListener getAddListener() {
        return e -> Utils.makeAddItemFrame();
    }

    public void addItemToInventory(Item item) {
        Constants.inventoryList.add(item);
        updateDisplayList();
    }

    public void setItemToInventory(Item item, int index) {
        Constants.inventoryList.set(item, index);
        updateDisplayList();
    }

    private ActionListener getDeleteListener() {
        return e -> Utils.makeDeleteAction();
    }

    private FocusListener getFocusListener() {
        return new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if(filter_box.getText().equalsIgnoreCase(FILTER_BOX_HINT)) {
                    filter_box.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                updateDisplayList();
                if(filter_box.getText().equalsIgnoreCase("")) {
                    filter_box.setText(FILTER_BOX_HINT);
                }
            }
        };
    }

    private ActionListener getEnterListener() {
        return e -> updateDisplayList();
    }
}