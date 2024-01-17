package com.coderbdk.localftp.di.ftp

import android.util.Log
import androidx.annotation.RestrictTo
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream


/**
 * Created by MD. ABDULLAH on Mon, Jan 15, 2024.
 */
class LocalFtpHandler {

    class HttpHeader {
        var httpRequestParam: String? = null
        var port: String? = null
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Throws(IOException::class)
    fun readHeader(input: InputStream) : HttpHeader {
        val data = ByteArray(1024)
        var len = 0
        len = input.read(data)
        val mData = String(data, 0, len)
        var d = mData.split("\\n+".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        d = arrayOf(d[0], d[1])
        val httpHeader = HttpHeader()
        httpHeader.httpRequestParam = d[0].replace("HTTP/1.1", "")
            .replace("GET /", "").replace("?", "").trim { it <= ' ' }
        httpHeader.port = d[1]
        //Log.i(javaClass.simpleName, "${mData}")
        return httpHeader
    }

    fun sendHTMLResponse(out: OutputStream?, responseCode: Int, messageHtmlCode: String?) {
        val ps = PrintStream(out)
        ps.println("HTTP/1.1 200 OK")
        ps.println("Content-Type: text/html")
        ps.println("\r\n")
        ps.println(messageHtmlCode)
        ps.flush()
        ps.close()
    }
    fun sendHTMLKeepResponse(out: OutputStream?, responseCode: Int, messageHtmlCode: String?) {
        val ps = PrintStream(out)
        ps.println("HTTP/1.1 200 OK")
        ps.println("Content-Type: text/html")
        ps.println("\r\n")
        ps.println(messageHtmlCode)
    }
    @Throws(IOException::class)
    fun readFile(path: String?): String {
        val input = FileInputStream(path)
        val data = ByteArray(input.available())
        var len = 0
        val buffer = StringBuffer()
        while (input.read(data).also { len = it } != -1) {
            buffer.append(String(data, 0, len))
        }
        input.close()
        return buffer.toString()
    }

    @Throws(IOException::class)
    fun sendHTMLFile(out: OutputStream?, responseCode: Int, path: String?) {
        val data = readFile(path)
        val ps = PrintStream(out)
        ps.println("HTTP/1.1 200 OK")
        ps.println("Content-Type: text/html")
        ps.println("\r\n")
        ps.println(java.lang.String(data) as String)
        ps.flush()
        ps.close()
    }

    @Throws(IOException::class)
    fun sendFile(out: OutputStream, mimeType: String, responseCode: Int, path: String?) {
        val input = FileInputStream(path)
        val ps = PrintStream(out)
        ps.println("HTTP/1.1 200 OK")
        ps.println("Content-Type: $mimeType")
        ps.println("\r\n")
        readSendFile(input, out)
        ps.flush()
        ps.close()
    }

    @Throws(IOException::class)
    fun readSendFile(input: FileInputStream, out: OutputStream) {
        val data = ByteArray(input.available())
        var len = 0
        while (input.read(data).also { len = it } > 0) {
            out.write(data, 0, len)
        }
        input.close()
    }

    fun writeLocalData(input: DataInputStream, output: DataOutputStream) {
        val data = ByteArray(1024)
        var len: Int
        while (input.read(data).also { len = it } != 0) {
            output.write(data, 0, len)
        }
        // input.close()
    }

    @Throws(IOException::class)
    fun readFromStream(input: InputStream): String {
        val data = ByteArray(1024)
        var len = 0
        val buffer = StringBuffer()
        while (input.read(data).also { len = it } != -1) {
            buffer.append(String(data, 0, len))
        }
         input.close()
        return buffer.toString()
    }
}