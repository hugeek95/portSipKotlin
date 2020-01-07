package com.portsip.sipsample.util

import android.text.TextUtils
import java.util.*

class ContactManager private constructor() {
    val MAX_LINES = 10
    var contacts: MutableList<Contact> = ArrayList()
    fun getContacts(): List<Contact> {
        return contacts
    }

    fun addContact(contact: Contact) {
        contacts.add(contact)
    }

    fun removeContact(contact: Contact?) {
        contacts.remove(contact)
    }

    fun removeContactAt(index: Int) {
        contacts.removeAt(index)
    }

    fun removeAll() {
        contacts.clear()
    }

    fun findContactBySipAddr(sipAddr: String): Contact? {
        var sipAddr = sipAddr
        if (TextUtils.isEmpty(sipAddr)) return null
        for (contact in contacts) {
            sipAddr = sipAddr.replaceFirst("sip:".toRegex(), "")
            val contactAddr = contact.sipAddr!!.replaceFirst("sip:".toRegex(), "")
            if (sipAddr == contactAddr) return contact
        }
        return null
    }

    companion object {
        private var mInstance: ContactManager? = null
        private val locker = Any()
        fun Instance(): ContactManager? {
            if (mInstance == null) {
                synchronized(locker) {
                    if (mInstance == null) {
                        mInstance = ContactManager()
                    }
                }
            }
            return mInstance
        }
    }
}