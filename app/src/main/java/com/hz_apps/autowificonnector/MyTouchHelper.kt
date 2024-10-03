package com.hz_apps.autowificonnector

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.hz_apps.autowificonnector.database.UserDao
import com.hz_apps.autowificonnector.database.WifiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface ItemTouchHelperCallback {
    fun onItemMove(fromPosition: Int, toPosition: Int)
    fun onItemMoved()
}
class MyItemTouchHelper(
    private val onItemMoved: (fromPosition: Int, toPosition: Int) -> Unit // Lambda for notifying movement
) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        onItemMoved(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        // No need for database logic here, just notify via callback
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Not handling swipe actions
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }
}
