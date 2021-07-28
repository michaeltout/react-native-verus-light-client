[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk](../index.md) / [SdkSynchronizer](index.md) / [getAddress](./get-address.md)

# getAddress

`suspend fun getAddress(accountId: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)

Overrides [Synchronizer.getAddress](../-synchronizer/get-address.md)

Gets the address for the given account.

### Parameters

`accountId` - the optional accountId whose address is of interest. By default, the first
account is used.