package com.example.iptvsonic.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.example.iptvsonic.model.Channel
import com.example.iptvsonic.model.Playlist
import com.example.iptvsonic.repository.PlaylistService

@Composable
fun EpgGuide(
    playlist: Playlist,
    playlistService: PlaylistService,
    onChannelSelected: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val epgFragment = remember(playlist.id) {
        IptvEpgFragment(
            playlist = playlist,
            playlistService = playlistService,
            onChannelSelected = onChannelSelected
        )
    }

    AndroidView(
        factory = { context ->
            val containerView = FragmentContainerView(context)
            containerView.id = androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            
            val activity = context as? androidx.fragment.app.FragmentActivity
            activity?.supportFragmentManager?.let { fragmentManager ->
                fragmentManager.commit {
                    replace(containerView.id, epgFragment)
                }
            }
            
            containerView
        },
        modifier = modifier
    )
}
