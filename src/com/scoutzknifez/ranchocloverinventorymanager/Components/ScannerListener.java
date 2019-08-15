package com.scoutzknifez.ranchocloverinventorymanager.Components;

import com.scoutzknifez.ranchocloverinventorymanager.Forms.Inventory;
import com.scoutzknifez.ranchocloverinventorymanager.Main;
import com.scoutzknifez.ranchocloverinventorymanager.Utility.Constants;
import com.scoutzknifez.ranchocloverinventorymanager.Utility.Utils;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ScannerListener implements KeyEventDispatcher {
    public volatile static ArrayList<Key> recentInput = new ArrayList<>();
    private volatile static boolean onCooldown = false;

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if(e.getID() == KeyEvent.KEY_PRESSED)
            processKeyInput(e);

        return false;
    }
    private void processKeyInput(KeyEvent e) {
        int kc = e.getKeyCode();

        if(kc == 27 || kc == 8 || kc == 32 || kc == 16 || kc == 17 || kc == 18 || kc == 127 || kc == 37 || kc == 38 || kc == 39 || kc == 40)
            return;

        if(kc == 10 && Inventory.itemFrameIsOpen)
            Inventory.itemFrame.doSubmission();

        if(kc != 10) {
            char eventKey = e.getKeyChar();
            Key key = new Key(eventKey);
            recentInput.add(key);
            scheduleRemoval(key);
        }
    }
    private void scheduleRemoval(Key key) {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.schedule(() -> {
            if(recentInput.size() < 1)
                return;

            checkInputList();

            if(recentInput.contains(key))
                recentInput.clear();

        }, 200, TimeUnit.MILLISECONDS);
    }
    private void checkInputList() {
        String scanned = "";
        for(int i = 0; i < recentInput.size(); i++) {
            scanned = scanned + recentInput.get(i).getKey();
        }

        if(onCooldown ||  scanned.length() < 6)
            return;

        onCooldown = true;
        if(Constants.operatingSystem == Utils.OS.WINDOWS)
            Main.inventoryPanel.getFilterBox().setText("");

        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.schedule(() -> {
            onCooldown = false;
        }, 300, TimeUnit.MILLISECONDS);

        Utils.checkScanned(scanned);
    }
    private class Key {
        char character;
        private Key(char in) {
            character = in;
        }
        private char getKey() {
            return character;
        }
    }
}