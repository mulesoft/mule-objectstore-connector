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
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.ObjectStoreStory.REMOVE;
import org.mule.runtime.core.api.Event;

import org.junit.Test;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(OS_CONNECTOR)
@Story(REMOVE)
public class RemoveTestCase extends AbstractObjectStoreTestCase {

  @Override
  protected String getConfigFile() {
    return "remove-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    objectStore.store(KEY, TEST_VALUE);
  }

  @Test
  @Description("Remove object of a given key")
  public void remove() throws Exception {
    Event event = flowRunner("remove").withVariable("key", KEY).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("OK"));
    assertThat(objectStore.contains(KEY), is(false));
  }

  @Test
  @Description("Removing object using an empty key throws INVALID_KEY error")
  public void removeWithEmptyKey() throws Exception {
    Event event = flowRunner("remove").withVariable("key", "").run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("INVALID_KEY"));
    assertThat(objectStore.contains(KEY), is(true));
  }

  @Test
  @Description("Removing object using a null key throws INVALID_KEY error")
  public void removeWithNullKey() throws Exception {
    Event event = flowRunner("remove").withVariable("key", null).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("INVALID_KEY"));
    assertThat(objectStore.contains(KEY), is(true));
  }

  @Test
  @Description("Removing object using a key which doesn't exists throws KEY_NOT_FOUND error")
  public void removeUnexisting() throws Exception {
    Event event = flowRunner("removeUnexisting").run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("KEY_NOT_FOUND"));
  }
}
