/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.internal;

import org.mule.runtime.api.store.ObjectStore;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of all the {@link ObjectStore stores} that were defined through this connector
 *
 * @since 1.0
 */
public class ObjectStoreRegistry {

  private Map<String, ObjectStore<Serializable>> stores = new ConcurrentHashMap<>();

  public void register(String name, ObjectStore<Serializable> store) {
    stores.put(name, store);
  }

  public void unregister(String name) {
    stores.remove(name);
  }

  public ObjectStore<Serializable> get(String name) {
    return stores.get(name);
  }
}
