/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.tck.core.util.store.InMemoryObjectStore;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.TypedValue.of;

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
    when(objectStore.retrieve(Matchers.any())).thenThrow(new ObjectDoesNotExistException());

    expectedException.expect(ModuleException.class);
    expectedException.expectMessage(containsString("ObjectStore 'objectStoreStringRepresentation'"));

    objectStoreOperations.retrieve("key", null, null);
  }

  @Test
  public void concurrentStoreOperationsDontLeadToExceptionsWhenFailIsPresentIsSetToFalse() throws InterruptedException {
    // Different locks to emulate the behavior on CloudHub cluster
    when(lockFactory.createLock(anyString())).thenAnswer(invocationOnMock -> new ReentrantLock());

    ObjectStore objectStore = new InMemoryObjectStore();

    int numberOfConcurrentStores = 150;
    CountDownLatch threadsStartedLatch = new CountDownLatch(numberOfConcurrentStores);
    Semaphore startProcessingSemaphore = new Semaphore(0);
    AtomicInteger numberOfExceptions = new AtomicInteger(0);

    List<Thread> threadList = new LinkedList<>();
    for (int i = 0; i < numberOfConcurrentStores; ++i) {
      Thread t = new Thread(() -> {
        try {
          threadsStartedLatch.countDown();
          startProcessingSemaphore.acquire();

          TypedValue<Serializable> value = of("value");
          objectStoreOperations.store("key", value, false, false, objectStore);
        } catch (Exception ex) {
          numberOfExceptions.incrementAndGet();
        }
      });
      t.start();

      threadList.add(t);
    }

    // Wait for all threads to be ready to process.
    threadsStartedLatch.await();

    // Signal the threads to start processing.
    startProcessingSemaphore.release(numberOfConcurrentStores);

    // Wait for threads completion.
    for (Thread thread : threadList) {
      thread.join();
    }

    assertThat(numberOfExceptions.get(), is(0));

    assert true;
  }

}
