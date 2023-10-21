package com.hz_apps.autowificonnector

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.hz_apps.autowificonnector.WifiViewAdapter.myViewHolder
import com.hz_apps.autowificonnector.database.WifiConfig
import com.hz_apps.autowificonnector.databinding.ItemLayoutBinding

class WifiViewAdapter (
    private val context : Context,
    private val wifiConfigList : List<WifiConfig>,
    private val itemListeners: WifiViewAdapter.ItemListeners?
) : RecyclerView.Adapter<myViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): myViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_layout, parent, false);
        return myViewHolder(view)
    }

    override fun onBindViewHolder(holder: myViewHolder, position: Int) {
        val wifiConfig = wifiConfigList[position]
        holder.number.text = (position + 1 ).toString() + " -"
        holder.ssid.text = wifiConfig.ssid
        holder.identity.text = wifiConfig.identity
        holder.itemView.setOnClickListener {

        }

        holder.itemView.setOnClickListener {
            itemListeners?.onItemClicked(position)
        }

        holder.editBtn.setOnClickListener {
            if (wifiConfig.isEditAllowed) {
                val intent = Intent(context, AddViewActivity::class.java)
                intent.putExtra("id", wifiConfig.id)
                intent.putExtra("ssid", wifiConfig.ssid)
                intent.putExtra("identity", wifiConfig.identity)
                intent.putExtra("password", wifiConfig.password)
                context.startActivity(intent)
            }else {
                Toast.makeText(context, "You can't edit this", Toast.LENGTH_SHORT).show()
            }

        }
    }
    override fun getItemCount(): Int {
        return wifiConfigList.size
    }

    interface ItemListeners {
        fun onItemClicked(position : Int)
        fun onItemLongClick(position: Int)
    }

    class myViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val number : TextView = itemView.findViewById(R.id.number)
        val ssid : TextView = itemView.findViewById(R.id.wifi_ssid)
        val identity : TextView = itemView.findViewById(R.id.identity)
        val editBtn : ImageButton = itemView.findViewById(R.id.edit_button)
    }
}
