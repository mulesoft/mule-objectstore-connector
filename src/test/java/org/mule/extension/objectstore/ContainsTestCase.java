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
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.ObjectStoreStory.CONTAINS;
import org.mule.runtime.core.api.Event;

import org.junit.Test;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(OS_CONNECTOR)
@Story(CONTAINS)
public class ContainsTestCase extends AbstractObjectStoreTestCase {

  @Override
  protected String getConfigFile() {
    return "contains-config.xml";
  }

  @Test
  @Description("Checks the existence of a given key")
  public void contains() throws Exception {
    Event event = flowRunner("contains").withPayload(TEST_VALUE).withVariable("key", KEY).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo(true));
    assertThat(objectStore.contains(KEY), is(true));
  }

  @Test
  @Description("Checks the unexistence of a given key")
  public void containsUnexisting() throws Exception {
    String unexistingKey = "unexistingKey";
    Event event = flowRunner("containsUnexisting").withVariable("key", unexistingKey).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo(false));
    assertThat(objectStore.contains(unexistingKey), is(false));
  }
}
