package com.arifin.whatsapp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.arifin.whatsapp.activity.ConversationActivity
import com.arifin.whatsapp.R
import com.arifin.whatsapp.adapter.ChatsAdapter
import com.arifin.whatsapp.listener.ChatClickListener
import com.arifin.whatsapp.listener.FailureCallBack
import com.arifin.whatsapp.util.Chat
import com.arifin.whatsapp.util.DATA_CHATS
import com.arifin.whatsapp.util.DATA_USERS
import com.arifin.whatsapp.util.DATA_USER_CHATS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_chats.*


class ChatsFragment : Fragment(), ChatClickListener {

    private var chatsAdapter = ChatsAdapter(arrayListOf())
    private val firebaseDb = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var failureCallback: FailureCallBack? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chats, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (userId.isNullOrEmpty()){
            failureCallback?.onUserError()
        }

    }

    fun setFailureCallback(listener: FailureCallBack){
        failureCallback = listener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chatsAdapter.setOnItemClickListener(this)
        rv_chats.apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(context)
            adapter = chatsAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        firebaseDb.collection(DATA_USERS).document(userId!!)
            .addSnapshotListener{documentSnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException == null){
                    refreshChat()
                }
            }

    }

    private fun refreshChat() {
        firebaseDb.collection(DATA_USERS).document(userId!!).get()
            .addOnSuccessListener {
                if (it.contains(DATA_USER_CHATS)){
                    val patners = it[DATA_USER_CHATS]
                    val chats = arrayListOf<String>()

                    for (patner in (patners as HashMap<String, String>).keys){
                        if (patners[patner] != null){ // melakukan pengulangan untuk memperbaharui data dalam userChats
                            chats.add(patners[patner]!!)
                        }
                    }
                    chatsAdapter.updateChats(chats)
                }
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
    }

    fun newChat(partnerId: String) {
        firebaseDb.collection(DATA_USERS).document(userId!!).get()
            .addOnSuccessListener { userDocument ->
                val userChatPartners = hashMapOf<String, String>()
                if (userDocument[DATA_USER_CHATS] != null &&
                    userDocument[DATA_USER_CHATS] is HashMap<*, *>
                ) {
                    val userDocumentMap = userDocument[DATA_USER_CHATS] as HashMap<String, String>
                    if (userDocumentMap.containsKey(partnerId)) {
                        return@addOnSuccessListener
                    } else {
                        userChatPartners.putAll(userDocumentMap)
                    }
                }

                firebaseDb.collection(DATA_USERS).document(partnerId).get()
                    .addOnSuccessListener { partnerDocument ->
                        val partnerChatPartners = hashMapOf<String, String>()
                        if (partnerDocument[DATA_USER_CHATS] != null &&
                            partnerDocument[DATA_USER_CHATS] is HashMap<*, *>) {
                            val partnerDocumentMap =
                                partnerDocument[DATA_USER_CHATS] as HashMap<String, String>
                            partnerChatPartners.putAll(partnerDocumentMap)
                        }

                        val chatParticipants = arrayListOf(userId, partnerId)
                        val chat = Chat(chatParticipants)
                        val chatRef = firebaseDb.collection(DATA_CHATS).document()
                        val userRef = firebaseDb.collection(DATA_USERS).document(userId)
                        val partnerRef = firebaseDb.collection(DATA_USERS).document(partnerId)

                        userChatPartners[partnerId] = chatRef.id
                        partnerChatPartners[userId] = chatRef.id

                        val batch = firebaseDb.batch()
                        batch.set(chatRef, chat)
                        batch.update(userRef, DATA_USER_CHATS, userChatPartners)
                        batch.update(partnerRef, DATA_USER_CHATS, partnerChatPartners)
                        batch.commit()
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                    }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    override fun onChatCliked(
        chatId: String?,
        otherUserId: String?,
        chatsImageUrl: String?,
        chatsName: String?
    ) {
        startActivity(
            ConversationActivity.newIntent(
                context,
                chatId,
                chatsImageUrl,
                otherUserId,
                chatsName
            )
        )

    }
}