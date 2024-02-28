package com.hz_apps.autowificonnector;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.PatternMatcher;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class WifiConnector {
    public static boolean connectToEAPWifi(Context context, String ssid, String identity, String password) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = "\"" + ssid + "\"";
        wifiConfig.preSharedKey = "\"" + password + "\"";

        // Set the EAP parameters
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);

        wifiConfig.enterpriseConfig = new WifiEnterpriseConfig();
        wifiConfig.enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.PEAP);
        wifiConfig.enterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.MSCHAPV2);
        wifiConfig.enterpriseConfig.setIdentity(identity);
        wifiConfig.enterpriseConfig.setPassword(password);

        int netId = wifiManager.addNetwork(wifiConfig);
        if (netId != -1) {
            // Save the configuration and enable it.
            wifiManager.saveConfiguration();
            wifiManager.disableNetwork(netId);
            wifiManager.disconnect();
            turnOffAndOnWifi(wifiManager);
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();
            return true;
        } else {
            // Failed to add the network configuration.
            return false;
        }


    }

    @RequiresApi(api = 29)
    private void connectToAndroid29(Context context, String ssid, String identity, String password) {
        WifiEnterpriseConfig wifiEnterpriseConfig = new WifiEnterpriseConfig();
        wifiEnterpriseConfig.setIdentity(identity);
        wifiEnterpriseConfig.setPassword(password);
        wifiEnterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.MSCHAPV2);
        wifiEnterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.PEAP);

//        // Set CA certificate validation to "do not validate"
//        wifiEnterpriseConfig.hasCaCertificate("do not validate");

//        // Create a WifiConfiguration and set the enterprise config
//        WifiConfiguration wifiConfig = new WifiConfiguration();
//        wifiConfig.SSID = "\"eduroam\"";
//        wifiConfig.enterpriseConfig = wifiEnterpriseConfig;
        WifiNetworkSpecifier wifiNetworkSpecifier = new WifiNetworkSpecifier.Builder()
                .setWpa2EnterpriseConfig(wifiEnterpriseConfig)
                .setSsid(ssid)
                .build();
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(wifiNetworkSpecifier)
                .build();
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback());
    }

    @RequiresApi(api = 29)
    public static boolean connectToEAPWifi29(Context context, String ssid, String identity, String password) {
        checkAndRequestFineLocationPermission(context, 1);
        WifiEnterpriseConfig wifiEnterpriseConfig = new WifiEnterpriseConfig();
        wifiEnterpriseConfig.setIdentity(identity);
        wifiEnterpriseConfig.setPassword(password);
        wifiEnterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.MSCHAPV2);
        wifiEnterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.PEAP);

        WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa3EnterpriseConfig(wifiEnterpriseConfig)
                .build();

        WifiNetworkSuggestion suggestion = new WifiNetworkSuggestion.Builder()
                .setPriority(1)  // You can adjust the priority as needed.
                .setWpa2EnterpriseConfig(wifiEnterpriseConfig)
                .setSsid(ssid)
                .build();

        List<WifiNetworkSuggestion> suggestionList = new ArrayList<>();
        suggestionList.add(suggestion);

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int status = wifiManager.addNetworkSuggestions(suggestionList);

        wifiManager.saveConfiguration();

        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            // Network suggestion added successfully. Now, you need to connect.
            return true;
        } else {
            // Failed to add network suggestion.
            return false;
        }
    }

    public static boolean checkAndRequestFineLocationPermission(Context context, int requestCode) {
        // Check if the ACCESS_FINE_LOCATION permission is granted.
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted.
            return true;
        } else {
            // Request ACCESS_FINE_LOCATION permission.
            ActivityCompat.requestPermissions((Activity) context, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, requestCode);
            return false;
        }
    }

    @RequiresApi(api = 29)
    public static boolean connectToEAPWifi299(Context context, String ssid, String identity, String password) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        WifiNetworkSuggestion suggestion = new WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build();

        WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsidPattern(new PatternMatcher(ssid, PatternMatcher.PATTERN_LITERAL))
                .build();

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build();

        wifiManager.addNetworkSuggestions(Collections.singletonList(suggestion));
        wifiManager.saveConfiguration();

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                // Successful connection
            }

            @Override
            public void onUnavailable() {
                // Failed connection
            }
        });

        return true;
    }


    private static void turnOffAndOnWifi(WifiManager wifiManager) {
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
            wifiManager.setWifiEnabled(true);
        }
    }


}
