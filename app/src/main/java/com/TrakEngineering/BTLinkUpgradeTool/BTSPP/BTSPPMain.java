package com.TrakEngineering.BTLinkUpgradeTool.BTSPP;

import static com.TrakEngineering.BTLinkUpgradeTool.UpgradeActivity.service;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.text.SpannableStringBuilder;
import android.util.Log;

import com.TrakEngineering.BTLinkUpgradeTool.BTSPP.BTSPP_Serial.SerialListener;
import com.TrakEngineering.BTLinkUpgradeTool.BTSPP.BTSPP_Serial.SerialSocket;

public class BTSPPMain implements SerialListener {

    public Activity activity;
    private static final String TAG = BTSPPMain.class.getSimpleName();
    private String newline = "\r\n";
    StringBuilder sb = new StringBuilder();

    @Override
    public void onSerialConnect() {
        BTConstants.BTLinkConnectionStatus = true;
        setStatus("Connected");
    }

    @Override
    public void onSerialConnectError(Exception e) {
        BTConstants.BTLinkConnectionStatus = false;
        setStatus("Disconnect");
        e.printStackTrace();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        BTConstants.BTLinkConnectionStatus = false;
        setStatus("Disconnect");
        Log.e(TAG, "BTSPPLink: SerialIoError: " + e.getMessage());
    }

    public void connect() {
        try {

            if (BTConstants.deviceAddress != null && !BTConstants.deviceAddress.isEmpty()) {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(BTConstants.deviceAddress);
                setStatus("Connecting...");
                SerialSocket socket = new SerialSocket(activity.getApplicationContext(), device);
                service.connect(socket);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(String str) {
        if (!BTConstants.BTLinkConnectionStatus) {
            BTConstants.CurrentCommand = "";
            Log.i(TAG, "BTSPPLink : Link not connected");
            return;
        }
        try {
            //Log command sent:str
            BTConstants.CurrentCommand = str;
            Log.i(TAG, "BTSPPLink : Requesting..." + str);
            byte[] data = (str + newline).getBytes();
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    public void sendBytes(byte[] data) {
        if (!BTConstants.BTLinkConnectionStatus) {
            BTConstants.CurrentCommand = "";
            Log.i(TAG, "BTSPPLink : Link not connected");
            return;
        }
        try {
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    public void receive(byte[] data) {
        String Response = new String(data);
        SpannableStringBuilder spn = new SpannableStringBuilder(Response + '\n');
        Log.i(TAG, "BTSPPLink : Request>>" + BTConstants.CurrentCommand);
        Log.i(TAG, "BTSPPLink : Response>>" + spn.toString());

        //==========================================
        if (BTConstants.CurrentCommand.equalsIgnoreCase(BTConstants.info_cmd) && Response.contains("records")) {
            BTConstants.isNewVersionLink = true;
        }
        if (Response.contains("$$")) {
            String res = Response.replace("$$", "");
            try {
                if (res.contains("}")) {
                    res = res.substring(0, (res.lastIndexOf("}") + 1)); // To remove extra characters after the last curly bracket (if any)
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!res.trim().isEmpty()) {
                sb.append(res.trim());
            }
            sendBroadcastIntentFromLink(sb.toString());
            sb.setLength(0);
        } else {
            if (BTConstants.isNewVersionLink) {
                sb.append(Response);
            } else {
                // For old version Link response
                sb.setLength(0);
                sendBroadcastIntentFromLink(spn.toString());
            }
        }
    }

    public void sendBroadcastIntentFromLink(String spn) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("BroadcastBlueLinkData");
        broadcastIntent.putExtra("Request", BTConstants.CurrentCommand);
        broadcastIntent.putExtra("Response", spn.trim());
        broadcastIntent.putExtra("Action", "BlueLink");
        activity.sendBroadcast(broadcastIntent);
    }

    public void setStatus(String str) {
        Log.i(TAG, "Status: " + str);
        BTConstants.BTStatusStr = str;
    }
}
