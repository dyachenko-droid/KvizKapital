package ru.kvizcap.quiz.app.startup

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

fun startupNetwork(context: Context): Flow<Boolean> = callbackFlow {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    fun publish() {
        val capabilities = manager.activeNetwork?.let(manager::getNetworkCapabilities)
        trySend(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
    }
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = publish()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = publish()
        override fun onLost(network: Network) = publish()
    }
    manager.registerDefaultNetworkCallback(callback)
    publish()
    awaitClose { manager.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()
