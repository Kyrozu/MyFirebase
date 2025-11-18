package ky.paba.myfirebase

import android.os.Bundle
import android.util.Log
import android.widget.Adapter
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class MainActivity : AppCompatActivity() {
    var dataProvinsi = ArrayList<daftarProvinsi>()
    var data: MutableList<Map<String, String>> = ArrayList()

    //    lateinit var lvAdapter: ArrayAdapter<daftarProvinsi>      // untuk arrayList
    lateinit var lvAdapter: SimpleAdapter   // untuk mutableList

    lateinit var _etProvinsi: EditText
    lateinit var _etIbuKota: EditText
    lateinit var _btnSimpan: Button
    lateinit var _lvData: ListView

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
            android.R.layout.simple_list_item_2,
            arrayOf("Pro", "Ibu"),
            intArrayOf(android.R.id.text1, android.R.id.text2)
        )

        _lvData.adapter = lvAdapter

        _btnSimpan.setOnClickListener {
            TambahData(db, _etProvinsi.text.toString(), _etIbuKota.text.toString())
        }

        readData(db)
    }

    fun TambahData(db: FirebaseFirestore, provinsi: String, ibuKota: String) {
        val dataBaru = daftarProvinsi(provinsi, ibuKota)

        db.collection("tbProvinsi")
            .document(_etProvinsi.text.toString())
            .set(dataBaru)
            .addOnSuccessListener {
                _etProvinsi.setText("")
                _etIbuKota.setText("")

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
                    data.add(itemData)
                }
                lvAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Log.d("Firebase", it.message.toString())
            }
    }

}