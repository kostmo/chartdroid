## Secondary Y-Axis ##

To use a secondary Y-Axis, you must specify such on a series, either through an Intent extra (`com.googlecode.chartdroid.intent.extra.SERIES_AXIS_SELECTION`), or the `COLUMN_SERIES_AXIS_SELECT` column in the `series` aspect of your ContentProvider.

Additionally, you must specify the Vertical Axis index (with the value `1`) for at least two Axes, via the `COLUMN_AXIS_EXPRESSION` column.

Finally, you may use the `com.googlecode.chartdroid.intent.extra.FORMAT_STRING_Y_SECONDARY` Intent extra to specify the format of this secondary axis.

![http://chartdroid.googlecode.com/svn/graphics/screenshots/devrev/revenue_price_overlay.png](http://chartdroid.googlecode.com/svn/graphics/screenshots/devrev/revenue_price_overlay.png)