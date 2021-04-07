package com.gappein.app.ui.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.gappein.app.R
import com.gappein.compose.components.channel.ChannelList
import com.gappein.compose.viewmodel.channel.ChannelViewModel
import com.gappein.compose.viewmodel.channel.ChannelViewModelFactory
import com.gappein.sdk.client.ChatClient
import com.gappein.sdk.model.User
import com.gappein.sdk.ui.view.channelview.ChannelListFragment
import com.gappein.sdk.ui.view.channelview.`interface`.OnChannelClick

class ChatActivity : ComponentActivity(), OnChannelClick {

    companion object {
        @JvmStatic
        fun buildIntent(context: Context) = Intent(context, ChatActivity::class.java)
    }

    private val channelViewModel: ChannelViewModel by viewModels {
        ChannelViewModelFactory(ChatClient.getInstance())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChannelList(channelViewModel)
        }
//        setContentView(R.layout.activity_chat)
//        setupFragment()
    }

    private fun setupFragment() {
//        supportFragmentManager
//            .beginTransaction()
//            .add(R.id.container, ChannelListFragment.newInstance(), ChannelListFragment.TAG)
//            .commit()
    }

    override fun onUserClick(user: User) {
        //Do Something with User
    }
}