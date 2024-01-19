package com.coderbdk.localftp.di.ftp

import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Created by MD. ABDULLAH on Sun, Jan 14, 2024.
 */
class LocalFtpServer(
    private val port: Int,
    private val ip: Int
) {

    private var isServerRunning = false
    private val server = ServerThread()

    private fun init() {
        if (isServerRunning) return
        isServerRunning = true
        server.start()
    }

    fun start() {
        init()
    }

    fun stop() {
        isServerRunning = false
        server.stopServer()
    }

    interface ServerListener {
        fun onError(message: String)
    }

    private var mListener: ServerListener? = null

    fun setServerListener(listener: ServerListener) {
        mListener = listener
    }

    private inner class ServerThread : Thread() {
        private var serverSocket: ServerSocket? = null
        private val ftpHandler: LocalFtpHandler = LocalFtpHandler()
        private val requestHandler: RequestHandler = RequestHandler(ftpHandler)
        override fun run() {
            super.run()
            createServer()
        }

        private fun createServer() {
            try {
                serverSocket = ServerSocket(port, -1, getIpAddress())
                while (isServerRunning) {
                    val socket = serverSocket?.accept()

                    if (socket != null && socket.isConnected) {
                        //val header = ftpHandler.readHeader(socket.getInputStream())
                        requestHandler.handleRequest(socket,socket.getInputStream(), socket.getOutputStream())

                    }
                }
            } catch (e: Exception) {
                e.message?.let { mListener?.onError(it) }
                isServerRunning = false
                Log.i(javaClass.simpleName, "${e.message}")
                serverSocket?.close()
                createServer()

            }
        }

        private fun handleRequest(
            socket: Socket,
            header: LocalFtpHandler.HttpHeader,
            input: InputStream,
            output: OutputStream
        ) {
            // requestHandler.handleRequest(socket, header, input, output)
        }

        fun stopServer() {
            serverSocket?.close()
            serverSocket = null
        }
    }

    private fun getIpAddress(): InetAddress? {
        val byteBuffer = ByteBuffer.allocate(4)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.putInt(ip)

        val inetAddress: InetAddress? = try {
            InetAddress.getByAddress(null, byteBuffer.array())
        } catch (e: UnknownHostException) {
            InetAddress.getByAddress("127.0.0.1".toByteArray())
        }
        return inetAddress
    }

    fun getLocalFtpAddress(): String {
        return if (ip == 0) {
            "127.0.0.1:$port"
        } else
            "${getIpAddress()}:$port"
    }
}