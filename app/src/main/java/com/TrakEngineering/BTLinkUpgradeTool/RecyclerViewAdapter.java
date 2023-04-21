package com.TrakEngineering.BTLinkUpgradeTool;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "RecyclerViewAdapter ";

    private ArrayList<String> deviceNames = new ArrayList<>();
    private ArrayList<String> deviceMacAddresses = new ArrayList<>();
    private Context mContext;
    private LinkSelectionListener linkSelectionListener;

    public RecyclerViewAdapter(Context context, ArrayList<String> deviceNames, ArrayList<String> deviceMacAddresses, LinkSelectionListener linkSelectionListener) {
        this.deviceNames = deviceNames;
        this.deviceMacAddresses = deviceMacAddresses;
        mContext = context;
        this.linkSelectionListener = linkSelectionListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_listitem, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: called.");

        holder.deviceName.setText(deviceNames.get(position));
        holder.deviceMacAddress.setText(deviceMacAddresses.get(position));
        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Log.d(TAG, "DEVICE NAME: " + deviceNames.get(position) + " \nDEVICE MAC: " + deviceMacAddresses.get(position));
                //Toast.makeText(mContext, mContext.getResources().getString(R.string.DeviceName).toUpperCase() + ": " + deviceNames.get(position) + " \n" + mContext.getResources().getString(R.string.DeviceMacAddress).toUpperCase() + ": " + deviceMacAddresses.get(position), Toast.LENGTH_LONG).show();

                Log.d(TAG, "BTLink: Selected DEVICE NAME:" + deviceNames.get(position) + "DEVICE MAC:" + deviceMacAddresses.get(position));

                linkSelectionListener.onLinkSelected(deviceNames.get(position), deviceMacAddresses.get(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceNames.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName, deviceMacAddress;
        LinearLayout parentLayout;

        public ViewHolder(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.image_name);
            deviceMacAddress = itemView.findViewById(R.id.image_mac);
            parentLayout = itemView.findViewById(R.id.parent_layout);
        }
    }

}
