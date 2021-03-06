/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package test.org.jppf.server.protocol;

import static org.junit.Assert.*;

import java.util.*;

import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.*;
import org.jppf.utils.ReflectionUtils;
import org.junit.*;

import test.org.jppf.test.runner.IgnoreForEmbeddedGrid;
import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link org.jppf.node.protocol.JobSLA JobSLA}.
 * In this class, we test that the behavior is the expected one, from the client point of view,
 * as specified in the job SLA.
 * @author Laurent Cohen
 */
public class TestJPPFJobSLA3 extends BaseTest {
  /**
   * Launches a driver and 1 node and start the client.
   * The node has the classes from server and common mpdules in its classpath.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    final TestConfiguration config = TestConfiguration.newDefault();
    final List<String> cp = new ArrayList<>();
    cp.add("../common/classes");
    cp.add("../server/classes");
    config.node.classpath.addAll(cp);
    client = BaseSetup.setup(1, 1, true, true, config);
  }

  /**
   * Stops the driver and nodes and close the client.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanup() throws Exception {
    try {
      BaseSetup.cleanup();
    } finally {
      BaseSetup.resetClientConfig();
    }
  }

  /**
   * Test that a job with remoteClassLoadingEnabled = false results in a ClassNotFoundException (at deserialization in the node).
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  @IgnoreForEmbeddedGrid
  public void testOfflineJob() throws Exception {
    final int nbTasks = 1;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, LifeCycleTask.class, 1L);
    job.getSLA().setRemoteClassLoadingEnabled(false);
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (int i=0; i<nbTasks; i++) {
      final Task<?> task = results.get(i);
      assertNull(task.getResult());
      final Throwable t = task.getThrowable();
      assertNotNull(t);
      assertEquals(ClassNotFoundException.class, t.getClass());
    }
  }

  /**
   * Test that a job with remoteClassLoadingEnabled = true executes normally.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testOnlineJob() throws Exception {
    final int nbTasks = 1;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, MyTask.class);
    job.getSLA().setRemoteClassLoadingEnabled(true);
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    for (int i=0; i<nbTasks; i++) {
      final Task<?> task = results.get(i);
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
      assertNull(task.getThrowable());
    }
  }

  /**
   * A simple task.
   */
  public static class MyTask extends AbstractTask<String> {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void run() {
      setResult(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE);
    }
  }
}
