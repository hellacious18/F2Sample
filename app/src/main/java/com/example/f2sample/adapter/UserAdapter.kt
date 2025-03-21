package com.example.f2sample.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.f2sample.R
import com.example.f2sample.UserDetailsActivity
import com.example.f2sample.data.User

class UserAdapter(private val users: MutableList<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvUserName)
        val email: TextView = view.findViewById(R.id.tvUserEmail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.name.text = user.name
        holder.email.text = user.email

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, UserDetailsActivity::class.java).apply {
                putExtra("name", user.name)
                putExtra("email", user.email)
                putExtra("birthdate", user.birthdate)
                putExtra("gender", user.gender)
                putExtra("height", user.height)
                putExtra("weight", user.weight)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }
}
