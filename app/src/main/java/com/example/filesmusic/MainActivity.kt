package com.example.filesmusic

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import java.io.File

class MainActivity : AppCompatActivity() {
    private var granted: Boolean = false
    private var root: File? = null
    private lateinit var pathView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = "${getString(R.string.app_name)} (Internal Explorer)"
        pathView = findViewById(R.id.txt_currentPath)
        pathView.setOnClickListener { requestPermissionThenStart() }
        findViewById<Button>(R.id.btn_toParent).setOnClickListener { pathUpOneLevel() }
        findViewById<Button>(R.id.btn_play).setOnClickListener { startPlayerHere() }
        findViewById<Button>(R.id.btn_exit).setOnClickListener { finishMainActivity() }
        requestPermissionThenStart()
        // needed for marquee
        pathView.isSelected = true
    }

    private fun requestPermissionThenStart() {
        if (granted) {
            Toast.makeText(this, "Storage access already granted.", Toast.LENGTH_SHORT).show()
            return
        }
        if (checkPermission()) {
            granted = true
            permissionGranted()
        } else {
            requestPermission()
        }
    }

    private fun checkPermission(): Boolean {
        return checkSelfPermission(READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                Toast.makeText(this, "Storage access allowed.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Storage access denied.", Toast.LENGTH_SHORT).show()
            }
        }

    private fun requestPermission(): Boolean {
        when {
            checkPermission() -> {
                // You can use the API that requires the permission.
                return true
            }

            shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected, and what
                // features are disabled if it's declined. In this UI, include a
                // "cancel" or "no thanks" button that lets the user continue
                // using your app without granting the permission.
                Toast.makeText(
                    this,
                    "Storage access is required. Please allow from Settings.",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }

            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(READ_EXTERNAL_STORAGE)
                return false
            }
        }
    }

    // replace the default FileExplorerFragment started with null path to storage path
    private fun permissionGranted() {
        onSelectNewStorage(getInternalStoragePath())
    }

    fun updatePathView(file: File?) {
        root = file
        pathView.text = file?.absolutePath ?: getString(R.string.default_path)
    }

    private fun pathUpOneLevel() {
        supportFragmentManager.popBackStack()
    }

    private fun onSelectNewStorage(path: File?): Boolean {
        if (path === null) {
            return false
        }
        if (!granted) {
            Toast.makeText(this, "No Permission.", Toast.LENGTH_SHORT).show()
            return false
        }
        updatePathView(path)
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction().replace(
            R.id.fragmentContainerView4, FileExplorerFragment.newInstance(path)
        ).commit()
        return true
    }

    private fun startPlayerHere() {
        if (root !== null) {
            root!!.absolutePath.let {
                startActivity(Intent(this, AudioPlayerActivity::class.java).apply {
                    putExtra("path", it)
                })
            }
            return
        }
        Toast.makeText(this, "File explorer navigation error.", Toast.LENGTH_SHORT).show()
    }

    private fun finishMainActivity() {
        // pop all backstack
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        this.finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_storage, menu)
        if (getExternalFilesDirs(null).getOrNull(1) !== null) {
            menu!!.findItem(R.id.menu_storage_sdcard).run {
                isEnabled = true
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_storage_internal -> if (onSelectNewStorage(getInternalStoragePath())) {
                item.isChecked = true
                supportActionBar?.title = "${getString(R.string.app_name)} (Internal Explorer)"
            } else Toast.makeText(this, "Internal Storage access error.", Toast.LENGTH_SHORT).show()

            R.id.menu_storage_sdcard -> if (onSelectNewStorage(getSdCardStoragePath())) {
                item.isChecked = true
                supportActionBar?.title = "${getString(R.string.app_name)} (SD Explorer)"
            } else Toast.makeText(this, "SD Card access error..", Toast.LENGTH_SHORT).show()
        }
        return true
    }


    private fun getInternalStoragePath(): File? {
        return getExternalFilesDirs(null).getOrNull(0)?.parentFile?.parentFile?.parentFile?.parentFile
    }

    private fun getSdCardStoragePath(): File? {
        return getExternalFilesDirs(null).getOrNull(1)?.parentFile?.parentFile?.parentFile?.parentFile
    }
}
