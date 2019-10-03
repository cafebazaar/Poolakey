package com.phelat.poolakey

data class PurchaseRequest(
    val sku: String,
    val requestCode: Int,
    val extraData: String = ""
)
