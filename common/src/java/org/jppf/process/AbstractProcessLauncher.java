/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
package org.jppf.process;

import java.io.EOFException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jppf.comm.socket.*;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * This class wraps a single slave node process and provides an API to start, stop and monitor it.
 * @author Laurent Cohen
 * @since 5.0
 * @exclude
 */
public abstract class AbstractProcessLauncher extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractProcessLauncher.class);
  /**
   * A reference to the JPPF driver subprocess, used to kill it when the driver launcher exits.
   */
  protected Process process = null;
  /**
   * The server socket the node listens to.
   */
  protected ServerSocket processServer = null;
  /**
   * The port number the server socket listens to.
   */
  protected int processPort = 0;
  /**
   * Wraps the socket accepted from the node process.
   * @since 5.0
   */
  protected SlaveSocketWrapper slaveSocketWrapper = null;
  /**
   * The registered listeners.
   */
  protected final List<ProcessLauncherListener> listeners = new CopyOnWriteArrayList<>();
  /**
   * Name given to this process, also used as root installation directory.
   */
  protected String name;

  /**
   * Parse the specified String as a list of JVM options.
   * @param s the string to parse.
   * @return A list of jvm options.
   */
  protected Pair<List<String>, List<String>> parseJvmOptions(final String s) {
    List<String> jvmOptions = new ArrayList<>();
    List<String> cpElements = new ArrayList<>();
    Pair<List<String>, List<String>> result = new Pair<>(jvmOptions, cpElements);
    if (s != null) {
      String[] options = s.split("\\s");
      int count = 0;
      while (count < options.length) {
        String option = options[count++];
        if ("-cp".equalsIgnoreCase(option) || "-classpath".equalsIgnoreCase(option)) cpElements.add(options[count++]);
        else jvmOptions.add(option);
      }
    }
    return result;
  }

  /**
   * Forcibly terminate the process.
   */
  public void stopProcess() {
    if (process != null) process.destroy();
  }

  /**
   * Called when the subprocess has exited with exit value n.
   * This allows for printing the residual output (both standard and error) to this pJVM's console and log file,
   * in order to get additional information if a problem occurred.
   * @param n the exit value of the subprocess.
   * @return true if this launcher is to be terminated, false if it should re-launch the subprocess.
   */
  protected abstract boolean onProcessExit(final int n);

  /**
   * Start a server socket that will accept one connection at a time with the JPPF driver, so the server can shutdown properly,
   * when this driver is killed, by a way other than the API (ie CTRL-C or killing the process through the OS shell).<br>
   * The port the server socket listens to is dynamically attributed, which is obtained by using the constructor
   * <code>new ServerSocket(0)</code>.<br>
   * The driver will connect and listen to this port, and exit when the connection is broken.<br>
   * The single connection at a time is obtained by doing the <code>ServerSocket.accept()</code> and the
   * <code>Socket.getInputStream().read()</code> in the same thread.
   * @return the port number on which the server socket is listening.
   */
  protected int startSocketListener() {
    try {
      if (processServer == null) {
        processServer = new ServerSocket(0);
        processPort = processServer.getLocalPort();
      }
      slaveSocketWrapper = new SlaveSocketWrapper();
      Thread thread = new Thread(slaveSocketWrapper, getName() + "ServerSocket");
      thread.setDaemon(true);
      thread.start();
    } catch(Exception e) {
      if (processServer != null) StreamUtils.closeSilent(processServer);
    }
    return processPort;
  }

  /**
   * Create a shutdown hook that is run when this JVM terminates.<br>
   * This is normally used to ensure the subprocess is terminated as well.
   */
  protected void createShutdownHook() {
    Runnable hook = new Runnable() {
      @Override
      public void run() {
        stopProcess();
      }
    };
    Runtime.getRuntime().addShutdownHook(new Thread(hook));
  }

  /**
   * Get the name given to this process.
   * @return the name as a string.
   */
  public String getName() {
    return name == null ? getClass().getSimpleName() : name;
  }

  /**
   * Add a listener to the list of listeners.
   * @param listener the listener to add.
   */
  public void addProcessLauncherListener(final ProcessLauncherListener listener) {
    if (listener == null) return;
    listeners.add(listener);
  }

  /**
   * Remove a listener from the list of listeners.
   * @param listener the listener to remove.
   */
  public void removeProcessLauncherListener(final ProcessLauncherListener listener) {
    if (listener == null) return;
    listeners.remove(listener);
  }

  /**
   * Notify all listeners that the process has started.
   */
  protected void fireProcessStarted() {
    if (log.isDebugEnabled()) log.debug("process [{}:{}] has started", getName(), process);
    ProcessLauncherEvent event = new ProcessLauncherEvent(this);
    for (ProcessLauncherListener listener: listeners) listener.processStarted(event);
  }

  /**
   * Notify all listeners that the process has stopped.
   */
  protected void fireProcessStopped() {
    if (log.isDebugEnabled()) log.debug("process [{}:{}] has stopped", getName(), process);
    ProcessLauncherEvent event = new ProcessLauncherEvent(this);
    for (ProcessLauncherListener listener: listeners) listener.processStopped(event);
  }

  /**
   * Send an action command to the slave node.
   * @param action the code of the action command to send.
   */
  public void sendActionCommand(final int action) {
    if (slaveSocketWrapper != null) slaveSocketWrapper.sendActionCommand(action);
  }

  /**
   * Accepts a connection from the child process and sends process commands as int values via the connection.
   * <p>The protocol is very simple: a single int command with no parametr is sent to the child process, which interprets it as it wishes.
   * No acknowledgement or response is expected.
   * @since 5.0
   * @exclude
   */
  protected class SlaveSocketWrapper implements Runnable {
    /**
     * Wrapper for the accepted socket.
     */
    private SocketWrapper socketClient = null;

    @Override
    public void run() {
      try {
        socketClient = new BootstrapSocketClient(processServer.accept());
        int n = socketClient.readInt();
        if (n == -1) throw new EOFException();
      } catch(Exception ioe) {
        if (log.isDebugEnabled()) log.debug(getName(), ioe);
        if (socketClient != null) StreamUtils.closeSilent(socketClient);
      }
    }

    /**
     * Send an action command to the slave node.
     * @param action the code of the action command to send.
     */
    protected void sendActionCommand(final int action) {
      if (socketClient == null) return;
      try {
        socketClient.writeInt(action);
      } catch (Exception e) {
        if (log.isDebugEnabled()) log.debug("could not send command to slave process {} : {}", getName(), ExceptionUtils.getStackTrace(e));
      }
    }
  }
}