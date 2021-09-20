/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.services;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import org.hyperledger.besu.plugin.BesuContext;
import org.hyperledger.besu.plugin.BesuPlugin;
import org.hyperledger.besu.plugin.services.BesuService;
import org.hyperledger.besu.plugin.services.PluginVersionsProvider;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BesuPluginContextImpl implements BesuContext, PluginVersionsProvider {

  private static final Logger LOG = LogManager.getLogger();

  private enum Lifecycle {
    LOADING,
    UNINITIALIZED,
    REGISTERING,
    REGISTERED,
    STARTING,
    STARTED,
    STOPPING,
    STOPPED
  }

  private Lifecycle state;
  private final Map<Class<?>, ? super BesuService> serviceRegistry = new HashMap<>();
  private final List<BesuPlugin> plugins = new ArrayList<>();

  public BesuPluginContextImpl() {
    this(System.getProperty("besu.plugins.dir"));
  }

  public BesuPluginContextImpl(final String pluginsDir) {
    Path pluginsDirPath;
    if (pluginsDir == null) {
      pluginsDirPath = new File(System.getProperty("besu.home", "."), "plugins").toPath();
    } else {
      pluginsDirPath = new File(pluginsDir).toPath();
    }

    final ClassLoader pluginLoader =
        pluginDirectoryLoader(pluginsDirPath).orElse(this.getClass().getClassLoader());

    ServiceLoader<BesuPlugin> serviceLoader = ServiceLoader.load(BesuPlugin.class, pluginLoader);
    serviceLoader.forEach(plugins::add);

    state = Lifecycle.UNINITIALIZED;
  }

  public <T extends BesuService> void addService(final Class<T> serviceType, final T service) {
    checkArgument(serviceType.isInterface(), "Services must be Java interfaces.");
    checkArgument(
        serviceType.isInstance(service),
        "The service registered with a type must implement that type");
    serviceRegistry.put(serviceType, service);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends BesuService> Optional<T> getService(final Class<T> serviceType) {
    return Optional.ofNullable((T) serviceRegistry.get(serviceType));
  }

  public void registerPlugins() {
    checkState(
        state == Lifecycle.UNINITIALIZED,
        "Besu plugins have already been registered {}.  Cannot register additional plugins.",
        state);

    state = Lifecycle.REGISTERING;

    final Iterator<BesuPlugin> pluginsIterator = plugins.iterator();

    while (pluginsIterator.hasNext()) {
      final BesuPlugin plugin = pluginsIterator.next();
      try {
        plugin.register(this);
        LOG.debug("Registered plugin of type {}.", plugin.getClass().getName());
      } catch (final Exception e) {
        LOG.error(
            "Error registering plugin of type "
                + plugin.getClass().getName()
                + ", start and stop will not be called.",
            e);
        pluginsIterator.remove();
      }
    }

    LOG.debug("Plugin registration complete.");

    state = Lifecycle.REGISTERED;
  }

  public void startPlugins() {
    checkState(
        state == Lifecycle.REGISTERED,
        "BesuContext should be in state %s but it was in %s",
        Lifecycle.REGISTERED,
        state);
    state = Lifecycle.STARTING;
    final Iterator<BesuPlugin> pluginsIterator = plugins.iterator();

    while (pluginsIterator.hasNext()) {
      final BesuPlugin plugin = pluginsIterator.next();

      try {
        plugin.start();
        LOG.debug("Started plugin of type {}.", plugin.getClass().getName());
      } catch (final Exception e) {
        LOG.error(
            "Error starting plugin of type "
                + plugin.getClass().getName()
                + ", stop will not be called.",
            e);
        pluginsIterator.remove();
      }
    }

    LOG.debug("Plugin startup complete.");
    state = Lifecycle.STARTED;
  }

  public void stopPlugins() {
    checkState(
        state == Lifecycle.STARTED,
        "BesuContext should be in state %s but it was in %s",
        Lifecycle.STARTED,
        state);
    state = Lifecycle.STOPPING;

    for (final BesuPlugin plugin : plugins) {
      try {
        plugin.stop();
        LOG.debug("Stopped plugin of type {}.", plugin.getClass().getName());
      } catch (final Exception e) {
        LOG.error("Error stopping plugin of type " + plugin.getClass().getName(), e);
      }
    }

    LOG.debug("Plugin shutdown complete.");
    state = Lifecycle.STOPPED;
  }

  private String getPluginVersion(final BesuPlugin plugin) {
    final Package pluginPackage = plugin.getClass().getPackage();
    final String implTitle =
        Optional.ofNullable(pluginPackage.getImplementationTitle())
            .filter(Predicate.not(String::isBlank))
            .orElse(plugin.getClass().getSimpleName());
    final String implVersion =
        Optional.ofNullable(pluginPackage.getImplementationVersion())
            .filter(Predicate.not(String::isBlank))
            .orElse("<Unknown Version>");
    return implTitle + "/v" + implVersion;
  }

  @Override
  public Collection<String> getPluginVersions() {
    return plugins.stream().map(this::getPluginVersion).collect(Collectors.toList());
  }

  private static URL pathToURIOrNull(final Path p) {
    try {
      return p.toUri().toURL();
    } catch (final MalformedURLException e) {
      return null;
    }
  }

  private Optional<ClassLoader> pluginDirectoryLoader(final Path pluginsDir) {
    if (pluginsDir != null && pluginsDir.toFile().isDirectory()) {
      LOG.debug("Searching for plugins in {}", pluginsDir.toAbsolutePath().toString());

      try (final Stream<Path> pluginFilesList = Files.list(pluginsDir)) {
        final URL[] pluginJarURLs =
            pluginFilesList
                .filter(p -> p.getFileName().toString().endsWith(".jar"))
                .map(BesuPluginContextImpl::pathToURIOrNull)
                .toArray(URL[]::new);
        return Optional.of(new URLClassLoader(pluginJarURLs, this.getClass().getClassLoader()));
      } catch (final MalformedURLException e) {
        LOG.error("Error converting files to URLs, could not load plugins", e);
      } catch (final IOException e) {
        LOG.error("Error enumerating plugins, could not load plugins", e);
      }
    } else {
      LOG.debug("Plugin directory does not exist, skipping registration. - {}", pluginsDir);
    }

    return Optional.empty();
  }

  public Map<String, BesuPlugin> getNamedPlugins() {
    return plugins.stream()
        .filter(plugin -> plugin.getName().isPresent())
        .collect(Collectors.toMap(plugin -> plugin.getName().get(), plugin -> plugin, (a, b) -> b));
  }
}
