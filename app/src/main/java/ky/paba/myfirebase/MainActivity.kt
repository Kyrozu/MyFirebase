package ky.paba.myfirebase

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SimpleAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.io.File

class MainActivity : AppCompatActivity() {
    var dataProvinsi = ArrayList<daftarProvinsi>()
    var data: MutableList<Map<String, Any>> = ArrayList()

    //    lateinit var lvAdapter: ArrayAdapter<daftarProvinsi>      // untuk arrayList
    lateinit var lvAdapter: SimpleAdapter   // untuk mutableList

    lateinit var _etProvinsi: EditText
    lateinit var _etIbuKota: EditText
    lateinit var _btnSimpan: Button
    lateinit var _lvData: ListView
    lateinit var _ivUpload: ImageView
    lateinit var _progressBarUpload: ProgressBar


    private var CLOUDINARY_CLOUD_NAME = "dsfakaa7m"
    private var UNSINGED_UPLOAD_PRESET = "preset1"
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // inisialisasi data base
        val db = Firebase.firestore

        _etProvinsi = findViewById(R.id.etProvinsi)
        _etIbuKota = findViewById(R.id.etIbuKota)
        _btnSimpan = findViewById(R.id.btnSimpan)
        _lvData = findViewById(R.id.lvData)

        _ivUpload = findViewById(R.id.ivUpload)

        _progressBarUpload = findViewById<ProgressBar>(R.id.progressBarUpload)

        // pakai arrayList
//        lvAdapter = ArrayAdapter<daftarProvinsi>(
//            this,
//            android.R.layout.simple_list_item_1,
//            dataProvinsi
//        )

        // pakai mutableList
        lvAdapter = SimpleAdapter(
            this,
            data,
//            android.R.layout.simple_list_item_2   // layout bawaan android
            R.layout.list_item_with_image,  // custom layout
            arrayOf("Img", "Pro", "Ibu"),
//            intArrayOf(android.R.id.text1, android.R.id.text2)    // layout bawaan android
            intArrayOf(R.id.imgLogo, R.id.text1, R.id.text2)    // custom layout
        )

        lvAdapter.setViewBinder { view, data, _ ->
            if (view.id == R.id.imgLogo) {
                val imgView = view as ImageView
                val defaultImage = com.google.android.gms.base.R.drawable
                    .common_google_signin_btn_icon_dark

                if (data is String && data.isNotEmpty()) {
                    Glide.with(this@MainActivity)
                        .load(data)
                        .placeholder(defaultImage)
                        .error(defaultImage)
                        .into(imgView)
                } else {
                    imgView.setImageResource(defaultImage)
                }
                return@setViewBinder true
            }
            false
        }

        _lvData.adapter = lvAdapter

        _lvData.setOnItemClickListener { adapterView, view, i, l ->
            _etProvinsi.setText(data[i].get("Pro").toString())
            _etIbuKota.setText(data[i].get("Ibu").toString())
        }

        _lvData.setOnItemLongClickListener { adapterView, view, i, l ->
            val namaProvinsi: String = data[i]["Pro"].toString()
            db.collection("tbProvinsi")
                .document(namaProvinsi)
                .delete()
                .addOnSuccessListener {
                    Log.d("Firebase", namaProvinsi + " Berhasil dihapus")
                    readData(db)
                }
                .addOnFailureListener {
                    Log.d("Firebase", it.message.toString())
                }
            true
        }

        _btnSimpan.setOnClickListener {
            if (selectedImageUri != null) {
                uploadToCloudinary(db, selectedImageUri!!)
            } else {
                TambahData(db, _etProvinsi.text.toString(), _etIbuKota.text.toString(), "")
            }
        }

        readData(db)

        val config: Map<String, String> =
            mapOf("cloud_name" to CLOUDINARY_CLOUD_NAME, "upload_preset" to UNSINGED_UPLOAD_PRESET)

        MediaManager.init(this, config)

        _ivUpload.setOnClickListener {
            showImagePickDialog()
        }
    }

    private val pickImageFromGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                _ivUpload.setImageURI(it)
            }
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) {
                selectedImageUri = cameraImageUri
                _ivUpload.setImageURI(cameraImageUri)
            }
        }

    private fun createImageUri(): Uri? {
        val imageFile = File(
            cacheDir,
            "temp_image_${System.currentTimeMillis()}.jpg"
        )
        return FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            imageFile
        )
    }

    private fun showImagePickDialog() {
        val options = arrayOf("Pilih dari Gallery", "Ambil Foto")
        AlertDialog.Builder(this)
            .setTitle("Pilih Gambar")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> pickImageFromGallery.launch("image/*")
                    1 -> {
                        createImageUri()?.let { uri ->
                            cameraImageUri = uri
                            takePicture.launch(uri)
                        }
                    }
                }
            }.show()
    }

    private fun uploadToCloudinary(db: FirebaseFirestore, uri: Uri) {
        MediaManager.get().upload(uri)
            .unsigned(UNSINGED_UPLOAD_PRESET)
            .option("folder", "cobaFirebase")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                    Log.d("Cloudinary", "Upload Start : $requestId")

                    runOnUiThread {
                        _progressBarUpload.visibility = View.VISIBLE
                        _progressBarUpload.progress = 0
                        _progressBarUpload.max = 100
                    }
                }

                override fun onProgress(
                    requestId: String?,
                    bytes: Long,
                    totalBytes: Long
                ) {
                    // biasa untuk progress bar
                    val progress = (bytes * 100 / totalBytes).toInt()
                    runOnUiThread {
                        _progressBarUpload.progress = progress
                    }
                }

                override fun onSuccess(
                    requestId: String?,
                    resultData: Map<*, *>?
                ) {
                    var url = resultData?.get("secure_url")?.toString()
                    Log.d("Cloudinary", "Upload Success : $url")

                    TambahData(
                        db,
                        _etProvinsi.text.toString(),
                        _etIbuKota.text.toString(),
                        url.toString()
                    )

                    runOnUiThread {
                        _progressBarUpload.visibility = View.GONE
                    }
                }

                override fun onError(
                    requestId: String?,
                    error: ErrorInfo?
                ) {
                    Log.d("Cloudinary", "Upload Error : ${error.toString()}")

                    runOnUiThread {
                        _progressBarUpload.visibility = View.GONE
                    }
                }

                override fun onReschedule(
                    requestId: String?,
                    error: ErrorInfo?
                ) {
                    Log.d("Cloudinary", "Upload Reschedule : ${error.toString()}")

                    runOnUiThread {
                        _progressBarUpload.visibility = View.GONE
                    }
                }

            }).dispatch()
        Log.d("Cloudinary", "Upload with preset : $UNSINGED_UPLOAD_PRESET")
    }

    fun TambahData(db: FirebaseFirestore, provinsi: String, ibuKota: String, imageUrl: String) {
        val dataBaru = daftarProvinsi(provinsi, ibuKota, imageUrl)

        db.collection("tbProvinsi")
            .document(_etProvinsi.text.toString())
            .set(dataBaru)
            .addOnSuccessListener {
                _etProvinsi.setText("")
                _etIbuKota.setText("")
                _ivUpload.setImageResource(
                    com.google.android.gms.base.R.drawable
                        .common_google_signin_btn_icon_dark
                )
                selectedImageUri = null

                readData(db)
                Log.d("Firebase", dataBaru.provinsi + " Berhasil ditambahkan")
            }
            .addOnFailureListener {
                Log.d("Firebase", it.message.toString())
            }
    }

    fun readData(db: FirebaseFirestore) {
        db.collection("tbProvinsi")
            .get()
            .addOnSuccessListener { result ->
                // Tidak Pakai MutableList
//                dataProvinsi.clear()

                // Pakai MutableList
                data.clear()
                for (item in result) {
                    // Tidak Pakai MutableList
//                    val itemData = daftarProvinsi(
//                        item.data.get("provinsi").toString(),
//                        item.data.get("ibuKota").toString()
//                    )
//                    dataProvinsi.add(itemData)

                    // Pakai MutableList
                    val itemData: MutableMap<String, String> = HashMap(2)
                    itemData["Pro"] = item.data.get("provinsi").toString()
                    itemData["Ibu"] = item.data.get("ibuKota").toString()
                    itemData["Img"] = item.data.get("imageUrl").toString()

                    Log.d("Firebase", item.data.get("provinsi").toString())
                    data.add(itemData)
                }
                lvAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Log.d("Firebase", it.message.toString())
            }
    }

}