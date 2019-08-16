package com.scoutzknifez.ranchocloverinventorymanager.Utility;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.scoutzknifez.ranchocloverinventorymanager.Components.ScannerListener;
import com.scoutzknifez.ranchocloverinventorymanager.Components.ShortcutListener;
import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Clover.*;
import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Item;
import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.RequestType;
import com.scoutzknifez.ranchocloverinventorymanager.Forms.AddItem;
import com.scoutzknifez.ranchocloverinventorymanager.Forms.Inventory;
import com.scoutzknifez.ranchocloverinventorymanager.Main;
import com.scoutzknifez.ranchocloverinventorymanager.Workers.CloverWorkers.*;
import com.scoutzknifez.ranchocloverinventorymanager.Workers.MySQLWorkers.GetWorker;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Utils {
    public static void initializeApplication() {
        Constants.hasInternet = hasInternetConnection();
        if(Constants.hasInternet || Constants.TEST_MODE) {
            initializeFetchers();
            showApplication();
            scheduleListRefresh();
        } else {
            showNoInternetDialog();
        }
    }

    private static void initializeFetchers() {
        Thread cloverTagFetcherThread = grabCloverTags();
        Thread cloverItemFetcherThread = grabCloverInventory();

        try {
            cloverTagFetcherThread.join();
            System.out.println("Tag List Size: " + Constants.cloverTagList.getObjectList().size());
            cloverItemFetcherThread.join();
            System.out.println("Item List Size: " + Constants.cloverInventoryList.getObjectList().size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void postItem(Item item) {
        CloverItem createdCloverItem = new CloverItem(item.getName(), item.getUpc(), item.getProductCode(), Utils.makeLong(item.getPrice()));
        CloverInsertWorker insertWorker = new CloverInsertWorker(createdCloverItem);
        Thread thread = new Thread(insertWorker);
        thread.start();

        CloverItem responseItem = null;
        try {
            thread.join();
            responseItem = insertWorker.getCloverItem();
            Main.inventoryPanel.addItemToInventory(item);
            Main.inventoryPanel.updateDisplayList();
        } catch(Exception ex) {
            Utils.log("Error running the inserter thread.");
        }

        if(responseItem == null)
            throw new RuntimeException("Item that was posted returned as null!");

        CloverTag cloverTag = getBrandTag(item);

        CloverItemTagWorker cloverItemTagWorker = new CloverItemTagWorker(responseItem, cloverTag);
        Thread tagThread = new Thread(cloverItemTagWorker);
        tagThread.start();

        CloverQuantityWorker cloverQuantityWorker = new CloverQuantityWorker(responseItem, item.getQuantity());
        Thread quantityThread = new Thread(cloverQuantityWorker);
        quantityThread.start();

        try {
            tagThread.join();
        } catch (Exception e) {
            Utils.log("Error running the tagger thread.");
        }

        try {
            quantityThread.join();
        } catch (Exception e) {
            Utils.log("Error running the quantity thread.");
        }
    }

    public static CloverItem getCloverItemFromItem(Item item) {
        return Constants.cloverInventoryList.getCloverItem(item.getUpc());
    }

    public static CloverTag getBrandTag(Item item) {
        if(!Constants.cloverTagList.contains(item.getBrand())) {
            return postTag(new CloverTag(item.getBrand()));
        }
        return Constants.cloverTagList.getCloverTag(item.getBrand());
    }

    public static void setItemQuantity(CloverItem cloverItem, int quantity) {
        String[] args = new String[1];
        args[0] = "item_stocks/" + cloverItem.getId();
        Response response = runRequest(buildRequest(RequestType.POST, getQuantityString(quantity), args));
        closeResponseBody(response);
    }

    private static Object getQuantityString(int quantity) {
        return "{\"quantity\":" + quantity + "}";
    }

    public static void saveData() {
        try {
            sortCloverItemList();

            ObjectWriter writer = Constants.OBJECT_MAPPER.writer(new DefaultPrettyPrinter());
            writer.writeValue(new File("Data.json"), Constants.cloverInventoryList.getObjectList());
        } catch (Exception e) {
            System.out.println("Could not make the data file!");
            e.printStackTrace();
        }
    }

    public static void loadData() {
        try {
            ArrayList<LinkedHashMap<String, Object>> map = Constants.OBJECT_MAPPER.readValue(new FileInputStream("Data.json"), ArrayList.class);
            ArrayList<CloverItem> cloverItems = parseList(map);
            for(CloverItem cloverItem : cloverItems)
                Constants.cloverInventoryList.add(cloverItem);
        } catch (Exception e) {
            System.out.println("Could not load data file!");
            e.printStackTrace();
        }
    }

    public static void sortCloverItemList() {
        // Sort before saving file to json
        Constants.cloverInventoryList.getObjectList().sort((o1, o2) -> {
            try {
                CloverItem c1 = (CloverItem) o1;
                CloverItem c2 = (CloverItem) o2;

                if(c1 == null || c2 == null || c1.getSku() == null || c2.getSku() == null)
                    return 0;

                int result = String.CASE_INSENSITIVE_ORDER.compare(c1.getSku(), c2.getSku());
                if(result == 0) {
                    result = c1.getSku().compareTo(c2.getSku());
                }
                return result;
            } catch (Exception e) {
                System.out.println("Could not compare clover items!");
                e.printStackTrace();
            }
            return 0;
        });
    }

    public static long makeLong(double d) {
        return ((long) (d * 100.000005));
    }

    private static Thread grabCloverTags() {
        Thread thread = new Thread(Utils::getCloverTags);
        thread.start();
        return thread;
    }

    private static Thread grabCloverInventory() {
        Thread thread = new Thread(CloverWorkerHandler::fetchInventory);
        thread.start();
        return thread;
    }

    private static Item getItemFromIndex(int index) {
        int maxSize = Constants.cloverInventoryList.getObjectList().size();
        if(index < maxSize)
            return ((Item) Constants.cloverInventoryList.get(index));
        else
            return ((Item) Constants.cloverInventoryList.get(maxSize - 1));
    }

    private static String getUpcOfIndex(int index) {
        return getItemFromIndex(index).getUpc();
    }

    public static ArrayList<CloverItem> parseList(ArrayList<LinkedHashMap<String, Object>> list) {
        ArrayList<CloverItem> cloverItems = new ArrayList<>();

        for(int i = list.size() - 1; i >= 0; i--) {
            LinkedHashMap<String, Object> mapping = list.get(i);

            String id = "";
            Object idObject = mapping.get("id");
            if(idObject instanceof String)
                id = (String) idObject;

            String name = "";
            Object nameObject = mapping.get("name");
            if(nameObject instanceof String)
                name = (String) nameObject;

            String sku = "";
            Object skuObject = mapping.get("sku");
            if(skuObject instanceof String)
                sku = (String) skuObject;

            String code = "";
            Object codeObject = mapping.get("code");
            if(codeObject instanceof String)
                code = (String) codeObject;

            Object obj = mapping.get("price");

            long price;
            if(obj instanceof Integer) {
                Integer integer = (Integer) obj;
                price = (long) integer;
            } else {
                String string = (String) obj;
                price = Long.parseLong(string);
            }

            Object tags = mapping.get("tags");
            Object itemStock = mapping.get("itemStock");

            try {
                cloverItems.add(new CloverItem(id, name, sku, code, price, tags, itemStock));
            } catch (Exception e) {
                System.out.println("Could not parse item into clover Item.");
            }
        }
        return cloverItems;
    }

    public static void linkItemToLabel(CloverItem item, CloverTag tag) {
        Object string = getItemLabelString(item, tag);
        Request request = buildRequest(RequestType.POST, string, "tag_items");
        Response response = runRequest(request);

        closeResponseBody(response);
    }

    public static void closeResponseBody(Response response) {
        try {
            response.body().close();
        } catch (Exception e) {
            System.out.println("Could not close the response body.");
        }
    }

    public static Result getCloverTags() {
        String[] args = new String[2];
        args[0] = "tags";
        args[1] = "limit=1000";
        Request request = buildRequest(RequestType.GET, args);

        Constants.cloverTagList.getObjectList().clear();
        Response response = runRequest(request);
        if(response != null) {
            try {
                CloverTagListResponseBody cloverTagListResponseBody = Constants.OBJECT_MAPPER.readValue(response.body().string(), CloverTagListResponseBody.class);
                ArrayList<LinkedHashMap<String, String>> unparsedTagList = cloverTagListResponseBody.getElements();
                for(LinkedHashMap<String, String> mapping : unparsedTagList) {
                    String id = mapping.get("id");
                    String name = mapping.get("name");
                    boolean showInReporting = Boolean.parseBoolean(mapping.get("showInReporting"));
                    Constants.cloverTagList.add(new CloverTag(id, name, showInReporting));
                }

                return Result.SUCCESS;
            } catch(Exception e) {
                System.out.println("Could not parse the clover tag list.");
                e.printStackTrace();
            }
        }
        closeResponseBody(response);

        return Result.FAILURE;
    }

    public static Response runRequest(Request request) {
        Response response = callRequest(request);

        if(response == null || isError429(response)) {
            try {
                System.out.println("Calling back in a second. Currently at max connections.");
                try {
                    response.body().close();
                } catch (Exception e) {
                    System.out.println("Response body could not be closed.");
                    e.printStackTrace();
                }
                Thread.sleep(Constants.MILLIS_IN_SECOND);
                return runRequest(request);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if(!isResponseValid(response)) {
                try {
                    response.body().close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                throw makeResponseError(response);
            }

            return response;
        }
        return null;
    }

    private static Response callRequest(Request request) {
        try {
            return Constants.HTTP_CLIENT.newCall(request).execute();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Used for getters only
     * @param requestType always GET
     * @param apiSection arguments for type of get
     * @return request
     */
    public static Request buildRequest(RequestType requestType, String... apiSection) {
        return buildRequest(requestType, "", apiSection);
    }

    /**
     * Used for building requests for getters and setters,
     * directly accessed for posts ONLY
     * @param requestType Type of request for header
     * @param jsonable Any type of object
     * @param apiSection arguments for html request
     * @return Built up Request
     */
    public static Request buildRequest(RequestType requestType, Object jsonable, String... apiSection) {
        Request.Builder builder = new Request.Builder();
        String url = buildUrl(apiSection);
        builder = builder.url(url);
        if(requestType == RequestType.GET)
            builder = builder.get();
        else if(requestType == RequestType.POST) {
            String jsonString = "";
            try {
                if(!(jsonable instanceof String))
                    jsonString = Constants.OBJECT_MAPPER.writeValueAsString(jsonable);
                else
                    jsonString = (String) jsonable;
            } catch (Exception e) {
                System.out.println("Could not make the string for " + jsonable);
            }
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), jsonString);
            builder = builder.post(requestBody);
        }
        else if(requestType == RequestType.DELETE) {
            builder = builder.delete();
        }

        builder = builder.header("accept", "application/json")
                .header("content-type", "application/json");

        return builder.build();
    }

    private static CloverTag postTag(CloverTag tag) {
        Request request = buildRequest(RequestType.POST, tag, "tags");
        try {
            Response response = runRequest(request);
            String responseBody = response.body().string();
            return Constants.OBJECT_MAPPER.readValue(responseBody, CloverTag.class);
        } catch (Exception e) {
            System.out.println("Could not parse the response body for the returning CloverTag post.");
            e.printStackTrace();
        }
        return null;
    }

    public static void printList(List<Object> list) {
        for(Object object : list)
            System.out.println(object);
    }

    public static void printResponseBody(Response response) {
        try {
            System.out.println(response.body().string());
        } catch (Exception e) {
            System.out.println("Could not print the response body.");
        }
    }

    private static boolean isError429(Response response) {
        return response.code() == 429;
    }

    private static boolean isResponseValid(Response response) {
        return (response != null && response.code() == 200);
    }

    private static RuntimeException makeResponseError(Response response) {
        try {
            System.out.println(response.body().string());
        } catch (Exception e) {
            System.out.println("Can not print out the response body of error.");
        }
        return new RuntimeException("Response came back with error code: " + response.code());
    }

    private static Object getItemLabelString(CloverItem item, CloverTag tag) {
        return "{\"elements\":[{ \"item\":{\"id\":\"" + item.getId() + "\"}, \"tag\":{\"id\":\"" + tag.getId() + "\"} }]}";
    }

    private static String buildUrl(String... args) {
        String baseURL = Constants.WEBSITE_URL + Constants.MERCHANT_ID + "/" + args[0] + Constants.API_TOKEN;

        for (int i = 1; i < args.length; i++) {
            baseURL += "&" + args[i];
        }

        return baseURL;
    }

    private static int oldSize = 0;
    private static int oldTagSize = 0;
    private static void listRefresh() {
        // update the Constants.cloverTagList
        Result result = getCloverTags();
        if(result == Result.SUCCESS)
            Utils.log("Updated the tag list. (Size " + oldTagSize + " -> " + Constants.cloverTagList.getObjectList().size() + ")");
        else
            Utils.log("Failed to update the tag list as scheduled.");

        oldTagSize = Constants.cloverTagList.getObjectList().size();

        // update the Constants.inventoryList
        result = CloverWorkerHandler.fetchInventory();
        if(result == Result.SUCCESS)
            Utils.log("Updated the inventory list. (Size " + oldSize + " -> " + Constants.inventoryList.getItemList().size() + ")");
        else
            Utils.log("Failed to update the inventory list as scheduled.");

       oldSize = Constants.inventoryList.getItemList().size();
    }

    private static void scheduleListRefresh() {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.schedule(() -> {
            // do refresh here
            listRefresh();
            Main.inventoryPanel.updateDisplayList();

            // call this method again to reschedule an update later
            scheduleListRefresh();
        }, Constants.REFRESH_RATE_IN_MINUTES, TimeUnit.MINUTES);
    }

    public static void log(String input) {
        if(Main.inventoryPanel != null) {
            Main.inventoryPanel.setInfo(input);
        }
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
    private static void makeNewFrame(String title, AddItem addItem) {
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
        Item fetchedItem = Constants.inventoryList.getItem(barcode);
        if(fetchedItem != null) {
            fetchedItem.setItemIsInPhysical(true);
            Utils.log("Could item already existing in inventory.");
            return fetchedItem;
        }

        fetchedItem = checkDatabase(barcode, true);
        if(fetchedItem != null) {
            Utils.log("Found item already existing in existing data.");
            return fetchedItem;
        }

        fetchedItem = checkDatabase(barcode, false);
        if(fetchedItem != null) {
            Utils.log("Found item already existing in backup data.");
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

        Utils.log("Found item data online.");
        return new Item(barcode, productCode, itemName, brand, price, quantity, description, size, color, false);
    }
    private static Item checkDatabase(String barcode, boolean isPhysical)
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
                getItem.setItemIsInPhysical(false);
                return getItem;
            }
        } catch(Exception e) {
            Utils.log("Could not get item from database.");
            return null;
        }
    }
    private static Element getElementByText(Elements elements, String searched) {
        for(Element element : elements) {
            if(element.text().toLowerCase().contains(searched.toLowerCase()))
                return element;
        }
        return null;
    }

    private static void showApplication() {
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
    private static void showFrameIsOpenError() {
        JOptionPane.showMessageDialog(null, "You are already working on an item.", "ERROR: Item Already In Work", JOptionPane.ERROR_MESSAGE);
    }
    private static void showNoInternetDialog() {
        JOptionPane.showMessageDialog(null, "This device has no internet access.  Please check your connection and restart the application.", "ERROR: No Internet Connection", JOptionPane.ERROR_MESSAGE);
    }
    private static boolean hasInternetConnection() {
        boolean status = false;
        Socket socket = new Socket();
        InetSocketAddress address = new InetSocketAddress("www.google.com", 80);
        try {
            socket.connect(address, 1500);
            if(socket.isConnected())
                status = true;
        } catch (Exception e) {
        } finally {
            try {
                socket.close();
            } catch (Exception e) {}
        }
        return status;
    }
    private static String makeEditFrameTitle(Item item) {
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
                    if(really == JOptionPane.YES_OPTION) {
                        CloverItem cloverItem = Constants.cloverInventoryList.getCloverItem(item.getUpc());
                        multiThreadWaiter.execute(new CloverDeleteWorker(cloverItem));
                    }
                }

                multiThreadWaiter.shutdown();

                // Remove all the items from the apps item list
                for(Item item : selectedItemList)
                    Constants.inventoryList.remove(item);

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