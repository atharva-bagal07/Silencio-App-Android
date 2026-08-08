package io.silencio.app.billing

import android.app.Activity
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val ENTITLEMENT_ID = "premium"
private const val OFFERING_ID = "default"

@Singleton
class PurchaseManager @Inject constructor() {

    suspend fun getLifetimePackage(): Package? =
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.getOfferingsWith(
                onError = { cont.resume(null) },
                onSuccess = { offerings ->
                    val pkg = offerings[OFFERING_ID]?.lifetime
                    cont.resume(pkg)
                }
            )
        }

    suspend fun purchaseLifetime(activity: Activity): PurchaseResult =
        suspendCancellableCoroutine { cont ->
            val pkg = null // fetched separately
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error ->
                    cont.resume(PurchaseResult.Error(error.message))
                },
                onSuccess = { offerings ->
                    val lifetimePkg = offerings[OFFERING_ID]?.lifetime
                    if (lifetimePkg == null) {
                        cont.resume(PurchaseResult.Error("Product not found"))
                        return@getOfferingsWith
                    }
                    // Build the purchase parameters wrapper required by the SDK
                    val purchaseParams = PurchaseParams.Builder(activity, lifetimePkg).build()

                    Purchases.sharedInstance.purchaseWith(
                        purchaseParams = purchaseParams,
                        onError = { error, userCancelled ->
                            if (userCancelled) cont.resume(PurchaseResult.Cancelled)
                            else cont.resume(PurchaseResult.Error(error.message))
                        },
                        onSuccess = { _, customerInfo ->
                            val isPremium =
                                customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
                            if (isPremium) cont.resume(PurchaseResult.Success)
                            else cont.resume(PurchaseResult.Error("Entitlement not active"))
                        }
                    )

                }
            )
        }

    suspend fun restorePurchases(): PurchaseResult =
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.restorePurchasesWith(
                onError = { error ->
                    cont.resume(PurchaseResult.Error(error.message))
                },
                onSuccess = { customerInfo ->
                    val isPremium = customerInfo
                        .entitlements[ENTITLEMENT_ID]?.isActive == true
                    if (isPremium) cont.resume(PurchaseResult.Success)
                    else cont.resume(PurchaseResult.Error("No active purchase found"))
                }
            )
        }

    suspend fun checkEntitlement(): Boolean =
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = { cont.resume(false) },
                onSuccess = { customerInfo ->
                    val isPremium = customerInfo
                        .entitlements[ENTITLEMENT_ID]?.isActive == true
                    cont.resume(isPremium)
                }
            )
        }
}

sealed class PurchaseResult {
    object Success : PurchaseResult()
    object Cancelled : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}