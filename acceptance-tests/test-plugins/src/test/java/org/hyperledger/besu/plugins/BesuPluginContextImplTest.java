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
package org.hyperledger.besu.plugins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.hyperledger.besu.plugin.BesuPlugin;
import org.hyperledger.besu.services.BesuPluginContextImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class BesuPluginContextImplTest {

  @BeforeClass
  public static void createFakePluginDir() throws IOException {
    if (System.getProperty("besu.plugins.dir") == null) {
      final Path pluginDir = Files.createTempDirectory("besuTest");
      pluginDir.toFile().deleteOnExit();
      System.setProperty("besu.plugins.dir", pluginDir.toAbsolutePath().toString());
    }
  }

  @After
  public void clearTestPluginState() {
    System.clearProperty("testPicoCLIPlugin.testOption");
  }

  @Test
  public void verifyEverythingGoesSmoothly() {
    final BesuPluginContextImpl contextImpl = new BesuPluginContextImpl();

    final Optional<TestPicoCLIPlugin> testPluginOptional =
        findTestPlugin(contextImpl.getNamedPlugins());
    assertThat(testPluginOptional).isPresent();
    final TestPicoCLIPlugin testPicoCLIPlugin = testPluginOptional.get();

    contextImpl.registerPlugins();
    assertThat(testPicoCLIPlugin.getState()).isEqualTo("registered");

    contextImpl.startPlugins();
    assertThat(testPicoCLIPlugin.getState()).isEqualTo("started");

    contextImpl.stopPlugins();
    assertThat(testPicoCLIPlugin.getState()).isEqualTo("stopped");
  }

  @Test
  public void registrationErrorsHandledSmoothly() {
    System.setProperty("testPicoCLIPlugin.testOption", "FAILREGISTER");

    final BesuPluginContextImpl contextImpl = new BesuPluginContextImpl();

    contextImpl.registerPlugins();
    assertThat(contextImpl.getNamedPlugins()).isNotInstanceOfAny(TestPicoCLIPlugin.class);

    contextImpl.startPlugins();
    assertThat(contextImpl.getNamedPlugins()).isNotInstanceOfAny(TestPicoCLIPlugin.class);

    contextImpl.stopPlugins();
    assertThat(contextImpl.getNamedPlugins()).isNotInstanceOfAny(TestPicoCLIPlugin.class);
  }

  @Test
  public void startErrorsHandledSmoothly() {
    System.setProperty("testPicoCLIPlugin.testOption", "FAILSTART");

    final BesuPluginContextImpl contextImpl = new BesuPluginContextImpl();

    contextImpl.registerPlugins();
    assertThat(contextImpl.getNamedPlugins().values())
        .extracting("class")
        .contains(TestPicoCLIPlugin.class);

    final Optional<TestPicoCLIPlugin> testPluginOptional =
        findTestPlugin(contextImpl.getNamedPlugins());
    assertThat(testPluginOptional).isPresent();
    final TestPicoCLIPlugin testPicoCLIPlugin = testPluginOptional.get();
    assertThat(testPicoCLIPlugin.getState()).isEqualTo("registered");

    contextImpl.startPlugins();
    assertThat(testPicoCLIPlugin.getState()).isEqualTo("failstart");
    assertThat(contextImpl.getNamedPlugins()).isNotInstanceOfAny(TestPicoCLIPlugin.class);

    contextImpl.stopPlugins();
    assertThat(contextImpl.getNamedPlugins()).isNotInstanceOfAny(TestPicoCLIPlugin.class);
  }

  @Test
  public void stopErrorsHandledSmoothly() {
    System.setProperty("testPicoCLIPlugin.testOption", "FAILSTOP");

    final BesuPluginContextImpl contextImpl = new BesuPluginContextImpl();

    contextImpl.registerPlugins();
    assertThat(contextImpl.getNamedPlugins().values())
        .extracting("class")
        .contains(TestPicoCLIPlugin.class);

    final Optional<TestPicoCLIPlugin> testPluginOptional =
        findTestPlugin(contextImpl.getNamedPlugins());
    assertThat(testPluginOptional).isPresent();
    final TestPicoCLIPlugin testPicoCLIPlugin = testPluginOptional.get();
    assertThat(testPicoCLIPlugin.getState()).isEqualTo("registered");

    contextImpl.startPlugins();
    assertThat(testPicoCLIPlugin.getState()).isEqualTo("started");

    contextImpl.stopPlugins();
    assertThat(testPicoCLIPlugin.getState()).isEqualTo("failstop");
  }

  @Test
  public void lifecycleExceptions() throws Throwable {
    final BesuPluginContextImpl contextImpl = new BesuPluginContextImpl();
    final ThrowableAssert.ThrowingCallable registerPlugins = () -> contextImpl.registerPlugins();

    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(contextImpl::startPlugins);
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(contextImpl::stopPlugins);

    registerPlugins.call();
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(registerPlugins);
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(contextImpl::stopPlugins);

    contextImpl.startPlugins();
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(registerPlugins);
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(contextImpl::startPlugins);

    contextImpl.stopPlugins();
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(registerPlugins);
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(contextImpl::startPlugins);
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(contextImpl::stopPlugins);
  }

  private Optional<TestPicoCLIPlugin> findTestPlugin(final Map<String, BesuPlugin> plugins) {
    return plugins.values().stream()
        .filter(p -> p instanceof TestPicoCLIPlugin)
        .map(p -> (TestPicoCLIPlugin) p)
        .findFirst();
  }
}
