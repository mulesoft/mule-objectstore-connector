/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.test.extension.localobjectstore.internal.error;

import static org.test.extension.localobjectstore.internal.error.ObjectStoreErrors.INVALID_KEY;
import static org.test.extension.localobjectstore.internal.error.ObjectStoreErrors.KEY_ALREADY_EXISTS;
import static org.test.extension.localobjectstore.internal.error.ObjectStoreErrors.NULL_VALUE;
import static org.test.extension.localobjectstore.internal.error.ObjectStoreErrors.STORE_NOT_AVAILABLE;
import static org.mule.runtime.extension.api.error.MuleErrors.ANY;

import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.HashSet;
import java.util.Set;

/**
 * Errors for the store operation
 *
 * @since 1.0
 */
public class StoreErrorTypeProvider implements ErrorTypeProvider {

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    Set<ErrorTypeDefinition> errors = new HashSet<>();

    errors.add(INVALID_KEY);
    errors.add(NULL_VALUE);
    errors.add(KEY_ALREADY_EXISTS);
    errors.add(STORE_NOT_AVAILABLE);
    errors.add(ANY);

    return errors;
  }
}
