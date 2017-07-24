/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.api;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.concurrent.TimeUnit;

@Alias("objectStore")
@XmlHints(allowTopLevelDefinition = true)
public class ExtensionObjectStore {

  @Parameter
  @Optional(defaultValue = "true")
  private boolean persistent;

  @Parameter
  @Optional
  private Integer maxEntries;

  @Parameter
  @Optional
  private Integer entryTtl;

  @Parameter
  @Optional(defaultValue = "SECONDS")
  private TimeUnit entryTtlUnit;


}
