/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.test.extension.localobjectstore.internal.lock;

import org.mule.runtime.api.lock.LockFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// This implementation has no reclamation of unused entries, it is not suitable for usage in applications that have a high
// cardinality of lock IDs.
public class LocalLockFactory implements LockFactory {

  private final Map<String, Lock> locks = new ConcurrentHashMap<>();

  @Override
  public synchronized Lock createLock(String lockId) {
    return locks.computeIfAbsent(lockId, key -> new ReentrantLock());
  }
}
