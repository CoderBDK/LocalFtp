package com.coderbdk.localftp.di.ftp

import android.os.Environment
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.StandardCharsets

/**
 * Created by MD. ABDULLAH on Mon, Jan 15, 2024.
 */
class RequestHandler(private val ftpHandler: LocalFtpHandler) {

    private val baseFileLocation =
        Environment.getExternalStorageDirectory().absolutePath + "/LocalFtpServer"

    private var isFileCanReceive = false
    private val LOG = javaClass.simpleName

    private val requestQueue = mutableListOf<RequestHandlerThread>()

    fun handleRequest(
        socket: Socket,
        input: InputStream,
        output: OutputStream
    ) {
        if(requestQueue.size == 10) {
            ftpHandler.sendHTMLResponse(output,200, "<h1>Please wait few moments</h1>")
        } else {
            val requestHandlerThread = RequestHandlerThread(socket) {
                val request = requestQueue.removeAt(0)
                // request.close()
            }
            requestQueue.add(requestHandlerThread)
            requestHandlerThread.start()
        }

    }

    private inner class RequestHandlerThread (private val socket: Socket, private val onCompleted: () -> Unit): Thread() {
        val input: InputStream = socket.getInputStream()
        val output: OutputStream = socket.getOutputStream()

        override fun run() {
            super.run()
            Log.i(LOG, "Request accepted")


            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val buffer = StringBuffer()
                var line: String = reader.readLine()
                val map = mutableMapOf<String,String>()
                map["method"] = line

                while (reader.readLine().also { line = it } != null) {
                    if (line.isEmpty())break
                    try {
                        val endKeyPos = line.indexOf(':')
                        val key = line.substring(0, endKeyPos)
                        val value = line.substring(endKeyPos, line.length)
                        map[key] = value
                        buffer.append(line)
                        buffer.append("</br>")
                    } catch (e: Exception) {
                        buffer.append("<h1>Error[${e.message}]</h1>")
                        ftpHandler.sendHTMLResponse(
                            output,
                            200,
                            "</h1>${map}</h1> </br> <h2>${buffer}</h2>"
                        )
                        return
                    }

                }
                val topHeaderData = map["method"]?.split(" ")!!
                val requestMethod = topHeaderData[0]
                var route = topHeaderData[1]

                if(route.endsWith("/")) {
                    route = route.substring(0, route.length -1)
                }
                if(route.startsWith("/")) {
                    route = route.substring(1,route.length)
                }


                Log.i(LOG, buffer.toString())
                /* ftpHandler.sendHTMLResponse(
                     output,
                     200,
                     "</h1>${map}</h1> </br> <h2>${buffer}</h2>"
                 )*/

                Log.i(LOG,"Route: $route")
                when(route) {
                    "test" -> {
                        ftpHandler.sendHTMLResponse(
                            output,
                            200,
                            "</h1>Test </h1>"
                        )
                    }
                    "upload" -> {
                        uploadHtmlSend(output)
                    }
                    "receiver" -> {
                        val fileName = "${System.currentTimeMillis()}"

                        // val dos = BufferedOutputStream(FileOutputStream("${baseFileLocation}/${fileName}.txt"))

                        ftpHandler.sendHTMLResponse(
                            output,
                            200,
                            "</h1>Received:${fileName} </h1>"
                        )


                    }
                    else -> {
                        ftpHandler.sendHTMLResponse(
                            output,
                            200,
                            "<h1>Home Route: $route</h1>"
                        )
                    }
                }

                onCompleted()
            } catch (e: Exception) {
                ftpHandler.sendHTMLResponse(
                    output,
                    404,
                    "<h1>Internal Error :${e.message}</h1>"
                )
                onCompleted()
            }


        }



        fun close() {
            socket.close()
        }

    }


    private fun uploadHtmlSend(output: OutputStream) {
        ftpHandler.sendHTMLResponse(
            output,
            200,
            """
                        <!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>File Upload</title>
</head>
<body>
    <input type="file" id="fileInput" />
    <button onclick="uploadFile()">Upload File</button>

    <script>
        function uploadFile() {
            const fileInput = document.getElementById('fileInput');
            const file = fileInput.files[0];

            const xhr = new XMLHttpRequest();
            const formData = new FormData();

            formData.append('file', file);

            xhr.open('POST', 'http://192.168.0.101:8088/receiver/', true);

            xhr.onload = function() {
                if (xhr.status >= 200 && xhr.status < 300) {
                    console.log('Success:', xhr.responseText);
                } else {
                    console.error('Error:', xhr.statusText);
                }
            };

            xhr.onerror = function() {
                console.error('Network error');
            };

            xhr.send(formData);
        }
    </script>
</body>
</html>

                    """.trimIndent())
    }

    fun handleRequestTest(
        socket: Socket,
        input: InputStream,
        output: OutputStream
    ) {

        if (isFileCanReceive) {
            isFileCanReceive = false
           // Log.i("File Receiver", ":${baseFileLocation}/a.png")
            try {
                val fos = FileOutputStream("${baseFileLocation}/a.txt")
               // writeLocalData(input, fos)
               ftpHandler.sendHTMLResponse(
                   output,
                  200,
                  "</h1>Uploaded</h1>"
               )
                socket.close()
                Log.i("File Receiver", "Completed")

            } catch (e: Exception) {
                Log.i("Erro - Receiver", e.message.toString())
                ftpHandler.sendHTMLResponse(
                    output,
                    200,
                    "${e.message}"
                )
            }

            isFileCanReceive = false
            return
        }
        val header = ftpHandler.readHeader(input)
        Log.i(javaClass.simpleName, "${header.httpRequestParam}")
        when (header.httpRequestParam) {
            "test/" -> {
                ftpHandler.sendHTMLResponse(
                    output,
                    200,
                    "<h1>Hello, World!</h1>"
                )
            }

            "help/" -> {
                ftpHandler.sendHTMLResponse(
                    output,
                    200,
                    "</h1>Help</h1>"
                )
            }

            "upload/" -> {
                ftpHandler.sendHTMLResponse(
                    output,
                    200,
                    """
                        <!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>File Upload</title>
</head>
<body>
    <input type="file" id="fileInput" />
    <button onclick="uploadFile()">Upload File</button>

    <script>
        function uploadFile() {
            const fileInput = document.getElementById('fileInput');
            const file = fileInput.files[0];

            const xhr = new XMLHttpRequest();
            const formData = new FormData();

            formData.append('file', file);

            xhr.open('POST', 'http://192.168.0.101:8088/upload/receiver/', true);

            xhr.onload = function() {
                if (xhr.status >= 200 && xhr.status < 300) {
                    console.log('Success:', xhr.responseText);
                } else {
                    console.error('Error:', xhr.statusText);
                }
            };

            xhr.onerror = function() {
                console.error('Network error');
            };

            xhr.send(formData);
        }
    </script>
</body>
</html>

                    """.trimIndent()
                )

            }

            "POST /upload/receiver/" -> {
                Log.i("upload_receiver", "Uploading...")
                ftpHandler.sendHTMLResponse(
                    output,
                    200,
                    "<h1>Uploading</h1>"
                )
                isFileCanReceive = true

                //writeLocalData(input, fos)
                /*ftpHandler.sendHTMLResponse(
                    output,
                    200,
                    "Uploaded"
                )*/
            }

            else -> {
                ftpHandler.sendHTMLResponse(
                    output,
                    200,
                    "<h1>Home</h1>"
                )
            }
        }
    }

    private fun writeLocalData(input: DataInputStream, output: DataOutputStream) {
        ftpHandler.writeLocalData(input, output)
    }

    fun handleRequestIO(socket: Socket, inputStream: InputStream?, outputStream: OutputStream?) {
        try {
            val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))

            val request = StringBuilder()
            var line: String? = reader.readLine()
            while (!line.isNullOrEmpty()) {
                request.append(line).append("\r\n")
                line = reader.readLine()
            }

            val boundary = extractBoundary(request.toString())
            val fileData = extractFileData(request.toString(), boundary)

            saveFile(fileData, "received_file.txt")

            val response = "HTTP/1.1 200 OK\r\n\r\nFile received successfully."
            outputStream?.write(response.toByteArray(StandardCharsets.UTF_8))
            socket.close()
        } catch (e: Exception) {
            ftpHandler.sendHTMLResponse(outputStream, 200, e.message.toString())
        }


    }


    private fun extractBoundary(request: String): String {
        val contentTypeLine = request.lines().find { it.startsWith("Content-Type: ") }
        return contentTypeLine?.substringAfter("boundary=") ?: ""
    }

    fun extractFileData(request: String, boundary: String): ByteArray {
        val parts = request.split(boundary)
        val filePart = parts.find { it.contains("filename=\"") }

        return filePart?.toByteArray(StandardCharsets.UTF_8) ?: byteArrayOf()
    }

    fun saveFile(fileData: ByteArray, filename: String) {
        FileOutputStream("$baseFileLocation/$filename").use { fileOutputStream ->
            fileOutputStream.write(fileData)
        }
    }
}