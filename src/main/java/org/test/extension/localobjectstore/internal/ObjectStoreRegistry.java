/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.test.extension.localobjectstore.internal;

import static java.util.Objects.hash;
import org.mule.runtime.api.store.ObjectStore;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of all the {@link ObjectStore stores} that were defined through this connector
 *
 * @since 1.0
 */
public class ObjectStoreRegistry {

  private Map<ObjectStoreKey, ObjectStore<Serializable>> stores = new ConcurrentHashMap<>();

  public void register(String name, String context, ObjectStore<Serializable> store) {
    stores.put(new ObjectStoreKey(context, name), store);
  }

  public void unregister(String name, String context) {
    stores.remove(new ObjectStoreKey(context, name));
  }

  public ObjectStore<Serializable> get(String name, String context) {
    return stores.get(new ObjectStoreKey(context, name));
  }

  private final static class ObjectStoreKey {

    private final String context;
    private final String name;

    ObjectStoreKey(String context, String name) {
      this.context = context;
      this.name = name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ObjectStoreKey that = (ObjectStoreKey) o;
      return Objects.equals(context, that.context) &&
          Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return hash(context, name);
    }
  }
}
