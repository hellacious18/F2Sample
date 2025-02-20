package com.example.f2sample

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.f2sample.data.ProfileItem

class ProfileAdapter(private val context: Context, private val items: List<ProfileItem>) : BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_profile, parent, false)
        val iconView: ImageView = view.findViewById(R.id.item_icon)
        val titleView: TextView = view.findViewById(R.id.item_title)

        val item = items[position]
        iconView.setImageResource(item.icon)
        titleView.text = item.title

        return view
    }
}
