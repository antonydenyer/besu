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
package org.hyperledger.besu.plugin.services.privacy;

import org.hyperledger.besu.plugin.data.Address;
import org.hyperledger.besu.plugin.data.PrivateTransaction;
import org.hyperledger.besu.plugin.data.Transaction;

import org.apache.tuweni.bytes.Bytes;

public interface PrivateMarkerTransactionSigner {

  Address getSender(PrivateTransaction privateTransaction, String privacyUserId);

  Bytes signPrivacyMarkerTransaction(Transaction privacyMarkerTransaction, String privacyUserId);
}
