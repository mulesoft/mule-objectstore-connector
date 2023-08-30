/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.internal;

import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.mule.runtime.core.api.util.ClassUtils.setFieldValue;
import org.mule.extension.objectstore.api.ExtensionObjectStore;
import org.mule.extension.objectstore.api.PrivateObjectStore;
import org.mule.extension.objectstore.api.TopLevelObjectStore;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.tck.core.util.store.InMemoryObjectStore;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExtensionObjectStoreTestCase {

  private static final String A_KEY = "aKey";
  private static final String A_VALUE = "aValue";
  public static final String TOP_OS_NAME_PARAMETER = "name";
  public static final String PRIVATE_OS_ALIAS_PARAMETER = "alias";

  @Rule
  public ExpectedException expectedException = none();

  @Mock
  private ObjectStoreManager runtimeObjectStoreManager;

  @Mock
  private ExtensionManager extensionManager;

  private ObjectStore<Serializable> delegate = new InMemoryObjectStore<>();

  @Spy
  private ObjectStoreRegistry registry = new ObjectStoreRegistry();

  @InjectMocks
  private ExtensionObjectStore privateObjectStore = new PrivateObjectStore();

  @InjectMocks
  private ExtensionObjectStore globalObjectStore = new TopLevelObjectStore();

  @Before
  public void setUp() throws Exception {
    when(runtimeObjectStoreManager.getOrCreateObjectStore(anyString(), any()))
        .thenReturn(delegate);

    injectStubParameters(privateObjectStore);
    injectStubParameters(globalObjectStore);
  }

  @Test
  public void storeWhenObjectStoreNotInitialized() throws ObjectStoreException {
    expectedException.expect(IllegalStateException.class);
    privateObjectStore.retrieve(A_KEY);
  }

  @Test
  public void createStoreOnstartStopLifecycle() throws MuleException, NoSuchFieldException, IllegalAccessException {
    int startExecutions = 2;
    int stopExecutions = 1;

    privateObjectStore.start();
    privateObjectStore.store(A_KEY, A_VALUE);
    privateObjectStore.stop();

    privateObjectStore.start();
    Serializable value = privateObjectStore.retrieve(A_KEY);
    assertThat(value, is(equalTo(A_VALUE)));

    verify(registry, times(startExecutions)).register(privateObjectStore.getName(), "application", privateObjectStore);
    verify(registry, times(startExecutions)).get(privateObjectStore.getName(), "application");
    verify(registry, times(startExecutions)).get(privateObjectStore.getName(), "domain");
    verify(registry, times(stopExecutions)).unregister(privateObjectStore.getName(), "application");
  }

  @Test
  public void restartIsIgnored() throws Exception {
    int startExecutions = 1;
    int stopExecutions = 1;

    privateObjectStore.start();
    privateObjectStore.start();
    privateObjectStore.store(A_KEY, A_VALUE);
    Serializable value = privateObjectStore.retrieve(A_KEY);
    privateObjectStore.stop();
    assertThat(value, is(equalTo(A_VALUE)));

    verify(registry, times(startExecutions)).register(privateObjectStore.getName(), "application", privateObjectStore);
    verify(registry, times(startExecutions)).get(privateObjectStore.getName(), "application");
    verify(registry, times(startExecutions)).get(privateObjectStore.getName(), "domain");
    verify(registry, times(stopExecutions)).unregister(privateObjectStore.getName(), "application");
  }

  @Test
  public void illegalRegisterOfTwoStoresWithSameName() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("An Object Store was already defined with the name ");

    final String osName = "MY_OS";
    setFieldValue(globalObjectStore, TOP_OS_NAME_PARAMETER, osName, true);
    setFieldValue(privateObjectStore, PRIVATE_OS_ALIAS_PARAMETER, osName, true);

    privateObjectStore.start();
    globalObjectStore.start();
  }

  private void injectStubParameters(ObjectStore<Serializable> objectStore) throws IllegalAccessException, NoSuchFieldException {
    setFieldValue(objectStore, "persistent", true, true);
    setFieldValue(objectStore, "maxEntries", 10, true);
    setFieldValue(objectStore, "entryTtlUnit", TimeUnit.SECONDS, true);
    setFieldValue(objectStore, "expirationInterval", 1000L, true);
    setFieldValue(objectStore, "expirationIntervalUnit", TimeUnit.SECONDS, true);
    setFieldValue(objectStore, "appName", of("application"), true);
    setFieldValue(objectStore, "domainName", of("domain"), true);
  }
}
