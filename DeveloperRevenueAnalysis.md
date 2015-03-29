![http://chartdroid.googlecode.com/svn/graphics/devrev/promo.png](http://chartdroid.googlecode.com/svn/graphics/devrev/promo.png)



# Introduction #

Note: Requires Android 2.2 (Froyo)

An essential app for independent Android developers and small/medium publishers.  Integrates data from Android Market with Google Checkout to provide sales insight (see [screenshots below](#Screenshots.md)).

## Features ##

  * Get notified of low-rated Market comments
  * Plot revenue across apps over time
  * Quantify how comments affect sales
  * Observe effect of app pricing on sales

|![http://chart.apis.google.com/chart?cht=qr&chs=350x350&chl=market://search%3Fq%3Dpname:com.kostmo.market.revenue&.png](http://chart.apis.google.com/chart?cht=qr&chs=350x350&chl=market://search%3Fq%3Dpname:com.kostmo.market.revenue&.png)|![http://chartdroid.googlecode.com/svn/graphics/devrev/mini_revenue_plot.png](http://chartdroid.googlecode.com/svn/graphics/devrev/mini_revenue_plot.png)|
|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------|

I also threw together some [slides](http://chartdroid.googlecode.com/svn/graphics/devrev/presentation/slides.pdf) about the application architecture.


## Usage ##

This app is best suited for developers with less than ~100K app purchases, since caching data from Google Checkout may take a long time, depending on sales volume.  If synching a large batch of records, you may want to plug in and find wifi.  Caching the ~3K sales records used in the screen grabs below took about two and a half minutes.

Using the app entails:

  1. Specifying a date range to plot
  1. Letting the app download records from Google Checkout
  1. Consolidating (automatically or manually) different versions of the same app
  1. Viewing/sharing the plot

Only transactions marked as "Charged" (as opposed to "Cancelled" or other statuses) on Google Checkout are counted.

Another feature of the app allows you to specify a "rating" threshold for each app to be notified about.  If a user leaves a comment with a rating at or below the threshold, you will be alerted in the notification area.  You can then click the notification to read the latest comments.

[Here's a direct market link if you're browsing on a phone.](http://market.android.com/details?id=com.kostmo.market.revenue)

If you want, you can compile the source yourself:
```
svn checkout http://chartdroid.googlecode.com/svn/trunk/devrev/app developer_revenue_analysis
```

# Limitations/Issues #

## SSL handshake failure ##
Occasionally users have encountered the following error when downloading records:
```
SSL handshake failure: I/O error during system call, Unknown error: 0
```
It may be due to [Issue #2690](http://code.google.com/p/android/issues/detail?id=2690) in Android, which has a fix [in the pipeline](http://code.google.com/p/android/issues/detail?id=2690#c22).  My workaround has been to switch from a 3G connection to a Wi-Fi internet connection.


## Android Device IDs ##
To interface with the Android Market through its API, you must have a valid device ID.  If your phone has been rooted and/or has gone through a factory reset, it is possible for the phone's device ID to one not recognized by the Market.  If this happens you will need to manually input a valid device ID into the application.

A device's ID can be obtained through the following code snippet:
```
String android_id = android.provider.Settings.Secure.getString(
	context.getContentResolver(),
	android.provider.Settings.Secure.ANDROID_ID);
```

## Google Checkout countries ##

Not all countries support the Google Checkout API, and thus do not provide a "Merchant Item Key".  Developers from these countries are out of luck, for now.

# Screenshots #
![http://chartdroid.googlecode.com/svn/graphics/screenshots/devrev/revenue_price_overlay.png](http://chartdroid.googlecode.com/svn/graphics/screenshots/devrev/revenue_price_overlay.png)
![http://chartdroid.googlecode.com/svn/graphics/screenshots/devrev/app_revenue_timelines.png](http://chartdroid.googlecode.com/svn/graphics/screenshots/devrev/app_revenue_timelines.png)
![http://chartdroid.googlecode.com/svn/graphics/screenshots/devrev/revenue_ratings_timeline.png](http://chartdroid.googlecode.com/svn/graphics/screenshots/devrev/revenue_ratings_timeline.png)
| ![http://chartdroid.googlecode.com/svn/graphics/screenshots/devrev/app_revenue_totals.png](http://chartdroid.googlecode.com/svn/graphics/screenshots/devrev/app_revenue_totals.png) | ![http://chartdroid.googlecode.com/svn/graphics/screenshots/devrev/app_consolidator_activity.png](http://chartdroid.googlecode.com/svn/graphics/screenshots/devrev/app_consolidator_activity.png) |
|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|