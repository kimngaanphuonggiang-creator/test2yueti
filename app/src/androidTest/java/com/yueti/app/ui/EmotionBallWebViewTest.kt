package com.yueti.app.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EmotionBallWebViewTest {
    @Test
    fun bundledEmotionBall_hasNonZeroSvgBoundsInAndroidWebView() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val completed = CountDownLatch(1)
        var svgWidth = 0f
        lateinit var webView: WebView

        instrumentation.runOnMainSync {
            webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.blockNetworkLoads = true
                measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(600, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(500, android.view.View.MeasureSpec.EXACTLY),
                )
                layout(0, 0, 600, 500)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        view.evaluateJavascript("document.querySelector('#bot svg').getBoundingClientRect().width.toString()") {
                            svgWidth = it.trim('"').toFloatOrNull() ?: 0f
                            completed.countDown()
                        }
                    }
                }
                loadDataWithBaseURL(
                    "https://yueti.local/emotion-ball/",
                    bundledEmotionBallHtml(context),
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
        }

        assertTrue("Emotion Ball page did not finish", completed.await(8, TimeUnit.SECONDS))
        assertTrue("Emotion Ball SVG must not collapse to 0px", svgWidth > 100f)
        instrumentation.runOnMainSync { webView.destroy() }
    }
}
