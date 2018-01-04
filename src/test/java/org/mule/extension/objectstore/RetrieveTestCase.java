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
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.OS_CONNECTOR;
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.ObjectStoreStory.RETRIEVE;
import static org.mule.runtime.api.metadata.DataType.JSON_STRING;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;

import java.io.Serializable;

import org.junit.Test;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(OS_CONNECTOR)
@Story(RETRIEVE)
public class RetrieveTestCase extends ParameterizedObjectStoreTestCase {

  public static final String DEFAULT_VALUE = "default";
  private static final String NOT_EXISTING_KEY = "missaNotThereJarJar";

  public RetrieveTestCase(String name) {
    super(name);
  }

  @Override
  protected String doGetConfigFile() {
    return "retrieve-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    getObjectStore().store(KEY, TEST_VALUE);
  }

  @Test
  @Description("Retrieve a value")
  public void retrieve() throws Exception {
    assertThat(doRetrieve(KEY), equalTo(TEST_VALUE));
  }

  @Test
  @Description("Verify that retrieving a value with an empty key throws INVALID_KEY error")
  public void retrieveEmptyKey() throws Exception {
    assertThat(doRetrieve(""), equalTo("INVALID_KEY"));
  }

  @Test
  @Description("Verify that retrieving a value for which key doesn't exists throws KEY_NOT_FOUND error")
  public void retrieveUnexisting() throws Exception {
    CoreEvent event = flowRunner("retrieveUnexisting").run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("KEY_NOT_FOUND"));
  }

  @Test
  @Description("Verify that retrieving a value for which key doesn't exists returns the default value but such value is not stored")
  public void retrieveUnexistingWithDefault() throws Exception {
    CoreEvent event = flowRunner("retrieveWithDefault").withVariable("key", NOT_EXISTING_KEY).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo(DEFAULT_VALUE));
    assertThat(getObjectStore().contains(NOT_EXISTING_KEY), is(false));
  }

  @Test
  @Description("Verify that retrieving a value for which key doesn't exists returns the default value with a custom media type "
      + "but such value is not stored")
  public void retrieveDefaultValueMaintainingDataType() throws Exception {
    Message message = flowRunner("retrieveWithExpressionDefault")
        .withPayload("default")
        .withMediaType(APPLICATION_JSON)
        .withVariable("key", NOT_EXISTING_KEY)
        .run()
        .getMessage();

    assertThat(message.getPayload().getValue(), is(DEFAULT_VALUE));
    assertThat(message.getPayload().getDataType().getMediaType().matches(JSON_STRING.getMediaType()), is(true));
    assertThat(getObjectStore().contains(NOT_EXISTING_KEY), is(false));
  }

  @Test
  @Description("Retrieves a value which was stored with custom media type and such type is preserved")
  public void retrieveMaintainingDataType() throws Exception {
    getObjectStore().remove(KEY);
    getObjectStore().store(KEY, new TypedValue<>(TEST_VALUE, JSON_STRING));

    Message message = flowRunner("retrieve").withVariable("key", KEY).run().getMessage();
    assertThat(message.getPayload().getValue(), equalTo(TEST_VALUE));
    assertThat(message.getPayload().getDataType().getMediaType().matches(JSON_STRING.getMediaType()), is(true));
    assertThat(message.getPayload().getDataType().getType(), equalTo(String.class));
  }

  @Test
  @Description("Verify that retrieve returns the correct value, even if defaultValue was provided")
  public void retrieveExistingWithDefault() throws Exception {
    CoreEvent event = flowRunner("retrieveWithDefault").withVariable("key", KEY).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo(TEST_VALUE));
  }

  private Serializable doRetrieve(String key) throws Exception {
    return (Serializable) flowRunner("retrieve").withVariable("key", key).run().getMessage().getPayload().getValue();
  }
}
