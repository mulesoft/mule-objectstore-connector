/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.Event;

import java.io.Serializable;

import org.junit.Test;

public class RetrieveTestCase extends AbstractObjectStoreTestCase {

  @Override
  protected String getConfigFile() {
    return "retrieve-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    objectStore.store(KEY, TEST_VALUE);
  }

  @Test
  public void retrieve() throws Exception {
    assertThat(doRetrieve(KEY), equalTo(TEST_VALUE));
  }

  @Test
  public void retrieveEmptyKey() throws Exception {
    assertThat(doRetrieve(""), equalTo("INVALID_KEY"));
  }

  @Test
  public void retrieveNullKey() throws Exception {
    assertThat(doRetrieve(null), equalTo("INVALID_KEY"));
  }

  @Test
  public void retrieveUnexisting() throws Exception {
    Event event = flowRunner("retrieveUnexisting").run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("KEY_NOT_FOUND"));
  }

  @Test
  public void retrieveUnexistingWithDefault() throws Exception {
    Event event = flowRunner("retrieveWithDefault").withVariable("key", "missaNotThereJarJar").run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("default"));
  }

  @Test
  public void retrieveExistingWithDefault() throws Exception {
    Event event = flowRunner("retrieveWithDefault").withVariable("key", KEY).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo(TEST_VALUE));
  }

  private Serializable doRetrieve(String key) throws Exception {
    return (Serializable) flowRunner("retrieve").withVariable("key", key).run().getMessage().getPayload().getValue();
  }
}
