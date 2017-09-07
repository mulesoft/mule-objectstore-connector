/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.internal;

import static java.lang.String.format;
import static org.mule.extension.objectstore.internal.error.ObjectStoreErrors.INVALID_KEY;
import static org.mule.extension.objectstore.internal.error.ObjectStoreErrors.KEY_ALREADY_EXISTS;
import static org.mule.extension.objectstore.internal.error.ObjectStoreErrors.KEY_NOT_FOUND;
import static org.mule.extension.objectstore.internal.error.ObjectStoreErrors.NULL_VALUE;
import static org.mule.extension.objectstore.internal.error.ObjectStoreErrors.STORE_NOT_AVAILABLE;
import static org.mule.extension.objectstore.internal.error.ObjectStoreErrors.STORE_NOT_FOUND;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.extension.api.error.MuleErrors.ANY;
import org.mule.extension.objectstore.internal.error.AvailabilityErrorTypeProvider;
import org.mule.extension.objectstore.internal.error.ContainsErrorTypeProvider;
import org.mule.extension.objectstore.internal.error.RemoveErrorTypeProvider;
import org.mule.extension.objectstore.internal.error.RetrieveErrorTypeProvider;
import org.mule.extension.objectstore.internal.error.StoreErrorTypeProvider;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreNotAvailableException;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.param.reference.ObjectStoreReference;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Operations for the ObjectStore connector
 *
 * @since 1.0
 */
public class ObjectStoreOperations {

  @Inject
  private LockFactory lockFactory;

  @Inject
  private ObjectStoreRegistry registry;

  @Inject
  @Named(OBJECT_STORE_MANAGER)
  private ObjectStoreManager runtimeObjectStoreManager;

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
   * on the same object store while this operation is running. If the runtime is running on cluster mode, this synchronization is
   * also guaranteed across nodes.
   *
   * @param key             the key of the {@code value} to be stored
   * @param value           the value to be stored. Should not be {@code null} if {@code failOnNullValue} is set to {@code true}
   * @param failIfPresent   Whether to fail or update the pre existing value if the {@code key} already exists on the store
   * @param failOnNullValue Whether to fail or skip the operation if the {@code value} is {@code null}
   * @param objectStore     A reference to the ObjectStore to be used. If not defined, the runtime's default partition will be used
   */
  @Throws(StoreErrorTypeProvider.class)
  @Summary("Stores the given value using the given key")
  public void store(String key,
                    @Content TypedValue<Serializable> value,
                    @Optional(defaultValue = "false") boolean failIfPresent,
                    @Optional(defaultValue = "true") boolean failOnNullValue,
                    @ObjectStoreReference @Optional String objectStore) {

    if (!validateValue(value, failOnNullValue)) {
      return;
    }

    validateKey(key);

    withLockedKey(objectStore, key, os -> {
      if (os.contains(key)) {
        if (failIfPresent) {
          throw new ModuleException(KEY_ALREADY_EXISTS, new ObjectAlreadyExistsException(
                                                                                         createStaticMessage("ObjectStore already contains an object for key '"
                                                                                             + key + "'")));
        } else {
          os.remove(key);
        }
      }

      os.store(key, value);
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
   * on the same object store while this operation is running. If the runtime is running on cluster mode, this synchronization is
   * also guaranteed across nodes.
   *
   * @param key          the key of the {@code value} to be retrieved
   * @param defaultValue value to be returned if the {@code key} doesn't exist in the store
   * @param objectStore  A reference to the ObjectStore to be used. If not defined, the runtime's default partition will be used
   * @return The stored value or the {@code defaultValue}
   */
  @Throws(RetrieveErrorTypeProvider.class)
  @Summary("Retrieves the value stored for the given key")
  public Result<Serializable, Void> retrieve(String key,
                                             @Content @Optional TypedValue<Serializable> defaultValue,
                                             @ObjectStoreReference @Optional String objectStore) {

    validateKey(key);
    Object value = withLockedKey(objectStore, key, os -> {
      if (os.contains(key)) {
        return os.retrieve(key);
      } else if (defaultValue != null && defaultValue.getValue() != null) {
        return defaultValue;
      } else {
        throw new ModuleException(KEY_NOT_FOUND, new ObjectDoesNotExistException(createStaticMessage(format(
                                                                                                            "ObjectStore '%s' doesn't contain any value for key '%s' and default value was not provided or "
                                                                                                                + "resolved to a null value.",
                                                                                                            objectStore, key))));
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
   * on the same object store while this operation is running. If the runtime is running on cluster mode, this synchronization is
   * also guaranteed across nodes.
   *
   * @param key         the key of the object to be removed
   * @param objectStore A reference to the ObjectStore to be used. If not defined, the runtime's default partition will be used
   */
  @Throws(RemoveErrorTypeProvider.class)
  @Summary("Removes the value associated to the given key")
  public void remove(String key, @ObjectStoreReference @Optional String objectStore) {
    validateKey(key);
    withLockedKey(objectStore, key, os -> {
      if (!os.contains(key)) {
        throw new ModuleException(KEY_NOT_FOUND, new ObjectDoesNotExistException(createStaticMessage(format(
                                                                                                            "ObjectStore doesn't contain any value for key '%s'",
                                                                                                            key))));
      }

      os.remove(key);
      return null;
    });
  }

  /**
   * Checks if there is any value associated to the given {@code key}. If no value exist for the key, then {@code false} will be returned.
   * <p>
   * This operation is synchronized on the key level. No other operation will be able to access the same key
   * on the same object store while this operation is running. If the runtime is running on cluster mode, this synchronization is
   * also guaranteed across nodes.
   *
   * @param key         the key of the object to be removed
   * @param objectStore A reference to the ObjectStore to be used. If not defined, the runtime's default partition will be used
   */
  @Summary("Returns whether the key is present or not")
  @Throws(ContainsErrorTypeProvider.class)
  public boolean contains(String key, @ObjectStoreReference @Optional String objectStore) {
    validateKey(key);
    return withLockedKey(objectStore, key, os -> os.contains(key));
  }

  /**
   * Removes all the contents in the store.
   *
   * @param objectStore A reference to the ObjectStore to be used. If not defined, the runtime's default partition will be used
   */
  @Throws(AvailabilityErrorTypeProvider.class)
  public void clear(@ObjectStoreReference @Optional String objectStore) {
    withLockedStore(objectStore, os -> {
      os.clear();
      return null;
    });
  }

  /**
   * Returns a List containing all keys that the {@code objectStore} currently holds values for.
   *
   * @param objectStore A reference to the ObjectStore to be used. If not defined, the runtime's default partition will be used
   * @return A List with all the keys or an empty one if the object store is empty
   */
  @Throws(AvailabilityErrorTypeProvider.class)
  public List<String> retrieveAllKeys(@ObjectStoreReference @Optional String objectStore) {
    return withLockedStore(objectStore, os -> os.allKeys());
  }

  /**
   * Retrieves all the key value pairs in the object store
   *
   * @param objectStore A reference to the ObjectStore to be used. If not defined, the runtime's default partition will be used
   * @return All the key value pairs or an empty Map if no values are present
   */
  @Throws(AvailabilityErrorTypeProvider.class)
  public Map<String, Serializable> retrieveAll(@ObjectStoreReference @Optional String objectStore) {
    return withLockedStore(objectStore, os -> {
      Map<String, Serializable> all = os.retrieveAll();
      all.entrySet().forEach(entry -> {
        Object value = entry.getValue();
        if (value instanceof TypedValue) {
          entry.setValue((Serializable) ((TypedValue) value).getValue());
        }
      });

      return all;
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

  private <T> T withLockedKey(String objectStoreName, String key, ObjectStoreTask<T> task) {
    ObjectStore<Serializable> objectStore = getObjectStore(objectStoreName);
    Lock lock = getKeyLock(key, objectStoreName);
    lock.lock();
    try {
      return task.run(objectStore);
    } catch (ObjectAlreadyExistsException e) {
      throw new ModuleException(createStaticMessage(format(
                                                           "Key '%s' is already present on object store '%s'", key,
                                                           objectStoreName)),
                                KEY_ALREADY_EXISTS, e);
    } catch (ObjectStoreNotAvailableException e) {
      throw new ModuleException(createStaticMessage(format(
                                                           "ObjectStore '%s' is not available at the moment", objectStoreName)),
                                STORE_NOT_AVAILABLE, e);
    } catch (ObjectDoesNotExistException e) {
      throw new ModuleException(createStaticMessage(format(
                                                           "Key '%s' does not exists on object store '%s'", key,
                                                           objectStoreName)),
                                KEY_NOT_FOUND, e);
    } catch (ObjectStoreException e) {
      throw new ModuleException(createStaticMessage(format(
                                                           "Found error trying to access ObjectStore '%s'", objectStoreName)),
                                ANY, e);
    } finally {
      lock.unlock();
    }
  }

  private <T> T withLockedStore(String objectStoreName, ObjectStoreTask<T> task) {
    ObjectStore<Serializable> objectStore = getObjectStore(objectStoreName);
    Lock lock = getStoreLock(objectStoreName);
    lock.lock();
    try {
      return task.run(objectStore);
    } catch (ObjectStoreNotAvailableException e) {
      throw new ModuleException(createStaticMessage(format(
                                                           "ObjectStore '%s' is not available at the moment", objectStoreName)),
                                STORE_NOT_AVAILABLE, e);
    } catch (ObjectStoreException e) {
      throw new ModuleException(createStaticMessage(format(
                                                           "Found error trying to access ObjectStore '%s'", objectStoreName)),
                                ANY, e);
    } finally {
      lock.unlock();
    }
  }

  private Lock getKeyLock(String key, String objectStoreName) {
    return lockFactory.createLock("_objectStoreConnector_" + objectStoreName + "_" + key);
  }

  private Lock getStoreLock(String objectStoreName) {
    return lockFactory.createLock("_objectStoreConnector_" + objectStoreName);
  }

  private ObjectStore<Serializable> getObjectStore(String objectStoreName) {
    if (objectStoreName == null) {
      return runtimeObjectStoreManager.getDefaultPartition();
    }

    ObjectStore<Serializable> objectStore = registry.get(objectStoreName);
    if (objectStore == null) {
      throw new ModuleException(createStaticMessage(format(
                                                           "ObjectStore '%s' was not defined. Is there a matching <os:object-store>?",
                                                           objectStoreName)),
                                STORE_NOT_FOUND);
    }

    return objectStore;
  }

  @FunctionalInterface
  private interface ObjectStoreTask<T> {

    T run(ObjectStore<Serializable> objectStore) throws ObjectStoreException;
  }
}
