/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.internal;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.store.ObjectStoreManager;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * A {@link ConnectionProvider} which returns the runtime's default {@link ObjectStoreManager}
 *
 * @since 1.0
 */
public class MuleObjectStoreManagerProvider implements ConnectionProvider<ObjectStoreManager> {

  @Inject
  @Named(OBJECT_STORE_MANAGER)
  private ObjectStoreManager objectStoreManager;

  @Override
  public ObjectStoreManager connect() throws ConnectionException {
    return objectStoreManager;
  }

  @Override
  public void disconnect(ObjectStoreManager connection) {}

  @Override
  public ConnectionValidationResult validate(ObjectStoreManager connection) {
    return success();
  }
}
