package com.pedro.rtspserver

import android.util.Log
import com.pedro.rtsp.rtsp.Protocol
import com.pedro.rtsp.rtsp.RtspSender
import com.pedro.rtsp.rtsp.commands.Method
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer

open class ServerClient(private val socket: Socket, serverIp: String, serverPort: Int,
    connectCheckerRtsp: ConnectCheckerRtsp, sps: ByteBuffer?,
    pps: ByteBuffer?, vps: ByteBuffer?, private val sampleRate: Int,
    isStereo: Boolean, isOnlyAudio: Boolean, user: String?, password: String?,
    private val listener: ClientListener) : Thread() {

  private val TAG = "Client"
  private var cSeq = 0
  private val output = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
  private val input = BufferedReader(InputStreamReader(socket.getInputStream()))
  val rtspSender = RtspSender(connectCheckerRtsp)
  private val commandsManager =
      ServerCommandManager(
          serverIp,
          serverPort,
          socket.inetAddress.hostAddress,
      )
  var canSend = false

  init {
    commandsManager.isOnlyAudio = isOnlyAudio
    commandsManager.isStereo = isStereo
    commandsManager.sampleRate = sampleRate
    commandsManager.setVideoInfo(sps, pps, vps)
    commandsManager.setAuth(user, password)
  }

  override fun run() {
    super.run()
    Log.i(TAG, "New client ${commandsManager.clientIp}")
    while (!interrupted()) {
      try {
        val request = commandsManager.getRequest(input)
        cSeq = request.cSeq //update cSeq
        if (cSeq == -1) { //If cSeq parsed fail send error to client
          output.write(commandsManager.createError(500, cSeq))
          output.flush()
          continue
        }
        val response = commandsManager.createResponse(request.method, request.text, cSeq)
        Log.i(TAG, response)
        output.write(response)
        output.flush()

        if (request.method == Method.PLAY) {
          Log.i(TAG, "Protocol ${commandsManager.protocol}")
          rtspSender.setSocketsInfo(commandsManager.protocol, commandsManager.videoClientPorts,
              commandsManager.audioClientPorts)
          if (!commandsManager.isOnlyAudio) {
            rtspSender.setVideoInfo(commandsManager.sps!!, commandsManager.pps!!, commandsManager.vps)
          }
          rtspSender.setAudioInfo(sampleRate)
          rtspSender.setDataStream(socket.getOutputStream(), commandsManager.clientIp!!)
          if (commandsManager.protocol == Protocol.UDP) {
            if (!commandsManager.isOnlyAudio) {
              rtspSender.setVideoPorts(commandsManager.videoPorts[0], commandsManager.videoPorts[1])
            }
            rtspSender.setAudioPorts(commandsManager.audioPorts[0], commandsManager.audioPorts[1])
          }
          rtspSender.start()
          canSend = true
        }
      } catch (e: SocketException) { // Client has left
        Log.e(TAG, "Client disconnected", e)
        listener.onDisconnected(this)
        break
      } catch (e: Exception) {
        Log.e(TAG, "Unexpected error", e)
      }
    }
  }

  fun stopClient() {
    canSend = false
    rtspSender.stop()
    interrupt()
    try {
      join(100)
    } catch (e: InterruptedException) {
      interrupt()
    } finally {
      socket.close()
    }
  }

  fun hasCongestion(): Boolean = rtspSender.hasCongestion()
}