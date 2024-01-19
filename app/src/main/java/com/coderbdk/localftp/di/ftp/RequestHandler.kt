package com.coderbdk.localftp.di.ftp

import android.os.Environment
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintStream
import java.net.Socket
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Created by MD. ABDULLAH on Mon, Jan 15, 2024.
 */
class RequestHandler(private val ftpHandler: LocalFtpHandler) {

    private val baseFileLocation =
        Environment.getExternalStorageDirectory().absolutePath + "/LocalFtpServer"

    private var isFileCanReceive = false
    private val TAG = javaClass.simpleName

    private val requestQueue = mutableListOf<RequestHandlerThread>()

    fun handleRequest(
        socket: Socket,
        input: InputStream,
        output: OutputStream
    ) {
        if (requestQueue.size == 10) {
            ftpHandler.sendHTMLResponse(output, 200, "<h1>Please wait few moments</h1>")
        } else {
            val requestHandlerThread = RequestHandlerThread(socket) {
                val request = requestQueue.removeAt(0)
                // request.close()
            }
            requestQueue.add(requestHandlerThread)
            requestHandlerThread.start()
        }

    }

    private inner class RequestHandlerThread(
        private val socket: Socket,
        private val onCompleted: () -> Unit
    ) : Thread() {
        val input: InputStream = socket.getInputStream()
        val output: OutputStream = socket.getOutputStream()

        override fun run() {
            super.run()

            if (false) {
                val data = ByteArray(1024)
                val len = input.read(data)
                Log.i(TAG, String(data, 0, len))
                ftpHandler.sendHTMLResponse(
                    output,
                    200,
                    "</h1>Received.</h2>"
                )
                return
            }
            try {

                val totalAvailable = input.available()
                var totalRead = 0
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val buffer = StringBuffer()
                var line: String = reader.readLine()
                val map = mutableMapOf<String, String>()
                map["method"] = line
                Log.i(TAG, "Request accepted")
                buffer.append(line)

                while (reader.readLine().also { line = it } != null) {
                    if (line.isEmpty()) break
                    try {
                        val endKeyPos = line.indexOfFirst { it == ':' }
                        val key = line.substring(0, endKeyPos)
                        val value = line.substring(endKeyPos, line.length)
                        map[key] = value
                        buffer.append(line)
                        buffer.append("\n")
                        totalRead += line.length
                    } catch (e: Exception) {

                        ftpHandler.sendHTMLResponse(
                            output,
                            200,
                            "</h1>${map}</h1> </br> <h2>Error[${e.message}}</h2>"
                        )
                        return
                    }
                }

                Log.i(TAG, "HeaderData:")
                Log.i(TAG, buffer.toString())
                val topHeaderData = map["method"]?.split(" ")!!
                val requestMethod = topHeaderData[0]


                var route = topHeaderData[1]

                if (route.endsWith("/")) {
                    route = route.substring(0, route.length - 1)
                }
                if (route.startsWith("/")) {
                    route = route.substring(1, route.length)
                }

                Log.i(TAG, "Request accepted , request method: $requestMethod")
                Log.i(TAG, "Route: $route")

                when (route) {
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

                    "upload/receiver" -> {
                        val bodyBuffer = fileReceiver(reader)
                        ftpHandler.sendHTMLResponse(
                            output,
                            200,
                            "$bodyBuffer"
                        )
                       /* ftpHandler.sendHTMLResponse(
                            output,
                            200, """
                                </h1>Total Length:${totalAvailable} </h1>
                                </h1>Input Length:${input.available()} </h1>
                              
                                  </h1>Total Read Length:${totalRead} </h1>
                                    </h1>Read Body:${bodyBuffer} </h1>
                                </h1>Received:$buffer </h1>
                                
                                 
                            """.trimIndent()
                        )*/
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

        private fun fileReceiver(reader: BufferedReader): StringBuffer {
            val buffer = StringBuffer()
            try {

                var line = ""

                val webkitBoundary = reader.readLine()
                val contentDisposition = reader.readLine()
                val contentType = reader.readLine()

                // skip empty line
                reader.readLine()

                Log.i(TAG, "Reading...")
                Log.i(TAG, "Boundary:${webkitBoundary}")
                Log.i(TAG, "Content-Disposition:$contentDisposition")
                Log.i(TAG, "Content-Type:${contentType}")

                val splitContentDisposition = contentDisposition.split(" ")
                var fileName = splitContentDisposition.find { it.startsWith("filename") }
                fileName?.apply {
                    fileName = substring(10, length - 1)
                }
                if (fileName == null) {
                    fileName = "my_file_${System.currentTimeMillis()}.txt"
                }

                Log.i(TAG, "fileName:${fileName}")

                var path = "$baseFileLocation/$fileName"

                if(File(path).exists()) {
                    val count = System.currentTimeMillis()
                    fileName = "${count}_$fileName"
                    path = "$baseFileLocation/$fileName"
                }
                val dos = DataOutputStream(FileOutputStream(path))

                while (reader.readLine().also { line = it } != null) {
                    if(line == webkitBoundary) break
                    // Log.i(TAG, line)
                    // buffer.append(line)
                    dos.writeBytes(line)
                }

                Log.i(TAG, "Reading Completed.")

                dos.close()
                buffer.append("<h1>Upload Completed.</h1>")
                buffer.append("<h1>File name:${fileName}</h1>")
                buffer.append("<h1>File path:$path</h1>")
            }catch (e: Exception) {
                buffer.append("<h1>Error:${e.message}</h1>")
            }


            return buffer
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
                <html>
                <body>

                <form action="receiver/" method="post" enctype="multipart/form-data">
                  <h1>File Uploader:</h1>
                  <input type="file" name="fileUpload" id="fileUpload">
                  <input type="submit" value="Upload File" name="submit">
                </form>

                </body>
                </html>
                    """.trimIndent()
        )
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

    fun handleRequestIO(reader: BufferedReader): ByteArray {
        val request = StringBuilder()
        var line: String? = reader.readLine()
        while (!line.isNullOrEmpty()) {
            request.append(line).append("\r\n")
            line = reader.readLine()
        }

        val boundary = extractBoundary(request.toString())
        return extractFileData(request.toString(), boundary)
    }


    private fun extractBoundary(request: String): String {
        val contentTypeLine = request.lines().find { it.startsWith("Content-Type: ") }
        return contentTypeLine?.substringAfter("boundary=") ?: ""
    }

    private fun extractFileData(request: String, boundary: String): ByteArray {
        val parts = request.split(boundary)
        val filePart = parts.find { it.contains("filename=\"") }

        return filePart?.toByteArray(StandardCharsets.UTF_8) ?: byteArrayOf()
    }

    fun saveFile(fileData: ByteArray, filename: String) {
        FileOutputStream("$baseFileLocation/$filename").use { fileOutputStream ->
            fileOutputStream.write(fileData)
        }
    }

    private fun addHeaderContent(ps: PrintStream, content: String) {
        ps.print(content)
        ps.print("\r\n")
    }

    private fun closeHeader(ps: PrintStream) {
        ps.print("\r\n")
    }


}