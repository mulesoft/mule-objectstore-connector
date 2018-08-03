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
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.ObjectStoreStory.REMOVE;

import org.mule.runtime.core.api.event.CoreEvent;

import org.junit.Test;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(OS_CONNECTOR)
@Story(REMOVE)
public class RemoveTestCase extends ParameterizedObjectStoreTestCase {

  public RemoveTestCase(String name) {
    super(name);
  }

  @Override
  protected String doGetConfigFile() {
    return "remove-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    getObjectStore().store(KEY, TEST_VALUE);
  }

  @Test
  @Description("Remove object of a given key")
  public void remove() throws Exception {
    CoreEvent event = flowRunner("remove").withVariable("key", KEY).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("OK"));
    assertThat(getObjectStore().contains(KEY), is(false));
  }

  @Test
  @Description("Removing object using an empty key throws INVALID_KEY error")
  public void removeWithEmptyKey() throws Exception {
    CoreEvent event = flowRunner("remove").withVariable("key", "").run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("INVALID_KEY"));
    assertThat(getObjectStore().contains(KEY), is(true));
  }

  @Test
  @Description("Removing object using a key which doesn't exists throws KEY_NOT_FOUND error")
  public void removeUnexisting() throws Exception {
    CoreEvent event = flowRunner("removeUnexisting").run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("KEY_NOT_FOUND"));
  }
}
