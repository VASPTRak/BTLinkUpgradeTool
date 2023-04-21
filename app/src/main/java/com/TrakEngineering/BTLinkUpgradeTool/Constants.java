package com.TrakEngineering.BTLinkUpgradeTool;

import java.util.ArrayList;
import java.util.HashMap;

public class Constants {

    public final static int REQUEST_ENABLE_BT = 1;
    public static final String SelectedLinkDetails = "SelectedLinkDetails";
    public static String CurrentNetworkType = "";
    public static ArrayList<HashMap<String, String>> LinkFirmwareList = new ArrayList<>();
    public static int selectedFirmwarePosition;
    public static String FOLDER_BIN = "FSBin";
}
