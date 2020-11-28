package se.magictechnology.kotlinbilling

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.android.billingclient.api.*

class MainActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null

    var skudetails : List<SkuDetails>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        billingClient = BillingClient.newBuilder(this).setListener(this).enablePendingPurchases().build()
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode ==  BillingClient.BillingResponseCode.OK) {

                }
            }
            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })

        findViewById<Button>(R.id.getProdBtn).setOnClickListener {
            querySkuDetails()
        }

        findViewById<Button>(R.id.buyBtn).setOnClickListener {
            var buyProduct = skudetails!![0]

            //buyProduct = skudetails!!.filter { it.sku == "premium" }.first()

            val billingFlowParams = BillingFlowParams.newBuilder().setSkuDetails(buyProduct).build()

            if(billingClient != null)
            {
                billingClient!!.launchBillingFlow(this, billingFlowParams)
            }
        }

    }

    fun querySkuDetails() {
        if(billingClient == null)
        {
            return
        }
        val skuList = ArrayList<String>()
        skuList.add("android.test.purchased")

        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
        billingClient!!.querySkuDetailsAsync(params.build()) { responseCode, skuDetailsList ->
            if (responseCode.responseCode == BillingClient.BillingResponseCode.OK) {

                Log.i("DEBUGBILLING", "GOT PRODUCTS " + skuDetailsList!!.size.toString())

                this.skudetails = skuDetailsList

                if(this.skudetails!!.size > 0)
                {
                    runOnUiThread {
                        findViewById<TextView>(R.id.productNameTV).text = this.skudetails!![0].title
                        findViewById<TextView>(R.id.productPriceTV).text = this.skudetails!![0].price

                    }
                }



            } else {
                // TODO: Error no response
            }
        }
    }

    override fun onPurchasesUpdated(p0: BillingResult, p1: MutableList<Purchase>?) {
        Log.i("DEBUGBILLING", "onPurchasesUpdated")

        p0?.let {
            if(it.responseCode == BillingClient.BillingResponseCode.OK)
            {
                p1?.let {  plist ->
                    plist.firstOrNull()?.let {  thepurchase ->
                        if (!thepurchase.isAcknowledged) {
                            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(thepurchase.purchaseToken)

                            if(billingClient != null)
                            {
                                billingClient!!.acknowledgePurchase(acknowledgePurchaseParams.build()) {
                                    Log.i("DEBUGBILLING", "OK BUY")

                                    if(billingClient != null)
                                    {
                                        billingClient!!.endConnection()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun checkHistory()
    {
        billingClient = BillingClient.newBuilder(this).setListener(this).enablePendingPurchases().build()
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode ==  BillingClient.BillingResponseCode.OK) {
                    billingClient!!.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS, { billingResultQuery, purchasesList ->
                        if (billingResultQuery.responseCode == BillingClient.BillingResponseCode.OK) {

                            val purchasesResult: Purchase.PurchasesResult = billingClient!!.queryPurchases(BillingClient.SkuType.INAPP)

                            for (purchase in purchasesResult!!.purchasesList!!) {
                                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                                        .setPurchaseToken(purchase.purchaseToken)

                                Log.i("BILLINGDEBUG", "HAVE BOUGHT "+purchase.sku)


                            }



                        }
                    })
                } else {

                }
            }
            override fun onBillingServiceDisconnected() {

            }
        })
    }


}