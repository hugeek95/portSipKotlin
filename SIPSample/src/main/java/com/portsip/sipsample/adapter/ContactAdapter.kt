package com.portsip.sipsample.adapter

import android.R
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckedTextView
import com.portsip.sipsample.util.Contact

class ContactAdapter(private val mContext: Context, private val mContacts: List<Contact>) : BaseAdapter() {
    override fun getCount(): Int {
        return mContacts.size
    }

    override fun getItem(position: Int): Any {
        return if (mContacts.size > position) {
            mContacts[position]
        } else {
            
        }
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, view: View, viewGroup: ViewGroup): View {
        val contactview = LayoutInflater.from(mContext).inflate(R.layout.simple_list_item_single_choice, null) as CheckedTextView
        val contact = getItem(i) as Contact
        contactview.text = contact.sipAddr + "   " + contact.currentStatusToString()
        return contactview
    }

}