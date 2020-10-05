package com.arifin.whatsapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arifin.whatsapp.R
import com.arifin.whatsapp.listener.ChatClickListener
import com.arifin.whatsapp.util.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_chats.*

class ChatsAdapter(val chats: ArrayList<String>): RecyclerView.Adapter<ChatsAdapter.ChatsViewHolder>(){

    private var chatClickListener: ChatClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)= ChatsViewHolder (
        LayoutInflater.from(parent.context).inflate(R.layout.item_chats, parent, false)
    )

    override fun getItemCount() = chats.size

    override fun onBindViewHolder(holder: ChatsViewHolder, position: Int) {
       holder.bindItem(chats[position], chatClickListener)
    }

    fun setOnItemClickListener(listener: ChatClickListener){
        chatClickListener = listener
        notifyDataSetChanged()
    }

    fun updateChats(updateChats: ArrayList<String>){
        chats.clear()
        chats.addAll(updateChats)
        notifyDataSetChanged()
    }

    class ChatsViewHolder(override val containerView: View):
    RecyclerView.ViewHolder(containerView), LayoutContainer {

        private val firebaseDb = FirebaseFirestore.getInstance()
        private val userId = FirebaseAuth.getInstance().currentUser?.uid
        private var partnerId: String? = null
        private var chatName: String? = null
        private var chatImageUrl: String? = null

        fun bindItem(chatId: String, listener: ChatClickListener?){
            proggress_layout_chats.visibility= View.VISIBLE
            proggress_layout_chats.setOnTouchListener { v, event -> true }

            firebaseDb.collection(DATA_CHATS)
                .document(chatId)
                .get()
                .addOnSuccessListener {
                    val chatPartipants = it[DATA_CHATS_PARTICIPANTS] //jika data dalam
                    if (chatPartipants != null) {  //chat{articipant tidak kosomg
                        for (participant in chatPartipants as ArrayList<String>) {
                            if (participant != null && !participant.equals(userId)) {
                                partnerId = participant
                                firebaseDb.collection(DATA_USERS).document(partnerId!!).get()
                                    .addOnSuccessListener {
                                        val user = it.toObject(User::class.java)
                                        chatImageUrl = user?.imageUrl
                                        chatName = user?.name
                                        txt_chats.text = user?.name
                                       // menghubungkan gambar dengan imageView , jika terjadi error gambar diset ic_use
                                        populateImage(img_chats.context,user?.imageUrl, img_chats, R.drawable.ic_user)
                                        proggress_layout_chats.visibility = View.GONE

                                    }
                                    .addOnFailureListener { e ->
                                        e.printStackTrace()
                                        proggress_layout_chats.visibility = View.GONE
                                    }
                            }
                        }
                    }
                    proggress_layout_chats.visibility = View.GONE
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    proggress_layout_chats.visibility = View.GONE
                }
            itemView.setOnClickListener {
                listener?.onChatCliked(chatId, userId, chatImageUrl, chatName)
            }
        }
    }
}