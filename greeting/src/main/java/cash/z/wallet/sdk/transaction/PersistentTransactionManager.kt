package cash.z.wallet.sdk.transaction

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import cash.z.wallet.sdk.db.PendingTransactionDao
import cash.z.wallet.sdk.db.PendingTransactionDb
import cash.z.wallet.sdk.db.entity.PendingTransaction
import cash.z.wallet.sdk.db.entity.PendingTransactionEntity
import cash.z.wallet.sdk.db.entity.isCancelled
import cash.z.wallet.sdk.db.entity.isSubmitted
import cash.z.wallet.sdk.ext.twig
import cash.z.wallet.sdk.service.LightWalletService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Facilitates persistent attempts to ensure that an outbound transaction is completed.
 *
 * @param db the database where the wallet can freely write information related to pending
 * transactions. This database effectively serves as the mempool for transactions created by this
 * wallet.
 * @property encoder responsible for encoding a transaction by taking all the inputs and returning
 * an [cash.z.wallet.sdk.entity.EncodedTransaction] object containing the raw bytes and transaction
 * id.
 * @property service the lightwallet service used to submit transactions.
 */
class PersistentTransactionManager(
    db: PendingTransactionDb,
    private val encoder: TransactionEncoder,
    private val service: LightWalletService
) : OutboundTransactionManager {

    private val daoMutex = Mutex()

    /**
     * Internal reference to the dao that is only accessed after locking the [daoMutex] in order
     * to enforce DB access in both a threadsafe and coroutinesafe way.
     */
    private val _dao: PendingTransactionDao = db.pendingTransactionDao()

    /**
     * Constructor that creates the database and then executes a callback on it.
     */
    constructor(
        appContext: Context,
        encoder: TransactionEncoder,
        service: LightWalletService,
        dataDbName: String = "PendingTransactions.db"
    ) : this(
        Room.databaseBuilder(
            appContext,
            PendingTransactionDb::class.java,
            dataDbName
        ).setJournalMode(RoomDatabase.JournalMode.TRUNCATE).build(),
        encoder,
        service
    )


    //
    // OutboundTransactionManager implementation
    //

    override suspend fun initSpend(
        zatoshiValue: Long,
        toAddress: String,
        memo: String,
        fromAccountIndex: Int
    ): PendingTransaction = withContext(Dispatchers.IO) {
        twig("constructing a placeholder transaction")
        var tx = PendingTransactionEntity(
            value = zatoshiValue,
            toAddress = toAddress,
            memo = memo.toByteArray(),
            accountIndex = fromAccountIndex
        )
        try {
            twig("creating tx in DB: $tx")
            pendingTransactionDao {
                val insertedTx = findById(create(tx))
                twig("pending transaction created with id: ${insertedTx?.id}")
                tx = tx.copy(id = insertedTx!!.id)
            }.also {
                twig("successfully created TX in DB")
            }
        } catch (t: Throwable) {
            twig("Unknown error while attempting to create pending transaction: ${t.message}" +
                    " caused by: ${t.cause}")
        }

        tx
    }

    override suspend fun applyMinedHeight(pendingTx: PendingTransaction, minedHeight: Int) {
        (pendingTx as? PendingTransactionEntity)?.let {
            twig("a pending transaction has been mined!")
            safeUpdate(pendingTx.copy(minedHeight = minedHeight))
        }
    }

    override suspend fun encode(
        spendingKey: String,
        pendingTx: PendingTransaction,
        sapling: String
    ): PendingTransaction = withContext(Dispatchers.IO) {
        twig("managing the creation of a transaction")
        var tx = pendingTx as PendingTransactionEntity
        try {
            twig("beginning to encode transaction with : $encoder")
            val encodedTx = encoder.createTransaction(
                spendingKey,
                tx.value,
                tx.toAddress,
                sapling,
                tx.memo,
                tx.accountIndex
            )
            twig("successfully encoded transaction for ${tx.memo}!!")
            tx = tx.copy(raw = encodedTx.raw, rawTransactionId = encodedTx.txId)
        } catch (t: Throwable) {
            val message = "failed to encode transaction due to : ${t.message} caused by: ${t.cause}"
            twig(message)
            message
            tx = tx.copy(errorMessage = message, errorCode = ERROR_ENCODING)
        } finally {
            tx = tx.copy(encodeAttempts = max(1, tx.encodeAttempts + 1))
        }
        safeUpdate(tx)

        tx
    }

    override suspend fun submit(pendingTx: PendingTransaction): PendingTransaction = withContext(Dispatchers.IO) {
        // reload the tx to check for cancellation
        var storedTx = pendingTransactionDao { findById(pendingTx.id) }
            ?: throw IllegalStateException("Error while submitting transaction. No pending" +
                    " transaction found that matches the one being submitted. Verify that the" +
                    " transaction still exists among the set of pending transactions.")
        var tx = storedTx
        try {
            // do nothing when cancelled
            if (!tx.isCancelled()) {
                twig("submitting transaction with memo: ${tx.memo} amount: ${tx.value}")
                val response = service.submitTransaction(tx.raw)
                val error = response.errorCode < 0
                twig("${if (error) "FAILURE! " else "SUCCESS!"} submit transaction completed with" +
                        " response: ${response.errorCode}: ${response.errorMessage}")
                tx = tx.copy(
                    errorMessage = if (error) response.errorMessage else null,
                    errorCode = response.errorCode,
                    submitAttempts = max(1, tx.submitAttempts + 1)
                )
                safeUpdate(tx)
            } else {
                twig("Warning: ignoring cancelled transaction with id ${tx.id}")
            }
        } catch (t: Throwable) {
            // a non-server error has occurred
            val message =
                "Unknown error while submitting transaction: ${t.message} caused by: ${t.cause}"
            twig(message)
            tx = tx.copy(
                errorMessage = t.message,
                errorCode = ERROR_SUBMITTING,
                submitAttempts = max(1, tx.submitAttempts + 1)
            )
            safeUpdate(tx)
        }

        tx
    }

    override suspend fun monitorById(id: Long): Flow<PendingTransaction> {
        return pendingTransactionDao { monitorById(id) }
    }

    override suspend fun isValidShieldedAddress(address: String) =
        encoder.isValidShieldedAddress(address)

    override suspend fun isValidTransparentAddress(address: String) =
        encoder.isValidTransparentAddress(address)

    override suspend fun cancel(pendingTx: PendingTransaction): Boolean {
        return pendingTransactionDao {
            val tx = findById(pendingTx.id)
            if (tx?.isSubmitted() == true) {
                false
            } else {
                cancel(pendingTx.id)
                true
            }
        }
    }

    override fun getAll() = _dao.getAll()


    //
    // Helper functions
    //

    /**
     * Remove a transaction and pretend it never existed.
     */
    suspend fun abortTransaction(existingTransaction: PendingTransaction) {
        pendingTransactionDao {
            delete(existingTransaction as PendingTransactionEntity)
        }
    }

    /**
     * Updating the pending transaction is often done at the end of a function but still should
     * happen within a try/catch block, surrounded by logging. So this helps with that.
     */
    private suspend fun safeUpdate(tx: PendingTransactionEntity): PendingTransaction {
        return try {
            twig("updating tx in DB: $tx")
            pendingTransactionDao { update(tx) }
            twig("successfully updated TX in DB")
            tx
        } catch (t: Throwable) {
            twig("Unknown error while attempting to update pending transaction: ${t.message}" +
                    " caused by: ${t.cause}")
            tx
        }
    }

    private suspend fun <T> pendingTransactionDao(block: suspend PendingTransactionDao.() -> T): T {
        return daoMutex.withLock {
            _dao.block()
        }
    }

    companion object {
        /** Error code for an error while encoding a transaction */
        const val ERROR_ENCODING = 2000
        /** Error code for an error while submitting a transaction */
        const val ERROR_SUBMITTING = 3000
    }
}
