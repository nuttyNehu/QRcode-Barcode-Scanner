package com.nuttynehu.qrbarcodescanner.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;

public class CheckNetwork {

    Context context;
    private NetworkConnectivityChangeListener listener;

    public CheckNetwork(Context context) {
        this.context = context;
        listener = (NetworkConnectivityChangeListener) context;
    }

    // Network Check
    public void registerNetworkCallback() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkRequest.Builder builder = new NetworkRequest.Builder();

            connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    GlobalVariables.isNetworkConnected = true; // Global Static Variable
                    listener.onNetworkAvailable();
                }

                @Override
                public void onLost(Network network) {
                    GlobalVariables.isNetworkConnected = false; // Global Static Variable
                    listener.onNetworkLost();
                }
            });
            
            GlobalVariables.isNetworkConnected = false;
        } catch (Exception e) {
            GlobalVariables.isNetworkConnected = false;
            listener.onNetworkLost();
        }
    }


    public interface NetworkConnectivityChangeListener{
        void onNetworkAvailable();
        void onNetworkLost();
    }

}
