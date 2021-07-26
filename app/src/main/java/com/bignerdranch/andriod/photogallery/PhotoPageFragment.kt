package com.bignerdranch.andriod.photogallery

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity


private  const val ARG_URI = "photo_page_url"

class PhotoPageFragment:VisibleFragment() {
    private  lateinit var uri:Uri
    private lateinit var webView:WebView
    private lateinit var progressBar: ProgressBar
    private var parentActivity = PhotoPageActivity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uri = arguments?.getParcelable(ARG_URI)?: Uri.EMPTY
    }

    /*override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        parentActivity = PhotoPageActivity()
    }*/


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_photo_page,container,false)
        progressBar = view.findViewById(R.id.progressBar)
        progressBar.max = 100

        //IN THIS ORDER!!!
        webView = view.findViewById(R.id.web_view)
        webView.settings.javaScriptEnabled = true

        webView.webChromeClient = object:WebChromeClient(){

            //sets the progress bar to match the progress of the loading web page
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if(newProgress==100){
                    progressBar.visibility= View.GONE
                }else{
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                }
            }

            //sets the subtitle of the fragment as the title of the picture
            override fun onReceivedTitle(view: WebView?, title: String?) {
                (activity as AppCompatActivity).supportActionBar?.subtitle = title
            }


        }

        webView.webViewClient = WebViewClient()
        webView.loadUrl(uri.toString())
        parentActivity.onDataReady(webView)
        return view
    }

     fun getWebView():WebView{
        return webView
    }

    companion object{
        fun  newInstance(uri: Uri):PhotoPageFragment{
            return  PhotoPageFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_URI,uri)
                }
             }
        }




    }
}