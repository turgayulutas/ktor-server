package com.qbitwise.ktorwebserver

import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.qbitwise.ktorwebserver.databinding.ActivityMainBinding
import io.ktor.network.tls.extensions.*
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.cors.CORS
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

private const val HTTP_SERVER_PORT_DEFAULT = 3544
private const val HTTPS_SERVER_PORT_DEFAULT = 3545

private const val CERT_ALIAS = "verifybyme"
private const val CERT_PASS = "vbm-pass"
private const val CERT_KEY_SIZE = 256
private const val CERT_DAYS_VALID = 365 * 25L
private val CERT_HASH_ALGORITHM = HashAlgorithm.SHA256
private val CERT_SIGNATURE_ALGORITHM = SignatureAlgorithm.ECDSA

private const val KEYSTORE_FILE_NAME = "ssl.keystore"
private const val KEYSTORE_PASS = CERT_PASS

class MainActivity : AppCompatActivity() {

    // binding
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        initViews()

        // create keystore for SSL
        val keyStore = buildKeyStore {
            certificate(CERT_ALIAS) {
                hash = CERT_HASH_ALGORITHM
                sign = CERT_SIGNATURE_ALGORITHM
                keySizeInBits = CERT_KEY_SIZE
                password = CERT_PASS
                daysValid = CERT_DAYS_VALID
            }
        }

        // create server and launch
        embeddedServer(Netty, applicationEngineEnvironment {
            parentCoroutineContext = EmptyCoroutineContext + parentCoroutineContext
            log = LoggerFactory.getLogger("ktor.application")

            connector {
                port = HTTP_SERVER_PORT_DEFAULT
            }

            sslConnector(
                keyStore,
                CERT_ALIAS,
                { KEYSTORE_PASS.toCharArray() },
                { CERT_PASS.toCharArray() }
            ) {
                port = HTTPS_SERVER_PORT_DEFAULT
                keyStorePath = File(filesDir, KEYSTORE_FILE_NAME).absoluteFile
            }

            module {
                install(CallLogging)

                install(CORS) {
                    anyHost()
                    allowHeaders { true }
                }

                routing {
                    get("/") { call.respondText { "Hello World" } }
                }
            }
        }).start(false)
    }

    private fun initViews() {
        binding.btnHttp.text = "http://${getDeviceLocalIp()}:$HTTP_SERVER_PORT_DEFAULT"
        binding.btnHttps.text = "https://${getDeviceLocalIp()}:$HTTPS_SERVER_PORT_DEFAULT"

        binding.btnHttp.setOnClickListener {
            openBrowser(binding.btnHttp.text.toString())
        }

        binding.btnHttps.setOnClickListener {
            openBrowser(binding.btnHttps.text.toString())
        }
    }

    private fun getDeviceLocalIp(): String {
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        return Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
    }

    private fun openBrowser(url: String) {
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(url))
    }
}