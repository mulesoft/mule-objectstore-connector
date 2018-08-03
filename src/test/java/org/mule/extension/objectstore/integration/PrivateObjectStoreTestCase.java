/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.integration;



import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsNull.nullValue;

import org.mule.runtime.api.store.ObjectStore;

import org.junit.Test;


public class PrivateObjectStoreTestCase extends AbstractObjectStoreTestCase {

  private static final String GLOBAL_OBJECT_STORE_NAME = "globalObjectStore";
  private static final String GLOBAL_OBJECT_STORE_FLOW_NAME = "globalFlow";
  private static final String PRIVATE_OBJECT_STORE_NAME = "privateObjectStore";
  private static final String PRIVATE_OBJECT_STORE_FLOW_NAME = "privateFlow";
  private static final String IMPLICIT_OBJECT_STORE_FLOW_NAME = "implicitFlow";
  private static final String IMPLICIT_OBJECT_STORE_NAME = "implicitObjectStore";


  @Override
  public String getConfigFile() {
    return "private-config.xml";
  }


  @Test
  public void checkGlobalObjectStore() throws Exception {
    String payload = "globalPayload";
    assertObjectStore(GLOBAL_OBJECT_STORE_FLOW_NAME, GLOBAL_OBJECT_STORE_NAME, payload);
  }

  @Test
  public void checkPrivateObjectStore() throws Exception {
    String payload = "privatePayload";
    assertObjectStore(PRIVATE_OBJECT_STORE_FLOW_NAME, PRIVATE_OBJECT_STORE_NAME, payload);
  }

  @Test
  public void checkImplicitObjectStore() throws Exception {
    String payload = "implicitPayload";
    assertObjectStore(IMPLICIT_OBJECT_STORE_FLOW_NAME, IMPLICIT_OBJECT_STORE_NAME, payload);
  }

  private void assertObjectStore(String flowName, String objectStoreName, String payload) throws Exception {
    flowRunner(flowName).withPayload(payload).run();
    ObjectStore objectStore = objectStoreManager.getObjectStore(objectStoreName);
    assertThat(objectStore, is(not(nullValue())));
    assertThat(objectStore.allKeys().size(), is(equalTo(1)));
    assertThat(objectStore.contains(payload), is(true));
    assertThat(objectStore.retrieve(payload), is(equalTo(payload)));
  }

}
