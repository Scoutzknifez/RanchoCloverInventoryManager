package com.scoutzknifez.ranchocloverinventorymanager.Utils;

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
import com.scoutzknifez.ranchocloverinventorymanager.Workers.DeleteWorker;
import com.scoutzknifez.ranchocloverinventorymanager.Workers.GetWorker;
import com.scoutzknifez.ranchocloverinventorymanager.Workers.WorkerHandler;
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
import java.util.stream.Collectors;

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

    public static void testUrl() {
        String[] args = new String[2];
        args[0] = "items/";
        args[1] = makeFilterExactSku("730176357294");
        System.out.println(buildUrl(args));
    }


    public static void initializeFetchers() {
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
    public static void printRequiredTags() {
        String listOfTags = "";
        for(Object object : Constants.cloverTagList.getObjectList()) {
            if(!listOfTags.equalsIgnoreCase(""))
                listOfTags += ", ";
            CloverTag cloverTag = (CloverTag) object;
            listOfTags += cloverTag.getName();
        }
        System.out.println("Current Labels: " + listOfTags);

        HashMap<String, Integer> brandToItemCount = new HashMap<>();
        listOfTags = "";
        int lineCount = 1;
        for(Object object : Constants.cloverInventoryList.getObjectList()) {
            Item item = (Item) object;
            if(!Constants.cloverTagList.contains(item.getBrand())) {
                if(!listOfTags.equalsIgnoreCase(""))
                    listOfTags += ", ";

                if(listOfTags.length() > 150 * lineCount) {
                    listOfTags += "\n";
                    lineCount++;
                }

                listOfTags += item.getBrand();
                brandToItemCount.put(item.getBrand(), 1);
            } else {
                if(brandToItemCount.get(item.getBrand()) == null)
                    brandToItemCount.put(item.getBrand(), 1);
                else {
                    int currentValue = brandToItemCount.get(item.getBrand()) + 1;
                    brandToItemCount.put(item.getBrand(), currentValue);
                }
            }
        }

        Map<String, Integer> sorted = sortByValue(brandToItemCount, false);
        for (String string : sorted.keySet()) {
            System.out.println(string + " - " + sorted.get(string));
        }
    }

    public static void syncItems() {
        sortCloverItemList();
        // Posts new items that didnt exist before
        postItems();
        sortCloverItemList();
        // Deletes items that were removed off of master
        deleteItems();
        sortCloverItemList();
        // updates changed items
        updateItems();
        sortCloverItemList();
    }

    private static void updateItems() {
        try {
            int index = 0;
            while(index < Constants.cloverInventoryList.getObjectList().size()) {
                CloverItem cloverItem = (CloverItem) Constants.cloverInventoryList.get(index);
                Item item = (Item) Constants.cloverInventoryList.get(index);

                if(cloverItem.equalsSku(item)) {
                    if(cloverItem.needsUpdate(item)) {
                        // Lets update with new stuff
                        updateItem(cloverItem, item, index);
                    }
                } else {
                    System.out.println("Items are off sync I guess");
                }

                index++;
            }
        } catch (Exception e) {
            System.out.println("Could not update the items in clover.");
            e.printStackTrace();
        }
    }

    private static void updateItem(CloverItem cloverItem, Item item, int index) {
        if(Constants.TEST_MODE)
            return;

        String[] args = new String[1];
        args[0] = "items/" + cloverItem.getId();
        CloverItemUpdateBody updatedCloverItemBody = new CloverItemUpdateBody(item.getName(), item.getProductCode(), makeLong(item.getPrice()));
        Request request = buildRequest(RequestType.POST, updatedCloverItemBody, args);
        Response response = runRequest(request);
        if(response != null) {
            System.out.println("Updating item:");
            System.out.println(cloverItem.getName() + " -> " + updatedCloverItemBody.getName());
            System.out.println(cloverItem.getCode() + " -> " + updatedCloverItemBody.getCode());
            System.out.println(cloverItem.getPrice() + " -> " + updatedCloverItemBody.getPrice());
            ((CloverItem) Constants.cloverInventoryList.getObjectList().get(index)).setName(item.getName());
            ((CloverItem) Constants.cloverInventoryList.getObjectList().get(index)).setCode(item.getProductCode());
            ((CloverItem) Constants.cloverInventoryList.getObjectList().get(index)).setPrice(makeLong(item.getPrice()));
        } else {
            System.out.println("Updating item failed.");
        }
    }

    public static void checkDuplicates() {
        int index = 0;
        while(index < Constants.cloverInventoryList.getObjectList().size() - 1) {
            CloverItem cloverItem = (CloverItem) Constants.cloverInventoryList.get(index);
            CloverItem nextCloverItem = (CloverItem) Constants.cloverInventoryList.get(index+1);
            if(cloverItem.equalsSku(nextCloverItem)) {
                // Lets delete the item (nextCloverItem) because its not unique
                deleteItem(nextCloverItem);
                System.out.println("Deleting item: " + nextCloverItem.getSku() + "> " + nextCloverItem.getName());
                Constants.cloverInventoryList.getObjectList().remove(index+1);
            } else {
                index++;
            }
        }
    }

    private static void deleteItems() {
        try {
            int index = 0;
            boolean isDeleting = true;
            while(isDeleting) {
                CloverItem cloverItem = (CloverItem) Constants.cloverInventoryList.get(index);
                Item item = (Item) Constants.cloverInventoryList.get(index);

                if(!cloverItem.equalsSku(item)) {
                    deleteItem(cloverItem);
                } else {
                    index++;
                }

                if(Constants.cloverInventoryList.getObjectList().size() == index)
                    isDeleting = false;
            }
        } catch (Exception e) {
            System.out.println("Can not delete items from clover.");
            e.printStackTrace();
        }
    }

    private static void deleteItem(CloverItem cloverItem) {
        if(Constants.TEST_MODE)
            return;

        if(cloverItem.getId().equalsIgnoreCase(""))
            throw new RuntimeException("Can not delete an item with not clover ID!");

        String[] args = new String[1];
        args[0] = "items/" + cloverItem.getId();
        Request request = buildRequest(RequestType.DELETE, args);
        Response response = runRequest(request);
        if(response != null) {
            Constants.cloverInventoryList.remove(cloverItem);
        }
    }

    public static void checkReverse() {
        try {
            CloverItem cloverItem = (CloverItem) Constants.cloverInventoryList.get(0);
            Item item = (Item) Constants.cloverInventoryList.get(0);
            if(!cloverItem.equalsItem(item)) {
                Collections.reverse(Constants.cloverInventoryList.getObjectList());
            }
        } catch (Exception e) {
            System.out.println("Failed to check if needing reverse");
        }
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

    public static void postItems() {
        if(Constants.TEST_MODE)
            return;

        for(int i = 0; i < Constants.cloverInventoryList.getObjectList().size(); i++) {
            Object object = Constants.cloverInventoryList.get(i);
            if (object instanceof Item) {
                Item item = (Item) object;
                if(!Constants.cloverInventoryList.contains(item.getUpc())) {
                    CloverItem cloverItem = new CloverItem(item.getName(), item.getUpc(), item.getProductCode(), makeLong(item.getPrice()));
                    postItem(cloverItem);
                    System.out.println("Posted item " + i);
                    System.out.println(cloverItem.getName());
                }
            }
        }
    }

    public static long makeLong(double d) {
        return ((long) (d * 100.000005));
    }

    public static void linkItems() {
        for(int i = 0; i < Constants.cloverInventoryList.getObjectList().size(); i++) {
            CloverItem cloverItem = (CloverItem) Constants.cloverInventoryList.get(i);
            CloverTag cloverTag = null;
            for(Object object : Constants.cloverInventoryList.getObjectList()) {
                if(object instanceof Item) {
                    Item item = (Item) object;
                    if(cloverItem.equalsItem(item)) {
                        cloverTag = getItemsTag(item);
                    }
                }
            }
            if(cloverTag != null) {
                linkItemToLabel(cloverItem, cloverTag);
                System.out.println("Linked item " + cloverItem.getName() + " (" + cloverItem.getId() + ") with " + cloverTag.getName() + " (" + cloverTag.getId() + ")");
            }
        }
    }

    public static CloverTag getItemsTag(Item item) {
        for (Object object : Constants.cloverTagList.getObjectList()) {
            if (object instanceof CloverTag) {
                CloverTag cloverTag = (CloverTag) object;
                if (cloverTag.getName().equals(item.getBrand())) {
                    return cloverTag;
                }
            }
        }
        return new CloverTag("N/A");
    }

    public static void makeNewTagsAndPost() {
        String listOfTags = "";
        for(Object object : Constants.cloverTagList.getObjectList()) {
            if(!listOfTags.equalsIgnoreCase(""))
                listOfTags += ", ";
            CloverTag cloverTag = (CloverTag) object;
            listOfTags += cloverTag.getName();
        }
        System.out.println("Current Labels: " + listOfTags);

        HashMap<String, Integer> brandToItemCount = new HashMap<>();
        listOfTags = "";
        int lineCount = 1;
        for(Object object : Constants.cloverInventoryList.getObjectList()) {
            Item item = (Item) object;
            if(!Constants.cloverTagList.contains(item.getBrand())) {
                if(!listOfTags.equalsIgnoreCase(""))
                    listOfTags += ", ";

                if(listOfTags.length() > 150 * lineCount) {
                    listOfTags += "\n";
                    lineCount++;
                }

                listOfTags += item.getBrand();
                brandToItemCount.put(item.getBrand(), 1);
            } else {
                if(brandToItemCount.get(item.getBrand()) == null)
                    brandToItemCount.put(item.getBrand(), 1);
                else {
                    int currentValue = brandToItemCount.get(item.getBrand()) + 1;
                    brandToItemCount.put(item.getBrand(), currentValue);
                }
            }
        }

        Map<String, Integer> sorted = sortByValue(brandToItemCount, false);

        java.util.List<CloverTag> tagList = new ArrayList<>();
        for (String string : sorted.keySet()) {
            System.out.println(string + " - " + sorted.get(string));
            tagList.add(new CloverTag(string));
        }


        System.out.println("Labels to create:");
        for (CloverTag tag : tagList) {
            if(!Constants.cloverTagList.contains(tag.getName())) {
                System.out.println(tag.getName());
                postTag(tag);
            }
        }
    }

    private static Map<String, Integer> sortByValue(Map<String, Integer> unsortMap, final boolean order)
    {
        java.util.List<Map.Entry<String, Integer>> list = new LinkedList<>(unsortMap.entrySet());

        // Sorting the list based on values
        list.sort((o1, o2) -> order ? o1.getValue().compareTo(o2.getValue()) == 0
                ? o1.getKey().compareTo(o2.getKey())
                : o1.getValue().compareTo(o2.getValue()) : o2.getValue().compareTo(o1.getValue()) == 0
                ? o2.getKey().compareTo(o1.getKey())
                : o2.getValue().compareTo(o1.getValue()));
        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

    }

    public static Thread grabCloverTags() {
        Thread thread = new Thread(Utils::getCloverTags);
        thread.start();
        return thread;
    }

    public static Thread grabCloverInventory() {
        Thread thread = new Thread(WorkerHandler::fetchInventory);
        thread.start();
        return thread;
    }

    public static CloverItem postItem(CloverItem item) {
        if(Constants.TEST_MODE)
            return null;

        Request request = buildRequest(RequestType.POST, item, "items");
        Response response = runRequest(request);
        try {
            String body = response.body().string();
            CloverItem cloverItem = Constants.OBJECT_MAPPER.readValue(body , CloverItem.class);
            Constants.cloverInventoryList.add(cloverItem);
            return cloverItem;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

    public static void getCloverItemListPerfectly() {
        try {
            for(int i = 0; i < Constants.cloverInventoryList.getObjectList().size(); i++) {
                String[] args = new String[3];
                args[0] = "items/";
                args[1] = "limit=1000";
                args[2] = makeFilterExactSku(getUpcOfIndex(i));
                Request request = buildRequest(RequestType.GET, args);
                Response response = runRequest(request);
                if(response != null) {
                    CloverItemListResponseBody cloverItemListResponseBody = Constants.OBJECT_MAPPER.readValue(response.body().string(), CloverItemListResponseBody.class);
                    ArrayList<LinkedHashMap<String, Object>> unparsedItemList = cloverItemListResponseBody.getElements();
                    ArrayList<CloverItem> items = parseList(unparsedItemList);
                    for(CloverItem cloverItem : items) {
                        Constants.cloverInventoryList.add(cloverItem);
                    }
                }
                System.out.println("Currently at item " + i + "/" + Constants.cloverInventoryList.getObjectList().size());
            }
        } catch (Exception e) {
            System.out.println("Could not get the perfect list!");
            e.printStackTrace();
        }
        sortCloverItemList();
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

    private static String makeFilterBySku(String sku) {
        return "filter=sku<=" + sku;
    }

    private static String makeFilterExactSku(String sku) {
        return "filter=sku=" + sku;
    }

    public static void linkItemToLabel(CloverItem item, CloverTag tag) {
        Request request = buildRequest(RequestType.POST, getItemLabelString(item, tag), "tag_items");
        Response response = runRequest(request);
        try {
            response.body().close();
        } catch (Exception e) {
            System.out.println("Could not close the response body.");
        }
    }

    public static void getCloverTags() {
        String[] args = new String[2];
        args[0] = "tags";
        args[1] = "limit=1000";
        Request request = buildRequest(RequestType.GET, args);
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
            } catch(Exception e) {
                System.out.println("Could not parse the clover tag list.");
                e.printStackTrace();
            }
        }
    }

    public static Response runRequest(Request request) {
        Response response = callRequest(request);

        if(isError429(response)) {
            try {
                System.out.println("Calling back in a second. Currently at max connections.");
                try {
                    response.body().close();
                } catch (Exception e) {
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

    private static Request buildRequest(RequestType requestType, Object jsonable, String... apiSection) {
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

    public static void postTag(CloverTag tag) {
        Request request = buildRequest(RequestType.POST, tag, "tags");
        runRequest(request);
    }

    public static void printList(List<Object> list) {
        for(Object object : list)
            System.out.println(object);
    }

    private static void printResponseBody(Response response) {
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
    private static void listRefresh() {
        // update the Constants.inventoryList
        Result result = WorkerHandler.fetchInventory();
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