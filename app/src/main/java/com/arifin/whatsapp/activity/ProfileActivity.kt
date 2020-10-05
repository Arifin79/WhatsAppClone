package com.arifin.whatsapp.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.arifin.whatsapp.R
import com.arifin.whatsapp.util.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.activity_profile.proggress_layout

class ProfileActivity : AppCompatActivity() {

    private val firebaseDb = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private val firebaseAuth= FirebaseAuth.getInstance()
    private val firebaseStorage = FirebaseStorage.getInstance().reference //mengakses firebase
    private var imageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        imbtn_profile.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_CODE_PHOTO)
        }

        btn_back.setOnClickListener {
            val intent  = Intent(this, MainActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }



        if (userId.isNullOrEmpty()){ //jika userId null, ProfileActivity akan dihentikan
            finish()                 //dan kembali ke MainActivity
        }

        proggress_layout.setOnTouchListener { v, event -> true }
        btn_apply.setOnClickListener {
            onApply()
        }

        btn_delete_account.setOnClickListener {
            onDelete()
        }

        populateInfo()

        storeImage()



    }

    private fun storeImage() {

    }

    private fun populateInfo() {
        proggress_layout.visibility = View.VISIBLE
        firebaseDb.collection(DATA_USERS).document(userId!!).get()//membaca data table user
            .addOnSuccessListener { //jika proses berhasil data akan ditampung lalu
                val user = it.toObject(User::class.java) //data di pasasng ke EditText
                imageUrl = user?.imageUrl //menampung imageUrl dengan property image Url
                edt_name_profile.setText(user?.name, TextView.BufferType.EDITABLE)
                edt_email_profile.setText(user?.email, TextView.BufferType.EDITABLE)
                edt_phone_profile.setText(user?.phone, TextView.BufferType.EDITABLE)
                if (imageUrl != null) { //jika imageUrl tidak null, gambar dipasangkan ke imageview
                    populateImage(this, user?.imageUrl, img_profile, R.drawable.ic_user)
                }
                proggress_layout.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                finish()
            }

    }



    private fun onApply(){
        proggress_layout.visibility = View.VISIBLE
        val name = edt_name_profile.text.toString()
        val email = edt_email_profile.text.toString()
        val phone = edt_phone_profile.text.toString() // data text dalam EditText akan diubah
        val map = HashMap<String, Any>()                      //menjadi String lalu ditampung di variable
        map[DATA_USERS_NAME]=name                             //yang nantinya di koleksi oleh HashMap
        map[DATA_USERS_EMAIL]=email                           //untuk kemudian dikirim ke table user
        map[DATA_USERS_PHONE]=phone                           //di database firebase sebagai pembaruan

        firebaseDb.collection(DATA_USERS).document(userId!!).update(map) //perintah update
            .addOnSuccessListener {
                Toast.makeText(this, "Update Successful", Toast.LENGTH_SHORT).show()
                finish()
            }

            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show()
                proggress_layout.visibility = View.GONE
            }
    }

    private fun onDelete() {
        proggress_layout.visibility = View.VISIBLE
        AlertDialog.Builder(this) //ketika tombol DELETE diklik, AlertDialog akan muncul
            .setTitle("Delete Account")   //Title AlertDialog
            .setMessage("This will delete yout profile Information.Are you sure?") //pesan info
            .setPositiveButton("Yes") {dialog, which -> //Button yes
                firebaseDb.collection(DATA_USERS).document(userId!!).delete()
                firebaseStorage.child(DATA_IMAGES).child(userId).delete()

                var intent = Intent(this, LoginActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
                firebaseAuth.currentUser?.delete() //perintah menghapus userId yang sedang digunakan
                    ?.addOnSuccessListener {
                        finish()
                    }
                    ?.addOnFailureListener {
                        finish()
                    }
                proggress_layout.visibility = View.GONE
            }

            .setNegativeButton("No"){dialog, which ->
                proggress_layout.visibility = View.GONE
            }

            .setCancelable(false) //AlertDialog tidak dapat hilang kecuali menekan button yes/no
            .show() //memunculkan AlertDialog
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PHOTO){
            storeImage(data?.data) //method storageImage dijalankan setelah pengguna memilih gambar
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun storeImage(uri: Uri?){
        if (uri != null){ //jika uri dari image tidak null
            Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show()
            proggress_layout.visibility = View.VISIBLE
            val filePath = firebaseStorage.child(DATA_IMAGES).child(userId!!)//membuat folder

            filePath.putFile(uri)
                .addOnSuccessListener {
                    filePath.downloadUrl
                        .addOnSuccessListener {
                            val url = it.toString()
                            firebaseDb.collection(DATA_USERS)
                                .document(userId)
                                .update(DATA_USERS_IMAGE_URL, url)
                                .addOnSuccessListener {
                                    imageUrl = url
                                    populateImage(this, imageUrl, img_profile, R.drawable.ic_user)
                                }
                            proggress_layout.visibility = View.GONE
                        }
                        .addOnFailureListener {
                            onUploadFailured()
                        }
                }
                .addOnFailureListener {
                    onUploadFailured()
                }

        }
    }

    private fun onUploadFailured() {
        Toast.makeText(this, "Image upload failed. Please try again later.",
        Toast.LENGTH_SHORT).show()
        proggress_layout.visibility= View.GONE
    }

    override fun onResume() {
        super.onResume()
        if (firebaseAuth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}