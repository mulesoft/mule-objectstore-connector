/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.test.extension.localobjectstore.internal.error;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

/**
 * Set of possible errors for the object store connector
 *
 * @since 1.0
 */
public enum ObjectStoreErrors implements ErrorTypeDefinition<ObjectStoreErrors> {

  /**
   * An object is trying to be stored, but the ObjectStore already has a value for that key
   */
  KEY_ALREADY_EXISTS,

  /**
   * The supplied key is invalid. Keys cannot be null nor blank
   */
  INVALID_KEY,

  /**
   * A null value was supplied to the ObjectStore. Null values are not supported
   */
  NULL_VALUE,

  /**
   * The ObjectStore needs to access a value, but the supplied key doesn't exist in that store
   */
  KEY_NOT_FOUND,

  /**
   * The operation refers to an {@link ObjectStore} which cannot be accessed at this time
   */
  STORE_NOT_AVAILABLE,

  ANY
}
