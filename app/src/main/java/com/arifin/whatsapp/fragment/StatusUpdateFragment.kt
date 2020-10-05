package com.arifin.whatsapp.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.arifin.whatsapp.R
import com.arifin.whatsapp.util.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_status_update.*
import kotlin.collections.HashMap

class StatusUpdateFragment : Fragment() {

    private val firebaseDB = FirebaseFirestore.getInstance()
    private val firebaseStorage = FirebaseStorage.getInstance().reference
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var imageUrl = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_status_update, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        proggress_layout.setOnTouchListener { v, event -> true }
        fab_status.setOnClickListener { onUpdate() }
        populateImage(
            context,
            imageUrl,
            img_status_update
        )

        lay_status.setOnClickListener {
            if (isAdded) {
                val intent =
                    Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivityForResult(intent,
                    REQUEST_CODE_PHOTO
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PHOTO){
            storeImage(data?.data)
        }
    }

    fun storeImage(imageUri: Uri?) {
        if (imageUri != null && userId != null){
            Toast.makeText(
                activity,
                "Uploading...",
                Toast.LENGTH_SHORT
            ).show()
            proggress_layout.visibility = View.VISIBLE

            val filePath = firebaseStorage.child(DATA_IMAGES).child("${userId}_status")

            filePath.putFile(imageUri)
                .addOnSuccessListener {
                    filePath.downloadUrl
                        .addOnSuccessListener { taskSnapshot ->
                            val url = taskSnapshot.toString()

                            firebaseDB.collection(DATA_USERS)
                                .document(userId)
                                .update(DATA_USERS_STATUS, url)
                                .addOnSuccessListener {
                                    imageUrl = url
                                    populateImage(
                                        context,
                                        imageUrl,
                                        img_status_update
                                    )
                                }
                            proggress_layout.visibility= View.GONE
                        }
                        .addOnFailureListener { onUploadFailure() }
                }

                .addOnFailureListener { onUploadFailure() }
        }
    }



    fun onUpdate() {
        proggress_layout.visibility = View.VISIBLE
        val map = HashMap<String, Any>()
        map[DATA_USERS_STATUS] = edt_status_update.text.toString()
        map[DATA_USERS_STATUS_URL] = imageUrl
        map[DATA_USERS_STATUS_TIME] = getTime()

        firebaseDB.collection(DATA_USERS)
            .document(userId!!)
            .update(map)
            .addOnSuccessListener {
                proggress_layout.visibility = View.GONE
                Toast.makeText(
                    activity,
                    "Status updated.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                proggress_layout.visibility = View.GONE
                Toast.makeText(
                    activity,
                    "Status update failed.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun onUploadFailure() {
        Toast.makeText(
            activity, "Image upload failed. Please try again later",
            Toast.LENGTH_SHORT
        ).show()
        proggress_layout.visibility = View.GONE
    }

}