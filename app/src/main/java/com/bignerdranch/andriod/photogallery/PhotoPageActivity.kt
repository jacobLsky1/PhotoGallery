package com.bignerdranch.andriod.photogallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.fragment_photo_page.*

class PhotoPageActivity : AppCompatActivity(), CommunicationInterface {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_page)
        val fm = supportFragmentManager
        val currentFragment =
            fm.findFragmentById(R.id.fragment_container)
        if (currentFragment == null) {

            val fragment = intent.data?.let { PhotoPageFragment.newInstance(it) }
            if (fragment != null) {
                fm.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit()
            }
        }
    }
    override fun onDataReady(webviewFromFragment: WebView) {

    }

    /*override fun onBackPressed() {
        if(webView.canGoBack()){
            webView.goBack()
        }else {
            super.onBackPressed()
        }
    }*/
    companion object {
        fun newIntent(context: Context, photoPageUri: Uri): Intent {
            return Intent(context,
                PhotoPageActivity::class.java).apply {
                data = photoPageUri!!
            }
        }
    }


}