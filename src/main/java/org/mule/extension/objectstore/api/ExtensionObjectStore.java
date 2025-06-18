/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.api;

import static java.lang.String.format;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.core.api.config.MuleProperties.LOCAL_OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.objectstore.internal.ObjectStoreConnector;
import org.mule.extension.objectstore.internal.ObjectStoreRegistry;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.dsl.xml.ParameterDsl;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.reference.ConfigReference;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

/**
 * Base class for {@link ObjectStore stores} defined through this connector
 *
 * @since 1.0
 */
public abstract class ExtensionObjectStore implements ObjectStore<Serializable>, Startable, Stoppable, NamedObject {

  private static final Logger LOGGER = getLogger(ExtensionObjectStore.class);
  private boolean started = false;

  @Inject
  private ExtensionManager extensionManager;

  @Inject
  private ObjectStoreRegistry registry;

  @Inject
  @Named("app.name")
  private java.util.Optional<String> appName = java.util.Optional.empty();

  @Inject
  @Named("domain.name")
  private java.util.Optional<String> domainName = java.util.Optional.empty();

  @Inject
  @Named(OBJECT_STORE_MANAGER)
  private ObjectStoreManager runtimeObjectStoreManager;

  @Inject
  @Named(LOCAL_OBJECT_STORE_MANAGER)
  private ObjectStoreManager runtimeLocalObjectStoreManager;

  /**
   * Whether the store is persistent or transient.
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Expression(NOT_SUPPORTED)
  private boolean persistent;

  /**
   * The max number of entries allowed. Exceeding entries will be removed when expiration thread runs. If absent, then the
   * described {@link ObjectStore} will have no size boundaries.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  private Integer maxEntries;

  /**
   * The entry timeout. If absent, then the described {@link ObjectStore} will have no time boundaries.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  private Long entryTtl;

  /**
   * A {@link TimeUnit} which qualifies the {@link #entryTtl}
   */
  @Parameter
  @Optional(defaultValue = "SECONDS")
  @Expression(NOT_SUPPORTED)
  private TimeUnit entryTtlUnit;

  private ComponentLocation location;

  /**
   * How frequently should the expiration thread run.
   * <p>
   * If {@link #maxEntries} nor {@link #entryTtl} are set, then the expiration thread will not run despite of the value set here.
   * <p>
   * If set to a value lower or equal to zero, then there will be no expiration.
   */
  @Parameter
  @Optional(defaultValue = "1")
  @Expression(NOT_SUPPORTED)
  private Long expirationInterval;

  /**
   * A {@link TimeUnit} which qualifies the {@link #expirationInterval}
   */
  @Parameter
  @Optional(defaultValue = "MINUTES")
  @Expression(NOT_SUPPORTED)
  private TimeUnit expirationIntervalUnit;

  /**
   * A reference to an {@code os:config} element which will be used to declare this ObjectStore.
   * <p>
   * If not provided, the runtime's default {@link ObjectStoreManager} will be used. Setting this parameter is only necessary when
   * you want to use a non default {@link ObjectStore} implementation. For example, if you want to have a store which is backed by
   * redis or JDBC
   */
  @Parameter
  @Optional
  @DisplayName("Configuration Reference")
  @Alias("config-ref")
  @ConfigReference(name = "CONFIG", namespace = "OS")
  @Expression(NOT_SUPPORTED)
  @ParameterDsl(allowInlineDefinition = false)
  protected ObjectStoreConnector config;

  /**
   * When running in cluster mode, this indicates that the {@link ObjectStore} is local to the node instead of being distributed.
   */
  @Parameter
  @Optional(defaultValue = "false")
  @Expression(NOT_SUPPORTED)
  private boolean local;

  private transient ConnectionProvider<ObjectStoreManager> storeManagerProvider;
  private transient ObjectStoreManager objectStoreManager;
  private transient ObjectStore<Serializable> delegateStore;

  protected abstract String resolveStoreName();

  public ExtensionObjectStore() {}

  @Override
  public void start() throws MuleException {
    if (started) {
      return;
    }
    storeManagerProvider = getObjectStoreManagerProvider();
    objectStoreManager = getObjectStoreManager();

    if (maxEntries != null && maxEntries < 0) {
      LOGGER
          .warn(format("The maxEntries parameter should not be negative (given value was: %d), otherwise there is no guarantee that the expiration policy will work correctly. For unlimited entries, just omit passing this parameter",
                       maxEntries));
    }

    final ObjectStoreSettings.Builder settings = ObjectStoreSettings.builder()
        .persistent(persistent)
        .maxEntries(maxEntries)
        .expirationInterval(expirationIntervalUnit.toMillis(expirationInterval));

    if (entryTtl != null) {
      settings.entryTtl(entryTtlUnit.toMillis(entryTtl));
    }

    final String storeName = resolveStoreName();

    if (registry.get(storeName, getContextId()) != null) {
      throwStoreAlreadyExists(storeName);
    }

    if (registry.get(storeName, getParentContextId()) != null) {
      throwStoreAlreadyExists(storeName);
    }

    delegateStore = objectStoreManager.getOrCreateObjectStore(storeName, settings.build());
    registry.register(storeName, getContextId(), this);
    started = true;
  }

  private void throwStoreAlreadyExists(String storeName) {
    throw new IllegalArgumentException(format("An Object Store was already defined with the name '%s'", storeName));
  }

  private String getContextId() {
    return appName.orElse(domainName.orElse(null));
  }

  private String getParentContextId() {
    return domainName.orElse(null);
  }

  @Override
  public void stop() {
    registry.unregister(resolveStoreName(), getContextId());

    if (delegateStore != null) {
      try {
        delegateStore.close();
      } catch (Exception e) {
        LOGGER.warn(format("Found exception trying to close Object Store '%s'", resolveStoreName()), e);
      }
    }

    if (storeManagerProvider != null && objectStoreManager != null) {
      try {
        storeManagerProvider.disconnect(objectStoreManager);
      } catch (Exception e) {
        LOGGER.warn(format("Found exception trying to disconnect from ObjectStoreManager obtained through config '%s'",
                           getConfigName()),
                    e);
      }
    }

    storeManagerProvider = null;
    objectStoreManager = null;
    delegateStore = null;
    started = false;
  }

  @Override
  public boolean contains(String key) throws ObjectStoreException {
    checkDelegatedStoreInitialized();
    return delegateStore.contains(key);
  }

  @Override
  public void store(String key, Serializable value) throws ObjectStoreException {
    checkDelegatedStoreInitialized();
    delegateStore.store(key, value);
  }

  @Override
  public Serializable retrieve(String key) throws ObjectStoreException {
    checkDelegatedStoreInitialized();
    return delegateStore.retrieve(key);
  }

  @Override
  public Serializable remove(String key) throws ObjectStoreException {
    checkDelegatedStoreInitialized();
    return delegateStore.remove(key);
  }

  @Override
  public boolean isPersistent() {
    checkDelegatedStoreInitialized();
    return delegateStore.isPersistent();
  }

  @Override
  public void clear() throws ObjectStoreException {
    checkDelegatedStoreInitialized();
    delegateStore.clear();
  }

  @Override
  public void open() throws ObjectStoreException {
    checkDelegatedStoreInitialized();
    delegateStore.open();
  }

  @Override
  public void close() throws ObjectStoreException {
    checkDelegatedStoreInitialized();
    delegateStore.close();
  }

  @Override
  public List<String> allKeys() throws ObjectStoreException {
    checkDelegatedStoreInitialized();
    return delegateStore.allKeys();
  }

  @Override
  public Map<String, Serializable> retrieveAll() throws ObjectStoreException {
    checkDelegatedStoreInitialized();
    return delegateStore.retrieveAll();
  }

  @Override
  public String getName() {
    return resolveStoreName();
  }

  private ObjectStoreManager getObjectStoreManager() throws MuleException {
    return storeManagerProvider.connect();
  }

  private ConnectionProvider<ObjectStoreManager> getObjectStoreManagerProvider() throws MuleException {
    if (config == null) {
      return new FallbackObjectStoreManagerProvider();
    }

    CoreEvent event = CoreEvent.builder(create(resolveStoreName(), "dummy", fromSingleComponent(resolveStoreName()),
                                               NullExceptionHandler.getInstance()))
        .message(Message.of("none"))
        .build();

    ConfigurationInstance configurationProvider;
    try {
      configurationProvider = extensionManager.getConfiguration(getConfigName(), event);
    } catch (IllegalArgumentException e) {
      throw new DefaultMuleException(format("ObjectStore '%s' points to configuration '%s' which doesn't exits",
                                            resolveStoreName(), getConfigName()),
                                     e);
    }

    return configurationProvider.getConnectionProvider().orElseGet(FallbackObjectStoreManagerProvider::new);
  }

  // TODO: this can be removed after MULE-15209 is fixed.
  private void checkDelegatedStoreInitialized() throws IllegalStateException {
    if (delegateStore == null) {
      throw new IllegalStateException(format("Can't perform operation on '%s'. ObjectStore not initialized.",
                                             resolveStoreName()));
    }
  }

  protected String getConfigName() {
    return config != null ? config.getConfigName() : "default";
  }

  private class FallbackObjectStoreManagerProvider implements ConnectionProvider<ObjectStoreManager> {

    @Override
    public ObjectStoreManager connect() throws ConnectionException {
      return local ? runtimeLocalObjectStoreManager : runtimeObjectStoreManager;
    }

    @Override
    public void disconnect(ObjectStoreManager connection) {

    }

    @Override
    public ConnectionValidationResult validate(ObjectStoreManager connection) {
      return success();
    }
  }

  public Integer getMaxEntries() {
    return maxEntries;
  }

  public Long getEntryTtl() {
    return entryTtl;
  }

  public TimeUnit getEntryTtlUnit() {
    return entryTtlUnit;
  }

  public ComponentLocation getLocation() {
    return location;
  }

  public Long getExpirationInterval() {
    return expirationInterval;
  }

  public TimeUnit getExpirationIntervalUnit() {
    return expirationIntervalUnit;
  }

  public void setPersistent(boolean persistent) {
    this.persistent = persistent;
  }

  public void setMaxEntries(Integer maxEntries) {
    this.maxEntries = maxEntries;
  }

  public void setEntryTtl(Long entryTtl) {
    this.entryTtl = entryTtl;
  }

  public void setEntryTtlUnit(TimeUnit entryTtlUnit) {
    this.entryTtlUnit = entryTtlUnit;
  }

  public void setLocation(ComponentLocation location) {
    this.location = location;
  }

  public void setExpirationInterval(Long expirationInterval) {
    this.expirationInterval = expirationInterval;
  }

  public void setExpirationIntervalUnit(TimeUnit expirationIntervalUnit) {
    this.expirationIntervalUnit = expirationIntervalUnit;
  }

  public ObjectStoreConnector getConfig() {
    return config;
  }

  public void setConfig(ObjectStoreConnector config) {
    this.config = config;
  }

  public boolean isLocal() {
    return local;
  }
}
