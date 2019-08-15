package com.scoutzknifez.ranchocloverinventorymanager.Workers.CloverWorkers;

import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Clover.CloverItem;
import com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Clover.CloverTag;
import com.scoutzknifez.ranchocloverinventorymanager.Utility.Utils;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class CloverItemTagWorker extends CloverWorkerParent {
    private CloverItem cloverItem;
    private CloverTag cloverTag;

    public CloverItemTagWorker(CloverItem cloverItem, CloverTag cloverTag) {
        setCloverItem(cloverItem);
        setCloverTag(cloverTag);
    }

    @Override
    public void run() {
        Utils.linkItemToLabel(getCloverItem(), getCloverTag());
    }
}