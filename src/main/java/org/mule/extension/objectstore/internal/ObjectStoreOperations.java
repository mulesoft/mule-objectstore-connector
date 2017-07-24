/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.internal;

import static org.mule.extension.objectstore.internal.error.ObjectStoreErrors.INVALID_KEY;
import static org.mule.extension.objectstore.internal.error.ObjectStoreErrors.KEY_ALREADY_EXISTS;
import static org.mule.extension.objectstore.internal.error.ObjectStoreErrors.KEY_NOT_FOUND;
import static org.mule.extension.objectstore.internal.error.ObjectStoreErrors.NULL_VALUE;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.extension.api.error.MuleErrors.ANY;
import org.mule.extension.objectstore.internal.error.RemoveErrorTypeProvider;
import org.mule.extension.objectstore.internal.error.RetrieveErrorTypeProvider;
import org.mule.extension.objectstore.internal.error.StoreErrorTypeProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

/**
 * Operations for the ObjectStore connector
 *
 * @since 1.0
 */
public class ObjectStoreOperations implements Startable {

  @Inject
  private LockFactory lockFactory;

  @Inject
  private ObjectStoreManager objectStoreManager;

  private ObjectStore objectStore;
  private final Lock objectStoreLock = new ReentrantLock();

  @Override
  public void start() throws MuleException {
    objectStore = objectStoreManager.getObjectStore(BASE_PERSISTENT_OBJECT_STORE_KEY);
  }

  /**
   * Stores the given {@code value} using the given {@code key}.
   * <p>
   * This operation can be used either for storing new values or updating existing ones, depending on the value
   * of the {@code failIfPresent}. When that parameter is set to {@code false} (default value) then any pre existing
   * value associated to that key will be overwritten. If the parameter is set to {@code true}, then a {@code OS:KEY_ALREADY_EXISTS}
   * error will be thrown instead.
   * <p>
   * Another important consideration is regarding {@code null} values. It is not allowed to store a null value. However,
   * a common use case is to obtain a value (most likely by evaluating a expression or transformation), testing the value for
   * {@code not null}, storing it if present and doing nothing otherwise. The {@code failOnNullValue} parameter simplifies this
   * use case. On its default value of {@code true}, a {@code OS:NULL_VALUE} error is thrown if a {@code null} value is supplied.
   * However, when set to {@code false}, a {@code null} value will cause this operation to do nothing, no error will be raised
   * but no value will be altered either.
   * <p>
   * Finally, this operation is synchronized on the key level. No other operation will be able to access the same key
   * on the same object store while this operation is running. If the runtime is running on cluster mode, this synchronism is
   * also guaranteed across nodes.
   *
   * @param key             the key of the {@code value} to be stored
   * @param value           the value to be stored. Should not be {@code null} if {@code failOnNullValue} is set to {@code true}
   * @param failIfPresent   Whether to fail or update the pre existing value if the {@code key} already exists on the store
   * @param failOnNullValue Whether to fail or skip the operation if the {@code value} is {@code null}
   */
  @Throws(StoreErrorTypeProvider.class)
  @Summary("Stores the given value using the given key")
  public void store(String key,
                    @Content TypedValue<Serializable> value,
                    @Optional(defaultValue = "false") boolean failIfPresent,
                    @Optional(defaultValue = "true") boolean failOnNullValue) {

    if (!validateValue(value, failOnNullValue)) {
      return;
    }
    validateKey(key);

    onLocked(key, () -> {
      if (objectStore.contains(key)) {
        if (failIfPresent) {
          throw new ModuleException(KEY_ALREADY_EXISTS,
                                    new IllegalArgumentException("ObjectStore already contains an object for key '" + key + "'"));
        } else {
          objectStore.remove(key);
        }
      }

      objectStore.store(key, value);
      return null;
    });
  }

  /**
   * Retrieves the value stored for the given {@code key}.
   * <p>
   * If no value exists for the {@code key}, behaviour will depend on the {@code defaultValue} parameter.
   * If the parameter was not provided or resolved to a {@code null} value, then a {@code OS:KEY_NOT_FOUND} error
   * will be thrown. Otherwise, the defaultValue will be returned <b>BUT</b> keep in mind that such value
   * <b>WILL NOT</b> be stored.
   * <p>
   * Finally, this operation is synchronized on the key level. No other operation will be able to access the same key
   * on the same object store while this operation is running. If the runtime is running on cluster mode, this synchronism is
   * also guaranteed across nodes.
   *
   * @param key          the key of the {@code value} to be retrieved
   * @param defaultValue value to be returned if the {@code key} doesn't exist in the store
   * @return The stored value or the {@code defaultValue}
   */
  @Throws(RetrieveErrorTypeProvider.class)
  @Summary("Retrieves the value stored for the given key")
  public Result<Serializable, Void> retrieve(String key, @Content @Optional TypedValue<Serializable> defaultValue) {

    validateKey(key);
    Object value = onLocked(key, () -> {
      if (objectStore.contains(key)) {
        return objectStore.retrieve(key);
      } else if (defaultValue != null && defaultValue.getValue() != null) {
        return defaultValue;
      } else {
        throw new ModuleException(KEY_NOT_FOUND, new IllegalArgumentException(
                                                                              "ObjectStore doesn't contain any value for key '"
                                                                                  + key + "' and default "
                                                                                  + "value was not provided or resolved to a null value."));
      }
    });

    TypedValue<Serializable> typedValue = value instanceof TypedValue
        ? (TypedValue<Serializable>) value
        : new TypedValue<>((Serializable) value, DataType.fromType(value.getClass()));

    return Result.<Serializable, Void>builder()
        .output(typedValue.getValue())
        .mediaType(typedValue.getDataType().getMediaType())
        .build();

  }

  /**
   * Removes the value associated to the given {@code key}. If no value exist for the key, then a {@code OS:KEY_NOT_FOUND}
   * error will be thrown.
   * <p>
   * This operation is synchronized on the key level. No other operation will be able to access the same key
   * on the same object store while this operation is running. If the runtime is running on cluster mode, this synchronism is
   * also guaranteed across nodes.
   *
   * @param key the key of the object to be removed
   */
  @Throws(RemoveErrorTypeProvider.class)
  @Summary("Removes the value associated to the given key")
  public void remove(String key) {
    validateKey(key);
    onLocked(key, () -> {
      if (!objectStore.contains(key)) {
        throw new ModuleException(KEY_NOT_FOUND, new IllegalArgumentException(
                                                                              "ObjectStore doesn't contain any value for key '"
                                                                                  + key + "'"));
      }

      objectStore.remove(key);
      return null;
    });
  }

  /**
   * Checks if there is any value associated to the given {@code key}. If no value exist for the key, then {@code false} will be returned.
   * <p>
   * This operation is synchronized on the key level. No other operation will be able to access the same key
   * on the same object store while this operation is running. If the runtime is running on cluster mode, this synchronism is
   * also guaranteed across nodes.
   *
   * @param key the key of the object to be removed
   */
  @Summary("Returns whether the key is present or not")
  public boolean contains(String key) {
    validateKey(key);
    Reference<Boolean> result = new Reference<>();
    onLocked(key, () -> result.set(objectStore.contains(key)));
    return result.get();
  }

  /**
   * Removes all the contents in the store.
   */
  public void clear() {
    onLock(objectStoreLock, () -> {
      objectStore.clear();
      return null;
    });
  }

  private boolean validateValue(TypedValue<Serializable> value, boolean failOnNullValue) {
    if (value == null || value.getValue() == null) {
      if (failOnNullValue) {
        throw new ModuleException(NULL_VALUE, new IllegalArgumentException(
                                                                           "A null value was provided. Please provided a non-null value or set the 'failOnNullValue' parameter to 'false'"));
      } else {
        return false;
      }
    }

    return true;
  }

  private void validateKey(String key) {
    if (key == null || key.trim().length() == 0) {
      throw new ModuleException(INVALID_KEY, new IllegalArgumentException("Key cannot be null nor empty"));
    }
  }

  private Serializable onLocked(String key, ObjectStoreTask task) {
    return onLock(getKeyLock(key), () -> onLock(objectStoreLock, task));
  }

  private Serializable execute(ObjectStoreTask task) {
    try {
      return task.run();
    } catch (ObjectStoreException e) {
      throw new ModuleException(ANY, e);
    }
  }

  private Lock getKeyLock(String key) {
    return lockFactory.createLock("_objectStoreConnector_" + BASE_PERSISTENT_OBJECT_STORE_KEY + "_" + key);
  }

  private Serializable onLock(Lock lock, ObjectStoreTask task) {
    lock.lock();
    try {
      return execute(task);
    } finally {
      lock.unlock();
    }
  }

  @FunctionalInterface
  private interface ObjectStoreTask {

    Serializable run() throws ObjectStoreException;
  }
}
