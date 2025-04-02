package com.example.f2sample.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.example.f2sample.R
import com.example.f2sample.data.ProfileItem
import com.stripe.android.customersheet.injection.CustomerSheetViewModelModule_ResourcesFactory.resources

class ProfileAdapter(private val context: Context, private val items: MutableList<ProfileItem>) : BaseAdapter() {

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    fun addAll(newItems: List<ProfileItem>) {
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_profile, parent, false)
        val iconView: ImageView = view.findViewById(R.id.item_icon)
        val titleView: TextView = view.findViewById(R.id.item_title)
        val valueView: TextView = view.findViewById(R.id.item_value)

        val item = items[position]

        titleView.text = item.title // Set the title

        if (item.icon != null) {
            iconView.setImageResource(item.icon)
            iconView.visibility = View.VISIBLE
            valueView.visibility = View.GONE // Hide value when icon is present

            // Apply tint if iconTint is set
            if (item.iconTint != null) {
                ImageViewCompat.setImageTintList(iconView, android.content.res.ColorStateList.valueOf(item.iconTint))
            }
        } else {
            iconView.visibility = View.GONE // Hide the icon if no icon is provided

            if (item.value != null) {
                valueView.text = item.value // Set the value
                valueView.visibility = View.VISIBLE //show value if no icon
            } else {
                valueView.visibility = View.GONE // Hide the value if no value is provided and no icon.
            }
        }

        if (item.title in listOf("Body Profile", "Your Preference","Subscription", "Payment History", "Settings", "Logout", "Delete Account")) {
            titleView.setTypeface(null, android.graphics.Typeface.BOLD)
        }

        if (item.title == "Delete Account") {
            titleView.setTextColor(ContextCompat.getColor(context, R.color.error)) // Set text color to red
            iconView.setColorFilter(ContextCompat.getColor(context, R.color.error)) // Set icon color to red
        }



        return view
    }
}