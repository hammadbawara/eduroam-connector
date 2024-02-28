package com.hz_apps.autowificonnector

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import com.hz_apps.autowificonnector.WifiViewAdapter.myViewHolder
import com.hz_apps.autowificonnector.database.WifiConfig
import java.util.Collections

class WifiViewAdapter(
    private val context: Context,
    private val itemListeners: WifiViewAdapter.ItemListeners?
) : RecyclerView.Adapter<myViewHolder>() {

    private var connectedWifiId: Int = -1
    private var wifiConfigList: List<WifiConfig> = mutableListOf()
    private var connectedWifiIdPosition = -1

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        Collections.swap(wifiConfigList, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun setItemOrder(position: Int, order: Int) {
        wifiConfigList[position].order = order
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): myViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_layout, parent, false);
        return myViewHolder(view)
    }

    fun setWifiConfigList(wifiConfigList: List<WifiConfig>) {
        this.wifiConfigList = wifiConfigList
    }

    override fun onBindViewHolder(holder: myViewHolder, position: Int) {
        val wifiConfig = wifiConfigList[position]
        holder.number.text = (position + 1).toString() + " -"
        holder.ssid.text = wifiConfig.ssid
        holder.identity.text = wifiConfig.identity
        holder.itemView.setOnClickListener {

        }

        holder.itemView.setOnClickListener {
            itemListeners?.onItemClicked(position)
        }

        if (wifiConfig.id == connectedWifiId) {
            connectedWifiIdPosition = position
            holder.identity.setTextColor(context.getColor(R.color.colorPrimary))
        } else {
            holder.identity.setTextColor(context.getColor(R.color.black))
        }



        holder.editBtn.setOnClickListener {
            if (wifiConfig.isEditAllowed) {
                val intent = Intent(context, AddViewActivity::class.java)
                intent.putExtra("id", wifiConfig.id)
                intent.putExtra("ssid", wifiConfig.ssid)
                intent.putExtra("identity", wifiConfig.identity)
                intent.putExtra("password", wifiConfig.password)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "You can't edit this", Toast.LENGTH_SHORT).show()
            }

        }
    }

    fun setConnectedWiFiID(id: Int) {
        connectedWifiId = id
    }

    override fun getItemCount(): Int {
        return wifiConfigList.size
    }

    @UiThread
    fun updateWiFiID(id: Int) {
        notifyItemChanged(connectedWifiIdPosition)
        connectedWifiId = id
        for (i in wifiConfigList.indices) {
            if (wifiConfigList[i].id == id) {
                connectedWifiIdPosition = i
                notifyItemChanged(i)
                break
            }
        }
    }

    fun getWifiConfig(position: Int): WifiConfig {
        return wifiConfigList[position]
    }

    fun getWiFiConfigList(): List<WifiConfig> {
        return wifiConfigList
    }

    interface ItemListeners {
        fun onItemClicked(position: Int)
        fun onItemLongClick(position: Int)
    }

    class myViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val number: TextView = itemView.findViewById(R.id.number)
        val ssid: TextView = itemView.findViewById(R.id.wifi_ssid)
        val identity: TextView = itemView.findViewById(R.id.identity)
        val editBtn: ImageButton = itemView.findViewById(R.id.edit_button)
    }
}
