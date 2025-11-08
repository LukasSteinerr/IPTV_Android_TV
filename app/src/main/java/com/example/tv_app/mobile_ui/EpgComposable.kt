package com.example.tv_app.mobile_ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.example.tv_app.model.Channel
import com.example.tv_app.model.Playlist
import com.example.tv_app.repository.PlaylistService

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
