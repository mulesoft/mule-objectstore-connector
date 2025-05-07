/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.internal.lock;

import org.mule.runtime.api.lock.LockFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Ideally we should be able to consume the LOCAL_OBJECT_LOCK_FACTORY however, that one also has the LockProvider overridden with
// the one for clustering mode.
// This implementation does not perform reclamation of unused entries. It is not suitable for applications that can potentially
// use an unbounded number of distinct lock IDs.
public class LocalLockFactory implements LockFactory {

  private final Map<String, Lock> locks = new ConcurrentHashMap<>();

  @Override
  public Lock createLock(String lockId) {
    return locks.computeIfAbsent(lockId, key -> new ReentrantLock(true));
  }
}
