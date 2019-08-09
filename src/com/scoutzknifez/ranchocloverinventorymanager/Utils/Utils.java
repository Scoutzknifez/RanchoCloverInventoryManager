package com.scoutzknifez.ranchocloverinventorymanager.Utils;

import com.scoutzknifez.ranchocloverinventorymanager.Components.ScannerListener;
import com.scoutzknifez.ranchocloverinventorymanager.Components.ShortcutListener;
import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Item;
import com.scoutzknifez.ranchocloverinventorymanager.Forms.AddItem;
import com.scoutzknifez.ranchocloverinventorymanager.Forms.Inventory;
import com.scoutzknifez.ranchocloverinventorymanager.Main;
import com.scoutzknifez.ranchocloverinventorymanager.Workers.DeleteWorker;
import com.scoutzknifez.ranchocloverinventorymanager.Workers.GetWorker;
import com.scoutzknifez.ranchocloverinventorymanager.Workers.WorkerHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Utils {
    private static int oldSize = 0;
    private static void listRefresh() {
        // update the Main.inventoryList
        Result result = WorkerHandler.fetchInventory();
        if(result == Result.SUCCESS)
            Utils.log("Updated the inventory list. (Size " + oldSize + " -> " + Main.inventoryList.getItemList().size() + ")");
        else
            Utils.log("Failed to update the inventory list as scheduled.");

       oldSize = Main.inventoryList.getItemList().size();
    }

    private static void scheduleListRefresh() {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.schedule(() -> {
            // do refresh here
            listRefresh();

            // call this method again to reschedule an update later
            scheduleListRefresh();
        }, Constants.REFRESH_RATE_IN_MINUTES, TimeUnit.MINUTES);
    }

    public static void log(String input) {
        Main.inventoryPanel.setInfo(input);
    }

    public enum  OS {
        WINDOWS,
        LINUX,
        MAC,
        SOLARIS
    }

    public static OS getOS() {
        OS os = null;
        if(Constants.operatingSystem == null) {
            String system = System.getProperty("os.name").toLowerCase();
            if(system.contains("win"))
                os = OS.WINDOWS;
            else if(system.contains("nix") || system.contains("nux") || system.contains("aix"))
                os = OS.LINUX;
            else if(system.contains("mac"))
                os = OS.MAC;
            else if(system.contains("sunos"))
                os = OS.SOLARIS;
        }
        return os;
    }

    public static void checkScanned(String barcode) {
        Item foundItem = lookupItem(barcode);

        if(Inventory.itemFrameIsOpen) {
            // Edit the already open frame by finding if it is an edit frame or not
            Inventory.itemFrame.close();
        }

        // Open up a frame and set the elements to foundItem data
        if(foundItem.isItemIsInPhysical()) {
            // Is an edit frame
            AddItem newEditItemFrame = new AddItem(false);
            makeNewFrame(makeEditFrameTitle(foundItem), newEditItemFrame, foundItem);
        } else {
            // popup with new create frame and populate info with what is found.
            AddItem newCreateItemFrame = new AddItem(true);
            makeNewFrame(Constants.ADD_ITEM_TITLE, newCreateItemFrame, foundItem);
        }
    }
    public static void makeNewFrame(String title, AddItem addItem, Item item) {
        if(Inventory.itemFrame != null && Inventory.itemFrame != addItem)
            return;

        makeNewFrame(title, addItem);
        addItem.getUpcField().setText(item.getUpc());
        addItem.getItemNameField().setText(item.getName());
        addItem.getProductCodeField().setText(item.getProductCode());
        addItem.getBrandField().setText(item.getBrand());
        addItem.getPriceField().setText("$" + item.getPrice());
        addItem.getQuantityField().setText("" + item.getQuantity());
    }
    public static void makeNewFrame(String title, AddItem addItem) {
        if(Inventory.itemFrame != null && Inventory.itemFrame != addItem)
            return;

        Inventory.itemFrame = addItem;
        Inventory.itemFrameIsOpen = true;

        JPanel panel = addItem.getAddItemPanel();
        JFrame itemJFrame = new JFrame(title);
        itemJFrame.addWindowListener(AddItem.getWindowCloser(addItem));
        itemJFrame.setContentPane(panel);
        itemJFrame.pack();

        itemJFrame.setSize(Constants.ITEM_FRAME_WIDTH, Constants.ITEM_FRAME_HEIGHT);
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        itemJFrame.setLocation((int) dimension.getWidth()/2 - Constants.ITEM_FRAME_WIDTH/2, (int) dimension.getHeight()/2 - Constants.ITEM_FRAME_HEIGHT/2);
        itemJFrame.setMinimumSize(new Dimension(Constants.ITEM_FRAME_WIDTH/2, Constants.ITEM_FRAME_HEIGHT/2));

        itemJFrame.setVisible(true);
    }
    private static Item lookupItem(String barcode)
    {
        if(!Constants.hasInternet)
            showNoInternetDialog();

        barcode = barcode.trim();
        Item fetchedItem = checkDatabase(barcode, true);
        if(fetchedItem != null) {
            System.out.println("found in the actual db");
            return fetchedItem;
        }
        fetchedItem = checkDatabase(barcode, false);
        if(fetchedItem != null) {
            System.out.println("found in the whole db");
            return fetchedItem;
        }
        String url = Constants.WEBSITE + barcode;

        Document doc;
        String itemName = "";
        double price = 0;
        int quantity = 0;
        String brand = "";
        String description = "";
        Element attributes;
        String size = "";
        String length = "";
        String width = "";
        String height = "";
        String weight = "";
        String color = "";
        String productCode = "N/A";

        try {
            doc = Jsoup.connect(url).ignoreContentType(true).ignoreHttpErrors(true).timeout(6000).get();

            try {
                itemName = doc.select("h4").first().text();
            } catch(NullPointerException ne) {
                itemName = "N/A";
            }
            try {
                brand = getElementByText(doc.select("div.product-text-label"), "Manufacturer").child(0).text().trim();
            } catch(NullPointerException ne) {
                brand = "N/A";
            }
            try {
                description = getElementByText(doc.select("div.product-text-label"), "Description").child(0).text().trim();
            } catch(NullPointerException ne) {
                description = "N/A";
            }
            try {
                attributes = getElementByText(doc.select("div.product-text-label"), "Attributes").child(0);
                try {
                    size = getElementByText(attributes.children(), "Size").text().split(":")[1].trim();
                } catch(NullPointerException ne) {
                    size = "N/A";
                }
                try {
                    length = getElementByText(attributes.children(), "Length").text().split(":")[1].trim();
                } catch(NullPointerException ne) {
                    length = "N/A";
                }
                try {
                    width = getElementByText(attributes.children(), "Width").text().split(":")[1].trim();
                } catch(NullPointerException ne) {
                    width = "N/A";
                }
                try {
                    height = getElementByText(attributes.children(), "Height").text().split(":")[1].trim();
                } catch(NullPointerException ne) {
                    height = "N/A";
                }
                try {
                    weight = getElementByText(attributes.children(), "Weight").text().split(":")[1].trim();
                } catch(NullPointerException ne) {
                    weight = "N/A";
                }
                try {
                    color = getElementByText(attributes.children(), "Color").text().split(":")[1].trim();
                } catch(NullPointerException ne) {
                    color = "N/A";
                }
            } catch(NullPointerException ne) {
                attributes = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Item(barcode, productCode, itemName, brand, price, quantity, description, size, color, false);
    }
    public static Item checkDatabase(String barcode, boolean isPhysical)
    {
        GetWorker getter = new GetWorker(barcode, isPhysical);
        Thread thread = new Thread(getter);
        thread.start();
        try {
            thread.join();
            Item getItem = getter.getItem();
            if(getItem == null) {
                return null;
            } else {
                getItem.setItemIsInPhysical(isPhysical);
                return getItem;
            }
        } catch(Exception e) {
            Utils.log("Could not get item from database.");
            return null;
        }
    }
    public static Element getElementByText(Elements elements, String searched) {
        for(Element element : elements) {
            if(element.text().toLowerCase().contains(searched.toLowerCase()))
                return element;
        }
        return null;
    }

    public static void initializeApplication() {
        Constants.hasInternet = hasInternetConnection();
        if(Constants.hasInternet || Constants.TEST_MODE) {
            if(WorkerHandler.fetchInventory() == Result.SUCCESS || Constants.TEST_MODE) {
                showApplication();
                scheduleListRefresh();
            }
        } else {
            showNoInternetDialog();
        }
    }
    public static void showApplication() {
        JFrame frame = new JFrame("Rancho Army-Navy Inventory");
        Main.inventoryPanel = new Inventory();
        frame.setContentPane(Main.inventoryPanel.getInventoryPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();

        frame.setSize(Constants.MAIN_APPLICATION_WIDTH, Constants.MAIN_APPLICATION_HEIGHT);
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((int) dimension.getWidth()/2 - Constants.MAIN_APPLICATION_WIDTH/2, (int) dimension.getHeight()/2 - Constants.MAIN_APPLICATION_HEIGHT/2);
        frame.setMinimumSize(new Dimension(700, Constants.MAIN_APPLICATION_HEIGHT/2));

        frame.setVisible(true);

        // This listens to key input and sees if it was a scan or not
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(new ScannerListener());
        manager.addKeyEventDispatcher(new ShortcutListener());
    }
    public static void showFrameIsOpenError() {
        JOptionPane.showMessageDialog(null, "You are already working on an item.", "ERROR: Item Already In Work", JOptionPane.ERROR_MESSAGE);
    }
    private static void showNoInternetDialog() {
        JOptionPane.showMessageDialog(null, "This device has no internet access.  Please check your connection and restart the application.", "ERROR: No Internet Connection", JOptionPane.ERROR_MESSAGE);
    }
    public static boolean hasInternetConnection() {
        boolean status = false;
        Socket socket = new Socket();
        InetSocketAddress address = new InetSocketAddress("www.google.com", 80);
        try {
            socket.connect(address, 1500);
            if(socket.isConnected())
                status = true;
        } catch (Exception e) {
            status = false;
        } finally {
            try {
                socket.close();
            } catch (Exception e) {}
        }
        return status;
    }
    public static String makeEditFrameTitle(Item item) {
        return Constants.EDIT_ITEM_TITLE + " '" + item.getName() + "'";
    }
    public static void makeAddItemFrame() {
        if(Inventory.itemFrameIsOpen) {
            showFrameIsOpenError();
            return;
        }

        makeNewFrame(Constants.ADD_ITEM_TITLE, new AddItem(true));
    }
    public static void makeEditItemFrame() {
        if(Main.inventoryPanel.getSelectedItemList().size() != 1) {
            Main.inventoryPanel.getEditButton().setEnabled(false);
            return;
        }

        if(Inventory.itemFrameIsOpen) {
            Utils.showFrameIsOpenError();
            return;
        }
        Item item = Main.inventoryPanel.getSelectedItemList().get(0);

        AddItem itemFrame = new AddItem(false);

        itemFrame.getUpcField().setText(item.getUpc());
        itemFrame.getItemNameField().setText(item.getName());
        itemFrame.getProductCodeField().setText(item.getProductCode());
        itemFrame.getBrandField().setText(item.getBrand());
        itemFrame.getPriceField().setText("$" + item.getPrice());
        itemFrame.getQuantityField().setText(item.getQuantity() + "");
        itemFrame.getCreateItemButton().setText("Edit Item");

        Utils.makeNewFrame(Utils.makeEditFrameTitle(item), itemFrame);
    }
    public static void makeDeleteAction() {
        ArrayList<Item> selectedItemList = (ArrayList<Item>) Main.inventoryPanel.getSelectedItemList();

        if(selectedItemList.size() == 0) {
            Main.inventoryPanel.getDeleteButton().setEnabled(false);
            return;
        }

        // First get the item associated with index, then remove from main list, then reset the view list by filter
        String deleteString;

        if(selectedItemList.size() > 1)
            deleteString = "(" + selectedItemList.size() + ") items";
        else {
            deleteString = "'" + selectedItemList.get(0).getName() + "'";
        }

        int action = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete " + deleteString + "?", "Deletion Confirmation", JOptionPane.YES_NO_OPTION);

        if(action == JOptionPane.YES_OPTION) {
            try {
                ExecutorService multiThreadWaiter = Executors.newCachedThreadPool();
                for(Item item : selectedItemList) {
                    // This thread goes to the MySQL database and deletes the selected item.
                    int really = JOptionPane.showConfirmDialog(null, "Are you REALLY sure you want to delete " + deleteString + "?", "Deletion Confirmation", JOptionPane.YES_NO_OPTION);
                    if(really == JOptionPane.YES_OPTION)
                        multiThreadWaiter.execute(new DeleteWorker(item.getUpc()));
                }

                multiThreadWaiter.shutdown();

                // Remove all the items from the apps item list
                for(Item item : selectedItemList)
                    Main.inventoryList.remove(item);

                boolean finished = multiThreadWaiter.awaitTermination(1, TimeUnit.MINUTES);

                if(finished) {
                    Utils.log("Deleter threads have finished.");
                    Main.inventoryPanel.updateDisplayList();
                    Main.inventoryPanel.getDeleteButton().setEnabled(false);
                } else {
                    Utils.log("Deleter threads could not finish in time.");
                }
            } catch(Exception ex) {
                Utils.log("Could not make the deleter threads.");
            }
        }
    }
}