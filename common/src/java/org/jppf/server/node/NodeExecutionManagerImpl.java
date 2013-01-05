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

package org.jppf.server.node;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.node.*;
import org.jppf.node.protocol.*;
import org.jppf.scheduling.*;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class manage the execution of JPPF tasks by a node.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public class NodeExecutionManagerImpl extends ThreadSynchronization implements NodeExecutionManager
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodeExecutionManagerImpl.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The node that uses this execution manager.
   */
  private Node node = null;
  /**
   * Timer managing the tasks timeout.
   */
  protected final JPPFScheduleHandler timeoutHandler = new JPPFScheduleHandler("Task Timeout Timer");
  /**
   * Mapping of internal number to the corresponding tasks.
   */
  protected final Map<Long, NodeTaskWrapper> taskMap = new Hashtable<Long, NodeTaskWrapper>();
  /**
   * The bundle whose tasks are currently being executed.
   */
  protected JPPFTaskBundle bundle = null;
  /**
   * The list of tasks to execute.
   */
  protected List<? extends Task> taskList = null;
  /**
   * The uuid path of the current bundle.
   */
  protected List<String> uuidList = null;
  /**
   * Holds a the set of futures generated by submitting each task.
   */
  protected final Map<Long, Future<?>> futureMap = new Hashtable<Long, Future<?>>();
  /**
   * Counter for the number of tasks whose execution is over,
   * including tasks that completed normally, were cancelled or timed out.
   */
  protected final AtomicLong taskCount = new AtomicLong(0L);
  /**
   * List of listeners to task execution events.
   */
  private final List<TaskExecutionListener> taskExecutionListeners = new ArrayList<TaskExecutionListener>();
  /**
   * Temporary array of listeners used for faster access.
   */
  private TaskExecutionListener[] listenersArray = new TaskExecutionListener[0];
  /**
   * Determines whether the number of threads or their priority has changed.
   */
  protected final AtomicBoolean configChanged = new AtomicBoolean(true);
  /**
   * Set if the node must reconnect to the driver.
   */
  protected JPPFNodeReconnectionNotification reconnectionNotification = null;
  /**
   * The thread manager that is used for execution.
   */
  private final ThreadManager threadManager;
  /**
   * Determines whether the current job has been cancelled.
   */
  private boolean jobCancelled = false;
  /**
   * The class loader used to load the tasks and the classes they need from the client.
   */
  private AbstractJPPFClassLoader taskClassLoader = null;
  /**
   * The data provider for the current job.
   */
  private DataProvider dataProvider = null;

  /**
   * Initialize this execution manager with the specified node.
   * @param node the node that uses this execution manager.
   */
  public NodeExecutionManagerImpl(final Node node)
  {
    this(node, "processing.threads");
  }

  /**
   * Initialize this execution manager with the specified node.
   * @param node the node that uses this execution manager.
   * @param nbThreadsProperty the name of the property which configures the number of threads.
   */
  public NodeExecutionManagerImpl(final Node node, final String nbThreadsProperty)
  {
    super();
    if(node == null) throw new IllegalArgumentException("node is null");
    this.node = node;
    TypedProperties config = JPPFConfiguration.getProperties();
    int poolSize = config.getInt(nbThreadsProperty, Runtime.getRuntime().availableProcessors());
    if (poolSize <= 0)
    {
      poolSize = Runtime.getRuntime().availableProcessors();
      config.setProperty(nbThreadsProperty, Integer.toString(poolSize));
    }
    log.info("running " + poolSize + " processing thread" + (poolSize > 1 ? "s" : ""));
    threadManager = createThreadManager(config, poolSize);
  }

  /**
   * Create the thread manager instance. Default is {@link ThreadManagerThreadPool}.
   * @param config The JPPF configuration properties.
   * @param poolSize the initial pool size.
   * @return an instance of {@link ThreadManager}.
   */
  private static ThreadManager createThreadManager(final TypedProperties config, final int poolSize)
  {
    ThreadManager result = null;
    String s = config.getString("jppf.thread.manager.class", "default");
    if(!"default".equalsIgnoreCase(s) && !"org.jppf.server.node.ThreadManagerThreadPool".equals(s) && s != null)
    {
      try
      {
        Class clazz = Class.forName(s);
        Object instance = ReflectionHelper.invokeConstructor(clazz, new Class[]{Integer.TYPE}, poolSize);
        if(instance instanceof ThreadManager) {
          result = (ThreadManager) instance;
          log.info("Using custom thread manager: " + s);
        }
      }
      catch(Exception e)
      {
        log.error(e.getMessage(), e);
      }
    }
    if (result == null)
    {
      log.info("Using default thread manager");
      return new ThreadManagerThreadPool(poolSize);
    }
    config.setProperty("processing.threads", Integer.toString(result.getPoolSize()));
    log.info("Node running " + poolSize + " processing thread" + (poolSize > 1 ? "s" : ""));
    boolean cpuTimeEnabled = result.isCpuTimeEnabled();
    config.setProperty("cpuTimeSupported", Boolean.toString(cpuTimeEnabled));
    log.info("Thread CPU time measurement is " + (cpuTimeEnabled ? "" : "not ") + "supported");
    return result;
  }

  /**
   * Execute the specified tasks of the specified tasks bundle.
   * @param bundle the bundle to which the tasks are associated.
   * @param taskList the list of tasks to execute.
   * @throws Exception if the execution failed.
   */
  public void execute(final JPPFTaskBundle bundle, final List<? extends Task> taskList) throws Exception
  {
    if ((taskList == null) || taskList.isEmpty()) return;
    if (debugEnabled) log.debug("executing " + taskList.size() + " tasks");
    NodeExecutionInfo info = threadManager.isCpuTimeEnabled() ? threadManager.computeExecutionInfo() : null;
    setup(bundle, taskList);
    if (!isJobCancelled())
    {
      for (Task task : taskList) performTask(task);
      waitForResults();
    }
    cleanup();
    if (info != null)
    {
      NodeExecutionInfo info2 = threadManager.computeExecutionInfo().subtract(info);
      if (debugEnabled) log.debug("total cpu time used: " + (info2.cpuTime/ 1000000L) + " ms, user time: " + (info2.userTime/ 1000000L));
    }
  }

  /**
   * Execute a single task.
   * @param task the task to execute.
   * @return a number identifying the task that was submitted.
   * @throws Exception if the execution failed.
   */
  public synchronized long performTask(final Task task) throws Exception
  {
    long number = incTaskCount();
    NodeTaskWrapper taskWrapper = new NodeTaskWrapper(this, task, number, taskClassLoader);
    taskMap.put(number, taskWrapper);
    Future<?> f = getExecutor().submit(taskWrapper);
    if (!f.isDone()) futureMap.put(number, f);
    JPPFSchedule schedule = task.getTimeoutSchedule();
    if ((schedule != null) && ((schedule.getDuration() > 0L) || (schedule.getDate() != null)))
    {
      if (schedule.getDuration() > 0L) processTaskTimeout(taskWrapper, number);
      else if (schedule.getDate() != null) processTaskExpirationDate(taskWrapper, number);
    }
    return number;
  }

  /**
   * Cancel all executing or pending tasks.
   * @param callOnCancel determines whether the onCancel() callback method of each task should be invoked.
   * @param requeue true if the job should be requeued on the server side, false otherwise.
   */
  public synchronized void cancelAllTasks(final boolean callOnCancel, final boolean requeue)
  {
    if (debugEnabled) log.debug("cancelling all tasks with: callOnCancel=" + callOnCancel + ", requeue=" + requeue);
    if (requeue)
    {
      bundle.setParameter(BundleParameter.JOB_REQUEUE, true);
      bundle.getSLA().setSuspended(true);
    }
    List<Long> list = new ArrayList<Long>(futureMap.keySet());
    for (Long n: list) cancelTask(n, callOnCancel);
  }

  /**
   * Cancel the execution of the tasks with the specified id.
   * @param number the index of the task to cancel.
   * @param callOnCancel determines whether the onCancel() callback method of each task should be invoked.
   */
  private synchronized void cancelTask(final Long number, final boolean callOnCancel)
  {
    if (debugEnabled) log.debug("cancelling task number = " + number);
    Future<?> future = futureMap.get(number);
    if (!future.isDone())
    {
      if (debugEnabled) log.debug("calling future.cancel(true) for task number = " + number);
      NodeTaskWrapper taskWrapper = taskMap.remove(number);
      if (taskWrapper != null) taskWrapper.cancel(callOnCancel);
      future.cancel(true);
      removeFuture(number);
    }
  }

  /**
   * Notify the timer that a task must be aborted if its timeout period expired.
   * @param taskWrapper the JPPF task for which to set the timeout.
   * @param number a number identifying the task submitted to the thread pool.
   * @throws Exception if any error occurs.
   */
  private void processTaskExpirationDate(final NodeTaskWrapper taskWrapper, final long number) throws Exception
  {
    Future<?> future = getFutureFromNumber(number);
    TimeoutTimerTask tt = new TimeoutTimerTask(future, taskWrapper);
    timeoutHandler.scheduleAction(future, taskWrapper.getTask().getTimeoutSchedule(), tt);
  }

  /**
   * Notify the timer that a task must be aborted if its timeout period expired.
   * @param taskWrapper the JPPF task for which to set the timeout.
   * @param number a number identifying the task submitted to the thread pool.
   * @throws Exception if any error occurs.
   */
  private void processTaskTimeout(final NodeTaskWrapper taskWrapper, final long number) throws Exception
  {
    Future<?> future = getFutureFromNumber(number);
    TimeoutTimerTask tt = new TimeoutTimerTask(future, taskWrapper);
    timeoutHandler.scheduleAction(future, taskWrapper.getTask().getTimeoutSchedule(), tt);
  }

  /**
   * Shutdown this execution manager.
   */
  public void shutdown()
  {
    getExecutor().shutdownNow();
    timeoutHandler.clear(true);
  }

  /**
   * Prepare this execution manager for executing the tasks of a bundle.
   * @param bundle the bundle whose tasks are to be executed.
   * @param taskList the list of tasks to execute.
   */
  @SuppressWarnings("unchecked")
  public void setup(final JPPFTaskBundle bundle, final List<? extends Task> taskList)
  {
    this.bundle = bundle;
    this.taskList = taskList;
    this.dataProvider = taskList.get(0).getDataProvider();
    this.uuidList = bundle.getUuidPath().getList();
    try
    {
      taskClassLoader = (AbstractJPPFClassLoader) ((node instanceof ClassLoaderProvider) ? ((ClassLoaderProvider)node).getClassLoader(uuidList) : null);
    }
    catch (Exception e)
    {
      String msg = ExceptionUtils.getMessage(e) + " - class loader lookup failed for uuidPath=" + uuidList;
      if (debugEnabled) log.debug(msg, e);
      else log.warn(msg);
    }
    taskCount.set(0L);
    node.getLifeCycleEventHandler().fireJobStarting(bundle, taskClassLoader, (List<Task>) taskList, dataProvider);
  }

  /**
   * Cleanup method invoked when all tasks for the current bundle have completed.
   */
  @SuppressWarnings("unchecked")
  public void cleanup()
  {
    node.getLifeCycleEventHandler().fireJobEnding(bundle, taskClassLoader, (List<Task>) taskList, dataProvider);
    taskClassLoader = null;
    this.bundle = null;
    this.taskList = null;
    this.uuidList = null;
    setJobCancelled(false);
    futureMap.clear();
    taskMap.clear();
    timeoutHandler.clear();
  }

  /**
   * Wait until all tasks are complete.
   * @throws Exception if the execution failed.
   */
  public synchronized void waitForResults() throws Exception
  {
    while (!futureMap.isEmpty() && (getReconnectionNotification() == null)) goToSleep();
    if (getReconnectionNotification() != null)
    {
      cancelAllTasks(true, false);
      throw reconnectionNotification;
    }
  }

  /**
   * Remove the specified future from the pending set and notify
   * all threads waiting for the end of the execution.
   * @param number task identifier for the future of the task to remove.
   */
  public synchronized void removeFuture(final long number)
  {
    Future<?> future = futureMap.remove(number);
    if (future != null) timeoutHandler.cancelAction(future);
    wakeUp();
  }

  /**
   * Increment the current task count and return the new value.
   * @return the new values a long.
   */
  private long incTaskCount()
  {
    return taskCount.incrementAndGet();
  }

  @Override
  public void taskEnded(final Task task, final long taskNumber, final NodeExecutionInfo info, final long elapsedTime)
  {
    long cpuTime = (info == null) ? 0L : (info.cpuTime / 1000000L);
    TaskExecutionEvent event = new TaskExecutionEvent(task, getCurrentJobId(), cpuTime, elapsedTime, task.getException() != null);
    TaskExecutionListener[] tmp;
    synchronized(taskExecutionListeners)
    {
      tmp = listenersArray;
    }
    for (TaskExecutionListener listener : tmp) listener.taskExecuted(event);
  }

  /**
   * Get the future corresponding to the specified task number.
   * @param number the number identifying the task.
   * @return a <code>Future</code> instance.
   */
  public synchronized Future<?> getFutureFromNumber(final long number)
  {
    return futureMap.get(number);
  }

  @Override
  public JPPFDistributedJob getCurrentJob()
  {
    return bundle;
  }

  @Override
  public List<Task> getTasks()
  {
    return taskList == null ? null : Collections.unmodifiableList(taskList);
  }

  @Override
  public String getCurrentJobId()
  {
    return (bundle != null) ? bundle.getUuid() : null;
  }

  /**
   * Add a task execution listener to the list of task execution listeners.
   * @param listener the listener to add.
   */
  public void addTaskExecutionListener(final TaskExecutionListener listener)
  {
    synchronized(taskExecutionListeners)
    {
      taskExecutionListeners.add(listener);
      listenersArray = taskExecutionListeners.toArray(new TaskExecutionListener[taskExecutionListeners.size()]);
    }
  }

  /**
   * Remove a task execution listener from the list of task execution listeners.
   * @param listener the listener to remove.
   */
  public void removeTaskExecutionListener(final TaskExecutionListener listener)
  {
    synchronized(taskExecutionListeners)
    {
      taskExecutionListeners.remove(listener);
      listenersArray = taskExecutionListeners.toArray(new TaskExecutionListener[taskExecutionListeners.size()]);
    }
  }

  /**
   * Get the executor used by this execution manager.
   * @return an <code>ExecutorService</code> instance.
   */
  public ExecutorService getExecutor()
  {
    return threadManager.getExecutorService();
  }

  /**
   * Determines whether the configuration has changed and resets the flag if it has.
   * @return true if the config was changed, false otherwise.
   */
  public boolean checkConfigChanged()
  {
    return configChanged.compareAndSet(true, false);
  }

  /**
   * Trigger the configuration changed flag.
   */
  public void triggerConfigChanged()
  {
    configChanged.compareAndSet(false, true);
  }

  @Override
  public synchronized JPPFNodeReconnectionNotification getReconnectionNotification()
  {
    return reconnectionNotification;
  }

  @Override
  public synchronized void setReconnectionNotification(final JPPFNodeReconnectionNotification reconnectionNotification)
  {
    try
    {
      if (this.reconnectionNotification != null) return;
      this.reconnectionNotification = reconnectionNotification;
    }
    finally
    {
      wakeUp();
    }
  }

  @Override
  public Node getNode()
  {
    return node;
  }

  /**
   * Set the size of the node's thread pool.
   * @param size the size as an int.
   */
  public void setThreadPoolSize(final int size)
  {
    if (size <= 0)
    {
      log.warn("ignored attempt to set the thread pool size to 0 or less: " + size);
      return;
    }
    int oldSize = getThreadPoolSize();
    threadManager.setPoolSize(size);
    int newSize = getThreadPoolSize();
    if(oldSize != newSize)
    {
      log.info("Node thread pool size changed from " + oldSize + " to " + size);
      JPPFConfiguration.getProperties().setProperty("processing.threads", Integer.toString(size));
      configChanged.set(true);
    }
  }

  /**
   * Get the size of the node's thread pool.
   * @return the size as an int.
   */
  public int getThreadPoolSize()
  {
    return threadManager.getPoolSize();
  }

  /**
   * Get the priority assigned to the execution threads.
   * @return the priority as an int value.
   */
  public int getThreadsPriority()
  {
    return threadManager.getPriority();
  }

  /**
   * Update the priority of all execution threads.
   * @param newPriority the new priority to set.
   */
  public void updateThreadsPriority(final int newPriority)
  {
    threadManager.setPriority(newPriority);
  }

  @Override
  public ThreadManager getThreadManager()
  {
    return threadManager;
  }

  /**
   * Determine whether the current job has been cancelled, including before starting its execution.
   * @return <code>true</code> if the job has been cancelled, <code>false</code> otherwise.
   */
  public synchronized boolean isJobCancelled()
  {
    return jobCancelled;
  }

  /**
   * Specify whether the current job has been cancelled, including before starting its execution.
   * @param jobCancelled <code>true</code> if the job has been cancelled, <code>false</code> otherwise.
   */
  public synchronized void setJobCancelled(final boolean jobCancelled)
  {
    this.jobCancelled = jobCancelled;
  }
}
