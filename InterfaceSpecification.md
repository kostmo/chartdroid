This document covers the basics to get started with charts. See AdvancedFeatures for topics not covered here.



# Content Provider Overview #
Using Content Providers may be intimidating at first, but learning them will help you [write interoperable applications](http://android-developers.blogspot.com/2009/11/integrating-application-with-intents.html).


## Event sequence ##
The following steps are approximately what happens under the hood when your application wants to plot data with **`ChartDroid`**.
  1. Application launches an `Intent` with `ACTION_VIEW` and a `Uri` that will be used to query its `ContentProvider`
  1. Android calls the content provider's `getType()` method
  1. Android resolves the appropriate **`ChartDroid`** `Activity` as an `Intent` match for the combination of `ACTION_VIEW` and the returned data type
  1. **`ChartDroid`** activity is launched
  1. **`ChartDroid`** initiates a query on the specified `Uri` for the data that should be plotted
  1. Android uses the "authority" portion of the `Uri` to instantiate the correct `ContentProvider` (yours), according to your application's `AndroidManifest.xml`
  1. **`ChartDroid`** receives data from your `ContentProvider` and plots it onscreen

To make this process go as planned, you must
  1. Subclass `ContentProvider` and handle the `getType()` and `query()` methods
    * `getType()` should return [one of these type strings](#Data_type.md)
    * `query()` can either directly query your SQLite database, or generate data on the fly with `MatrixCursor`. If you intend to use this `ContentProvider` for multiple types of queries, you can use a `UriMatcher` to choose between different queries based on the "path" of your `Uri`, or you can devise your own `switch` cases based on [query parameters added](http://developer.android.com/reference/android/net/Uri.Builder.html#appendQueryParameter%28java.lang.String,%20java.lang.String%29) to the `Uri`.
  1. Declare the content provider in your `AndroidManifest.xml` with an "authority" string of your choosing.  Usually it should start with your application's package name to help ensure that it is unique system-wide.
  1. Construct a `Uri` using the aforementioned authority


# Charts in version 2.x #
## Terms ##
In this document:
  * The term **datum** refers to a single "point" on the graph, which may be multidimensional.
  * The term **datum component** is a single scalar value for one of the dimensions (axes) of a **datum**.


## Data type ##
There are several ways to present data with **`ChartDroid`**. To give the user a choice among all of them, the [getType()](http://developer.android.com/reference/android/content/ContentProvider.html#getType%28android.net.Uri%29) method of your Content Provider should return the string:
`vnd.android.cursor.dir/vnd.com.googlecode.chartdroid.graphable`

For event data (values plotted against a time axis; timelines), `getType()` should return:
`vnd.android.cursor.dir/vnd.com.googlecode.chartdroid.timeline`

To limit the presentation options, [categories may be added](http://developer.android.com/reference/android/content/Intent.html#addCategory%28java.lang.String%29) to the Intent:
  * **Radial:** `com.googlecode.chartdroid.intent.category.PIE_CHART`
    * Pie charts and donut charts
  * **X-Y:** `com.googlecode.chartdroid.intent.category.XY_CHART`
    * Bar charts, line charts


## Content Provider ##
Your Content Provider should handle three types of queries:
  * `data`
  * `series`
  * `axes`

When displaying a chart, these strings will be appended **as a value of the "aspect" query parameter** of the source Uri as a "callback" to retrieve the various chart data. Therefore, a Content Provider should examine the "aspect" parameter of the Uri and serve the appropriate columns below. (Using query parameters for this instead of the last path segment permits use of the `ContentUris.parseId()` function.)
```
if ("axes".equals(uri.getQueryParameter("aspect") )) {
   ... // serve the axes metadata
} else if ("series".equals(uri.getQueryParameter("aspect") )) {
   ... // serve the series metadata
} else {
   ... // serve the data 
}
```

**Note:** The data to chart may be generated dynamically in the client by populating a [MatrixCursor](http://developer.android.com/reference/android/database/MatrixCursor.html).  If your data already exists in an SQLite-backed database, it is simple to translate the column names.  Use a query such as:
```
String[] projection = new String[] {
    "ROWID AS " + BaseColumns._ID,    // ROWID is the implicit Primary Key generator in SQLite
    MY_AXIS_INDEX_COLUMN + " AS " + "COLUMN_AXIS_INDEX",
    MY_SERIES_INDEX_COLUMN + " AS " + "COLUMN_SERIES_INDEX",
    ...
};

db.query(MY_TABLE, projection, ...);
```

**Suggestion:** You may want to create a `VIEW` in your database with the column names already transformed to the ones **`ChartDroid`** needs. Android lacks facilities to easily perform column name translation within a `ContentProvider`.

### "data" Columns ###

  * Datum component values will be interpreted as `double` type.
  * Datum labels may be specified or left `null`.

Developers may choose between the scheme below or an [alternate column scheme](AlternateColumnScheme.md). Currently the "`_id`" column is ignored in both schemes.

#### Standard Column Scheme ####

In this scheme, all datum components are in the same row.  This scheme is assumed if there exist any column names that start with the prefix "`AXIS_`".  For the purpose of [assigning axis properties](#%22axes%22_Columns.md), axes are ordered lexographically.

| **Column name** | **Data type** |
|:----------------|:--------------|
| `_id` | `long` |
| `COLUMN_SERIES_INDEX` | `int` |
| `COLUMN_DATUM_LABEL` | `String` |
| `AXIS_A` | `double` |
| `AXIS_B` | `double` |
| `AXIS_C` | `double` |
| `...` | `double` |

Here is an example results table that your application might produce in response to a "data" query:

| `_id` | `COLUMN_SERIES_INDEX` | `COLUMN_DATUM_LABEL` | `AXIS_X`  | `AXIS_Y` |
|:------|:----------------------|:---------------------|:----------|:---------|
| 3546 | 0 | "beef" | 17 | 4 |
| 3548 | 0 | "chicken" | 24 | 6 |
| 3550 | 0 | "pork" | 13 | 8 |
| 3552 | 1 | "apples" | 30 | 10 |
| 3554 | 1 | "oranges" | 12 | 4 |
| 3556 | 1 | "pears" | 5 | 2 |


### "series" Columns ###
The "series" query provides labels and other properties for each data series. All columns are optional.

| **Column name** | **Data type** | **Default** |
|:----------------|:--------------|:------------|
| `_id` | `long` |
| `COLUMN_SERIES_LABEL` | `String` | "Untitled" |
| `COLUMN_SERIES_COLOR` | `int` | depends... |
| `COLUMN_SERIES_MARKER`<sup>†</sup> | `int` | circle |
| `COLUMN_SERIES_LINE_STYLE`<sup>‡</sup> | `int` | solid |
| `COLUMN_SERIES_LINE_THICKNESS` | `float` | 2 |
| `COLUMN_SERIES_AXIS_SELECT` | `int` | 0 |

<sup>†</sup>Marker styles:
| **Index** | **Description** |
|:----------|:----------------|
| 0 | x |
| 1 | circle |
| 2 | triangle |
| 3 | square |
| 4 | diamond |
| 5 | point |

<sup>‡</sup>Line styles:
| **Index** | **Description** |
|:----------|:----------------|
| 0 | none |
| 1 | dotted |
| 2 | dashed |
| 3 | solid |

### "axes" Columns ###
The "axes" query provides labels for each axis.

| **Column name** | **Data type** | **Optional** |
|:----------------|:--------------|:-------------|
| `_id` | `long` | Y |
| `COLUMN_AXIS_LABEL` | `String` | N |
| `COLUMN_AXIS_MIN` (todo) | `double` | Y |
| `COLUMN_AXIS_MAX` (todo) | `double` | Y |
| `COLUMN_AXIS_ROLE`<sup>†</sup> (todo) | `int` | Y |

If the columns `COLUMN_AXIS_MIN` and `COLUMN_AXIS_MAX` are not present or their value is `null`, the axis min and max are calculated automatically, and the axis limits are set to **ten percent** above and below the data range. In the case of a single value, the default axis limits are set to **one unit** above and below the value.

<sup>†</sup>COLUMN\_AXIS\_ROLE indicates how the axis values should be expressed, using an integer index from the following table:
| **Index** | **Value expression method** | **Notes** |
|:----------|:----------------------------|:----------|
| `null` | default | (see below) |
| `0` | Horizontal axis | abscissa |
| `1` | Vertical axis (Primary or Secondary) | ordinate |
| `2` | Marker size | Axis range is scaled within 50% of its base marker size |
| `3` | Marker hue | Axis range is scaled to the range 0 to 360 |

If the field is `null` or the column is omitted entirely, the default sequence of value expression methods is used:
  1. Horizontal axis
  1. Vertical axis
  1. Marker size

If only a single axis is present in the data, then the value will be expressed along the Vertical axis, with a natural number sequence indexing the values along the Horizontal axis.

## Intent Extras ##

The only really "mandatory" query in your Content Provider is `data`; other aspects of the chart may be delivered through Intent extras.  The Intent extras, if present, take precedence over the Content Provider.

| **Key** | **Data type** | **default value** |
|:--------|:--------------|:------------------|
| `Intent.EXTRA_TITLE` | `String` |
| `com.googlecode.chartdroid.intent.extra.SERIES_LABELS` | `String[]` |
| `com.googlecode.chartdroid.intent.extra.SERIES_COLORS` | `int[]` |
| `com.googlecode.chartdroid.intent.extra.SERIES_MARKERS` | `int[]` |
| `com.googlecode.chartdroid.intent.extra.SERIES_AXIS_SELECTION` | `int[]` |
| `com.googlecode.chartdroid.intent.extra.SERIES_LINE_STYLES` | `int[]` |
| `com.googlecode.chartdroid.intent.extra.SERIES_LINE_THICKNESSES` | `float[]` |
| `com.googlecode.chartdroid.intent.extra.AXIS_TITLES` | `ArrayList<String>` |
| `com.googlecode.chartdroid.intent.extra.FORMAT_STRING_X` | `String` | "%d |
| `com.googlecode.chartdroid.intent.extra.FORMAT_STRING_Y` | `String` | "%.1f%%" |
| `com.googlecode.chartdroid.intent.extra.AXIS_VISIBLE_X` | `boolean` | true |
| `com.googlecode.chartdroid.intent.extra.AXIS_VISIBLE_Y` | `boolean` | true |
| `com.googlecode.chartdroid.intent.extra.RAINBOW_COLORS` | `boolean` | false |



# Charts in version 1.x #
## Actions ##
  * com.googlecode.chartdroid.intent.action.PLOT
## Categories ##
  * com.googlecode.chartdroid.intent.category.PIE\_CHART
  * com.googlecode.chartdroid.intent.category.CALENDAR
## Extras ##

### Pie Chart ###
| **Key** | **Data type** | **Required?**|
|:--------|:--------------|:|
| `com.googlecode.chartdroid.intent.extra.DATA` | `int[]` | Y |
| `com.googlecode.chartdroid.intent.extra.LABELS` | `String[]` | N |
| `com.googlecode.chartdroid.intent.extra.COLORS` | `int[]` | N |
| `Intent.EXTRA_TITLE` | `String` | N |

# Defining Java Constants #
It is handy to have these long strings defined as constants in your application.  An easy way to do this, and at the same time keep your application up-to-date against the spec, is to use svn "externals".

In your application's `src/` directory, create the directory tree
`com/googlecode/chartdroid`.
Then `cd` to the leaf directory you just created, and set the externals property to keep your project in sync with the **`chartdroid`** definitions.

```
cd src
mkdir -p com/googlecode/chartdroid
svn add com
cd com/googlecode/chartdroid
svn propset svn:externals 'core http://chartdroid.googlecode.com/svn/trunk/core/src/com/googlecode/chartdroid/core' .
cd ../../..
svn ci -m "added constants defined in external repo"
svn up
```

Now when you call `svn up` in your project, it automatically fetches the contants I've defined for use with **`chartdroid`**.