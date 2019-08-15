package com.scoutzknifez.ranchocloverinventorymanager.Components;

import com.scoutzknifez.ranchocloverinventorymanager.Main;
import com.scoutzknifez.ranchocloverinventorymanager.Utility.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

import static com.scoutzknifez.ranchocloverinventorymanager.Main.inventoryPanel;

public class ShortcutListener implements KeyEventDispatcher {
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if(e.getID() == KeyEvent.KEY_PRESSED)
            processKeyInput(e);

        return false;
    }

    private void processKeyInput(KeyEvent e) {
        if(e.getSource().equals(inventoryPanel.getFilterBox()))
            return;

        if(!onInventoryScreen((Component) e.getSource()))
            return;

        if(e.isControlDown())
            return;

        int kc = e.getKeyCode();

        if(kc == 65) {
            Utils.makeAddItemFrame();
        }
        else if(kc == 68 || kc == 127) {
            Utils.makeDeleteAction();
        }
        else if(kc == 69) {
            Utils.makeEditItemFrame();
        }
    }

    private boolean onInventoryScreen(Component component) {
        JFrame componentParent = getJFrameParent(component);
        JFrame inventoryScreen = getJFrameParent(Main.inventoryPanel.getInventoryPanel());

        return inventoryScreen.equals(componentParent);
    }

    private JFrame getJFrameParent(Component component) {
        while(!(component instanceof JFrame) && (component.getParent() != null)) {
            component = component.getParent();
        }
        return (component instanceof JFrame) ? ((JFrame) component): null;
    }
}