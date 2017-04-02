/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.caching;

import java.util.*;

import org.slf4j.*;

/**
 * Cache implementation backed by a {@link Set} wihh synchronized access.
 * @param <E> the type of the cache elements.
 * @author Laurent Cohen
 */
public class JPPFSimpleSetCache<E> implements JPPFCollectionCache<E> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFSimpleSetCache.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The backing set for this cache.
   */
  private final Set<E> set = new HashSet<>();
  /**
   * Name assigned to this cache, for debugging and tracing purposes.
   */
  private final String name;

  /**
   * Initialize with the default name {@code getClass().getSimpleName()}.
   */
  public JPPFSimpleSetCache() {
    name = getClass().getSimpleName();
  }

  /**
   * Initialize with the specified name.
   * @param name the name assigned to this cache.
   */
  public JPPFSimpleSetCache(final String name) {
    this.name = name;
  }

  @Override
  public void add(final E element) {
    synchronized (set) {
      if (debugEnabled) log.debug("{}: adding {}", name, element);
      set.add(element);
    }
  }

  @Override
  public boolean has(final E element) {
    synchronized (set) {
      return set.contains(element);
    }
  }

  @Override
  public E remove(final E element) {
    boolean b;
    synchronized (set) {
      if (debugEnabled) log.debug("{}: removing {}", name, element);
      b = set.remove(element);
    }
    return b ? element : null;
  }

  @Override
  public void clear() {
    synchronized (set) {
      if (debugEnabled) log.debug("{}: clearing all elements", name);
      set.clear();
    }
  }
}
