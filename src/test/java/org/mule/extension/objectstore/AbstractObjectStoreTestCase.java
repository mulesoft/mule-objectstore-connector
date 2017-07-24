/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.store.ObjectStore;

import java.io.Serializable;

abstract class AbstractObjectStoreTestCase extends MuleArtifactFunctionalTestCase {

  protected static final String KEY = "myKey";
  protected static final Serializable TEST_VALUE = "I've been stored";
  protected ObjectStore objectStore;

  @Override
  protected void doSetUp() throws Exception {
    objectStore = muleContext.getObjectStoreManager().getObjectStore(BASE_PERSISTENT_OBJECT_STORE_KEY);
  }

  protected void retrieveAndCompare(String key, Serializable value) throws Exception {
    TypedValue<Serializable> typedValue = (TypedValue<Serializable>) objectStore.retrieve(key);
    assertThat(typedValue.getValue(), equalTo(value));
  }

}
