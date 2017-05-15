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
import org.mule.runtime.core.api.Event;

import org.junit.Test;

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
  public void remove() throws Exception {
    Event event = flowRunner("remove").withVariable("key", KEY).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("OK"));
    assertThat(objectStore.contains(KEY), is(false));
  }

  @Test
  public void removeWithEmptyKey() throws Exception {
    Event event = flowRunner("remove").withVariable("key", "").run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("INVALID_KEY"));
    assertThat(objectStore.contains(KEY), is(true));
  }

  @Test
  public void removeWithNullKey() throws Exception {
    Event event = flowRunner("remove").withVariable("key", null).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("INVALID_KEY"));
    assertThat(objectStore.contains(KEY), is(true));
  }

  @Test
  public void removeUnexisting() throws Exception {
    Event event = flowRunner("removeUnexisting").run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("KEY_NOT_FOUND"));
  }
}
