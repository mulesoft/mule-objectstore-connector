/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.integration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.OS_CONNECTOR;
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.ObjectStoreStory.STORE;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;

import java.io.Serializable;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(OS_CONNECTOR)
@Story(STORE)
public class StoreTestCase extends ParameterizedObjectStoreTestCase {

  public StoreTestCase(String name) {
    super(name);
  }

  @Override
  protected String doGetConfigFile() {
    return "store-config.xml";
  }

  @Test
  @Description("Store a value using new key")
  public void store() throws Exception {
    CoreEvent event = flowRunner("store")
        .withPayload(TEST_VALUE)
        .withVariable("key", KEY)
        .run();

    assertThat(event.getMessage().getPayload().getValue(), equalTo("OK"));
    retrieveAndCompare(KEY, TEST_VALUE);
  }

  @Test
  @Description("Store a value which has a custom mediaType and verify that the same mediaType is available when retrieved")
  public void storeWithCustomMediaType() throws Exception {
    flowRunner("store")
        .withPayload(TEST_VALUE)
        .withMediaType(APPLICATION_JSON)
        .withVariable("key", KEY)
        .run();

    TypedValue<Serializable> storedValue = (TypedValue<Serializable>) getObjectStore().retrieve(KEY);
    assertThat(storedValue.getValue(), equalTo(TEST_VALUE));
    assertThat(storedValue.getDataType().getMediaType(), is(APPLICATION_JSON));
  }

  @Test
  @Description("Verify that INVALID_KEY error is thrown when using an empty key")
  public void storeWithEmptyKey() throws Exception {
    CoreEvent event = flowRunner("store")
        .withVariable("key", "")
        .withPayload(TEST_VALUE)
        .run();

    assertThat(event.getMessage().getPayload().getValue(), equalTo("INVALID_KEY"));
  }

  @Test
  @Description("Overwrite a value for which a key already exists")
  public void overwriteValue() throws Exception {
    store();

    final String overwrittenValue = "Some other value";
    CoreEvent event = flowRunner("store")
        .withPayload(overwrittenValue)
        .withVariable("key", KEY)
        .run();

    assertThat(event.getMessage().getPayload().getValue(), equalTo("OK"));
    retrieveAndCompare(KEY, overwrittenValue);
  }

  @Test
  @Description("Fail when a storing a value which a key already exists")
  public void failIfKeyAlreadyPresent() throws Exception {
    store();

    CoreEvent event = flowRunner("idempotentStore")
        .withPayload("Some other value")
        .withVariable("key", KEY)
        .run();

    assertThat(event.getMessage().getPayload().getValue(), equalTo("KEY_ALREADY_EXISTS"));
    retrieveAndCompare(KEY, TEST_VALUE);
  }

  @Test
  @Description("Verify that operation skips when the value is null")
  public void skipNullValue() throws Exception {
    flowRunner("storeNullValue").withVariable("failOnNullValue", false).run();
    assertThat(getObjectStore().contains(KEY), is(false));
  }

  @Test
  @Description("Verify that operation fails when the value is null")
  public void failOnNullValue() throws Exception {
    CoreEvent event = flowRunner("storeNullValue").withVariable("failOnNullValue", true).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("NULL_VALUE"));
    assertThat(getObjectStore().contains(KEY), is(false));
  }

}
