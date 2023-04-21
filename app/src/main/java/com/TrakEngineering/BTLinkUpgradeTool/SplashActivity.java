package com.TrakEngineering.BTLinkUpgradeTool;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashAct ";
    private ConnectionDetector cd = new ConnectionDetector(SplashActivity.this);

    private static final int CODE_CORSE_LOCATION = 1;
    private static final int CODE_READ_PHONE_STATE = 2;
    private static final int CODE_BLUETOOTH_CONNECT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        getSupportActionBar().setTitle(R.string.app_name);

        if (cd.isConnecting()) {

            //Enable bluetooth
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
            }

            try {
                checkPermissionTask checkPermissionTask = new checkPermissionTask();
                checkPermissionTask.execute();
                checkPermissionTask.get();

                if (checkPermissionTask.isValue) {

                    Log.i(TAG, "SplashActivity executeTask OnCreate");

                    executeTask();
                }
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
        } else {
            // no internet
            CommonUtils.showMessageDialogFinish(SplashActivity.this, "Check Internet", getResources().getString(R.string.no_internet));
        }
    }

    public class checkPermissionTask extends AsyncTask<Void, Void, Void> {
        boolean isValue = false;

        @Override
        protected Void doInBackground(Void... params) {

            isValue = TestPermissions();
            return null;
        }
    }

    private boolean TestPermissions() {
        boolean isValue = false;
        boolean isGranted = false;

        try {
            String[] permissions = {android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.READ_PHONE_STATE, android.Manifest.permission.BLUETOOTH};

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions = new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.READ_PHONE_STATE, android.Manifest.permission.BLUETOOTH, android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN};
            }

            for (int i = 0; i < permissions.length; i++) {
                isGranted = checkPermission(SplashActivity.this, permissions[i]);
                if (!isGranted) {
                    break;
                }
            }

            if (!isGranted) {
                ActivityCompat.requestPermissions(SplashActivity.this, permissions, CODE_READ_PHONE_STATE);
                isValue = false;
            } else {
                isValue = true;
            }

        } catch (Exception ex) {
        }
        return isValue;
    }

    private boolean checkPermission(Activity context, String permission) {
        int result = ContextCompat.checkSelfPermission(context, permission);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void executeTask() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (cd.isConnecting()) {
                    try {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                } else {
                    CommonUtils.showMessageDialogFinish(SplashActivity.this, "Check Internet", getResources().getString(R.string.no_internet));
                }
            }
        }, 5000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {

            case CODE_CORSE_LOCATION:
                if (grantResults.length > 0 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    CommonUtils.showMessageDialogFinish(SplashActivity.this, "Permission Granted", "Please press to ok for restart the app.");
                    Toast.makeText(SplashActivity.this, "Permission Granted, Now you can access app", Toast.LENGTH_SHORT).show();
                } else {
                    CommonUtils.showMessageDialogFinish(SplashActivity.this, "No GPS Permission", "Please enable gps and Allow the gps permission for this app to continue.");
                }
                break;

            case CODE_READ_PHONE_STATE:
                if (grantResults.length > 0 && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    CommonUtils.showMessageDialogFinish(SplashActivity.this, "Permission Granted", "Please press to ok for restart the app.");
                    Toast.makeText(SplashActivity.this, "Permission Granted, Now you can access app.", Toast.LENGTH_SHORT).show();
                } else {
                    CommonUtils.showMessageDialogFinish(SplashActivity.this, "No Phone State Permission", "Please enable read phone permission for this app to continue.");
                }
                break;

            case CODE_BLUETOOTH_CONNECT:
                if (grantResults.length > 4 && grantResults[4] == PackageManager.PERMISSION_GRANTED) {
                    CommonUtils.showMessageDialogFinish(SplashActivity.this, "Permission Granted", "Please press to ok and Restart the app.");
                    Toast.makeText(SplashActivity.this, "Permission Granted, Now you can access app.", Toast.LENGTH_SHORT).show();
                } else {
                    CommonUtils.showMessageDialogFinish(SplashActivity.this, "Bluetooth Connect permission not allowed.", "Please enable 'Bluetooth Connect Permission' for this app to continue.");
                }
                break;
        }

    }

}