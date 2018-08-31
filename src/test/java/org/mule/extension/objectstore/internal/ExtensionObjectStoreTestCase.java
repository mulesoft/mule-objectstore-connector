/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.internal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.extension.objectstore.api.ExtensionObjectStore;
import org.mule.extension.objectstore.api.TopLevelObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;

import static org.junit.rules.ExpectedException.none;

public class ExtensionObjectStoreTestCase {

  @Rule
  public ExpectedException expectedException = none();

  private ExtensionObjectStore extensionObjectStore;

  @Before
  public void setUp() {
    extensionObjectStore = new TopLevelObjectStore();
  }

  @Test
  public void storeWhenObjectStoreNotInitialized() throws ObjectStoreException {
    expectedException.expect(IllegalStateException.class);
    extensionObjectStore.retrieve("aKey");
  }
}
