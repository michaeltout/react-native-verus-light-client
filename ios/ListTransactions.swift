//
//  ListTransactions.swift
//  VerusLightClient
//
//  Created by Michael Filip Toutonghi on 11/03/2020.
//  Copyright © 2020 Facebook. All rights reserved.
//

import Foundation
import ZcashLightClientKit

func listZTransactions(wallet: CoinWallet, params: [String], id: Int?, completion: @escaping (Int?, Any?, RequestError?) ->()) {
    var typeParam: String?
    
    if params.count > 1 {
        return completion(id, nil, RequestError.badRequestParams(desc: "listprivatetransactions expected max 1 parameter, received " + String(params.count)))
    } else if params.count == 1 {
        typeParam = params[0]
    }
    
    do {
        let type = typeParam ?? "all"
        let synchronizer = try wallet.getSynchronizer()
        let addressList = try wallet.listAddresses()
        var transactions = [TransactionJson]()
        
        switch type {
        case "cleared":
            transactions = transactions + synchronizer.clearedTransactions.map {
                return CompactTransaction(address: $0.toAddress, amount: Int64($0.value).fromSats(), dbType: "cleared", time: $0.blockTimeInSeconds, txid: $0.transactionEntity.transactionId.toHexStringTxId(), height: $0.minedHeight, memo: $0.memo?.base64EncodedString(), localAddrs: addressList).toJson()
            }
        case "received",
             "all":
            transactions = transactions + synchronizer.receivedTransactions.map {
              return CompactTransaction(address: $0.toAddress, amount: Int64($0.value).fromSats(), dbType: "received", time: $0.blockTimeInSeconds, txid: $0.transactionEntity.transactionId.toHexStringTxId(), height: $0.minedHeight, memo: $0.memo?.base64EncodedString(), localAddrs: addressList).toJson()
            }
            
            if type == "all" {
              fallthrough
            }
        case "sent":
            transactions = transactions + synchronizer.sentTransactions.map {
              return CompactTransaction(address: $0.toAddress, amount: Int64($0.value).fromSats(), dbType: "sent", time: $0.blockTimeInSeconds, txid: $0.transactionEntity.transactionId.toHexStringTxId(), height: $0.minedHeight, memo: $0.memo?.base64EncodedString(), localAddrs: addressList).toJson()
            }
            
            if type == "all" {
              fallthrough
            }
        case "pending":
            var pendingTransactions = synchronizer.pendingTransactions.map {
                return CompactTransaction(address: $0.toAddress, amount: Int64($0.value).fromSats(), dbType: "pending", time: $0.createTime, txid: $0.transactionEntity.transactionId.toHexStringTxId(), height: $0.minedHeight, memo: $0.memo?.base64EncodedString(), localAddrs: addressList).toJson()
            }
            
            // Remove duplicates
            if type == "all" {
                pendingTransactions = pendingTransactions.filter { pendingTx in
                    let pendingTxid: String = ((pendingTx["txid"] ?? nil) ?? "") as! String
                    
                    if pendingTxid == "" {
                        return false
                    }
                    
                    return !(transactions.contains { processedTx in
                        let processedTxid: String = ((processedTx["txid"] ?? nil) ?? "") as! String
                        
                        return processedTxid == pendingTxid
                    })
                }
            }
            
            transactions = transactions + pendingTransactions
        default:
            return completion(id, nil, RequestError.badRequestParams(desc: "Invalid params, for listprivatetransactions, " + type + " is not a valid transaction list type."))
        }
        
        return completion(id, transactions, nil)
    } catch {
        return completion(id, nil, RequestError.internalError(error: error))
    }
}

