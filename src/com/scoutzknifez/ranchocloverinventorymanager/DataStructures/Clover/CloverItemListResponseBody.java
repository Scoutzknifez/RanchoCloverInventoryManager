package com.scoutzknifez.ranchocloverinventorymanager.DataStructures.Clover;

import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;

@Getter
public class CloverItemListResponseBody {
    private ArrayList<LinkedHashMap<String, String>> elements;
    private String href;
}