/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.internal;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.extension.api.exception.ModuleException;

import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.Matchers.containsString;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ObjectStoreOperationsTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Mock
  private ObjectStoreManager runtimeObjectStoreManager;

  @Mock
  private LockFactory lockFactory;

  @InjectMocks
  private ObjectStoreOperations objectStoreOperations;

  @Test
  public void retrieveValueWhenObjectStoreAndDefaultValueAreNull() throws ObjectStoreException {
    when(lockFactory.createLock(anyString())).thenReturn(new ReentrantLock());

    ObjectStore objectStore = mock(ObjectStore.class);
    when(objectStore.toString()).thenReturn("objectStoreStringRepresentation");
    when(runtimeObjectStoreManager.getDefaultPartition()).thenReturn(objectStore);
    when(objectStore.retrieve(any())).thenThrow(new ObjectDoesNotExistException());

    expectedException.expect(ModuleException.class);
    expectedException.expectMessage(containsString("ObjectStore 'objectStoreStringRepresentation'"));

    objectStoreOperations.retrieve("key", null, null);
  }

  @Test
  public void rateLimitExceededRetrieve() throws ObjectStoreException {


    when(lockFactory.createLock(anyString())).thenReturn(new ReentrantLock());

    ObjectStore objectStore = mock(ObjectStore.class);
    when(objectStore.toString()).thenReturn("objectStoreStringRepresentation");
    when(runtimeObjectStoreManager.getDefaultPartition()).thenReturn(objectStore);
    Exception e =
        new Exception("Unable to check existence of object with key test in store APP_osdemo__defaultPersistentObjectStore, status code was 429, response was null");
    when(objectStore.retrieve(any())).thenThrow(new ObjectStoreException(e));

    expectedException.expect(ModuleException.class);
    expectedException.expectMessage(containsString("Rate Limit"));
    objectStoreOperations.retrieve("key", null, null);

  }

  @Test
  public void removeWhenStoringException() throws ObjectStoreException {
    when(lockFactory.createLock(anyString())).thenReturn(new ReentrantLock());

    ObjectStore objectStore = mock(ObjectStore.class);
    when(objectStore.toString()).thenReturn("objectStoreStringRepresentation");
    when(runtimeObjectStoreManager.getDefaultPartition()).thenReturn(objectStore);
    Exception exceptionAlreadyExists = new Exception("ObjectStore already contains entry for key 123");
    doThrow(new ObjectAlreadyExistsException(exceptionAlreadyExists)).when(objectStore).store(any(), any());
    Exception exceptionDoesNotExists = new Exception("ObjectStore doesn't contain any value for key '123'");
    doThrow(new ObjectDoesNotExistException(exceptionDoesNotExists)).when(objectStore).remove(any());

    expectedException.expect(ModuleException.class);
    expectedException.expectMessage(containsString("ObjectStore doesn't contain any value for key '123'"));
    objectStoreOperations.store("123", TypedValue.of("value"), false, false, null);
  }
}
