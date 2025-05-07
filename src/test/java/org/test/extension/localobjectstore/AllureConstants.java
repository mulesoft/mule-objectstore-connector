/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.test.extension.localobjectstore;

public interface AllureConstants {

  interface ObjectStoreFeature {

    String OS_CONNECTOR = "ObjectStore Connector";

    interface ObjectStoreStory {

      String STORE = "Store";
      String RETRIEVE = "Retrieve";
      String REMOVE = "Remove";
      String CONTAINS = "Contains";
      String CLEAR = "Clear";
      String RETRIEVE_ALL = "Retrieve All";
      String RETRIEVE_ALL_KEYS = "Retrieve All Keys";
      String IMPLICIT_STORE = "Use default partition as Implicit Store";
    }
  }
}

