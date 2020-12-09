package com.nuttynehu.qrbarcodescanner.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

@SuppressWarnings("deprecation")
public class NetworkUtils {

    public static boolean isConnected(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities activeNetwork = null;
            if (connectivityManager.getActiveNetwork() != null)
                activeNetwork = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (activeNetwork != null) {
                if (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
                    return true;
            }
        }else{
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            if (netInfo != null){
                return true;
            }
        }

        return false;
    }
}
