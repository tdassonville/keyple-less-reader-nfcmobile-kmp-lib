package org.eclipse.keyple.keyplelessreaderlib

import kotlinx.coroutines.sync.Mutex

class MultiplatformReader(private val nfcReader: LocalNfcReader) {
    private val mutex = Mutex()

    fun setScanMessage(msg: String) {
        nfcReader.scanMessage = msg
    }

    suspend fun waitForCardPresent(): Boolean {
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

    suspend fun startCardDetection(onCardFound: () -> Unit) {
        if (mutex.isLocked) {
            throw ReaderIOException("Reader is already in use")
        }

        try {
            mutex.lock()
            nfcReader.startCardDetection {
                mutex.unlock()
                onCardFound()
            }
        } catch (e: Exception) {
            mutex.unlock()
            throw e
        }
    }

    suspend fun openPhysicalChannel() {
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

    fun closePhysicalChannel() {
        nfcReader.closePhysicalChannel()
    }

    fun getPowerOnData(): String {
        return nfcReader.getPowerOnData()
    }

    suspend fun transmitApdu(commandApdu: ByteArray): ByteArray {
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

    fun release() {
        nfcReader.releaseReader()
        if (mutex.isLocked) {
            mutex.unlock()
        }
    }
}

expect class LocalNfcReader {
    var scanMessage: String

    /**
     * Waits for a card to be inserted in the reader
     * @returns True if a card is detected, otherwise false
     *
     * @throws TimeoutException or CancellationException
     */
    suspend fun waitForCardPresent(): Boolean

    suspend fun startCardDetection(onCardFound: () -> Unit)

    fun releaseReader()

    /**
     * Attempts to open the physical channel (to establish communication with the card).
     * <exception cref="ReaderNotFoundException">If the communication with the reader has failed.</exception>
     * <exception cref="CardIOException">If the communication with the card has failed.</exception>
     */
    fun openPhysicalChannel()

    /**
     * Attempts to close the current physical channel.
     * The physical channel may have been implicitly closed previously by a card withdrawal.
     *
     * <exception cref="ReaderNotFoundException">If the communication with the reader has failed.</exception>
     */
    fun closePhysicalChannel()

    /**
     * Gets the power-on data.
     * The power-on data is defined as the data retrieved by the reader when the card is inserted.
     *
     * In the case of a contactless reader, the reader decides what this data is. Contactless
     * readers provide a virtual ATR (partially standardized by the PC/SC standard), but other devices
     * can have their own definition, including for example elements from the anti-collision stage of
     * the ISO14443 protocol (ATQA, ATQB, ATS, SAK, etc).
     *
     * These data being variable from one reader to another, they are defined here in string format
     * which can be either a hexadecimal string or any other relevant information.
     *
     * @return a non empty String
     */
    fun getPowerOnData(): String

    /**
     * Transmits an Application Protocol Data Unit (APDU) command to the smart card and receives the response.
     *
     * @param commandApdu: The command APDU to be transmitted.
     * @return The response APDU received from the smart card.
     *
     * <exception cref="ReaderNotFoundException">If the communication with the reader has failed.</exception>
     * <exception cref="CardIOException">If the communication with the card has failed.</exception>
     *
     */
    fun transmitApdu(commandApdu: ByteArray): ByteArray
}

class UnexpectedStatusWordException(message: String) : Exception(message)
class ReaderIOException(message: String) : Exception(message)
class CardIOException(message: String) : Exception(message)
