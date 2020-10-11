package com.gappein.sdk.ui.view.chatView

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.gappein.sdk.Gappein
import com.gappein.sdk.client.ChatClient
import com.gappein.sdk.model.Message
import com.gappein.sdk.model.User
import com.gappein.sdk.ui.R
import com.gappein.sdk.ui.base.ChatBaseView
import com.gappein.sdk.ui.view.chatView.adapter.MessageListAdapter
import com.gappein.sdk.ui.view.chatView.imageviewer.openImage
import com.gappein.sdk.ui.view.util.*
import com.giphy.sdk.core.models.Media
import com.giphy.sdk.ui.GPHContentType
import com.giphy.sdk.ui.GPHSettings
import com.giphy.sdk.ui.themes.GPHTheme
import com.giphy.sdk.ui.themes.GridType
import com.giphy.sdk.ui.views.GiphyDialogFragment
import kotlinx.android.synthetic.main.activity_message.*
import java.io.File
import java.io.IOException


class MessageListActivity : AppCompatActivity(), ChatBaseView {

    private var photoFile: File? = null
    private lateinit var adapter: MessageListAdapter
    private val chats = mutableListOf<Message>()
    val TAG = "MessageActivity"

    companion object {
        private const val REQUEST_TAKE_PHOTO = 1
        private const val REQUEST_GALLERY_PHOTO = 2
        private const val CHANNEL_ID = "channelId"
        private const val RECEIVER = "receiver"
        private const val DEFAULT_STRING = ""
        private val EMPTY_USER = User()
        private const val CAMERA_PERMISSION_CODE = 100
//        const val API_KEY = "aNfHztXC7KA73PjHGgzHN9rYvuSFrJye"
        /**
         * Returns intent of MessageListActivity
         *
         */
        @JvmStatic
        fun buildIntent(context: Context, channelId: String, receiver: User) =
            Intent(context, MessageListActivity::class.java).apply {
                putExtra(CHANNEL_ID, channelId)
                putExtra(RECEIVER, receiver)
            }
    }

    private val mediaTypeConfig = arrayOf(
        GPHContentType.gif,
        GPHContentType.sticker,
        GPHContentType.text,
        GPHContentType.recents
    )

    private val settings = GPHSettings(
        gridType = GridType.waterfall,
        useBlurredBackground = false,
        theme = GPHTheme.Light,
        stickerColumnCount = 3,
        mediaTypeConfig = mediaTypeConfig
    )


    private val channelId by lazy { intent.getStringExtra(CHANNEL_ID) ?: DEFAULT_STRING }
    private val receiver by lazy { intent.getParcelableExtra(RECEIVER) ?: EMPTY_USER }
    private val currentUser by lazy { ChatClient.getInstance().getUser() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)
        setupUI()
        setupRecyclerView()
        fetchMessages()
        setupSendMessageListener()
        setupTextChangeListener()
        setupGifMessageListener()
    }

    // Function to pass the Gif into the sendMessage() fun
    private fun gifSelectionListener() = object : GiphyDialogFragment.GifSelectionListener {
        override fun onGifSelected(
            media: Media,
            searchTerm: String?,
            selectedContentType: GPHContentType
        ) {
//            GPHCore.gifById(media.id) { result, e ->
//                if (result != null) {
//                    result.data?.embedUrl?.let { setupGifMessageListener(it) }
//                }
//                e.let {}
//            }
            media.embedUrl?.let {
                ChatClient.getInstance().sendMessage(it, receiver.token, {
                    Log.d(TAG, "--- Sent message: " + media.embedUrl)
                }, {
                    Log.d(TAG, "--- Caught Exception Message: " + it.message)
                })
            }
        }

        override fun onDismissed(selectedContentType: GPHContentType) {
            Log.d(TAG, "onDismissed")
        }

        override fun didSearchTerm(term: String) {
            Log.d(TAG, "didSearchTerm " + term)
        }
    }

    private fun setupTextChangeListener() {
        editTextChatBox.addTypeChangeListener { isUserTyping ->
            if (isUserTyping) {
                ChatClient.getInstance().setTypingStatus(channelId, currentUser.token, true) {
                    //handle onSuccess
                }
            } else {
                ChatClient.getInstance().setTypingStatus(channelId, currentUser.token, false) {
                    //handle onSuccess
                }
            }
        }
    }

    private fun setupUI() {
        toolbar.init {
            channelId
        }
    }

    private fun setupSendMessageListener() {
        buttonSend.setOnClickListener {
            val message = editTextChatBox.text.toString()
            if (message.isNotEmpty()) {
                ChatClient.getInstance().sendMessage(message, receiver.token, {
                    editTextChatBox.text.clear()
                }, {

                })
            }
        }
        toolbar.setOnBackPressed {
            onBackPressed()
        }

        imageButtonAttach.setOnClickListener {
            checkForPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE)
        }
    }

    // Function to setup GiphyDialogFragment
    // Hides the button if Giphy API Key is not provided
    private fun setupGifMessageListener() {
        if (Gappein.isAPIProvided != "") {
            gifSend.setOnClickListener {
                val gifDialog = GiphyDialogFragment.newInstance(settings)
                gifDialog.gifSelectionListener = gifSelectionListener()
                gifDialog.show(supportFragmentManager, "gifs_dialog")
            }
        } else {
            gifSend.hide()
        }
    }

    private fun checkForPermission(camera: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this, camera) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(camera), requestCode)
        } else {
            dispatchTakePictureIntent()
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageListAdapter(chatClient = ChatClient.getInstance(), onImageClick = {
            openImage(this, it)
        }, onMessageClick = {
            ChatClient.getInstance().deleteMessage(channelId, it) {
                //Handle onSuccess of Delete
            }
        }, onMessageLike = {
            ChatClient.getInstance().likeMessage(channelId, it) {

            }
        })
        recyclerViewMessages.layoutManager = LinearLayoutManager(this@MessageListActivity)
        recyclerViewMessages.adapter = adapter

    }

    private fun fetchMessages() {
        ChatClient.getInstance().getMessages(channelId) {
            chats.run {
                clear()
                addAll(it)
                adapter.addAll(this)
                if (this.isNotEmpty()) {
                    recyclerViewMessages.smoothScrollToPosition(this.size - 1)
                }
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.permission_deined),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
            if (photoFile != null) {
                val photoURI: Uri = FileProvider.getUriForFile(this, "Gappein.provider", photoFile)
                this.photoFile = photoFile
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {

            if (requestCode == REQUEST_TAKE_PHOTO) {
                sendImageMessage(photoFile)

            } else if (requestCode == REQUEST_GALLERY_PHOTO) {
                val selectedImage = data?.data
                photoFile = File(getRealPathFromUri(selectedImage))
                sendImageMessage(photoFile)
            }
        }
    }

    private fun sendImageMessage(photo: File?) {
        if (photo != null) {
            val file = ImageCompressor(this).compressToFile(photo)
            file?.toUri()?.let {
                ChatClient.getInstance().sendMessage(it, receiver.token, {
                    progress.hide()
                }, {
                    progress.show()
                }, {
                    progress.hide()
                })
            }
        }
    }

    override fun getClient() = ChatClient.getInstance()

}