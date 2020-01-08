package com.phelat.poolakey.request

data class PurchaseRequest(
    val productId: String,
    val requestCode: Int,
    val payload: String? = null
)
