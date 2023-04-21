package com.TrakEngineering.BTLinkUpgradeTool;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.TrakEngineering.BTLinkUpgradeTool.BTSPP.BTConstants;

import java.util.ArrayList;
import java.util.Set;

public class PairDeviceActivity extends AppCompatActivity implements LinkSelectionListener {

    private static final String TAG = "PairDeviceActivity ";

    private ArrayList<String> deviceNames = new ArrayList<>();
    private ArrayList<String> deviceMacAddresses = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;
    Button btn_pair_new_device;
    private boolean IsPairNewDeviceClicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair_device);

        btn_pair_new_device = (Button) findViewById(R.id.btn_pair_new_device);

        GetPairedDevicesList();
        initRecyclerView();

        btn_pair_new_device.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                deviceNames.clear();
                deviceMacAddresses.clear();
                IsPairNewDeviceClicked = true;
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onLinkSelected(String deviceName, String deviceMacAddress) {

        String message = "";
        if (!deviceName.isEmpty()) {
            message = getResources().getString(R.string.selectedLinkMessage);
            message = message.replace("linkName", "<b>" + deviceName + "</b>");
        }
        message = message + "<br>" + getResources().getString(R.string.linkSelectionConfirmation);

        CustomMessageWithYesOrNo(PairDeviceActivity.this, message, deviceName, deviceMacAddress);
    }

    @Override
    protected void onResume() {
        super.onResume();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
        }

        if (IsPairNewDeviceClicked){
            IsPairNewDeviceClicked = false;
            GetPairedDevicesList();
            initRecyclerView();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reader, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (item.getItemId()) {

            case R.id.mreload:
                this.recreate();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void GetPairedDevicesList() {

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Get paired devices.
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if (deviceName != null) {
                    //if (deviceName.startsWith("FSBT-") || deviceName.startsWith("FSAST-") || deviceName.startsWith("FSDB-") ||
                    //        deviceName.startsWith("FSAF3-") || deviceName.startsWith("FSAF7-") || deviceName.startsWith("FSAG2-") ||
                    //        deviceName.startsWith("FSAG3-") || deviceName.startsWith("FSFV-") || deviceName.startsWith("FSFH3-") ||
                    //        deviceName.startsWith("FSFH7-") || deviceName.startsWith("FSGH2-") || deviceName.startsWith("FSGH3-")) {

                    deviceMacAddresses.add(deviceHardwareAddress);
                    deviceNames.add(deviceName);
                    Log.i(TAG, "DeviceName:" + deviceName + "\n" + "MacAddress:" + deviceHardwareAddress);
                    //}
                }
            }
        }
    }

    private void initRecyclerView() {
        Log.d(TAG, "initRecyclerView: init recyclerview.");
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerv_view);
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(this, deviceNames, deviceMacAddresses, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public void SaveSelectedLinkDetails(String deviceName, String deviceMacAddress) {
        SharedPreferences sharedPref = PairDeviceActivity.this.getSharedPreferences(Constants.SelectedLinkDetails, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("DeviceName", deviceName);
        editor.putString("DeviceMacAddress", deviceMacAddress);
        editor.commit();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void CustomMessageWithYesOrNo(final Activity context, String message, String deviceName, String deviceMacAddress) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));
        builder.setCancelable(false);

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        dialog.dismiss();
                        SaveSelectedLinkDetails(deviceName, deviceMacAddress);
                        BTConstants.deviceAddress = deviceMacAddress;

                        Intent i = new Intent(context, UpgradeActivity.class); // GO TO NEXT Activity
                        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(i);
                    }
                }
        );

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        dialog.dismiss();
                        context.recreate();
                    }
                }
        );
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

}