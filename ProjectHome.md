**`ChartDroid`** is an [Intent](http://developer.android.com/reference/android/content/Intent.html)-based "library application" for static chart and graph generation on Android.  It can graph/plot/display numerical data in [many representations](Screenshots.md).  As a developer, you need only to have your users [install the library](http://code.google.com/p/chartdroid/source/browse/trunk/client/src/com/googlecode/chartdroid/demo/Market.java#35), then implement a [Content Provider](http://developer.android.com/guide/topics/providers/content-providers.html) according to [this simple specification](InterfaceSpecification.md).

**`ChartDroid`** is consistent with the "Unix toolkit" philosophy of building small, interoperable tools. Some developers may prefer to import graphing libraries (`.jar`s) into their project; for that they should [look elsewhere](http://code.google.com/p/achartengine/). Having **`ChartDroid`** exist as an independent application conserves storage space and eliminates redundant code. A [minimal example](http://code.google.com/p/chartdroid/source/browse/#svn/trunk/example_apps/minimal_linechart) of using **`ChartDroid`** with a Content Provider is available.

## Documentation ##
  * InterfaceSpecification
  * ChartFeatures

## Users ##
Is your app using **`ChartDroid`**? Feel free to make a plug in the [discussion group](http://groups.google.com/group/chartdroid-users), and I'll [add your app to the list](AppsUsingChartDroid.md).


## Screenshots ##
See [more screenshots here](Screenshots.md).

![http://chartdroid.googlecode.com/svn/graphics/devrev/presentation/images/android_screenshots/app_revenue_plots/25bins.png](http://chartdroid.googlecode.com/svn/graphics/devrev/presentation/images/android_screenshots/app_revenue_plots/25bins.png)