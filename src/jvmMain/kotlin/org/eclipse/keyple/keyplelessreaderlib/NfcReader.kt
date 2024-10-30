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
package org.eclipse.keyple.keyplelessreaderlib

import io.github.aakira.napier.Napier
import javax.smartcardio.Card
import javax.smartcardio.CardChannel
import javax.smartcardio.CommandAPDU
import javax.smartcardio.TerminalFactory
import kotlinx.coroutines.delay

actual class LocalNfcReader {
  actual var scanMessage: String = ""

  private var card: Card? = null
  private var channel: CardChannel? = null

  actual suspend fun waitForCardPresent(): Boolean {
    val terminalFactory = TerminalFactory.getDefault()
    val terminals = terminalFactory.terminals().list()
    Napier.d("Terminals: $terminals")
    if (terminals.isEmpty()) {
      return false
    }

    val terminal = terminals[0]

    while (!terminal.isCardPresent) {
      // wait for card to be present
    }

    card = terminal.connect("T=0")
    Napier.d("Card: $card")

    return true
  }

  actual suspend fun startCardDetection(onCardFound: () -> Unit) {
    delay(5000)
    onCardFound()
  }

  /**
   * Attempts to open the physical channel (to establish communication with the card). <exception
   * cref="ReaderNotFoundException">If the communication with the reader has failed.</exception>
   * <exception cref="CardIOException">If the communication with the card has failed.</exception>
   */
  actual fun openPhysicalChannel() {
    channel = card!!.basicChannel
  }

  /**
   * Attempts to close the current physical channel. The physical channel may have been implicitly
   * closed previously by a card withdrawal.
   *
   * <exception cref="ReaderNotFoundException">If the communication with the reader has
   * failed.</exception>
   */
  actual fun closePhysicalChannel() {}

  /**
   * Gets the power-on data. The power-on data is defined as the data retrieved by the reader when
   * the card is inserted.
   *
   * In the case of a contactless reader, the reader decides what this data is. Contactless readers
   * provide a virtual ATR (partially standardized by the PC/SC standard), but other devices can
   * have their own definition, including for example elements from the anti-collision stage of the
   * ISO14443 protocol (ATQA, ATQB, ATS, SAK, etc).
   *
   * These data being variable from one reader to another, they are defined here in string format
   * which can be either a hexadecimal string or any other relevant information.
   *
   * @return a non empty String
   */
  actual fun getPowerOnData(): String {
    return ""
  }

  /**
   * Transmits an Application Protocol Data Unit (APDU) command to the smart card and receives the
   * response.
   *
   * @param commandApdu: The command APDU to be transmitted.
   * @return The response APDU received from the smart card.
   *
   * <exception cref="ReaderNotFoundException">If the communication with the reader has
   * failed.</exception> <exception cref="CardIOException">If the communication with the card has
   * failed.</exception>
   */
  @OptIn(ExperimentalStdlibApi::class)
  actual fun transmitApdu(commandApdu: ByteArray): ByteArray {
    Napier.d("APDU: ${commandApdu.toHexString()}")
    val response = channel!!.transmit(CommandAPDU(commandApdu))
    Napier.d("APDU response: ${response.bytes.toHexString()}")
    return response.bytes
  }

  actual fun releaseReader() {
    card?.disconnect(false)
  }
}
