package com.animalme.animalme

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

   private lateinit var myWebView: WebView
   private lateinit var errorView: View
   private lateinit var btnTryAgain: Button

   // Guardamos la ULR para poder usarla fácilmente en el incio y en el botón
   private val miUrl = "http://jasrama.com"

   @SuppressLint("SetJavaScriptEnabled")
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       setContentView(R.layout.activity_main)

       myWebView = findViewById(R.id.miWebView)
       errorView = findViewById(R.id.errorView)
       btnTryAgain = errorView.findViewById(R.id.btnTryAgain)

       // Configuración de WebView Client fortalecida
       myWebView.webViewClient = object : WebViewClient() {

            // Para móviles modernos (Android 6.0+)
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                showError()
            }

           // Para mantener la compatibilidad con móviles Android más antiguos
           @Deprecated("Deprecated in Java")
           override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
               super.onReceivedError(view, errorCode, description, failingUrl)
               showError()
           }
       }

       myWebView.settings.javaScriptEnabled = true
       myWebView.loadUrl(miUrl)

       // Configuración corregida del botón
       btnTryAgain.setOnClickListener {
           hideError()              // 1. Ocultamos la pantalla roja
           myWebView.loadUrl(miUrl) // 2. Volvemos a cargar la URL
       }

       // Configuración del botón Atrás
       onBackPressedDispatcher.addCallback(this){
           if (myWebView.canGoBack()) {
               myWebView.goBack()
           } else if (errorView.visibility == View.VISIBLE) {
               hideError()
           } else {
               finish()
           }
       }
   }

   // Funciones auxiliares para mostrar y ocultar el error
   private fun showError() {
       myWebView.visibility = View.GONE
       errorView.visibility = View.VISIBLE
   }

   private fun hideError() {
         errorView.visibility = View.GONE
         myWebView.visibility = View.VISIBLE
   }
}