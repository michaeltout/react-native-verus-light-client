
# react-native-verus-light-client

## Getting started

`$ npm install git+https://github.com/VerusCoin/react-native-verus-light-client.git --save`

### Mostly automatic installation

`$ react-native link react-native-verus-light-client`

## Usage
javascript
`import VerusLightClient from 'react-native-verus-light-client';

## Installation on Android
#### Improtant: Do you have Rust installed? Without rust the library will not work

#### Improtant: Do you have the NDK installed? check the version. the version should be : 20.0.5594570

To install this on android, add this package to your package.JSON in dependancies.
make sure that the version is at least: 417f646d73bf2434c1ff64a69569dd068af07502, this commit hash.
once you have yarn installed this. Open properties.gradle. This file you can find in android/settings.gradle. Underneath the rootname line add:

`include ':react-native-verus-light-client'
project(':react-native-verus-light-client').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-verus-light-client/android')
include ':greeting'
project(':greeting').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-verus-light-client/greeting')`

Now it should compile. If you still get the error ':greeting' project not found you have not correctly done the above step. If you get the error 'compactblockprocessor' missing or something like this, it means you don't have rust installed or the arm is missing. If you get a cant find 'react-native-verus-light-cient' error you have probably done something in your settings.gralde in addtion to the step above that is having a unwanted interaction with what we are doing.

Have fun with the module.


|VerusLightClient Member Functions                                                                                                                                                                                                                                                       |                             |||||
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
|Purpose                                                                                                                                                                                                                                                                                 |VerusLightClient function    |Parameters                                                                                                                                                    |Returns                                                                                                                                  |Action                                                                                                                                                                                                 |                                                                                                                   |
|Create/Initialize wallet                                                                                                                                                                                                                                                                |VerusLightClient.addWallet   |(coinId: String, coinProtocol: String, walletHostAddress: String, walletHostPort: String, accountHash: String, numAddresses: Int, seed: String)               |A JS Promise resolving to 'true' if success, a rejected JS promise if error                                                              |Creates a wallet with number of accounts = numAddresses (defaults to 100), and starts a synchronizer, service, and initializer instance.                                                               |                                                                                                                   |
|Request wallet function                                                                                                                                                                                                                                                                 |VerusLightClient.request     |(callId: Int, method: String, params: String[ ])                                                                                                              |A JS Promise NEVER REJECTING, that always resolves to a JSON-RPC response object, as specified here https://www.jsonrpc.org/specification|Calls the speicifed request method with the specified parameters                                                                                                                                       |                                                                                                                   |
|Start Syncing                                                                                                                                                                                                                                                                           |VerusLightClient.startSync   |(coinId: String, coinProtocol: String, accountHash: String)                                                                                                   |A JS Promise resolving to 'true' if success, a rejected JS promise if error                                                              |Starts synchronizing with the blockchain by calling blockProcessor.start()                                                                                                                             |                                                                                                                   |
|Stop Syncing                                                                                                                                                                                                                                                                            |VerusLightClient.stopSync    |(coinId: String, coinProtocol: String, accountHash: String)                                                                                                   |A JS Promise resolving to 'true' if success, a rejected JS promise if error                                                              |Stops synchronizing with the blockchain by calling blockProcessor.stop()                                                                                                                               |                                                                                                                   |
|Create Wallet                                                                                                                                                                                                                                                                           |VerusLightClient.createWallet|(coinId: String, coinProtocol: String, walletHostAddress: String, walletHostPort: String, accountHash: String, numAddresses: Int, seed: String, birthday: Int)|                                                                                                                                         |                                                                                                                                                                                                       |                                                                                                                   |
|Open Wallet                                                                                                                                                                                                                                                                             |VerusLightClient.openWallet  |(coinId: String, coinProtocol: String, accountHash: String)                                                                                                   |A JS Promise resolving to 'true' if success, a rejected JS promise if error                                                              |Opens and initializes a wallet (DOES NOT START SYNCING YET)                                                                                                                                            |                                                                                                                   |
|close Wallet                                                                                                                                                                                                                                                                            |VerusLightClient.closeWallet |(coinId: String, coinProtocol: String, accountHash: String)                                                                                                   |A JS Promise resolving to 'true' if success, a rejected JS promise if error                                                              |Stops synchronizer and stops syncing blockchain                                                                                                                                                        |                                                                                                                   |
|Delete Wallet                                                                                                                                                                                                                                                                           |VerusLightClient.deleteWallet|(coinId: String, coinProtocol: String, accountHash: String)                                                                                                   |A JS Promise resolving to 'true' if success, a rejected JS promise if error                                                              |Closes wallet and deletes DB information                                                                                                                                                               |                                                                                                                   |
|                                                                                                                                                                                                                                                                                        |                             |                                                                                                                                                              |                                                                                                                                         |                                                                                                                                                                                                       |                                                                                                                   |
|NOTE: All param arrays passed to the request function need to have [coinId: String, coinProtocol: String,  accountHash: String] as their first three indexes, the arrays below start at index 3 and move on. Calls without at least 3 indexes in their array are resolved with an error.|                             |                                                                                                                                                              |                                                                                                                                         |                                                                                                                                                                                                       |                                                                                                                   |
|Request Methods                                                                                                                                                                                                                                                                         |                             |                                                                                                                                                              |                                                                                                                                         |                                                                                                                                                                                                       |                                                                                                                   |
|Purpose                                                                                                                                                                                                                                                                                 |Method name                  |Param Array                                                                                                                                                   |Result                                                                                                                                   |Success Result Example                                                                                                                                                                                 |Error Response example                                                                                             |
|To get the current block height                                                                                                                                                                                                                                                         |getblockcount                |[ ]                                                                                                                                                           |The current block height or an error                                                                                                     |{"result": 834133, "error": null, "id": 1, "jsonrpc": "2.0"}                                                                                                                                           |{"result": null, "error": "getblockcount expected no params, received 3.", "id": 1, "jsonrpc": "2.0"}              |
|To list addresses associated with an account                                                                                                                                                                                                                                            |listaddresses                |[ ]                                                                                                                                                           |An array of strings containing every address of every account created when the wallet was created                                        |{"result": ["zaddr", "zaddr"], "error": null, "id": 1, "jsonrpc": "2.0"}                                                                                                                               |{"result": null, "error": "listaddresses expected no params, received 3.", "id": 1, "jsonrpc": "2.0"}              |
|To get the total private wallet balance                                                                                                                                                                                                                                                 |getprivatebalance            |[ ]                                                                                                                                                           |The total private wallet balance                                                                                                         |{"result": {total: 40.34234, confirmed: 30.43242}, "error": null, "id": 1, "jsonrpc": "2.0"}                                                                                                           |{"result": null, "error": "getprivatebalance expected boolean as param, received Int", "id": 1, "jsonrpc": "2.0"}  |
|A specific address balance                                                                                                                                                                                                                                                              |z_getbalance                 |[address: String (optional), includePending: boolstring* (optional)]                                                                                          |The total balance of an address                                                                                                          |{"result": 40.34234, "error": null, "id": 1, "jsonrpc": "2.0"}                                                                                                                                         |{"result": null, "error": "z_getbalance expected boolean as second param, received Int", "id": 1, "jsonrpc": "2.0"}|
|To get sync information                                                                                                                                                                                                                                                                 |getinfo                      |[ ]                                                                                                                                                           |An object containing the sync percent, the number of blocks synced, the total number of blocks, and a word describing sync status        |{"result": {"status": "scanning", "percent": 15.160704, "longestchain": 839172, "blocks": 839172}, error: null, "id": 1, "jsonrpc": "2.0"}                                                             |{"result": null, "error": "getinfo expected no params, received 3.", "id": 1, "jsonrpc": "2.0"}                    |
|To list transactions associated with all wallet accounts                                                                                                                                                                                                                                |listprivatetransactions      |[ "pending" OR "cleared" OR "received" OR "sent" OR "all"] (optional)                                                                                         |An array of JSON transaction objects                                                                                                     |{"result": [{"address": "2ei2joffd2", "amount": 15.160704, "category": "sent", "status": "confirmed", time: "341431", "txid": "3242edc2c2", "height": 312312}], error: null, "id": 1, "jsonrpc": "2.0"}|{"result": null, "error": "listtransactions expected max 1 param, received 3.", "id": 1, "jsonrpc": "2.0"}         |
|To send a transaction                                                                                                                                                                                                                                                                   |send                         |[toAddress, fromAddress, amount, memo]                                                                                                                        |An JSON with sucsess.                                                                                                                    |                                                                                                                                                                                                       |                                                                                                                   |
|*all params in the param array are sent as strings, so "true" == true and "false" == false                                                                                                                                                                                              
Identity Calls |These are also request methods|||
Get Identity| get an identity| [identity] | a JSON Identity object | {identity{name:"name", contentmap:{"content":"content", ...}, primaryaddresses:["addresses"], minimumsignatures:INT, privateaddress:"privateaddress", revocationauthoriy:"revocationauthority", recoverauthority:"recoverauthority", flags:INT, version:INT, parent:"parent"}| 
Get Identity with info |get an identity with extra information| [identity] |A JSON object containing an identity and extra info| {identity:{...}, status:"status", cansignfor:"boolstring", canspendfor:"boolstring",  blockheight:INT, txid:"string", vout:INT}| |
