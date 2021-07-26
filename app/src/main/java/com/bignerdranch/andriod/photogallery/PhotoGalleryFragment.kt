package com.bignerdranch.andriod.photogallery

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.GridLayout
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.bignerdranch.andriod.photogallery.api.FlickrApi
import com.bignerdranch.andriod.photogallery.api.FlickrResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

private  const val TAG = "PhotoGalleryFragment"
private  const val POLL_WORK = "POLL_WORK"

class PhotoGalleryFragment :VisibleFragment(){

    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var photoGalleryViewModel: PhotoGalleryViewModel
    private lateinit var thumbNailDownloader: ThumbNailDownloader<PhotoHolder>
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setTitle("Downloading photos")
        progressDialog.setMessage("It might take a few seconds..")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);


        retainInstance = true
        setHasOptionsMenu(true)

        photoGalleryViewModel = ViewModelProviders.of(this).get(PhotoGalleryViewModel::class.java)
        val responseHandler= Handler()
        thumbNailDownloader = ThumbNailDownloader(responseHandler){
            photoHolder,bitmap ->
            val drawable = BitmapDrawable(resources,bitmap)
            photoHolder.bindDrawable(drawable)

        }
        lifecycle.addObserver(thumbNailDownloader.fragmentLifecycleObserver)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {



        //viewLifecycleOwner.lifecycle.addObserver(thumbNailDownloader.viewLifecycleObserver)

        val view = inflater.inflate(R.layout.fragment_photo_gallery,container,false)
        photoRecyclerView = view.findViewById(R.id.photo_recycler_view)
        photoRecyclerView.layoutManager = GridLayoutManager(context,3)

        if(photoRecyclerView!=null){
            viewLifecycleOwnerLiveData.observe(viewLifecycleOwner, Observer { it.lifecycle.addObserver(thumbNailDownloader.viewLifecycleObserver) })
        }

        return  view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoGalleryViewModel.galleryItemLiveData.observe(
            viewLifecycleOwner, Observer { galleryItems ->
                Log.d(TAG,"Have gallery items from ViewModel $galleryItems")
                progressDialog.dismiss()
                photoRecyclerView.adapter = PhotoAdapter(galleryItems)
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewLifecycleOwner.lifecycle.removeObserver(
            thumbNailDownloader.viewLifecycleObserver
        )
    }



    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(thumbNailDownloader.fragmentLifecycleObserver)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_photo_gallery,menu)

        val searchItem: MenuItem = menu.findItem(R.id.menu_item_search)
        val searchView = searchItem.actionView as SearchView
        searchView.apply {
            setOnQueryTextListener(object :
                SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(queryText: String):
                        Boolean {
                    Log.d(TAG, "QueryTextSubmit: $queryText")
                    progressDialog.show()
                    photoGalleryViewModel.fetchPhotos(queryText)

                    clearFocus()
                    return true
                }
                override fun onQueryTextChange(queryText: String):
                        Boolean {
                    Log.d(TAG, "QueryTextChange: $queryText")
                    return false
                }
            })

            setOnFocusChangeListener{ v,hasFocus ->
                if(!hasFocus){
                    searchItem.collapseActionView()
                }
            }

            setOnClickListener{
                searchView.setQuery(photoGalleryViewModel.searchTerm,false)
            }
        }

        val toggleItem = menu.findItem(R.id.menu_item_toggle_polling)
        val isPolling = QueryPreferences.isPolling(requireContext())
        if(isPolling){
            toggleItem.setTitle(R.string.stop_polling)
        }else{
            toggleItem.setTitle(R.string.start_polling)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
       return when (item.itemId){
           R.id.menu_item_clear ->{
               photoGalleryViewModel.fetchPhotos("")
               true
           }

           R.id.menu_item_toggle_polling -> {
               val isPolling = QueryPreferences.isPolling(requireContext())
               if (isPolling) {
                   WorkManager.getInstance().cancelUniqueWork(POLL_WORK)
                   QueryPreferences.setPolling(requireContext(), false)
               } else {
                   val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build()
                   val periodicRequest = PeriodicWorkRequest.Builder(PollWorker::class.java, 15, TimeUnit.MINUTES)
                       .setConstraints(constraints).build()
                   WorkManager.getInstance().enqueueUniquePeriodicWork(POLL_WORK, ExistingPeriodicWorkPolicy.KEEP, periodicRequest)
                   QueryPreferences.setPolling(requireContext(), true)
               }
               activity?.invalidateOptionsMenu()
               return true
           }

           else -> return super.onOptionsItemSelected(item)
       }
    }

    private inner class PhotoHolder(itemImageView:ImageView):RecyclerView.ViewHolder(itemImageView),View.OnClickListener{
        val bindDrawable: (Drawable) ->Unit = itemImageView::setImageDrawable
        private  lateinit var galleryItem:GalleryItem

        init{
            itemView.setOnClickListener(this)
        }

        fun bindGalleryItem(item:GalleryItem){
            galleryItem= item
        }

        override fun onClick(view: View) {
          val intent = PhotoPageActivity.newIntent(requireContext(),galleryItem.photoPageUri)
            startActivity(intent)
        }
    }



    private inner class PhotoAdapter(private  val galleryItems :List<GalleryItem>) : RecyclerView.Adapter<PhotoHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
            val view = layoutInflater.inflate(R.layout.list_item_gallery,parent,false) as ImageView
            return PhotoHolder(view)

        }

        override fun getItemCount(): Int {
            return  galleryItems.size
        }

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            val galleryItem = galleryItems[position]
            holder.bindGalleryItem(galleryItem)
            val placeholder:Drawable = ContextCompat.getDrawable(requireContext(),R.drawable.bill_up_close)?:ColorDrawable()
            holder.bindDrawable(placeholder)
            thumbNailDownloader.queueThumbnail(holder,galleryItem.url)
        }



    }




    companion object{
        fun newInstance() = PhotoGalleryFragment()
    }
}