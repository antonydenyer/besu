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
package org.hyperledger.besu.cli.options.unstable;

import org.hyperledger.besu.cli.options.CLIOptions;
import org.hyperledger.besu.ethereum.eth.transactions.ImmutableTransactionPoolConfiguration;
import org.hyperledger.besu.ethereum.eth.transactions.TransactionPoolConfiguration;

import java.time.Duration;

import picocli.CommandLine;

public class TransactionPoolOptions
    implements CLIOptions<ImmutableTransactionPoolConfiguration.Builder> {
  private static final String TX_MESSAGE_KEEP_ALIVE_SEC_FLAG =
      "--Xincoming-tx-messages-keep-alive-seconds";

  private static final String ETH65_TX_ANNOUNCED_BUFFERING_PERIOD_FLAG =
      "--Xeth65-tx-announced-buffering-period-milliseconds";

  @CommandLine.Option(
      names = {TX_MESSAGE_KEEP_ALIVE_SEC_FLAG},
      paramLabel = "<INTEGER>",
      hidden = true,
      description =
          "Keep alive of incoming transaction messages in seconds (default: ${DEFAULT-VALUE})",
      arity = "1")
  private Integer txMessageKeepAliveSeconds =
      TransactionPoolConfiguration.DEFAULT_TX_MSG_KEEP_ALIVE;

  @CommandLine.Option(
      names = {ETH65_TX_ANNOUNCED_BUFFERING_PERIOD_FLAG},
      paramLabel = "<LONG>",
      hidden = true,
      description =
          "The period for which the announced transactions remain in the buffer before being requested from the peers in milliseconds (default: ${DEFAULT-VALUE})",
      arity = "1")
  private long eth65TrxAnnouncedBufferingPeriod =
      TransactionPoolConfiguration.ETH65_TRX_ANNOUNCED_BUFFERING_PERIOD.toMillis();

  private TransactionPoolOptions() {}

  public static TransactionPoolOptions create() {
    return new TransactionPoolOptions();
  }

  public static TransactionPoolOptions fromConfig(final TransactionPoolConfiguration config) {
    final TransactionPoolOptions options = TransactionPoolOptions.create();
    options.txMessageKeepAliveSeconds = config.getTxMessageKeepAliveSeconds();
    options.eth65TrxAnnouncedBufferingPeriod =
        config.getEth65TrxAnnouncedBufferingPeriod().toMillis();
    return options;
  }

  @Override
  public ImmutableTransactionPoolConfiguration.Builder toDomainObject() {
    return ImmutableTransactionPoolConfiguration.builder()
        .txMessageKeepAliveSeconds(txMessageKeepAliveSeconds)
        .eth65TrxAnnouncedBufferingPeriod(Duration.ofMillis(eth65TrxAnnouncedBufferingPeriod));
  }
}
