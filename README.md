<img src="https://github.com/PHELAT/Poolakey/raw/master/asset/Poolakey.jpg"/><br/>
[![](https://api.bintray.com/packages/m4hdi/Poolakey/Poolakey/images/download.svg)](https://bintray.com/beta/#/m4hdi/Poolakey?tab=packages)  
-
Android In-App Billing for [Cafe Bazaar](https://cafebazaar.ir/?l=en) App Store
### Dependency
```groovy
dependencies {
    implementation "com.phelat:poolakey:[latest_version]"
}
```
### How to use
For more information regarding the usage of Poolakey, Please check out the [wiki](https://github.com/PHELAT/Poolakey/wiki) page.
### Sample
There is a fully functional sample application which demonstrates the usage of Poolakey, All you have to do is cloning the project and running the [app](https://github.com/PHELAT/Poolakey/tree/master/app) module.
### Reactive Extension Support
Yes, You've read that right! Poolakey supports Reactive Extension framework. Just add it's dependency in your `build.gradle` file:
```groovy
dependencies {
    implementation "com.phelat:poolakey-rx:[latest_version]"
}
```
And instead of using Poolakey's callbacks, use the reactive types:
```kotlin
payment.getPurchasedProducts()
    .subscribe({ purchasedProducts ->
        ...
    }, { throwable ->
        ...
    })
```
For more information regarding the reactive extension support, Please check out the [wiki](https://github.com/PHELAT/Poolakey/wiki) page.
