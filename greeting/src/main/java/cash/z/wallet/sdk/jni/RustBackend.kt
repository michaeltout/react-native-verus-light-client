package cash.z.wallet.sdk.jni

import cash.z.wallet.sdk.exception.BirthdayException
import cash.z.wallet.sdk.ext.ZcashSdk.OUTPUT_PARAM_FILE_NAME
import cash.z.wallet.sdk.ext.ZcashSdk.SPEND_PARAM_FILE_NAME
import cash.z.wallet.sdk.ext.twig
import java.io.File

/**
 * Serves as the JNI boundary between the Kotlin and Rust layers. Functions in this class should
 * not be called directly by code outside of the SDK. Instead, one of the higher-level components
 * should be used such as Wallet.kt or CompactBlockProcessor.kt.
 */
class RustBackend : RustBackendWelding {

    init {
        load()
    }

    // Paths
    internal lateinit var pathDataDb: String
    internal lateinit var pathCacheDb: String
    internal lateinit var pathParamsDir: String
    internal lateinit var chain_network_string: String

    internal var birthdayHeight: Int = -1
        get() = if (field != -1) field else throw BirthdayException.UninitializedBirthdayException

    /**
     * Loads the library and initializes path variables. Although it is best to only call this
     * function once, it is idempotent.
     */
    fun init(
        cacheDbPath: String,
        dataDbPath: String,
        paramsPath: String,
        paramsType: String
    ): RustBackend {
        twig("Creating RustBackend") {
            pathCacheDb = cacheDbPath
            pathDataDb = dataDbPath
            pathParamsDir = paramsPath
            chain_network_string = paramsType
        }
        return this
    }

    fun clear(clearCacheDb: Boolean = true, clearDataDb: Boolean = true) {
        if (clearCacheDb) {
            twig("Deleting cache database!")
            File(pathCacheDb).delete()
        }
        if (clearDataDb) {
            twig("Deleting data database!")
            File(pathDataDb).delete()
        }
    }


    //
    // Wrapper Functions
    //

    override fun initDataDb() = initDataDb(pathDataDb)

   override fun initAccountsTable(extfvks: Array<String>) =
        initAccountsTableWithKeys(pathDataDb, extfvks )

    override fun initAccountsTable(
        viewingKey: String,
        numberOfAccounts: Int
    ) = initAccountsTableWithKeys(pathDataDb, viewingKey, numberOfAccounts)

    override fun initBlocksTable(
        height: Int,
        hash: String,
        time: Long,
        saplingTree: String
    ): Boolean {
        birthdayHeight = height
        return initBlocksTable(pathDataDb, height, hash, time, saplingTree)
    }

    override fun getAddress(account: Int) = getAddress(pathDataDb, account)

    override fun getBalance(account: Int) = getBalance(pathDataDb, account)

    override fun getVerifiedBalance(account: Int) = getVerifiedBalance(pathDataDb, account)

    override fun getReceivedMemoAsUtf8(idNote: Long) =
        getReceivedMemoAsUtf8(pathDataDb, idNote)

    override fun getSentMemoAsUtf8(idNote: Long) = getSentMemoAsUtf8(pathDataDb, idNote)

    override fun validateCombinedChain() = validateCombinedChain(pathCacheDb, pathDataDb, chain_network_string)

    override fun rewindToHeight(height: Int) = rewindToHeight(pathDataDb, height, chain_network_string)

    override fun scanBlocks(limit: Int): Boolean {
        return if (limit > 0) {
            scanBlockBatch(pathCacheDb, pathDataDb, limit, chain_network_string)
        } else {
            scanBlocks(pathCacheDb, pathDataDb, chain_network_string)
        }
    }

    override fun decryptAndStoreTransaction(tx: ByteArray) = decryptAndStoreTransaction(pathDataDb, tx, chain_network_string)

    override fun createToAddress(
        account: Int,
        extsk: String,
        to: String,
        value: Long,
        sapling: String,
        memo: ByteArray?
    ): Long = createToAddress(
        pathDataDb,
        account,
        extsk,
        to,
        value,
        sapling,
        memo ?: ByteArray(0),
        "${pathParamsDir}/$SPEND_PARAM_FILE_NAME",
        "${pathParamsDir}/$OUTPUT_PARAM_FILE_NAME",
        chain_network_string
    )

    override fun deriveSpendingKeys(seed: ByteArray, numberOfAccounts: Int) =
        deriveExtendedSpendingKeys(seed, numberOfAccounts)

    override fun deriveViewingKeys(seed: ByteArray, numberOfAccounts: Int) =
        deriveExtendedFullViewingKeys(seed, numberOfAccounts)

    override fun deriveViewingKey(spendingKey: String) = deriveExtendedFullViewingKey(spendingKey)

    override fun deriveAddress(viewingKey: String) = deriveAddressFromViewingKey(viewingKey)

    override fun isValidShieldedAddr(addr: String) =
        isValidShieldedAddress(addr)

    override fun isValidTransparentAddr(addr: String) =
        isValidTransparentAddress(addr)

    /**
     * Exposes all of the librustzcash functions along with helpers for loading the static library.
     */
    companion object {
        private var loaded = false

        fun load() {
            // It is safe to call these things twice but not efficient. So we add a loose check and
            // ignore the fact that it's not thread-safe.
            if (!loaded) {
                twig("Loading RustBackend") {
                    loadRustLibrary()
                    initLogs()
                }
            }
        }

        /**
         * The first call made to this object in order to load the Rust backend library. All other
         * external function calls will fail if the libraries have not been loaded.
         */
        private fun loadRustLibrary() {
            try {
                System.loadLibrary("zcashwalletsdk")
                loaded = true
            } catch (e: Throwable) {
                twig("Error while loading native library: ${e.message}")
            }
        }


        //
        // External Functions
        //

        @JvmStatic private external fun initDataDb(dbDataPath: String): Boolean

        @JvmStatic private external fun initAccountsTableWithKeys(
            dbDataPath: String,
            viewingKey: String,
            accounts: Int
        ): Array<String>

    @JvmStatic private external fun initAccountsTableWithKeys(
        dbDataPath: String,
        extfvk: Array<String>
    )

        @JvmStatic private external fun initBlocksTable(
            dbDataPath: String,
            height: Int,
            hash: String,
            time: Long,
            saplingTree: String
        ): Boolean

        @JvmStatic private external fun getAddress(dbDataPath: String, account: Int): String

        @JvmStatic private external fun isValidShieldedAddress(addr: String): Boolean

        @JvmStatic private external fun isValidTransparentAddress(addr: String): Boolean

        @JvmStatic private external fun getBalance(dbDataPath: String, account: Int): Long

        @JvmStatic private external fun getVerifiedBalance(dbDataPath: String, account: Int): Long

        @JvmStatic private external fun getReceivedMemoAsUtf8(dbDataPath: String, idNote: Long): String

        @JvmStatic private external fun getSentMemoAsUtf8(dbDataPath: String, idNote: Long): String

        @JvmStatic private external fun validateCombinedChain(dbCachePath: String, dbDataPath: String, chain_network_string: String): Int

        @JvmStatic private external fun rewindToHeight(dbDataPath: String, height: Int, chain_network_string: String): Boolean

        @JvmStatic private external fun scanBlocks(dbCachePath: String, dbDataPath: String, chain_network_string: String): Boolean

        @JvmStatic private external fun scanBlockBatch(dbCachePath: String, dbDataPath: String, limit: Int, chain_network_string: String): Boolean

        @JvmStatic private external fun decryptAndStoreTransaction(dbDataPath: String, tx: ByteArray, chain_network_string: String)

        @JvmStatic private external fun createToAddress(
            dbDataPath: String,
            account: Int,
            extsk: String,
            to: String,
            value: Long,
            sapling: String,
            memo: ByteArray,
            spendParamsPath: String,
            outputParamsPath: String,
            chain_network_string: String
        ): Long

        @JvmStatic private external fun initLogs()

        @JvmStatic private external fun deriveExtendedSpendingKeys(seed: ByteArray, numberOfAccounts: Int): Array<String>

        @JvmStatic private external fun deriveExtendedFullViewingKeys(seed: ByteArray, numberOfAccounts: Int): Array<String>

        @JvmStatic private external fun deriveExtendedFullViewingKey(spendingKey: String): String

        @JvmStatic private external fun deriveAddressFromViewingKey(key: String): String
    }
}
