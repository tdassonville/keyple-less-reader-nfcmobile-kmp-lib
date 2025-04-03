/* **************************************************************************************
 * Copyright (c) 2024 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.keypleless.reader.nfcmobile

import io.github.aakira.napier.Napier
import kotlinx.coroutines.sync.Mutex
import org.eclipse.keyple.keypleless.distributed.client.spi.CardIOException
import org.eclipse.keyple.keypleless.distributed.client.spi.LocalReader
import org.eclipse.keyple.keypleless.distributed.client.spi.ReaderIOException

/**
 * An implementation of Keyple-less LocalReader that provides NFC reading capability for Android,
 * iOS and PCSC readers on Windows/Mac/Linux. For exotic NFC reader hardware, or advanced fine
 * tuning, you can provide your own implementation of the LocalReader interface to the
 * keyple-less-distributed lib.
 */
class MultiplatformNfcReader(private val nfcReader: LocalNfcReader) : LocalReader {
  private val mutex = Mutex()

  /**
   * Used to set the scan instructions to the user, for applicable NFC readers. Main usage is for
   * iOS: the provided msg is displayed in the iOS system-driven NFC popup
   */
  override fun setScanMessage(msg: String) {
    nfcReader.scanMessage = msg
  }

  /**
   * Waits (suspends) for a card to be detected
   *
   * @returns True if a card is detected, otherwise false
   * @throws CancellationException
   */
  override suspend fun waitForCardPresent(): Boolean {
    if (mutex.isLocked) {
      throw ReaderIOException("Reader is already in use")
    }
    try {
      mutex.lock()
      return nfcReader.waitForCardPresent()
    } finally {
      mutex.unlock()
    }
  }

  /**
   * Waits (asynchronously) for a card to be inserted in the reader, then triggers the provided
   * callback.
   *
   * @param onCard The callback that will be called when a card is detected.
   * @throws ReaderIOException on IO error communicating with the reader (USB unplugged, etc.)
   */
  override fun startCardDetection(onCardFound: () -> Unit) {
    try {
      if (!mutex.tryLock()) {
        throw ReaderIOException("Reader is already in use")
      }
      nfcReader.startCardDetection {
        Napier.d(tag = "NFCReader", message = "Card found")
        mutex.unlock()
        onCardFound()
      }
    } catch (e: Exception) {
      mutex.unlock()
      throw e
    }
  }

  /**
   * Attempts to open the physical channel (to establish communication with the card).
   *
   * @throws ReaderIOException on IO error communicating with the reader (USB unplugged, etc.)
   * @throws CardIOException on IO error with the card (card exited the NFC field, etc.)
   */
  override suspend fun openPhysicalChannel() {
    if (mutex.isLocked) {
      throw ReaderIOException("Reader is already in use")
    }

    try {
      mutex.lock()
      nfcReader.openPhysicalChannel()
    } finally {
      mutex.unlock()
    }
  }

  /**
   * Attempts to close the current physical channel. The physical channel may have been implicitly
   * closed previously by a card withdrawal.
   *
   * @throws ReaderNotFoundException If the communication with the reader has failed.
   */
  override fun closePhysicalChannel() {
    nfcReader.closePhysicalChannel()
  }

  /**
   * Gets the power-on data. The power-on data is defined as the data retrieved by the reader when
   * the card is inserted. This is not used in our main target (ISO cards)
   *
   * @return an empty String
   */
  override fun getPowerOnData(): String {
    return nfcReader.getPowerOnData()
  }

  /** Returns the name of this reader (mostly indicative) */
  override fun name(): String {
    return nfcReader.name
  }

  /**
   * Transmits an Application Protocol Data Unit (APDU) command to the smart card and receives the
   * response.
   *
   * @param commandApdu: The command APDU to be transmitted.
   * @return The response APDU received from the smart card.
   * @throws ReaderNotFoundException If the communication with the reader has failed.
   * @throws CardIOException If the communication with the card has failed
   */
  override suspend fun transmitApdu(commandApdu: ByteArray): ByteArray {
    if (mutex.isLocked) {
      throw ReaderIOException("Reader is already in use")
    }

    try {
      mutex.lock()
      return nfcReader.transmitApdu(commandApdu)
    } finally {
      mutex.unlock()
    }
  }

  /** Stop scanning for NFC cards. Release the reader resources. */
  override fun release() {
    nfcReader.releaseReader()
    if (mutex.isLocked) {
      mutex.unlock()
    }
  }
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class LocalNfcReader {
  var scanMessage: String
  var name: String

  /**
   * Waits for a card to be inserted in the reader
   *
   * @returns True if a card is detected, otherwise false
   * @throws TimeoutException
   * @throws CancellationException
   */
  suspend fun waitForCardPresent(): Boolean

  /** Waits for a card to be inserted in the reader, then triggers the provided callback. */
  fun startCardDetection(onCardFound: () -> Unit)

  /** Stop NFC polling and release resources. */
  fun releaseReader()

  /**
   * Attempts to open the physical channel (to establish communication with the card).
   *
   * @throws ReaderNotFoundException If the communication with the reader has failed.
   * @throws CardIOException If the communication with the card has failed
   */
  fun openPhysicalChannel()

  /**
   * Attempts to close the current physical channel. The physical channel may have been implicitly
   * closed previously by a card withdrawal.
   *
   * @throws ReaderNotFoundException If the communication with the reader has failed.
   */
  fun closePhysicalChannel()

  /**
   * Gets the power-on data. The power-on data is defined as the data retrieved by the reader when
   * the card is inserted.
   *
   * @return an empty String as this data is not used in mobile context where we only work with ISO cards.
   */
  fun getPowerOnData(): String

  /**
   * Transmits an Application Protocol Data Unit (APDU) command to the smart card and receives the
   * response.
   *
   * @param commandApdu: The command APDU to be transmitted.
   * @return The response APDU received from the smart card.
   * @throws ReaderNotFoundException If the communication with the reader has failed.
   * @throws CardIOException If the communication with the card has failed
   */
  fun transmitApdu(commandApdu: ByteArray): ByteArray
}
