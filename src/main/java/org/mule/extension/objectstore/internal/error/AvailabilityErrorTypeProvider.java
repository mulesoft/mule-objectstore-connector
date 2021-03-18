/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.internal.error;

import static org.mule.extension.objectstore.internal.error.ObjectStoreErrors.STORE_NOT_AVAILABLE;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;

import java.util.HashSet;
import java.util.Set;

/**
 * Errors for the clear operation
 *
 * @since 1.0
 */
public class AvailabilityErrorTypeProvider implements ErrorTypeProvider {

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    Set<ErrorTypeDefinition> errors = new HashSet<>();
    errors.add(STORE_NOT_AVAILABLE);
    errors.add(MuleErrors.ANY);

    return errors;
  }
}
