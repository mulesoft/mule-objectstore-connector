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
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;

import java.io.Serializable;

import org.junit.Test;

public class StoreTestCase extends AbstractObjectStoreTestCase {

  @Override
  protected String getConfigFile() {
    return "store-config.xml";
  }

  @Test
  public void store() throws Exception {
    Event event = flowRunner("store")
        .withPayload(TEST_VALUE)
        .withVariable("key", KEY)
        .run();

    assertThat(event.getMessage().getPayload().getValue(), equalTo("OK"));
    retrieveAndCompare(KEY, TEST_VALUE);
  }

  @Test
  public void storeWithCustomMediaType() throws Exception {
    flowRunner("store")
        .withPayload(TEST_VALUE)
        .withMediaType(APPLICATION_JSON)
        .withVariable("key", KEY)
        .run();

    TypedValue<Serializable> storedValue = (TypedValue<Serializable>) objectStore.retrieve(KEY);
    assertThat(storedValue.getValue(), equalTo(TEST_VALUE));
    assertThat(storedValue.getDataType().getMediaType(), is(APPLICATION_JSON));
  }

  @Test
  public void storeWithEmptyKey() throws Exception {
    Event event = flowRunner("store")
        .withVariable("key", "")
        .withPayload(TEST_VALUE)
        .run();

    assertThat(event.getMessage().getPayload().getValue(), equalTo("INVALID_KEY"));
    assertThat(objectStore.contains(""), is(false));
  }

  @Test
  public void storeWithNullKey() throws Exception {
    Event event = flowRunner("store")
        .withVariable("key", null)
        .withPayload(TEST_VALUE)
        .run();

    assertThat(event.getMessage().getPayload().getValue(), equalTo("INVALID_KEY"));
    assertThat(objectStore.contains(""), is(false));
  }

  @Test
  public void overwriteValue() throws Exception {
    store();

    final String overwrittenValue = "Some other value";
    Event event = flowRunner("store")
        .withPayload(overwrittenValue)
        .withVariable("key", KEY)
        .run();

    assertThat(event.getMessage().getPayload().getValue(), equalTo("OK"));
    retrieveAndCompare(KEY, overwrittenValue);
  }

  @Test
  public void failIfKeyAlreadyPresent() throws Exception {
    store();

    Event event = flowRunner("idempotentStore")
        .withPayload("Some other value")
        .withVariable("key", KEY)
        .run();

    assertThat(event.getMessage().getPayload().getValue(), equalTo("KEY_ALREADY_EXISTS"));
    retrieveAndCompare(KEY, TEST_VALUE);
  }

  @Test
  public void skipNullValue() throws Exception {
    flowRunner("storeNullValue").withVariable("failOnNullValue", false).run();
    assertThat(objectStore.contains(KEY), is(false));
  }

  @Test
  public void failOnNullValue() throws Exception {
    Event event = flowRunner("storeNullValue").withVariable("failOnNullValue", true).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("NULL_VALUE"));
    assertThat(objectStore.contains(KEY), is(false));
  }
}
