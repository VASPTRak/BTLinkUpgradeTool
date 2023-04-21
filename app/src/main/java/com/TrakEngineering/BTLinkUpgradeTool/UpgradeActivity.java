package com.TrakEngineering.BTLinkUpgradeTool;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.widget.TextView;

import com.TrakEngineering.BTLinkUpgradeTool.BTSPP.BTConstants;
import com.TrakEngineering.BTLinkUpgradeTool.BTSPP.BTSPPMain;
import com.TrakEngineering.BTLinkUpgradeTool.BTSPP.BTSPP_Serial.SerialService;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class UpgradeActivity extends AppCompatActivity implements ServiceConnection {

    private static final String TAG = "UpgradeActivity ";
    public String deviceName, deviceMacAddress;
    public String FirmwareFileName, FirmwareFilePath, Version;
    public ProgressDialog pdUpgradeProcess;
    public static SerialService service;

    // ============ Bluetooth receiver for Upgrade =========//
    public BroadcastBlueLinkData broadcastBlueLinkData = null;
    public boolean isBroadcastReceiverRegistered = false;
    public IntentFilter intentFilter;
    public String upRequest = "", upResponse = "";
    public int connectionAttemptCount = 0;
    //======================================================//


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade);

        startBTSppMain(); //BT link connection
        ConnectToBTLink();

        SetUpgradeFirmwareDetails();
        FirmwareFileCheckAndDownload();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeBTSppMain();
    }

    private void startBTSppMain() {
        try {
            UpgradeActivity.this.startService(new Intent(this, SerialService.class));
            UpgradeActivity.this.bindService(new Intent(this, SerialService.class), this, Context.BIND_AUTO_CREATE);
            Log.i(TAG, "BTLink : startBTSppMain");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void closeBTSppMain() {
        UpgradeActivity.this.stopService(new Intent(this, SerialService.class));
        Log.i(TAG, "BTLink : closeBTSppMain");
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinderService) {

        String className = componentName.getClassName();
        if (className.equalsIgnoreCase("com.TrakEngineering.BTLinkUpgradeTool.BTSPP.BTSPP_Serial.SerialService")) {

            BTSPPMain btspp = new BTSPPMain();
            btspp.activity = UpgradeActivity.this;
            service = ((SerialService.SerialBinder) iBinderService).getService();
            service.attach(btspp);
            btspp.connect();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

        String className = componentName.getClassName();
        if (className.equalsIgnoreCase("com.TrakEngineering.BTLinkUpgradeTool.BTSPP.BTSPP_Serial.SerialService")) {
            service = null;
        }
    }

    private void ConnectToBTLink() {
        try {
            BTSPPMain btspp = new BTSPPMain();
            btspp.activity = UpgradeActivity.this;
            btspp.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void AlertDialogBox(final Context ctx, String message, int textSize) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
        alertDialogBuilder.setMessage(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));

        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        dialog.dismiss();

                        // Go to Main Activity
                        Intent i = new Intent(ctx, MainActivity.class); // GO TO NEXT Activity
                        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        ctx.startActivity(i);
                    }
                }
        );

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        TextView textView = (TextView) alertDialog.findViewById(android.R.id.message);
        textView.setTextSize(textSize);
    }

    private void SetUpgradeFirmwareDetails() {
        try {
            FirmwareFileName = Constants.LinkFirmwareList.get(Constants.selectedFirmwarePosition).get("FirmwareFileName");
            FirmwareFilePath = Constants.LinkFirmwareList.get(Constants.selectedFirmwarePosition).get("FirmwareFilePath");
            Version = Constants.LinkFirmwareList.get(Constants.selectedFirmwarePosition).get("Version");

            if (FirmwareFileName != null) {
                if (FirmwareFileName.isEmpty()) {
                    FirmwareFileName = Version + ".bin";
                }
            }
            if (FirmwareFilePath == null) {
                FirmwareFilePath = "";
            }
        } catch (Exception e) {
            Log.e(TAG, "SetUpgradeFirmwareDetails Exception: " + e.getMessage());
        }
    }

    private void FirmwareFileCheckAndDownload() {
        try {

            String binFolderPath = String.valueOf(getApplicationContext().getExternalFilesDir(Constants.FOLDER_BIN));
            File folder = new File(binFolderPath);
            boolean success = true;
            if (!folder.exists()) {
                success = folder.mkdirs();
            }

            String LocalPath = binFolderPath + "/" + FirmwareFileName;

            File f = new File(LocalPath);
            if (f.exists()) {
                Log.i(TAG, "Link upgrade firmware file (" + FirmwareFileName + ") already exist. Skip download.");
                // Continue to upgrade
                CheckBTLinkStatusForUpgrade(false);
            } else {
                if (!FirmwareFilePath.isEmpty()) {
                    Log.i(TAG, "Downloading link upgrade firmware file (" + FirmwareFileName + ")");
                    new DownloadFileFromURL().execute(FirmwareFilePath, binFolderPath, FirmwareFileName);
                } else {
                    Log.i(TAG, "Link upgrade File path empty.");
                    BackToMainActivity("Failed to process upgrade. Link upgrade File path empty. Please try again later.", 25);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "FirmwareFileCheckAndDownload Exception: " + e.getMessage());
            BackToMainActivity("Something went wrong. Please try again later. <br><b>Exception:</b> " + e.getMessage(), 18);
        }
    }

    private void BackToMainActivity(String alertMessage, int textSize) {

        if (isBroadcastReceiverRegistered) {
            isBroadcastReceiverRegistered = false;
            UnregisterReceiver();
        }
        if (pdUpgradeProcess != null) {
            if (pdUpgradeProcess.isShowing()) {
                pdUpgradeProcess.dismiss();
            }
        }
        AlertDialogBox(UpgradeActivity.this, alertMessage, textSize);
    }

    public CharSequence GetSpinnerMessage(String message) {
        try {
            SpannableString ss2 = new SpannableString(message);
            ss2.setSpan(new RelativeSizeSpan(1.2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            return ss2;
        } catch (Exception ex) {
            Log.e(TAG, "Exception in GetSpinnerMessage. " + ex.getMessage());
            return message;
        }
    }

    public class DownloadFileFromURL extends AsyncTask<String, String, String> {

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(UpgradeActivity.this);
            String message = getResources().getString(R.string.FileDownloadInProgress) + "\n" + getResources().getString(R.string.PleaseWaitSeveralSeconds);
            pd.setMessage(GetSpinnerMessage(message));
            pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {

                URL url = new URL(f_url[0]);
                URLConnection connection = url.openConnection();
                connection.connect();
                // getting file length
                int lenghtOfFile = connection.getContentLength();

                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                // Output stream to write file
                OutputStream output = new FileOutputStream(f_url[1] + "/" + f_url[2]);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }

        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            pd.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(String file_url) {
            pd.dismiss();
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Continue to upgrade
                    CheckBTLinkStatusForUpgrade(false);
                }
            }, 100);
        }
    }

    public void ShowUpgradeProcessLoader(String message) {

        pdUpgradeProcess = new ProgressDialog(UpgradeActivity.this);
        pdUpgradeProcess.setMessage(GetSpinnerMessage(message));
        pdUpgradeProcess.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pdUpgradeProcess.setCancelable(false);
        pdUpgradeProcess.show();

    }

    private void RegisterBTReceiver() {
        broadcastBlueLinkData = new BroadcastBlueLinkData();
        intentFilter = new IntentFilter("BroadcastBlueLinkData");
        registerReceiver(broadcastBlueLinkData, intentFilter);
        isBroadcastReceiverRegistered = true;
    }

    private void UnregisterReceiver() {
        unregisterReceiver(broadcastBlueLinkData);
    }

    public class BroadcastBlueLinkData extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            try {
                Bundle notificationData = intent.getExtras();
                String Action = notificationData.getString("Action");
                if (Action == null) {
                    Action = "";
                }
                if (Action.equalsIgnoreCase("BlueLink")) {

                    upRequest = notificationData.getString("Request");
                    upResponse = notificationData.getString("Response");

                    if (upResponse == null) {
                        upResponse = "";
                    }

                    Log.i(TAG, "BTLink: Response from Link >>" + upResponse.trim());

                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "BTLink: onReceive Exception: " + e.getMessage());
            }
        }
    }

    private void CheckBTLinkStatusForUpgrade(boolean retryAttempt) {
        try {
            if (!retryAttempt) {
                ShowUpgradeProcessLoader(getResources().getString(R.string.PleaseWaitSeveralSeconds));
            }

            new CountDownTimer(10000, 2000) {
                public void onTick(long millisUntilFinished) {
                    if (BTConstants.BTStatusStr.equalsIgnoreCase("Connected")) {
                        Log.i(TAG, "BTLink: Link is connected.");
                        RegisterBTReceiver();
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                infoCommandBeforeUpgrade(); // Continue to BT upgrade
                            }
                        }, 1000);
                        cancel();
                    } else {
                        Log.i(TAG, "BTLink: Checking Connection Status...");
                    }
                }

                public void onFinish() {

                    if (BTConstants.BTStatusStr.equalsIgnoreCase("Connected")) {
                        Log.i(TAG, "BTLink: Link is connected.");
                        RegisterBTReceiver();
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                infoCommandBeforeUpgrade(); // Continue to BT upgrade
                            }
                        }, 1000);
                    } else {
                        if (connectionAttemptCount > 0) {
                            connectionAttemptCount = 0;
                            Log.i(TAG, "BTLink: Link not connected.");
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    BackToMainActivity("Link not connected. Please try again later.", 25);
                                }
                            }, 100);
                        } else {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    connectionAttemptCount++;
                                    retryBTConnection();
                                    CheckBTLinkStatusForUpgrade(true);
                                }
                            }, 100);
                        }
                    }
                }
            }.start();

        } catch (Exception e) {
            Log.e(TAG, "BTLink: CheckBTLinkStatusForUpgrade Exception:>>" + e.getMessage());
            BackToMainActivity("Error occurred while checking BT Link connection. Please try again later. <br><b>Exception:</b> " + e.getMessage(), 18);
        }
    }

    private void retryBTConnection() {
        try {
            if (!BTConstants.BTStatusStr.equalsIgnoreCase("Connected")) {
                Log.i(TAG, "BTLink: Link not connected. Retrying to connect.");
                //Retrying to connect to link
                BTSPPMain btspp = new BTSPPMain();
                btspp.activity = UpgradeActivity.this;
                btspp.connect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void infoCommandBeforeUpgrade() {
        try {
            //Execute info command before upgrade to get link version
            upRequest = "";
            upResponse = "";
            String LinkName = "";

            SharedPreferences sharedPref = UpgradeActivity.this.getSharedPreferences(Constants.SelectedLinkDetails, Context.MODE_PRIVATE);
            LinkName = sharedPref.getString("DeviceName", "");

            BTConstants.isNewVersionLink = false;

            Log.i(TAG, "BTLink: Sending Info command (before upgrade) to Link: " + LinkName);
            BTSPPMain btspp = new BTSPPMain();
            btspp.send(BTConstants.info_cmd);

            new CountDownTimer(5000, 1000) {

                public void onTick(long millisUntilFinished) {
                    long attempt = (5 - (millisUntilFinished / 1000));
                    if (attempt > 0) {
                        if (upRequest.equalsIgnoreCase(BTConstants.info_cmd) && !upResponse.equalsIgnoreCase("")) {
                            //Info command (before upgrade) success.
                            if (upResponse.contains("records") && upResponse.contains("mac_address")) {
                                Log.i(TAG, "BTLink: Checking Info command response (before upgrade). Response: true");
                                BTConstants.isNewVersionLink = true;
                                upResponse = "";
                            } else {
                                Log.i(TAG, "BTLink: Checking Info command response (before upgrade). Response:>>" + upResponse.trim());
                                BTConstants.isNewVersionLink = false;
                            }
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (pdUpgradeProcess != null) {
                                        if (pdUpgradeProcess.isShowing()) {
                                            pdUpgradeProcess.setMessage(GetSpinnerMessage(getResources().getString(R.string.SoftwareUpdateInProgress) + "\n" + getResources().getString(R.string.PleaseWaitSeveralSeconds)));
                                        }
                                    }
                                    upgradeCommand();
                                }
                            }, 1000);
                            cancel();
                        } else {
                            Log.i(TAG, "BTLink: Checking Info command response (before upgrade). Response: false");
                        }
                    }
                }

                public void onFinish() {

                    if (upRequest.equalsIgnoreCase(BTConstants.info_cmd) && !upResponse.equalsIgnoreCase("")) {
                        //Info command (before upgrade) success.
                        if (upResponse.contains("records") && upResponse.contains("mac_address")) {
                            Log.i(TAG, "BTLink: Checking Info command response (before upgrade). Response: true");
                            BTConstants.isNewVersionLink = true;
                            upResponse = "";
                        } else {
                            Log.i(TAG, "BTLink: Checking Info command response (before upgrade). Response:>>" + upResponse.trim());
                            BTConstants.isNewVersionLink = false;
                        }
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (pdUpgradeProcess != null) {
                                    if (pdUpgradeProcess.isShowing()) {
                                        pdUpgradeProcess.setMessage(GetSpinnerMessage(getResources().getString(R.string.SoftwareUpdateInProgress) + "\n" + getResources().getString(R.string.PleaseWaitSeveralSeconds)));
                                    }
                                }
                                upgradeCommand();
                            }
                        }, 1000);
                    } else {
                        Log.i(TAG, "BTLink: Checking Info command response (before upgrade). Response: false.");
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                BackToMainActivity("Info command response is empty. Please try again later.", 25);
                            }
                        }, 100);
                    }
                }
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "BTLink: infoCommandBeforeUpgrade Exception:>>" + e.getMessage());
            BackToMainActivity("Error occurred while sending info command before upgrade. Please try again later. <br><b>Exception:</b> " + e.getMessage(), 18);
        }
    }

    private void upgradeCommand() {
        try {
            //Execute upgrade Command
            upRequest = "";
            upResponse = "";

            String LinkName = "";

            SharedPreferences sharedPref = UpgradeActivity.this.getSharedPreferences(Constants.SelectedLinkDetails, Context.MODE_PRIVATE);
            LinkName = sharedPref.getString("DeviceName", "");

            String binFolderPath = String.valueOf(getApplicationContext().getExternalFilesDir(Constants.FOLDER_BIN));
            String LocalPath = binFolderPath + "/" + FirmwareFileName;
            Log.i(TAG, "BTLink: BTLinkUpgradeFunctionality file name: " + FirmwareFileName);

            File file = new File(LocalPath);
            long file_size = file.length();

            Log.i(TAG, "BTLink: Sending upgrade command to Link: " + LinkName);
            BTSPPMain btspp = new BTSPPMain();
            btspp.send(BTConstants.linkUpgrade_cmd + file_size);

            new CountDownTimer(10000, 2000) {

                public void onTick(long millisUntilFinished) {
                    if (upRequest.contains(BTConstants.linkUpgrade_cmd) && !upResponse.isEmpty()) {
                        //upgrade command success.
                        Log.i(TAG, "BTLink: Checking upgrade command response. Response:>>" + upResponse.trim());
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                new BTUpgradeFileUploadFunctionality().execute();
                            }
                        }, 1000);
                        cancel();
                    }
                }

                public void onFinish() {

                    if ((upRequest.contains(BTConstants.linkUpgrade_cmd) && !upResponse.isEmpty()) || (!BTConstants.isNewVersionLink)) {
                        //upgrade command success.
                        if (BTConstants.isNewVersionLink) {
                            Log.i(TAG, "BTLink: Checking upgrade command response. Response:>>" + upResponse.trim());
                        }
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                new BTUpgradeFileUploadFunctionality().execute();
                            }
                        }, 1000);
                    } else {
                        // Terminating the transaction as per Bolong's comment in #2120 => DO NOT send any command after sending upgrade command.
                        Log.i(TAG, "BTLink: Checking upgrade command response. Response: false.");
                        if (pdUpgradeProcess != null) {
                            if (pdUpgradeProcess.isShowing()) {
                                pdUpgradeProcess.setMessage(GetSpinnerMessage(getResources().getString(R.string.LINKConnectionLost) + "\n" + getResources().getString(R.string.TryAgainLater)));
                            }
                        }
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                BackToMainActivity("LINK connection lost. Please try again later.", 25);
                            }
                        }, 2000);
                    }
                }
            }.start();

        } catch (Exception e) {
            Log.e(TAG, "BTLink: upgradeCommand Exception:>>" + e.getMessage());
            BackToMainActivity("Error occurred while sending upgrade command. Please try again later. <br><b>Exception:</b> " + e.getMessage(), 18);
        }
    }

    public class BTUpgradeFileUploadFunctionality extends AsyncTask<String, String, String> {

        int counter = 0;
        String LinkName = "";

        @Override
        protected void onPreExecute() {
            BTConstants.BTUpgradeStatus = "";
        }

        @Override
        protected String doInBackground(String... f_url) {

            try {
                SharedPreferences sharedPref = UpgradeActivity.this.getSharedPreferences(Constants.SelectedLinkDetails, Context.MODE_PRIVATE);
                LinkName = sharedPref.getString("DeviceName", "");

                String binFolderPath = String.valueOf(getApplicationContext().getExternalFilesDir(Constants.FOLDER_BIN));
                String LocalPath = binFolderPath + "/" + FirmwareFileName;

                File file = new File(LocalPath);

                long file_size = file.length();
                long tempFileSize = file_size;

                InputStream inputStream = new FileInputStream(file);

                int BUFFER_SIZE = 256; //490; //8192;
                byte[] bufferBytes = new byte[BUFFER_SIZE];

                if (inputStream != null) {
                    long bytesWritten = 0;
                    int amountOfBytesRead;

                    Log.i(TAG, "BTLink: Upload (" + FirmwareFileName + ") started...");
                    while ((amountOfBytesRead = inputStream.read(bufferBytes)) != -1) {

                        bytesWritten += amountOfBytesRead;
                        int progressValue = (int) (100 * ((double) bytesWritten) / ((double) file_size));

                        if (pdUpgradeProcess != null) {
                            if (pdUpgradeProcess.isShowing()) {
                                pdUpgradeProcess.setMessage(GetSpinnerMessage((getResources().getString(R.string.SoftwareUpdateInProgress) + "\n" + getResources().getString(R.string.PleaseWaitSeveralSeconds)) + " " + String.valueOf(progressValue) + " %"));
                            }
                        }
                        //publishProgress(String.valueOf(progressValue));

                        if (BTConstants.BTStatusStr.equalsIgnoreCase("Connected")) {
                            BTSPPMain btspp = new BTSPPMain();
                            btspp.sendBytes(bufferBytes);

                            tempFileSize = tempFileSize - BUFFER_SIZE;
                            if (tempFileSize < BUFFER_SIZE){
                                int i = (int) (long) tempFileSize;
                                if (i > 0) {
                                    //i = i + BUFFER_SIZE;
                                    bufferBytes = new byte[i];
                                }
                            }

                            Thread.sleep(10);
                        } else {
                            //BTConstants.IsFileUploadCompleted = false;
                            Log.i(TAG, "BTLink:  LINK connection lost while uploading the upgrade file. Progress: " + progressValue + " %");
                            BTConstants.BTUpgradeStatus = "Incomplete";
                            break;
                        }
                    }
                    inputStream.close();
                    if (BTConstants.BTUpgradeStatus.isEmpty()) {
                        BTConstants.BTUpgradeStatus = "Completed";
                    }
                }
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
                Log.i(TAG, "BTLink: UpgradeFileUploadFunctionality InBackground Exception: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String file_url) {
            //pd.dismiss();
            Log.i(TAG, "BTLink: LINK Status: " + BTConstants.BTStatusStr);

            if (BTConstants.BTUpgradeStatus.equalsIgnoreCase("Completed")) {
                BTConstants.BTUpgradeStatus = "";

                if (pdUpgradeProcess != null) {
                    if (pdUpgradeProcess.isShowing()) {
                        pdUpgradeProcess.setMessage(GetSpinnerMessage(getResources().getString(R.string.UpgradeCompleted)));
                    }
                }

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BackToMainActivity(getResources().getString(R.string.UpgradeCompleted), 25);
                    }
                }, 1000);

                /*if (pdUpgradeProcess != null) {
                    if (pdUpgradeProcess.isShowing()) {
                        pdUpgradeProcess.setMessage(GetSpinnerMessage(getResources().getString(R.string.ConnectingToTheLINK) + "\n" + getResources().getString(R.string.PleaseWaitSeveralSeconds)));
                    }
                }

                Handler handler = new Handler();
                int delay = 10000;

                handler.postDelayed(new Runnable() {
                    public void run() {
                        if (BTConstants.BTStatusStr.equalsIgnoreCase("Connected")) {
                            counter = 0;
                            handler.removeCallbacksAndMessages(null);
                            Log.i(TAG, "BTLink: Link is connected.");
                            //BackToMainActivity(getResources().getString(R.string.UpgradeCompleted), 25);
                        } else {
                            counter++;
                            if (counter < 3) {
                                retryBTConnection();
                                Log.i(TAG, "BTLink: Waiting to reconnect... (Attempt: " + counter + ")");
                                handler.postDelayed(this, delay);
                            } else {
                                Log.i(TAG, "BTLink: Failed to connect to the link. (Status: " + BTConstants.BTStatusStr + ")");
                                if (pdUpgradeProcess != null) {
                                    if (pdUpgradeProcess.isShowing()) {
                                        pdUpgradeProcess.setMessage(GetSpinnerMessage(getResources().getString(R.string.LINKConnectionLost) + "\n" + getResources().getString(R.string.TryAgainLater)));
                                    }
                                }
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        counter = 0;
                                        BackToMainActivity("Failed to connect to the link. Please try again later.", 25);
                                    }
                                }, 1000);
                            }
                        }
                    }
                }, delay);*/
            } else {
                Log.i(TAG, "BTLink: LINK connection lost.");
                BTConstants.BTUpgradeStatus = "";

                if (pdUpgradeProcess != null) {
                    if (pdUpgradeProcess.isShowing()) {
                        pdUpgradeProcess.setMessage(GetSpinnerMessage(getResources().getString(R.string.LINKConnectionLost) + "\n" + getResources().getString(R.string.TryAgainLater)));
                    }
                }
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BackToMainActivity("LINK connection lost. Please try again later.", 25);
                    }
                }, 1000);
            }
        }
    }
}