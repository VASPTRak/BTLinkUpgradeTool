package com.TrakEngineering.BTLinkUpgradeTool.BTSPP.BTSPP_Serial;

public interface SerialListener {

    void onSerialConnect();
    void onSerialConnectError(Exception e);
    void onSerialRead(byte[] data);
    void onSerialIoError(Exception e);
}
