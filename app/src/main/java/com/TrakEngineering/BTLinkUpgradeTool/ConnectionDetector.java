package com.TrakEngineering.BTLinkUpgradeTool;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

public class ConnectionDetector {

    private Context _context;
    private static final String TAG = ConnectionDetector.class.getSimpleName();
    public ConnectionDetector(Context context){
        this._context = context;
    }

    //----------------------------------------------------------------------------------------------------
    public boolean isConnectingToInternet(){

        if (isConnecting() || IsTypeStable()) {
            return true;
        }
        return false;
    }

    public boolean IsTypeStable() {

        Constants.CurrentNetworkType = "";
        //POOR Bandwidth under 150 kbps.
        //MODERATE Bandwidth between 150 and 550 kbps.
        //GOOD Bandwidth over 2000 kbps.
        //EXCELLENT Bandwidth over 2000 kbps.
        //UNKNOWN connection quality cannot be found.

        ConnectivityManager Connectivity = (ConnectivityManager) _context.getSystemService(_context.CONNECTIVITY_SERVICE);
        TelephonyManager mTelephonyManager = (TelephonyManager) _context.getSystemService(_context.TELEPHONY_SERVICE);
        int subType = mTelephonyManager.getNetworkType();
        NetworkInfo info = Connectivity.getActiveNetworkInfo();

        if (info.getType() == ConnectivityManager.TYPE_WIFI) {
            Constants.CurrentNetworkType = "_wifi on";
            return false;
        } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {

            // check NetworkInfo subtype
            switch (subType) {
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    Constants.CurrentNetworkType = "50-100 kbps";
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    Constants.CurrentNetworkType = "14-64 kbps";
                    return false; // ~ 14-64 kbps
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    Constants.CurrentNetworkType = "50-100 kbps";
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    Constants.CurrentNetworkType = "400-1000 kbps";
                    return true; // ~ 400-1000 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    Constants.CurrentNetworkType = "600-1400 kbps";
                    return true; // ~ 600-1400 kbps
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    Constants.CurrentNetworkType = "100 kbps";
                    return false; // ~ 100 kbps
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                    Constants.CurrentNetworkType = "2-14 Mbps";
                    return true; // ~ 2-14 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    Constants.CurrentNetworkType = "700-1700 kbps";
                    return true; // ~ 700-1700 kbps
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                    Constants.CurrentNetworkType = "1-23 Mbps";
                    return true; // ~ 1-23 Mbps
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    Constants.CurrentNetworkType = "400-7000 kbps";
                    return true; // ~ 400-7000 kbps
                /*
                 * Above API level 7, make sure to set android:targetSdkVersion
                 * to appropriate level to use these
                 */
                case TelephonyManager.NETWORK_TYPE_EHRPD: // API level 11
                    Constants.CurrentNetworkType = "1-2 Mbps";
                    return true; // ~ 1-2 Mbps
                case TelephonyManager.NETWORK_TYPE_EVDO_B: // API level 9
                    Constants.CurrentNetworkType = "5 Mbps";
                    return true; // ~ 5 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPAP: // API level 13
                    Constants.CurrentNetworkType = "10-20 Mbps";
                    return true; // ~ 10-20 Mbps
                case TelephonyManager.NETWORK_TYPE_IDEN: // API level 8
                    Constants.CurrentNetworkType = "25 kbps";
                    return false; // ~25 kbps
                case TelephonyManager.NETWORK_TYPE_LTE: // API level 11
                    Constants.CurrentNetworkType = "10+ Mbps";
                    return true; // ~ 10+ Mbps
                // Unknown
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                default:
                    Constants.CurrentNetworkType = "_unknown";
                    return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Checking for all possible internet providers
     * **/
    public boolean isConnecting() {
        boolean isConnected = false;

        ConnectivityManager connectivity = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo activeNetwork = connectivity.getActiveNetworkInfo();
            isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return isConnected;
    }
}
