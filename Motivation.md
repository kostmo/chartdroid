There [exists](http://code.google.com/p/achartengine/) a `.jar`-based library for charting in Android (from which **`ChartDroid`** branched), so why take the separate `.apk` approach?

  * smaller client application size
  * lets you use data directly from your database with SQLite queries in your Content Provider
  * receives updates (visual improvements or bugfixes) independently of the client application
  * uses resource files (XML layouts especially) without polluting resource namespace of client application (c.f. "[library projects](http://developer.android.com/guide/developing/eclipse-adt.html#libraryProject)")
  * Can be embedded in a TabActivity