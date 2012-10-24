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

package org.jppf.classloader;

import java.util.*;
import java.util.concurrent.Future;

/**
 * Instances of this class are intended for grouping multiple class loading requests together.
 * @author Laurent Cohen
 * @author Martin JANDA
 * @exclude
 */
public class CompositeResourceWrapper extends JPPFResourceWrapper
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Key for the list of requests.
   */
  private static final String RESOURCES_KEY = "resources";
  /**
   * Mapping of futures to corresponding resource requests.
   */
  private final transient Map<JPPFResourceWrapper, Future<JPPFResourceWrapper>> futureMap = new HashMap<JPPFResourceWrapper, Future<JPPFResourceWrapper>>();

  /**
   *
   */
  public CompositeResourceWrapper()
  {
  }

  @SuppressWarnings("unchecked")
  @Override
  public JPPFResourceWrapper[] getResources()
  {
    synchronized (getMonitor()) {
      Set<JPPFResourceWrapper> resources = (Set<JPPFResourceWrapper>) getData(RESOURCES_KEY);
      if(resources == null || resources.isEmpty()) return EMPTY_RESOURCE_WRAPPER_ARRAY;
      else return resources.toArray(new JPPFResourceWrapper[resources.size()]);
    }
  }

  /**
   * Add or replace request to this composite request.
   * @param resource the request to add or replace.
   */
  @SuppressWarnings("unchecked")
  public void addOrReplaceResource(final JPPFResourceWrapper resource) {
    synchronized (getMonitor()) {
      Set<JPPFResourceWrapper> resources = (Set<JPPFResourceWrapper>) getData(RESOURCES_KEY);
      if(resources == null) {
        resources = new HashSet<JPPFResourceWrapper>();
        setData(RESOURCES_KEY, resources);
      } else resources.remove(resource);
      resources.add(resource);
    }
  }

  /**
   * Add a request to this composite request.
   * @param resource the request to add.
   * @return a future for getting the response at a later time.
   */
  public Future<JPPFResourceWrapper> addResource(final JPPFResourceWrapper resource)
  {
    Future<JPPFResourceWrapper> f = futureMap.get(resource);
    if (f == null)
    {
      addOrReplaceResource(resource);
      f = new ResourceFuture<JPPFResourceWrapper>();
      futureMap.put(resource, f);
    }
    return f;
  }

  /**
   * Get the mapping of futures to corresponding resource requests.
   * @return a map of resource definitions to their corresponding future.
   */
  public Map<JPPFResourceWrapper, Future<JPPFResourceWrapper>> getFutureMap()
  {
    return futureMap;
  }
}
