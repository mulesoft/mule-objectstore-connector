/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.metadata.DataType.JSON_STRING;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;

import java.io.Serializable;

import org.junit.Test;

public class RetrieveTestCase extends AbstractObjectStoreTestCase {

  public static final String DEFAULT_VALUE = "default";
  private static final String NOT_EXISTING_KEY = "missaNotThereJarJar";

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
    Event event = flowRunner("retrieveWithDefault").withVariable("key", NOT_EXISTING_KEY).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo(DEFAULT_VALUE));
    assertThat(objectStore.contains(NOT_EXISTING_KEY), is(false));
  }

  @Test
  public void retrieveDefaultValueMaintainingDataType() throws Exception {
    Message message = flowRunner("retrieveWithExpressionDefault")
        .withPayload("default")
        .withMediaType(APPLICATION_JSON)
        .withVariable("key", NOT_EXISTING_KEY)
        .run()
        .getMessage();

    assertThat(message.getPayload().getValue(), is(DEFAULT_VALUE));
    assertThat(message.getPayload().getDataType().getMediaType().matches(JSON_STRING.getMediaType()), is(true));
    assertThat(objectStore.contains(NOT_EXISTING_KEY), is(false));
  }

  @Test
  public void retrieveMaintainingDataType() throws Exception {
    objectStore.remove(KEY);
    objectStore.store(KEY, new TypedValue<>(TEST_VALUE, JSON_STRING));

    Message message = flowRunner("retrieve").withVariable("key", KEY).run().getMessage();
    assertThat(message.getPayload().getValue(), equalTo(TEST_VALUE));
    assertThat(message.getPayload().getDataType().getMediaType().matches(JSON_STRING.getMediaType()), is(true));
    assertThat(message.getPayload().getDataType().getType(), equalTo(String.class));
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
