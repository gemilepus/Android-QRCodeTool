package com.example.user.QRCodeTool

import android.Manifest
import android.app.Activity
import androidx.appcompat.widget.AppCompatButton
import android.widget.EditText
import android.graphics.Bitmap
import android.annotation.SuppressLint
import android.os.Bundle
import com.example.user.QRCodeTool.VariableEditor
import android.view.View.OnTouchListener
import android.view.MotionEvent
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import android.content.Intent
import com.example.user.QRCodeTool.ScannerActivity
import android.view.WindowManager
import com.example.user.QRCodeTool.View.MyAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import android.os.Environment
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*

class MainActivity : Activity() {
    private var btn_save: AppCompatButton? = null
    private var QRCode_Str: String? = null
    private var mEditText: EditText? = null
    private var bit: Bitmap? = null
    private var mBright = false
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrcode)
        VariableEditor.ScanText = ""
        mEditText = findViewById<View>(R.id.editTextQRCode) as EditText
        mEditText!!.setOnTouchListener(OnTouchListener { v, event ->
            val DRAWABLE_RIGHT = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= mEditText!!.right - mEditText!!.compoundDrawables[DRAWABLE_RIGHT].bounds.width()) {
                    // Gets a handle to the clipboard service.
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    // Creates a new text clip to put on the clipboard
                    val clip = ClipData.newPlainText("text", mEditText!!.text)
                    // Set the clipboard's primary clip.
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this@MainActivity, "copied to clipboard", Toast.LENGTH_SHORT)
                        .show()
                    return@OnTouchListener true
                }
            }
            false
        })
        val btn_QRCode = findViewById<View>(R.id.btn_QRCode) as AppCompatButton
        btn_QRCode.setOnClickListener {
            hideSoftKeyboard()
            code
        }
        val btn_QRCode_scan = findViewById<View>(R.id.btn_QRCode_scan) as AppCompatButton
        btn_QRCode_scan.setOnClickListener {
            val intent = Intent(application, ScannerActivity::class.java)
            startActivity(intent)
        }
        val btn_screenBrightness = findViewById<View>(R.id.btn_screenBrightness) as AppCompatButton
        btn_screenBrightness.setOnClickListener {
            val layout = window.attributes
            if (mBright) {
                layout.screenBrightness = 0f
            } else {
                layout.screenBrightness = 0.5f
            }
            window.attributes = layout
            mBright = !mBright
        }
        btn_save = findViewById<View>(R.id.btn_save) as AppCompatButton
        btn_save!!.setOnClickListener { SaveImage(bit) }
        btn_save!!.visibility = View.GONE
        SetRecyclerView()
    }

    private val CodeArray = ArrayList<String?>()
    private var adapter: MyAdapter? = null
    private fun SetRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.mRecyclerView)
        recyclerView.setHasFixedSize(true)
        val rLayoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = rLayoutManager
        adapter = MyAdapter(CodeArray)
        recyclerView.adapter = adapter
        val itemDecoration: ItemDecoration =
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.ACTION_STATE_IDLE
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.absoluteAdapterPosition
                val toPos = target.absoluteAdapterPosition
                //adapter.notifyItemMoved(fromPos, toPos);
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (viewHolder != null) {
                    val textView = viewHolder.itemView.findViewById<TextView>(R.id.mtext)
                    Log.d("debug", "Value : " + textView.text.toString())
                    mEditText!!.setText(textView.text.toString())
                    code
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun SaveImage(finalBitmap: Bitmap?) {
        // permission check
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        val grant = ContextCompat.checkSelfPermission(this, permission)
        if (grant != PackageManager.PERMISSION_GRANTED) {
            val permission_list = arrayOfNulls<String>(1)
            permission_list[0] = permission
            ActivityCompat.requestPermissions(this, permission_list, 1)
        }
        val root = Environment.getExternalStorageDirectory().toString()
        val FileDir = File("$root/Pictures/QRCode")
        FileDir.mkdirs()
        val now = Date()
        val mNow = DateFormat.format("yyyy-MM-dd", now)
        val filename = QRCode_Str + "_" + mNow.toString() + ".jpg"
        val file = File(FileDir, filename)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            finalBitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
            Toast.makeText(this@MainActivity, "saved $filename", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this@MainActivity, "Error... ", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
        btn_save!!.visibility = View.GONE
    }

    // notify adapter
    val code: Unit
        get() {
            val ivCode = findViewById<View>(R.id.imageView) as ImageView
            val encoder = BarcodeEncoder()
            try {
                QRCode_Str = mEditText!!.text.toString()
                bit = encoder.encodeBitmap(
                    mEditText!!.text.toString(),
                    BarcodeFormat.QR_CODE,
                    500,
                    500
                )
                ivCode.setImageBitmap(bit)
                btn_save!!.visibility = View.VISIBLE
            } catch (e: WriterException) {
                e.printStackTrace()
            }
            if (!CodeArray.contains(QRCode_Str)) {
                CodeArray.add(QRCode_Str)
                if (CodeArray.size > 5) {
                    CodeArray.removeAt(0)
                }
                // notify adapter
                adapter!!.notifyDataSetChanged()
            }
        }

    fun hideSoftKeyboard() {
        if (currentFocus != null) {
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("debug", "onResume()")
        if (VariableEditor.ScanText != "") {
            mEditText!!.setText(VariableEditor.ScanText)
            code
            VariableEditor.ScanText = ""
        }
    }

    private fun takeScreenshot() {
        val now = Date()
        DateFormat.format("yyyy-MM-dd_hh:mm:ss", now)
        try {
            // image naming and path  to include sd card  appending name you choose for file
            val mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg"

            // create bitmap screen capture
            val v1 = window.decorView.rootView
            v1.isDrawingCacheEnabled = true
            val bitmap = Bitmap.createBitmap(v1.drawingCache)
            v1.isDrawingCacheEnabled = false
            val imageFile = File(mPath)
            val outputStream = FileOutputStream(imageFile)
            val quality = 100
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()
            openScreenshot(imageFile)
        } catch (e: Throwable) {
            // Several error may come out with file handling or DOM
            e.printStackTrace()
        }
    }

    private fun openScreenshot(imageFile: File) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        val uri = Uri.fromFile(imageFile)
        intent.setDataAndType(uri, "image/*")
        startActivity(intent)
    }
}