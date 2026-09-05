package com.yueti.app.music

import android.content.Context
import java.io.IOException
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.HttpUrl.Companion.toHttpUrl

private const val MusicNetworkPreferences = "music_network_preferences"
private const val CompatibilityDnsKey = "compatibility_dns_enabled"
private const val DnsPodBootstrapIpv4 = "119.29.29.29"
private const val DnsPodDohEndpoint = "https://doh.pub/dns-query"

internal fun isMusicFallbackDnsHost(hostname: String): Boolean {
    val host = hostname.trim().trimEnd('.').lowercase()
    return host == "music.163.com" ||
        host.endsWith(".music.163.com") ||
        host.endsWith(".music.126.net") ||
        host.endsWith(".music.127.net")
}

object MusicNetworkSettings {
    fun compatibilityDnsEnabled(context: Context): Boolean =
        context.getSharedPreferences(MusicNetworkPreferences, Context.MODE_PRIVATE)
            .getBoolean(CompatibilityDnsKey, true)

    fun setCompatibilityDnsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(MusicNetworkPreferences, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(CompatibilityDnsKey, enabled)
            .apply()
    }
}

internal class FallbackMusicDns(
    private val system: Dns = Dns.SYSTEM,
    private val fallback: Dns,
    private val enabled: () -> Boolean,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return try {
            system.lookup(hostname).ifEmpty { throw UnknownHostException(hostname) }
        } catch (systemFailure: UnknownHostException) {
            if (!enabled() || !isMusicFallbackDnsHost(hostname)) throw systemFailure
            fallback.lookup(hostname).ifEmpty { throw systemFailure }
        }
    }
}

object MusicNetworkStack {
    @Volatile
    private var sharedClient: OkHttpClient? = null

    fun client(context: Context): OkHttpClient = sharedClient ?: synchronized(this) {
        sharedClient ?: buildClient(context.applicationContext).also { sharedClient = it }
    }

    fun callFactory(context: Context): Call.Factory = client(context)

    private fun buildClient(context: Context): OkHttpClient {
        val bootstrapAddress = InetAddress.getByName(DnsPodBootstrapIpv4)
        val bootstrapClient = OkHttpClient.Builder()
            .dns(
                object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> =
                        if (hostname.equals("doh.pub", ignoreCase = true)) listOf(bootstrapAddress)
                        else Dns.SYSTEM.lookup(hostname)
                },
            )
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
        val encryptedDns = DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url(DnsPodDohEndpoint.toHttpUrl())
            .bootstrapDnsHosts(bootstrapAddress)
            .includeIPv6(false)
            .build()
        return bootstrapClient.newBuilder()
            .dns(
                FallbackMusicDns(
                    fallback = encryptedDns,
                    enabled = { MusicNetworkSettings.compatibilityDnsEnabled(context) },
                ),
            )
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}

internal fun musicNetworkErrorMessage(error: Throwable): String {
    val causes = generateSequence(error as Throwable?) { it.cause }.toList()
    return when {
        causes.any { it is UnknownHostException } ->
            "无法解析网易云服务地址，兼容 DNS 已重试。请检查网络后再试"
        causes.any { it is SocketTimeoutException } ->
            "连接网易云超时，请稍后重试"
        causes.any { it is IOException } ->
            "网络连接中断，请检查网络后重试"
        error.message?.isNotBlank() == true -> error.message.orEmpty()
        else -> "网易云音乐暂时不可用，请稍后重试"
    }
}
