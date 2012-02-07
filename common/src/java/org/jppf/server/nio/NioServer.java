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
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.*;

import org.jppf.classloader.ResourceProvider;
import org.jppf.comm.socket.SocketWrapper;
import org.slf4j.*;


/**
 * Generic server for non-blocking asynchronous socket channel based communications.<br>
 * Instances of this class rely on a number of possible states for each socket channel,
 * along with the possible transitions between thoses states.<br>
 * The design of this class enforces the use of typesafe enumerations for the states
 * and transitions, so the developers must think ahead of how to implement their server
 * as a state machine.
 * @param <S> the type of the states to use.
 * @param <T> the type of the transitions to use.
 * @author Laurent Cohen
 */
public abstract class NioServer<S extends Enum<S>, T extends Enum<T>> extends Thread
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NioServer.class);
  /**
   * the selector of all socket channels open with providers or nodes.
   */
  protected Selector selector;
  /**
   * Reads resource files from the classpath.
   */
  protected ResourceProvider resourceProvider = new ResourceProvider();
  /**
   * Flag indicating that this socket server is closed.
   */
  private AtomicBoolean stopped = new AtomicBoolean(false);
  /**
   * The ports this server is listening to.
   */
  protected int[] ports = null;
  /**
   * The SSL ports this server is listening to.
   */
  protected int[] sslPorts = null;
  /**
   * Timeout for the select() operations. A value of 0 means no timeout, i.e.
   * the <code>Selector.select()</code> will be invoked without parameters.
   */
  protected long selectTimeout = 0L;
  /**
   * The factory for this server.
   */
  protected NioServerFactory<S, T> factory = null;
  /**
   * Lock used to synchronize selector operations.
   */
  protected ReentrantLock lock = new ReentrantLock();
  /**
   * Performs all operations that relate to channel states.
   */
  protected StateTransitionManager<S, T> transitionManager = null;
  /**
   * Shutdown requested for this server
   */
  protected final AtomicBoolean requestShutdown = new AtomicBoolean(false);
  /**
   * The SSL context associated with this server.
   */
  protected SSLContext sslContext = null;

  /**
   * Initialize this server with a specified port number and name.
   * @param name the name given to this thread.
   * @param sequential determines whether the submission of state transitions should be
   * performed sequentially or through the executor thread pool.
   * @throws Exception if the underlying server socket can't be opened.
   */
  protected NioServer(final String name, final boolean sequential) throws Exception
  {
    super(name);
    selector = Selector.open();
    factory = createFactory();
    transitionManager = new StateTransitionManager<S, T>(this, sequential);
  }

  /**
   * Initialize this server with a specified list of port numbers and name.
   * @param ports the list of ports this server accepts connections from.
   * @param sslPorts the list of SSL ports this server accepts connections from.
   * @param name the name given to this thread.
   * @param sequential Determines whether the submission of state transitions should be
   * performed sequentially or through the executor thread pool.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public NioServer(final int[] ports, final int[] sslPorts, final String name, final boolean sequential) throws Exception
  {
    this(name, sequential);
    if (ports != null) this.ports = Arrays.copyOf(ports, ports.length);
    if (sslPorts != null) this.sslPorts = Arrays.copyOf(sslPorts, sslPorts.length);
    init();
  }

  /**
   * Create the factory holding all the states and transition mappings.
   * @return an <code>NioServerFactory</code> instance.
   */
  protected abstract NioServerFactory<S, T> createFactory();

  /**
   * Initialize the underlying server sockets.
   * @throws Exception if any error occurs w<hile initializing the server sockets.
   */
  protected final void init() throws Exception
  {
    if ((ports != null) && (ports.length != 0))
    {
      for (int port: ports)
      {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.socket().setReceiveBufferSize(SocketWrapper.SOCKET_RECEIVE_BUFFER_SIZE);
        server.socket().bind(new InetSocketAddress(port));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
      }
    }
    if ((sslPorts != null) && (sslPorts.length != 0))
    {
      sslContext = SSLContext.getInstance("SSLv2");
      SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
      for (int port: sslPorts)
      {
        ServerSocket socket = factory.createServerSocket();
        socket.setReceiveBufferSize(SocketWrapper.SOCKET_RECEIVE_BUFFER_SIZE);
        socket.bind(new InetSocketAddress(port));
        ServerSocketChannel channel = socket.getChannel();
        channel.configureBlocking(false);
        channel.bind(new InetSocketAddress(port));
        channel.register(selector, SelectionKey.OP_ACCEPT);
      }
    }
  }

  /**
   * Start the underlying server socket by making it accept incoming connections.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    try
    {
      boolean hasTimeout = selectTimeout > 0L;
      while (!isStopped() && !externalStopCondition())
      {
        try
        {
          lock.lock();
        }
        finally
        {
          lock.unlock();
        }
        int n = hasTimeout ? selector.select(selectTimeout) : selector.select();
        if (n > 0) go(selector.selectedKeys());
        postSelect();
      }
    }
    catch (Throwable t)
    {
      log.error(t.getMessage(), t);
    }
    finally
    {
      end();
    }
  }

  /**
   * Determine whether a stop condition external to this server has been reached.
   * The default implementation always returns whether shutdown was requested.<br>
   * Subclasses may override this behavior.
   * @return true if this server should be stopped, false otherwise.
   */
  protected boolean externalStopCondition()
  {
    return requestShutdown.get();
  }

  /**
   * Initiates shutdown of this server.
   */
  public void shutdown() {
    requestShutdown.set(true);
  }

  /**
   * Process the keys selected by the selector for IO operations.
   * @param selectedKeys the set of keys that were selected by the latest <code>select()</code> invocation.
   * @throws Exception if an error is raised while processing the keys.
   */
  public void go(final Set<SelectionKey> selectedKeys) throws Exception
  {
    Iterator<SelectionKey> it = selectedKeys.iterator();
    while (it.hasNext())
    {
      SelectionKey key = it.next();
      it.remove();
      try
      {
        if (!key.isValid()) continue;
        if (key.isAcceptable()) doAccept(key);
        else
        {
          NioContext context = (NioContext) key.attachment();
          transitionManager.submitTransition(context.getChannel());
        }
      }
      catch (Exception e)
      {
        log.error(e.getMessage(), e);
        if (!(key.channel() instanceof ServerSocketChannel))
        {
          try
          {
            key.channel().close();
          }
          catch (Exception e2)
          {
            log.error(e2.getMessage(), e2);
          }
        }
      }
    }
  }

  /**
   * This method is invoked after all selected keys have been processed.
   * This implementation does nothing. Subclasses should override this method as needed.
   */
  public void postSelect()
  {
  }

  /**
   * accept the incoming connection.
   * It accept and put it in a state to define what type of peer is.
   * @param key the selection key that represents the channel's registration with the selector.
   */
  @SuppressWarnings("unchecked")
  private void doAccept(final SelectionKey key)
  {
    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
    SocketChannel channel;
    try
    {
      channel = serverSocketChannel.accept();
    }
    catch (IOException ignored)
    {
      log.error(ignored.getMessage(), ignored);
      return;
    }
    if (channel == null) return;
    accept(channel);
  }

  /**
   * Register an incoming connection with his server's selector.
   * @param channel the socket channel representing the connection.
   * @return a wrapper for the newly registered channel.
   */
  @SuppressWarnings("unchecked")
  public ChannelWrapper<?> accept(final SocketChannel channel)
  {
    try
    {
      channel.socket().setSendBufferSize(SocketWrapper.SOCKET_RECEIVE_BUFFER_SIZE);
      channel.configureBlocking(false);
    }
    catch (IOException e)
    {
      log.error(e.getMessage(), e);
      try
      {
        channel.close();
      }
      catch (IOException ignored)
      {
        log.error(ignored.getMessage(), ignored);
      }
      return null;
    }
    NioContext context = createNioContext();
    SelectionKeyWrapper wrapper = null;
    try
    {
      SelectionKey selKey = channel.register(selector,	getInitialInterest(), context);
      wrapper = new SelectionKeyWrapper(selKey);
      context.setChannel(wrapper);
      postAccept(wrapper);
    }
    catch (ClosedChannelException e)
    {
      wrapper = null;
      log.error(e.getMessage(), e);
    }
    return wrapper;
  }

  /**
   * Process a channel that was accepted by the server socket channel.
   * @param key the selection key for the socket channel to process.
   */
  public abstract void postAccept(ChannelWrapper<?> key);

  /**
   * Define a context for a newly created channel.
   * @return an <code>NioContext</code> instance.
   */
  public abstract NioContext<?> createNioContext();

  /**
   * Get the IO operations a connection is initially interested in.
   * @return a bit-wise combination of the interests, taken from {@link java.nio.channels.SelectionKey SelectionKey}
   * constants definitions.
   */
  public abstract int getInitialInterest();

  /**
   * Close the underlying server socket and stop this socket server.
   */
  public void end()
  {
    if (!isStopped())
    {
      setStopped(true);
      removeAllConnections();
    }
  }

  /**
   * Close and remove all connections accepted by this server.
   */
  public void removeAllConnections()
  {
    if (!isStopped()) return;
    try
    {
      selector.wakeup();
      Set<SelectionKey> keySet = selector.keys();
      for (SelectionKey key: keySet)
      {
        key.channel().close();
        key.cancel();
      }
      selector.close();
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Get the selector for this server.
   * @return a Selector instance.
   */
  public Selector getSelector()
  {
    return selector;
  }

  /**
   * Get the factory for this server.
   * @return an <code>NioServerFactory</code> instance.
   */
  public synchronized NioServerFactory<S, T> getFactory()
  {
    if (factory == null) factory = createFactory();
    return factory;
  }

  /**
   * Get the lock used to synchronize selector operations.
   * @return a <code>ReentrantLock</code> instance.
   */
  public ReentrantLock getLock()
  {
    return lock;
  }

  /**
   * Set this server in the specified stopped state.
   * @param stopped true if this server is stopped, false otherwise.
   */
  protected void setStopped(final boolean stopped)
  {
    this.stopped.set(stopped);
  }

  /**
   * Get the stopped state of this server.
   * @return  true if this server is stopped, false otherwise.
   */
  protected boolean isStopped()
  {
    return stopped.get();
  }

  /**
   * Get the manager that performs all operations that relate to channel states.
   * @return a <code>StateTransitionManager</code> instance.
   */
  public StateTransitionManager<S, T> getTransitionManager()
  {
    return transitionManager;
  }

  /**
   * Get the ports this server is listening to.
   * @return an array of int values.
   */
  public int[] getPorts()
  {
    return ports;
  }
}
