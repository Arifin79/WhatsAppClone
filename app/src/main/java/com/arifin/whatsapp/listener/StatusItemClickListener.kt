package com.arifin.whatsapp.listener

import com.arifin.whatsapp.StatusListElement

interface StatusItemClickListener {
    fun onItemClicked(statusElement: StatusListElement)
}