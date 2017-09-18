/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore;

import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;

import org.mule.extension.objectstore.api.ExtensionObjectStore;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import javax.inject.Inject;

public class ObjectStoreDefinitionTestCase extends AbstractObjectStoreTestCase {

  @Inject
  private Registry registry;

  private ObjectStoreManager objectStoreManager;

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {
        TRANSIENT_STORE_DEFINITION_CONFIG_FILE_NAME,
        PERSISTENT_STORE_DEFINITION_CONFIG_FILE_NAME,
        IMPLICIT_STORE_DEFINITION_CONFIG_FILE_NAME
    };
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    objectStoreManager = mock(ObjectStoreManager.class, RETURNS_DEEP_STUBS);
    return singletonMap(OBJECT_STORE_MANAGER, objectStoreManager);
  }

  @Test
  public void defineTransientStore() throws Exception {
    assertStoreDefinition("transientStore", false);
  }

  @Test
  public void definePersistentStore() throws Exception {
    assertStoreDefinition("persistentStore", true);
  }

  @Test
  public void defineImplicitStore() throws Exception {
    assertStoreDefinition("implicitStore", false);
  }

  private void assertStoreDefinition(String storeName, boolean persistent) {
    ExtensionObjectStore objectStore = registry.<ExtensionObjectStore>lookupByName(storeName).get();
    assertThat(objectStore, is(notNullValue()));
    ArgumentCaptor<ObjectStoreSettings> settingsCaptor = ArgumentCaptor.forClass(ObjectStoreSettings.class);
    verify(objectStoreManager).createObjectStore(eq(storeName), settingsCaptor.capture());

    ObjectStoreSettings settings = settingsCaptor.getValue();
    assertThat(settings, is(notNullValue()));
    assertThat(settings.isPersistent(), is(persistent));
    assertThat(settings.getEntryTTL().get(), is(HOURS.toMillis(1)));
    assertThat(settings.getExpirationInterval(), is(HOURS.toMillis(2)));
    assertThat(settings.getMaxEntries().get(), is(10));
  }
}
