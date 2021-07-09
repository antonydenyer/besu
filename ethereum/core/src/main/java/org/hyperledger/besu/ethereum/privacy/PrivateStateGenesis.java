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
package org.hyperledger.besu.ethereum.privacy;

import org.hyperledger.besu.config.experimental.PrivacyGenesisConfigOptions;
import org.hyperledger.besu.ethereum.core.Address;
import org.hyperledger.besu.ethereum.core.EvmAccount;
import org.hyperledger.besu.ethereum.core.MutableAccount;
import org.hyperledger.besu.ethereum.core.MutableWorldState;
import org.hyperledger.besu.ethereum.core.WorldUpdater;
import org.hyperledger.besu.ethereum.privacy.group.OnChainGroupManagement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

public class PrivateStateGenesis {
  private static final Logger LOG = LogManager.getLogger();

  private final Boolean isOnchainPrivacyEnabled;
  private final PrivacyGenesisConfigOptions privacyGenesisConfigOptions;

  public PrivateStateGenesis(
      final Boolean isOnchainPrivacyEnabled,
      final PrivacyGenesisConfigOptions privacyGenesisConfigOptions) {
    this.isOnchainPrivacyEnabled = isOnchainPrivacyEnabled;
    this.privacyGenesisConfigOptions = privacyGenesisConfigOptions;
  }

  public PrivacyGenesisConfigOptions getPrivacyGenesisConfigOptions() {
    return this.privacyGenesisConfigOptions;
  }

  public void applyGenesisToPrivateWorldState(
      final MutableWorldState disposablePrivateState, final WorldUpdater privateWorldStateUpdater) {

    LOG.info("Applying {} allocations ", privacyGenesisConfigOptions.getAllocations());

    if (isOnchainPrivacyEnabled) {
      // inject management
      final EvmAccount managementPrecompile =
          privateWorldStateUpdater.createAccount(Address.DEFAULT_ONCHAIN_PRIVACY_MANAGEMENT);
      final MutableAccount mutableManagementPrecompiled = managementPrecompile.getMutable();
      // this is the code for the simple management contract
      mutableManagementPrecompiled.setCode(
          OnChainGroupManagement.DEFAULT_GROUP_MANAGEMENT_RUNTIME_BYTECODE);

      // inject proxy
      final EvmAccount proxyPrecompile =
          privateWorldStateUpdater.createAccount(Address.ONCHAIN_PRIVACY_PROXY);
      final MutableAccount mutableProxyPrecompiled = proxyPrecompile.getMutable();
      // this is the code for the proxy contract
      mutableProxyPrecompiled.setCode(OnChainGroupManagement.PROXY_RUNTIME_BYTECODE);
      // manually set the management contract address so the proxy can trust it
      mutableProxyPrecompiled.setStorageValue(
          UInt256.ZERO,
          UInt256.fromBytes(Bytes32.leftPad(Address.DEFAULT_ONCHAIN_PRIVACY_MANAGEMENT)));
    }

    privateWorldStateUpdater.commit();
    disposablePrivateState.persist(null);
  }
}
