**Note:** The [Standard Column Scheme](InterfaceSpecification#Standard_Column_Scheme.md) is easier to use.

Each datum component must be identified by axis and series.  The axis and series numbering scheme is arbitrary.  If multiple axes are specified, datum components from each axis are combined in the order they appear to form a complete multi-dimensional datum (e.g. a 2D point).

If a label is specified (non-null) for components of the same datum in multiple axes, the label corresponding to the highest axis index will take precedence.

| **Column name** | **Data type** |
|:----------------|:--------------|
| `_id` | `long` |
| `COLUMN_AXIS_INDEX` | `int` |
| `COLUMN_SERIES_INDEX` | `int` |
| `COLUMN_DATUM_VALUE` | `double` |
| `COLUMN_DATUM_LABEL` | `String` |


Here is an example results table that could be produced in response to a "data" query:

| `_id` | `COLUMN_AXIS_INDEX` | `COLUMN_SERIES_INDEX` | `COLUMN_DATUM_VALUE` | `COLUMN_DATUM_LABEL` |
|:------|:--------------------|:----------------------|:---------------------|:---------------------|
| 3546 | 0 | 0 | 17 | "beef" |
| 3547 | 1 | 0 | 4 | `null` |
| 3548 | 0 | 0 | 24 | "chicken" |
| 3549 | 1 | 0 | 6 | `null` |
| 3550 | 0 | 0 | 13 | "pork" |
| 3551 | 1 | 0 | 8 | `null` |
| 3552 | 0 | 1 | 30 | "apples" |
| 3553 | 1 | 1 | 10 | `null` |
| 3554 | 0 | 1 | 12 | "oranges" |
| 3555 | 1 | 1 | 4 | `null` |
| 3556 | 0 | 1 | 5 | "pears" |
| 3557 | 1 | 1 | 2 | `null` |

This dataset could be plotted on a line chart with two series of four labeled points apiece.  It could also be plotted in a donut chart, where values from the second axis would be ignored.