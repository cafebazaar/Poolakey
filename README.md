<img src="https://github.com/PHELAT/Poolakey/raw/master/asset/Poolakey.jpg"/><br/>
[![Build Status](https://travis-ci.com/cafebazaar/Poolakey.svg?branch=master)](https://travis-ci.com/cafebazaar/Poolakey)
[![CodeFactor](https://www.codefactor.io/repository/github/cafebazaar/poolakey/badge)](https://www.codefactor.io/repository/github/cafebazaar/poolakey) [![](https://api.bintray.com/packages/cafebazaar/Poolakey/Poolakey/images/download.svg)](https://bintray.com/beta/#/cafebazaar/Poolakey?tab=packages)
-
Android In-App Billing SDK for [Cafe Bazaar](https://cafebazaar.ir/?l=en) App Store.
## Getting Started
To start working with Poolakey, you need to add its dependency into your `build.gradle` file:
### Dependency
```groovy
dependencies {
    implementation "com.github.cafebazaar.Poolakey:poolakey:[latest_version]"
}
```

Then you need to add jitpack as your maven repository in `settings.gradle`  file:

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url "https://jitpack.io" }
        jcenter()
    }
}
```

### How to use
For more information regarding the usage of Poolakey, please check out the [wiki](https://github.com/cafebazaar/Poolakey/wiki) page.
### Sample
There is a fully functional sample application that demonstrates the usage of Poolakey, all you have to do is cloning the project and running the [app](https://github.com/cafebazaar/Poolakey/tree/master/app) module.
### Reactive Extension Support
Yes, you've read that right! Poolakey supports Reactive Extension framework. Just add its dependency into your `build.gradle` file:
```groovy
dependencies {
    // RxJava 3
    implementation "com.github.cafebazaar.Poolakey:poolakey-rx3:[latest_version]"
    // RxJava 2
    implementation "com.github.cafebazaar.Poolakey:poolakey-rx:[latest_version]"
}
```

And instead of using Poolakey's callbacks, use the reactive fuctions:
```kotlin
payment.getPurchasedProducts()
    .subscribe({ purchasedProducts ->
        ...
    }, { throwable ->
        ...
    })
```
