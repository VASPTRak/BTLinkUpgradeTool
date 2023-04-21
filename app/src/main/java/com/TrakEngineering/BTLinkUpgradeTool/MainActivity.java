package com.TrakEngineering.BTLinkUpgradeTool;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private String TAG = this.getClass().getSimpleName();
    public static String webIP = "https://fluidsecure.net/";
    public static String API_GetLinkFirmwares = webIP + "api/External/getlinkfirmwares";
    ArrayAdapter adapter;
    private Spinner spinner_firmware_version;
    private Button btnGo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinner_firmware_version = (Spinner) findViewById(R.id.spinner_firmware_version);
        btnGo = (Button) findViewById(R.id.btnGo);
        TextView tvVersionNum = (TextView) findViewById(R.id.tvVersion);
        String version = getResources().getString(R.string.VersionHeading) + CommonUtils.getVersionCode(MainActivity.this);
        tvVersionNum.setText(version);

        // Get Link Firmware Version list from server
        new GetLinkFirmwares().execute();

        spinner_firmware_version.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Constants.selectedFirmwarePosition = i - 1; // To handle first entry of dropdown i.e. Select Firmware
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Constants.selectedFirmwarePosition >= 0) {
                    Intent intent = new Intent(MainActivity.this, PairDeviceActivity.class);
                    startActivity(intent);
                } else {
                    CommonUtils.AlertDialogBox(MainActivity.this, "Please select firmware.");
                }
            }
        });
    }

    /*@Override
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
    }*/

    public void BindFirmwareList() {

        adapter = new ArrayAdapter(this, R.layout.spinner_item_list, GetFirmwareVersionList());
        adapter.setDropDownViewResource(R.layout.spinner_item_list);
        //adapter.notifyDataSetChanged();
        spinner_firmware_version.setAdapter(adapter);
        /*if (Spinner_tankNumber.getSelectedItemPosition() == 0) {
            if (CommonUtils.TankDataList.size() > 0) {
                Spinner_tankNumber.setSelection(1);
                addnewlinkViewModel.TankPositionSel = 0;
            }
        }
        if (newTankAdded) {
            if (CommonUtils.TankDataList.size() > 0) {
                Spinner_tankNumber.setSelection(CommonUtils.TankDataList.size()); // last item
                addnewlinkViewModel.TankPositionSel = CommonUtils.TankDataList.size() - 1;
            }
        }*/
    }

    public class GetLinkFirmwares extends AsyncTask<String, Void, String> {

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("Please wait...");
            pd.show();
        }

        protected String doInBackground(String... param) {
            String resp = "";

            try {
                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(API_GetLinkFirmwares)
                        .get()
                        .addHeader("Content-Type", "application/json")
                        .build();

                Response response = client.newCall(request).execute();
                resp = response.body().string();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            pd.dismiss();
            if (result != null && !result.isEmpty()) {

                try {
                    if (result.contains("firmwares")) {

                        JSONObject jsonObject = new JSONObject(result);
                        JSONArray jsonArray = jsonObject.getJSONArray("firmwares");

                        for (int i = 0; i < jsonArray.length(); i++) {

                            JSONObject jObj = jsonArray.getJSONObject(i);
                            String FirmwareId = jObj.getString("FirmwareId");
                            String FirmwareFileName = jObj.getString("FirmwareFileName");
                            String FirmwareFilePath = jObj.getString("FirmwareFilePath");
                            String Version = jObj.getString("Version");

                            HashMap<String, String> map = new HashMap<>();
                            map.put("FirmwareId", FirmwareId);
                            map.put("FirmwareFileName", FirmwareFileName);
                            map.put("FirmwareFilePath", FirmwareFilePath);
                            map.put("Version", Version);

                            Constants.LinkFirmwareList.add(map);
                        }

                        Log.i(TAG, "API Call Success" + result);
                    } else {
                        Log.i(TAG, "API Call fail" + result);
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "API Call fail" + e.getMessage());
                    e.printStackTrace();
                }
                BindFirmwareList();
            } else {
                Log.i(TAG, "InPost Response err:" + result);
            }
        }
    }

    public ArrayList<String> GetFirmwareVersionList() {

        ArrayList<String> firmwares = new ArrayList<>();
        try {
            firmwares.add("Select Firmware");
            for (int i = 0; i < Constants.LinkFirmwareList.size(); i++) {
                String Version = Constants.LinkFirmwareList.get(i).get("Version");
                firmwares.add(Version);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "GetFirmwareVersionList Exception: " + e.getMessage());
        }
        return firmwares;
    }

}