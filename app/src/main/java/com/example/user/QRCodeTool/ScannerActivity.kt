package com.example.user.QRCodeTool

import android.Manifest
import android.app.Activity
import me.dm7.barcodescanner.zxing.ZXingScannerView
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.view.ViewGroup
import com.example.user.QRCodeTool.VariableEditor
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.zxing.Result

class ScannerActivity : Activity(), ZXingScannerView.ResultHandler {
    private var mScannerView: ZXingScannerView? = null
    private var OnResult = 0
    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.activity_scanner)

        // permission check
        val permission = Manifest.permission.CAMERA
        val grant = ContextCompat.checkSelfPermission(this, permission)
        if (grant != PackageManager.PERMISSION_GRANTED) {
            val permission_list = arrayOfNulls<String>(1)
            permission_list[0] = permission
            ActivityCompat.requestPermissions(this, permission_list, 1)
        }
        val contentFrame = findViewById<View>(R.id.content_frame) as ViewGroup
        mScannerView = ZXingScannerView(this)
        contentFrame.addView(mScannerView)
    }

    override fun handleResult(rawResult: Result) {
        if (OnResult == 0) {
            OnResult = 1

            // Note:
            // * Wait 2 seconds to resume the preview.
            // * On older devices continuously stopping and resuming camera preview can result in freezing the app.
            // * I don't know why this is the case but I don't have the time to figure out.

            // mScannerView.stopCameraPreview();
            val handler = Handler()
            handler.postDelayed({ mScannerView!!.resumeCameraPreview(this@ScannerActivity) }, 10)
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Info")
            builder.setMessage(
                """
    Contents = ${rawResult.text}
    Format = ${rawResult.barcodeFormat}
    """.trimIndent()
            )
            VariableEditor.ScanText = rawResult.text
            builder.setNegativeButton("Cancel") { dialog, id ->
                dialog.dismiss()
                OnResult = 0
                mScannerView!!.resumeCameraPreview(this@ScannerActivity)
            }
            builder.setPositiveButton("OK") { dialog, id ->
                dialog.dismiss()
                OnResult = 0
                mScannerView!!.resumeCameraPreview(this@ScannerActivity)
                finish()
            }
            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun loadBitmapFromView(v: View): Bitmap {
        val w = v.width
        val h = v.height
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        v.layout(0, 0, w, h)
        v.draw(c)
        return bmp
    }

    public override fun onResume() {
        super.onResume()
        mScannerView!!.setResultHandler(this)
        mScannerView!!.startCamera()
    }

    public override fun onPause() {
        super.onPause()
        mScannerView!!.stopCamera()
    }
}