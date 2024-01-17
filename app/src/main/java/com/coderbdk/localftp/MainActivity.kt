package com.coderbdk.localftp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.coderbdk.localftp.di.ftp.LocalFtpServer
import com.coderbdk.localftp.ui.theme.LocalFtpService
import com.coderbdk.localftp.ui.theme.LocalFtpTheme
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : ComponentActivity() {

    private val prefs by lazy {
        getSharedPreferences("local_ftp_server", MODE_PRIVATE)
    }
    private var localFtpServer: LocalFtpServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // startService(Intent(this, LocalFtpService::class.java))

        // val localFtpServer = LocalFtpService.getServer()

        setContent {
            LocalFtpTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainUi()
                }
            }
        }
    }

    private fun onInitServerListener(localFtpServer: LocalFtpServer, onError: () -> Unit) {
        localFtpServer.setServerListener(
            object : LocalFtpServer.ServerListener {
                override fun onError(message: String) {

                }

            }
        )
    }

    @Composable
    fun MainUi() {
        var ftpShareCode by remember {
            mutableStateOf("1045453")
        }
        Column(
            Modifier
                .fillMaxSize()
        ) {
            TopMenuBar(ftpShareCode) {
                ftpShareCode = it
            }
            FileManager()
        }
    }

    @Composable
    fun TopMenuBar(value: String, onValueChange: (String) -> Unit) {
        val isOldServerRunning = prefs.getBoolean("is_running", false)
        if(isOldServerRunning) {
            localFtpServer = LocalFtpService.getServer()
        }
        var isServerRunning by remember {
            mutableStateOf(isOldServerRunning)
        }
        var showSettings by remember {
            mutableStateOf(false)
        }
        if (showSettings) {
            DialogSetting({
                showSettings = false
            }, {
                showSettings = false
            })
        }

        ElevatedCard(
            Modifier
                .padding(8.dp)
                .fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    modifier = Modifier
                        .height(56.dp)
                        .padding(end = 8.dp),
                    onClick = {
                        isServerRunning = !isServerRunning
                        if (isServerRunning) {
                            startServer()
                            Handler(Looper.getMainLooper()).postDelayed(
                                {
                                    localFtpServer = LocalFtpService.getServer()!!
                                    onInitServerListener(localFtpServer!!,
                                        onError = {
                                        isServerRunning = false
                                    },)
                                }, 1000
                            )

                        } else {
                            stopServer()
                        }
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(

                    ) {
                        Icon(
                            imageVector = if (isServerRunning) Icons.Default.Refresh else Icons.Filled.PlayArrow,
                            contentDescription = ""
                        )
                        Text(text = if (isServerRunning) "Stop" else "Start")
                    }

                }

                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    value = value,
                    placeholder = {
                        Text(text = "Code")
                    },
                    onValueChange = onValueChange,
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = "")
                    }
                )

                OutlinedButton(
                    modifier = Modifier
                        .height(56.dp),
                    onClick = {
                        showSettings = true
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Settings, contentDescription = "")
                }
            }
        }
    }


    @Composable
    fun DialogSetting(
        onDismissRequest: () -> Unit,
        onConfirmation: () -> Unit,
    ) {

        Dialog(onDismissRequest = { onDismissRequest() }) {
            Card(
                modifier = Modifier,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SettingsContent()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(
                            onClick = { onDismissRequest() },
                            modifier = Modifier.padding(8.dp),
                        ) {
                            Text("Dismiss")
                        }
                        TextButton(
                            onClick = { onConfirmation() },
                            modifier = Modifier.padding(8.dp),
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("CommitPrefEdits")
    @Composable
    fun SettingsContent() {

        var link = "Not available"
        if(localFtpServer !=null) {
            link = "http://${localFtpServer!!.getLocalFtpAddress()}/"
        }
        val oldPortValue = prefs.getInt("port", 8088)
        var portValue by remember {
            mutableStateOf(
               "$oldPortValue"
            )
        }

        Column(
            Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {

            Text(text = "Link:${link}")
            Text(text = if(localFtpServer == null) "Server: Not started" else "Server: Running")
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                value = portValue,
                placeholder = {
                    Text(text = "Port")
                },
                onValueChange = {
                    portValue = it
                    if(it.isNotEmpty()) {
                        if(it.length > 3) {
                            prefs.edit().putInt("port", it.toInt()).apply()
                        }
                    }
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.LocationOn, contentDescription = "")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )
        }

    }
    @SuppressLint("CommitPrefEdits")
    private fun startServer() {
        prefs.edit().putBoolean("is_running", true).apply()
        startService(Intent(this, LocalFtpService::class.java))
    }

    private fun stopServer() {
        localFtpServer = null
        prefs.edit().putBoolean("is_running", false).apply()
        stopService(Intent(this, LocalFtpService::class.java))
    }

    data class FileInfo(
        val name: String,
        val path: String
    )

    @Composable
    fun FileManager() {
        val baseFile =
            File(Environment.getExternalStorageDirectory().absolutePath + "/LocalFtpServer/")
        if (!baseFile.isDirectory) {
            Text(text = "Dir not found")
            Text(text = "Dir created:${baseFile.mkdir()}")
        }
        var currentDirPath by remember {
            mutableStateOf(
                baseFile.absolutePath
            )
        }
        val list = remember {
            mutableListOf(
                FileInfo(
                    baseFile.name,
                    baseFile.absolutePath
                )
            )
        }

        // Text(text = "Path: $currentDirPath")

        val pathNavigatorState = rememberLazyListState()
        val coroutine = rememberCoroutineScope()
        var selectedPathItem by remember {
            mutableIntStateOf(0)
        }

        LazyRow(
            Modifier.padding(8.dp),
            state = pathNavigatorState
        ) {
            item {
                Icon(
                    modifier = Modifier
                        .size(40.dp),
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Home"
                )
            }
            items(list.size) { index ->
                val fileInfo = list[index]

                Text(
                    modifier = if (index == selectedPathItem) Modifier
                        .widthIn(min = 72.dp, max = 104.dp)
                        .padding(end = 8.dp)
                        .border(
                            width = 1.dp,
                            color = Color.Green,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                    else Modifier
                        .clickable {
                            currentDirPath = list[index].path
                            selectedPathItem = index
                            val removeSize = (list.size - 1) - selectedPathItem
                            repeat(removeSize) {
                                list.removeLast()
                            }
                        }
                        .widthIn(min = 72.dp, max = 104.dp)
                        .padding(end = 8.dp)
                        .border(
                            width = 1.dp,
                            color = Color.Gray,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp),
                    text = fileInfo.name,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }
        }

        val newFileList = openDir(currentDirPath) ?: return

        LazyColumn {
            items(newFileList.size) { index ->
                val file = newFileList[index]
                FileItem(file) {
                    if (file.isDirectory) {
                        currentDirPath = file.absolutePath
                        list.add(
                            FileInfo(
                                file.name,
                                file.absolutePath
                            )
                        )
                        selectedPathItem = list.size - 1
                        coroutine.launch {
                            pathNavigatorState.animateScrollToItem(list.size - 1)
                        }
                    }
                }
            }
        }
        BackHandler {
            if (list.size >= 2) {
                currentDirPath = File(currentDirPath).parent
                list.removeAt(list.size - 1)
                selectedPathItem = list.size - 1
                coroutine.launch {
                    pathNavigatorState.animateScrollToItem(list.size - 1)
                }
            }

        }
    }

    private fun openDir(currentDirPath: String): Array<out File>? {
        return File(currentDirPath).listFiles()
    }


    @Composable
    fun FileItem(file: File, onClick: () -> Unit) {
        Card(
            Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable {
                    onClick()
                }
        ) {

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier
                        .size(40.dp),
                    painter = painterResource(id = if (file.isFile) R.drawable.baseline_insert_drive_file_24 else R.drawable.baseline_folder_24),
                    contentDescription = ""
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp),
                    text = file.name,
                    fontSize = 22.sp
                )
            }


        }
    }

    @Preview(showBackground = true)
    @Composable
    fun AppPreview() {
        LocalFtpTheme {

            Column {

                Text(
                    modifier = Modifier
                        .drawBehind {
                            drawLine(
                                color = Color.Black,
                                start = Offset(size.width, 0f),
                                end = Offset(size.width, size.height),
                                strokeWidth = 4f
                            )
                        }
                        .padding(8.dp),
                    text = "Name"
                )

                TopMenuBar(value = "", onValueChange = {})
                FileItem(file = File("")) {

                }
            }

        }
    }

}
