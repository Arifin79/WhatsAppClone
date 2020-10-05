package com.arifin.whatsapp.listener

interface ChatClickListener {
    fun onChatCliked(
        chatId: String?,
        otherUserId: String?,
        chatsImageUrl: String?,
        chatsName: String?)
}