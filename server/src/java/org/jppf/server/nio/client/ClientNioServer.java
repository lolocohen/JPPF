/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.server.nio.client;

import java.util.*;

import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.*;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * Instances of this class serve task execution requests to the JPPF nodes.
 * @author Laurent Cohen
 */
public class ClientNioServer extends NioServer<ClientState, ClientTransition>
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ClientNioServer.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Reference to the driver.
   */
  private static JPPFDriver driver;
  /**
   * 
   */
  private List<ChannelWrapper<?>> channels = new ArrayList<ChannelWrapper<?>>();

  /**
   * Initialize this class loader server.
   * @param driver reference to the driver.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public ClientNioServer(final JPPFDriver driver) throws Exception
  {
    super(NioConstants.CLIENT_SERVER);
    if (driver == null) throw new IllegalArgumentException("driver is null");

    this.driver = driver;
    this.selectTimeout = NioConstants.DEFAULT_SELECT_TIMEOUT;
  }

  @Override
  protected NioServerFactory<ClientState, ClientTransition> createFactory()
  {
    return new ClientServerFactory(this);
  }

  @Override
  public void postAccept(final ChannelWrapper<?> channel)
  {
    try
    {
      synchronized(channels)
      {
        channels.add(channel);
      }
      transitionManager.transitionChannel(channel, ClientTransition.TO_WAITING_HANDSHAKE);
    }
    catch (Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
      closeClient(channel);
    }
    driver.getStatsManager().newClientConnection();
    if (JPPFDriver.JPPF_DEBUG) driver.getInitializer().getServerDebug().addChannel(channel, NioConstants.CLIENT_SERVER);
  }

  @Override
  public NioContext createNioContext()
  {
    return new ClientContext();
  }

  /**
   * Remove the specified channel.
   * @param channel the channel to remove.
   */
  public void removeChannel(final ChannelWrapper<?> channel)
  {
    synchronized(channels)
    {
      channels.remove(channel);
    }
  }

  /**
   * Attempts to close the connection witht he specified uuid.
   * @param connectionUuid the connection uuid to correlate.
   */
  public void closeClientConnection(final String connectionUuid)
  {
    ChannelWrapper<?> channel = null;
    if (debugEnabled) log.debug("closing client channel with connectionUuid=" + connectionUuid);
    synchronized(channels)
    {
      for (ChannelWrapper<?> ch: channels)
      {
        ClientContext context = (ClientContext) ch.getContext();
        if (context.getConnectionUuid().equals(connectionUuid))
        {
          channel = ch;
          break;
        }
      }
      if (channel != null) closeClient(channel);
    }
  }

  /**
   * Close a connection to a node.
   * @param channel a <code>SocketChannel</code> that encapsulates the connection.
   */
  public static void closeClient(final ChannelWrapper<?> channel)
  {
    if (debugEnabled) log.debug("closing client channel " + channel);
    if (JPPFDriver.JPPF_DEBUG) driver.getInitializer().getServerDebug().removeChannel(channel, NioConstants.CLIENT_SERVER);
    try
    {
      driver.getClientNioServer().removeChannel(channel);
      channel.close();
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }
    try
    {
      driver.getStatsManager().clientConnectionClosed();
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public boolean isIdle(final ChannelWrapper<?> channel)
  {
    return ClientState.IDLE == channel.getContext().getState();
  }
}
