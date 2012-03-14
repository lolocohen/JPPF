/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.server.nio;

import java.io.IOException;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.*;

import javax.net.ssl.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Wrapper for an {@link SSLEngine} and an associated channel.
 */
public class SSLHandler
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SSLHandler.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The socket channel from which data is read or to which data is written.
   */
  private SocketChannel channel;
  /**
   * The SSLEngine performs the SSL-related operations before sending data/after receiving data.
   */
  private SSLEngine sslEngine;
  /**
   * Contains the result of the latest <code>wrap()</code> or <code>unwrap()</code> operation on the <code>SSLEngine</code>.
   */
  private SSLEngineResult sslEngineResult = null;
  /**
   * Single thread pool used to executed the delegated tasks generated by the SSL handshake.
   */
  private ExecutorService executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("SSLDelegatedTasks"));
  /**
   * The data the application is sending.
   */
  private ByteBuffer applicationSendBuffer;
  /**
   * The SSL data sent by the <code>SSLEngine</code>.
   */
  private ByteBuffer channelSendBuffer;
  /**
   * The data the application is receiving.
   */
  private ByteBuffer applicationReceiveBuffer;
  /**
   * The data recevied yb the <code>SSLEngine</code>.
   */
  private ByteBuffer channelReceiveBuffer;

  /**
   * Instantiate this SSLHandler with the specified channel and SSL engine.
   * @param channel the channel from which data is read or to which data is written. 
   * @param sslEngine performs the SSL-related operations before sending data/after receiving data.
   * @throws Exception if any error occurs.
   */
  public SSLHandler(final ChannelWrapper<?> channel, final SSLEngine sslEngine) throws Exception
  {
    this.channel = (SocketChannel) ((SelectionKey) channel.getChannel()).channel();
    this.sslEngine = sslEngine;
    SSLSession session = sslEngine.getSession();
    this.applicationSendBuffer = ByteBuffer.wrap(new byte[session.getApplicationBufferSize()]);
    this.channelSendBuffer = ByteBuffer.wrap(new byte[session.getPacketBufferSize()]);
    this.applicationReceiveBuffer = ByteBuffer.wrap(new byte[session.getApplicationBufferSize()]);
    this.channelReceiveBuffer = ByteBuffer.wrap(new byte[session.getPacketBufferSize()]);
  }

  /**
   * Read from the channel via the SSLEngine into the application receive buffer.
   * Called in blocking mode when input is expected, or in non-blocking mode when the channel is readable.
   * @return the number of bytes read.
   * @throws Exception if any error occurs.
   */
  public int read() throws Exception
  {
    int sslCount = 0;
    int count = applicationReceiveBuffer.position();
    do
    {
      flush();
      if (sslEngine.isInboundDone()) return count > 0 ? count : -1;
      int readCount = channel.read(channelReceiveBuffer);
      channelReceiveBuffer.flip();
      sslEngineResult = sslEngine.unwrap(channelReceiveBuffer, applicationReceiveBuffer);
      channelReceiveBuffer.compact();
      switch (sslEngineResult.getStatus())
      {
        case BUFFER_UNDERFLOW:
          if (traceEnabled) log.trace("reading into netRecv=" + channelReceiveBuffer);
          sslCount = channel.read(channelReceiveBuffer);
          if (traceEnabled) log.trace("sslCount=" + sslCount + ", channelReceiveBuffer=" + channelReceiveBuffer);
          if (sslCount == 0) return count;
          if (sslCount == -1)
          {
            if (traceEnabled) log.trace("reached EOF, closing inbound");
            sslEngine.closeInbound();
          }
          break;

        case BUFFER_OVERFLOW:
          return 0;

        case CLOSED:
          channel.socket().shutdownInput();
          break;

        case OK:
          count = applicationReceiveBuffer.position();
          break;
      }
      while (processHandshakeStatus());
      count = applicationReceiveBuffer.position();
    }
    while (count == 0);
    if (sslEngine.isInboundDone()) count = -1;
    return count;
  }

  /**
   * Write from the application send buffer to the channel via the SSLEngine.
   * @return the number of bytes consumed from the application.
   * @throws Exception if any error occurs.
   */
  public int write() throws Exception
  {
    if (traceEnabled) log.trace("position=" + applicationSendBuffer.position());
    int remaining = applicationSendBuffer.position();
    int writeCount = 0;
    if ((remaining > 0) && (flush() > 0)) return 0;
    while (remaining > 0)
    {
      if (traceEnabled) log.trace("before flip/wrap/compact applicationSendBuffer=" + applicationSendBuffer + " channelSendBuffer=" + channelSendBuffer + " count=" + remaining);
      applicationSendBuffer.flip();
      sslEngineResult = sslEngine.wrap(applicationSendBuffer, channelSendBuffer);
      applicationSendBuffer.compact();
      if (traceEnabled) log.trace("after flip/wrap/compact  applicationSendBuffer=" + applicationSendBuffer + " channelSendBuffer=" + channelSendBuffer);
      switch (sslEngineResult.getStatus())
      {
        case BUFFER_UNDERFLOW:
          if (traceEnabled) log.trace("write", new BufferUnderflowException());
          throw new BufferUnderflowException();

        case BUFFER_OVERFLOW:
          if (traceEnabled) log.trace("buffer overflow, before flush() channelSendBuffer=" + channelSendBuffer);
          int flushCount = flush();
          if (traceEnabled) log.trace("buffer overflow, after flush()  channelSendBuffer=" + channelSendBuffer + ", flushCount=" + flushCount);
          if (flushCount == 0) return 0;
          continue;

        case CLOSED:
          throw new SSLException("outbound closed");

        case OK:
          int n = sslEngineResult.bytesConsumed();
          writeCount += n;
          remaining -= n;
          break;
      }
      while (processHandshakeStatus());
    }
    return writeCount;
  }

  /**
   * Flush the underlying channel.
   * @return the number of bytes flushed.
   * @throws IOException if any error occurs.
   */
  public int flush() throws IOException
  {
    channelSendBuffer.flip();
    int n = channel.write(channelSendBuffer);
    channelSendBuffer.compact();
    return n;
  }

  /**
   * Close the underlying channel and SSL engine.
   * @throws Exception if any error occurs.
   */
  public void close() throws Exception
  {
    if (!sslEngine.isInboundDone() && !channel.isBlocking()) read();
    while (channelSendBuffer.position() > 0)
    {
      int n = flush();
      if (n == 0)
      {
        log.error("unable to flush remaining " + channelSendBuffer.remaining() + " bytes");
        break;
      }
    }
    sslEngine.closeOutbound();
    if (traceEnabled) log.trace("close outbound handshake");
    while (processHandshakeStatus());
    if (channelSendBuffer.position() > 0 && flush() == 0) log.error("unable to flush remaining " + channelSendBuffer.position() + " bytes");
    if (traceEnabled) log.trace("close outbound done");
    channel.close();
    if (traceEnabled) log.trace("SSLEngine closed");
  }

  /**
   * 
   * @throws Exception if any error occurs.
   */
  private void processEngineResult() throws Exception
  {
    while (processStatus() && processHandshakeStatus()) continue;
  }

  /**
   * Process the current handshaking sttaus.
   * @return <code>true</code> if handshaking is still ongoing, <code>false</code> otherwise.
   * @throws Exception if any error occurs.
   */
  private boolean processHandshakeStatus() throws Exception
  {
    int count;
    switch (sslEngine.getHandshakeStatus())
    {
      case NOT_HANDSHAKING:
      case FINISHED:
        return false;

      case NEED_TASK:
        performDelegatedTasks();
        return true;

      case NEED_WRAP:
        applicationSendBuffer.flip();
        sslEngineResult = sslEngine.wrap(applicationSendBuffer, channelSendBuffer);
        applicationSendBuffer.compact();
        if (sslEngineResult.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW)
        {
          count = flush();
          return count > 0;
        }
        return true;

      case NEED_UNWRAP:
        channelReceiveBuffer.flip();
        sslEngineResult = sslEngine.unwrap(channelReceiveBuffer, applicationReceiveBuffer);
        channelReceiveBuffer.compact();
        if (sslEngineResult.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW)
        {
          if (sslEngine.isInboundDone()) count = -1;
          else count = channel.read(channelReceiveBuffer);
          if (traceEnabled) log.trace("readCount=" + count);
          return count > 0;
        }
        if (sslEngineResult.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) return false;
        return true;

      default:
        return false;
    }
  }

  /**
   * 
   * @return true if a full SSL packet was read or written, false otherwise.
   * @throws Exception if any error occurs.
   */
  boolean processStatus() throws Exception
  {
    int count;
    if (traceEnabled) log.trace("engineResult=" + sslEngineResult);
    switch (sslEngineResult.getStatus())
    {
      case OK:
        return true;

      case CLOSED:
        return sslEngineResult.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;

      case BUFFER_OVERFLOW:
        switch (sslEngineResult.getHandshakeStatus())
        {
          case NEED_WRAP:
            flush();
            return channelSendBuffer.position() == 0;

          case NEED_UNWRAP:
            //if (traceEnabled) log.trace("netSendBuffer=" + netSendBuffer + ", netRecvBuffer=" + netRecvBuffer + ", appSendBuffer=" + appSendBuffer + ", appRecvBuffer=" + appRecvBuffer);
            return false;

          default:
            return false;
        }

      case BUFFER_UNDERFLOW:
        //if (traceEnabled) log.trace("netSendBuffer=" + netSendBuffer + ", netRecvBuffer=" + netRecvBuffer + ", appSendBuffer=" + appSendBuffer + ", appRecvBuffer=" + appRecvBuffer);
        flush();
        count = channel.read(channelReceiveBuffer);
        //if (traceEnabled) log.trace("underflow: read " + count + " netRecv=" + netRecvBuffer);
        return count > 0;

      default:
        return false;
    }
  }

  /**
   * Run delegated tasks for the handshake.
   */
  private void performDelegatedTasks()
  {
    Runnable delegatedTask;
    while ((delegatedTask = sslEngine.getDelegatedTask()) != null)
    {
      if (traceEnabled) log.trace("running delegated task " + delegatedTask);
      Future<?> f = executor.submit(delegatedTask);
      try
      {
        f.get();
      }
      catch (Exception e)
      {
        if (traceEnabled) log.trace(e.getMessage(), e);
        else log.warn(ExceptionUtils.getMessage(e));
      }
    }
  }

  /**
   * Get the application receive buffer.
   * @return a {@link ByteBuffer} instance.
   */
  public ByteBuffer getApplicationReceiveBuffer()
  {
    return applicationReceiveBuffer;
  }

  /**
   * Get the application send buffer.
   * @return a {@link ByteBuffer} instance.
   */
  public ByteBuffer getApplicationSendBuffer()
  {
    return applicationSendBuffer;
  }

  /**
   * Get the channel receive buffer.
   * @return a {@link ByteBuffer} instance.
   */
  public ByteBuffer getChannelReceiveBuffer()
  {
    return channelReceiveBuffer;
  }
  /**
   * Get the channel send buffer.
   * @return a {@link ByteBuffer} instance.
   */
  public ByteBuffer getChannelSendBuffer()
  {
    return channelSendBuffer;
  }
}
