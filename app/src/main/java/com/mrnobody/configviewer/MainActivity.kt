package com.mrnobody.configviewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.TextField
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import com.mrnobody.configviewer.ui.theme.ConfigViewerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    companion object {
        const val TAG = "ConfigViewer"
        const val NEKO_PKG = "moe.nb4a"
        const val V2RAY_ANG_PKG = "com.v2ray.ang"
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var selectedTabIndex by remember { mutableIntStateOf(1) }
            val tabsName = listOf("Subs", "Configs")
            val serverIp = remember { mutableStateOf<String?>(null) }
            val isReachable = remember { mutableStateOf(false) }
            ConfigViewerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    if (serverIp.value == null && !isReachable.value) {
                        PingServer(serverIp, updateReachable = {
                            isReachable.value = it
                        })
                    } else {
                        ShowOptions(
                            selectedTabIndex = selectedTabIndex,
                            tabsName = tabsName,
                            onSelectedChanged = {
                                selectedTabIndex = it
                            },
                            ipAddress = serverIp.value ?: ""
                        )
                    }
                }
            }
        }
    }
}

/*fun isServerReachable(url: String): Boolean {
    return try {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(url)
            .build()
        val response = client.newCall(request).execute()
        response.isSuccessful
    } catch (e: Exception) {
        false
    }
}*/

fun isServerReachable(ip: String, port: Int, timeout: Int = 3000): Boolean {
    return try {
        val socket = Socket()
        socket.connect(InetSocketAddress(ip, port), timeout)
        socket.close() // Close the connection if successful
        true
    } catch (e: SocketTimeoutException) {
        false // Timeout indicates the server is not reachable
    } catch (e: Exception) {
        false // Any other exception means the server is not reachable
    }
}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PingServer(
    ipAddress: MutableState<String?>,
    updateReachable: (Boolean) -> Unit
) {
    var isReachable by remember { mutableStateOf<Boolean?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var _ipAddress by remember { mutableStateOf<String>("") }

    fun pingServer() {
        isLoading = true
        isReachable = null
        Log.i(MainActivity.TAG, "pingServer: ${_ipAddress}")
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val split = _ipAddress.split(":")
                isReachable = isServerReachable(split[0], split[1].toInt())
                Log.i(MainActivity.TAG, "pingServer: ${_ipAddress} : $isReachable")
            } catch (e: Exception) {
                isReachable = false
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            singleLine = true,
            value = _ipAddress,
            onValueChange = { _ipAddress = it },
            label = { Text("Enter IP Address") },
            placeholder = {
//                Text("Enter IP Address")
                          },
            modifier = Modifier.padding(16.dp)
        )
        var isVisible by remember { mutableStateOf(true) }
        AnimatedVisibility(
            isVisible
        ) {
            Button(onClick = {
                isVisible = false
                pingServer()
            }, modifier = Modifier.padding(16.dp)) {
                Text("Ping Server")
            }
        }

        when {
            isLoading -> CircularProgressIndicator()
            isReachable == true -> {
                Text("Server is reachable!", color = MaterialTheme.colorScheme.primary)
                LaunchedEffect(Unit) {
                    delay(3000)
                    ipAddress.value = _ipAddress
                    updateReachable(true)
                }
            }

            isReachable == false -> {
                isVisible = true
                Text("Server is not reachable!", color = MaterialTheme.colorScheme.error)
            }

            else -> {
                Text("Enter an IP and click 'Ping Server'")
                isVisible = true
            }
        }
    }
}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ShowOptions(
    modifier: Modifier = Modifier,
    selectedTabIndex: Int,
    onSelectedChanged: (Int) -> Unit,
    tabsName: List<String>,
    ipAddress: String
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabsName.forEachIndexed { index, name ->
                Tab(
                    modifier = Modifier.padding(all = 16.dp),
                    selected = selectedTabIndex == index,
                    onClick = {
                        onSelectedChanged(index)
                    },
                    onFocus = {}
                ) {
                    Text(text = name)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        ShowScreen(selectedTabIndex = selectedTabIndex, ipAddress = ipAddress)
    }


}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ShowScreen(
    modifier: Modifier = Modifier,
    selectedTabIndex: Int,
    ipAddress: String
) {
    val _localFileDirs = remember {
        mutableStateOf(if (selectedTabIndex == 0) "http://${ipAddress}/Config/configs.txt" else "http://${ipAddress}/Subs/subs.txt")
    }

    //192.168.1.254:5643

    var fileContent by remember { mutableStateOf<List<String>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedTabIndex) {
        Log.i(MainActivity.TAG, "ShowScreen: Launched")
        fileContent = null
        _localFileDirs.value =
            if (selectedTabIndex == 0) "http://${ipAddress}/Config/configs.txt" else "http://${ipAddress}/Subs/subs.txt"
        isLoading = false
        errorMessage = null
    }

    fun fetchFileContent() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(_localFileDirs.value).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val lines = body.split("\n")
                    fileContent = lines
                } else {
                    errorMessage = "Failed to fetch file : ${response.message}"
                }
            } catch (e: IOException) {
                errorMessage = "${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        val context = LocalContext.current
        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            Text("Error : ${errorMessage} ", color = MaterialTheme.colorScheme.error)
        } else if (fileContent != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                fileContent?.let {
                    items(fileContent!!.size) {
                        Text(text = fileContent!![it], modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                copyToClipboard(context, fileContent!![it])
                                Toast
                                    .makeText(context, "Copied", Toast.LENGTH_LONG)
                                    .show()
                                launchNekoBox(context)
                            })
                    }
                }
            }
        } else {
            Button(onClick = { fetchFileContent() }) {
                Text("Fetch File Content")
            }
        }
    }

}

fun launchNekoBox(context: Context) {
    try {
        // Check if the package exists
        val packageManager: PackageManager = context.packageManager
        val intent: Intent? = packageManager.getLaunchIntentForPackage(MainActivity.NEKO_PKG)
        if (intent != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "App not found", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to open the app: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}


fun copyToClipboard(context: Context, text: String) {
    /*val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Copied Text", text)
    clipboard.setPrimaryClip(clip)*/
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // Create a ClipData object to hold the copied data
    val clipData = ClipData.newPlainText("vpnConfig", text)

    // Set the ClipData to the clipboard
    clipboardManager.setPrimaryClip(clipData)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ConfigViewerTheme {
        var selectedTabIndex by remember { mutableStateOf(0) }
        val tabsName = listOf("Subs", "Configs")
        ShowOptions(
            selectedTabIndex = selectedTabIndex,
            tabsName = tabsName,
            onSelectedChanged = { selectedTabIndex = it },
            ipAddress = ""
        )
    }
}