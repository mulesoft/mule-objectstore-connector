/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.integration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.OS_CONNECTOR;
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.ObjectStoreStory.CONTAINS;

import org.mule.runtime.core.api.event.CoreEvent;

import org.junit.Test;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(OS_CONNECTOR)
@Story(CONTAINS)
public class ContainsTestCase extends ParameterizedObjectStoreTestCase {

  public ContainsTestCase(String name) {
    super(name);
  }

  @Override
  protected String doGetConfigFile() {
    return "contains-config.xml";
  }

  @Test
  @Description("Checks the existence of a given key")
  public void contains() throws Exception {
    CoreEvent event = flowRunner("contains").withPayload(TEST_VALUE).withVariable("key", KEY).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo(true));
    assertThat(getObjectStore().contains(KEY), is(true));
  }
}
