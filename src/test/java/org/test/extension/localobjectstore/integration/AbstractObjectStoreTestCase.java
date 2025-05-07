/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.test.extension.localobjectstore.integration;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.store.ObjectStoreManager;

import java.io.Serializable;

abstract class AbstractObjectStoreTestCase extends MuleArtifactFunctionalTestCase {

  protected static final String KEY = "myKey";
  protected static final Serializable TEST_VALUE = "I've been stored";
  protected static final String PERSISTENT_STORE_DEFINITION_CONFIG_FILE_NAME = "store-persistent-definition-config.xml";
  protected static final String TRANSIENT_STORE_DEFINITION_CONFIG_FILE_NAME = "store-transient-definition-config.xml";
  protected static final String IMPLICIT_STORE_DEFINITION_CONFIG_FILE_NAME = "store-implicit-definition-config.xml";

  protected ObjectStoreManager objectStoreManager;

  @Override
  protected void doSetUp() throws Exception {
    objectStoreManager = muleContext.getObjectStoreManager();
  }
}
