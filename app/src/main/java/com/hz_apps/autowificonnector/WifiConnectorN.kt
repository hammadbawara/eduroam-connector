package com.hz_apps.autowificonnector

import android.content.Context
import android.net.wifi.WifiEnterpriseConfig
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import androidx.annotation.RequiresApi
import java.security.KeyStore
import java.security.cert.X509Certificate

class WifiConnectorN {
    @RequiresApi(29)
    fun connectToWifi(context: Context, identity: String, password: String, domain: String): Boolean {
        val enterpriseConfig = WifiEnterpriseConfig().apply {
            eapMethod = WifiEnterpriseConfig.Eap.PEAP
            phase2Method = WifiEnterpriseConfig.Phase2.MSCHAPV2

            this.identity = identity
            this.password = password
            this.domainSuffixMatch = domain
            this.anonymousIdentity = identity
        }

        enterpriseConfig.caCertificates = getSystemCertificates()

        val wifiNetworkSuggestion = WifiNetworkSuggestion.Builder().apply {
            setSsid("eduroam")
            setWpa2EnterpriseConfig(enterpriseConfig)
            setIsMetered(false)
            setPriority(1)
        }.build()

        val wifiManager = context.getSystemService(WifiManager::class.java)
        val status = wifiManager.addNetworkSuggestions(listOf(wifiNetworkSuggestion))

        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            return true
        } else {
            return false
        }
    }

    fun removeNetwork(context: Context, identity: String, password: String, domain: String): Boolean {
        val enterpriseConfig = WifiEnterpriseConfig().apply {
            eapMethod = WifiEnterpriseConfig.Eap.PEAP
            phase2Method = WifiEnterpriseConfig.Phase2.MSCHAPV2

            this.identity = identity
            this.password = password
            this.domainSuffixMatch = domain
            this.anonymousIdentity = identity
        }

        enterpriseConfig.caCertificates = getSystemCertificates()

        val wifiNetworkSuggestion = WifiNetworkSuggestion.Builder().apply {
            setSsid("eduroam")
            setWpa2EnterpriseConfig(enterpriseConfig)
            setIsMetered(false)
            setPriority(1)
        }.build()

        val wifiManager = context.getSystemService(WifiManager::class.java)
        val status = wifiManager.removeNetworkSuggestions(listOf(wifiNetworkSuggestion))

        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            return true
        } else {
            return false
        }
    }

    fun getSystemCertificates(): Array<X509Certificate>? {
        return try {
            val keyStore = KeyStore.getInstance("AndroidCAStore")
            keyStore.load(null)

            val certificates = mutableListOf<X509Certificate>()

            val aliases = keyStore.aliases()
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()

                val cert = keyStore.getCertificate(alias)
                if (cert is X509Certificate) {
                    certificates.add(cert)
                }
            }
            certificates.toTypedArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
