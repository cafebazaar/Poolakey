package ir.cafebazaar.poolakey.entity

data class PurchaseInfo(
    val orderId: String,
    val purchaseToken: String,
    val payload: String,
    val packageName: String,
    val purchaseState: PurchaseState,
    val purchaseTime: Long,
    val productId: String,
    val originalJson: String,
    val dataSignature: String
)
