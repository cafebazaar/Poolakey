package com.phelat.poolakey

data class PurchaseRequest(
    val productId: String,
    val requestCode: Int,
    val payload: String
)
