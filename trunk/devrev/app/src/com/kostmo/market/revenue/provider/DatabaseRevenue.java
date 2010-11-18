package com.kostmo.market.revenue.provider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.gc.android.market.api.model.Market.App;
import com.gc.android.market.api.model.Market.Comment;
import com.gc.android.market.api.model.Market.AppsRequest.ViewType;
import com.googlecode.chartdroid.core.ColumnSchema;
import com.kostmo.market.revenue.CalendarPickerConstants;
import com.kostmo.market.revenue.activity.AppsOverviewActivity;
import com.kostmo.market.revenue.activity.ConsolidationActivity;
import com.kostmo.market.revenue.activity.RevenueActivity;
import com.kostmo.market.revenue.activity.AppsOverviewActivity.WindowEvaluatorMode;
import com.kostmo.market.revenue.container.DateRange;
import com.kostmo.market.revenue.xml.CheckoutXmlUtils.ChargeAmount;
import com.kostmo.tools.DurationStrings.TimescaleTier;

public class DatabaseRevenue extends SQLiteOpenHelper {

	static final String TAG = "DatabaseRevenue";

	static final String DATABASE_NAME = "REVENUE";
	static final int DATABASE_VERSION = 7;	// TODO Return to 6

	public static final int FALSE = 0;
	public static final int TRUE = 1;
	
	public static final int INVALID_PUBLISHER_ID = -1;
	

	// ========================================================================

	// These three tables are concerned with Google Checkout data
	public static final String TABLE_GOOGLE_CHECKOUT_PURCHASES = "TABLE_GOOGLE_CHECKOUT_PURCHASES";
	/**
	 * Each Google Checkout item record possesses a "canonical ID", which must
	 * be a "Merchant Item ID" from this same table.
	 */
	public static final String TABLE_GOOGLE_CHECKOUT_PRODUCTS = "TABLE_GOOGLE_CHECKOUT_ITEMS";
	public static final String TABLE_CACHE_SPANS = "TABLE_CACHE_SPANS";

	// These tables are concerned with Android Market data
	public static final String TABLE_MARKET_COMMENTS = "TABLE_MARKET_COMMENTS";
	public static final String TABLE_MARKET_APPS = "TABLE_MARKET_APPS";
	
	public static final String TABLE_PUBLISHERS = "TABLE_PUBLISHERS";

	// ========================================================================

	// These views select the highest "version" out of all of the apps with a
	// shared package name. This is used in the AppsOverviewActivity, for
	// avoiding
	// duplicates when apps are synched after a new version was published.
	public static final String VIEW_LATEST_MARKET_APPS = "VIEW_LATEST_MARKET_APPS";
	public static final String VIEW_RATED_LATEST_MARKET_APPS = "VIEW_LATEST_RATED_MARKET_APPS";

	/**
	 * This view simply aggregates all ratings for a given Android Market app
	 * ID, maintaining the earliest and latest cached comment date.
	 */
	public static final String VIEW_AGGREGATE_RATINGS = "VIEW_AGGREGATE_RATINGS";

	/** This view JOINs the aggregated ratings to the Android Market apps table. */
	public static final String VIEW_RATED_MARKET_APPS = "VIEW_RATED_MARKET_APPS";

	/**
	 * This view filters out records from {@link #VIEW_RATED_MARKET_APPS} that
	 * have a price of zero.
	 */
	public static final String VIEW_PAID_RATED_MARKET_APPS = "VIEW_PAID_MARKET_APPS";

	/**
	 * A straightforward JOIN of {@link #TABLE_MARKET_APPS} and
	 * {@link #TABLE_MARKET_COMMENTS} on {@link #KEY_APP_ID}, with a computed
	 * column {@link #KEY_RATING_INSIDE_THRESHOLD}
	 */
	public static final String VIEW_MARKET_COMMENTS_LINKED = "VIEW_MARKET_COMMENTS_LINKED";

	/**
	 * This view was created so I can get the individual app ratings to compare
	 * to the threshold. It filters out comments from
	 * {@link #VIEW_MARKET_COMMENTS_LINKED} that are above the threshold.
	 */
	public static final String VIEW_SEMIAGGREGATED_SUBTHRESHOLD_MARKET_APP_COMMENTS = "VIEW_SEMIAGGREGATED_SUBTHRESHOLD_MARKET_APP_COMMENTS";

	/**
	 * This view JOINs the sub-threshold comments view with the
	 * {@link #VIEW_AGGREGATE_RATINGS} view so that app information can be
	 * displayed for each app that has at least one sub-threshold comment.
	 */
	public static final String PARTIAL_AND_FULL_LINKED_COMMENT_AGGREGATION = "PARTIAL_AND_FULL_LINKED_COMMENT_AGGREGATION";

	/**
	 * This view aggregates Google Checkout purchases by "Merchant Item ID". The
	 * "canonical" IDs are available as a column, but not used for grouping.
	 */
	public static final String VIEW_AGGREGATE_PURCHASES = "VIEW_AGGREGATE_PURCHASES";

	/**
	 * This view is nearly identical to {@link #TABLE_GOOGLE_CHECKOUT_PRODUCTS},
	 * but it contains all of the fields of the parent indicated by
	 * {@link #KEY_CANONICAL_ITEM_ID} instead of the original field values. It
	 * is used indirectly for merging in the {@link #ConsolidationActivity}, and
	 * as an intermediary for {@link #VIEW_LABELED_TRANSACTIONS}.
	 */
	public static final String VIEW_CONSOLIDATED_GOOGLE_CHECKOUT_ITEMS = "VIEW_CONSOLIDATED_GOOGLE_CHECKOUT_ITEMS";

	// FIXME - Maybe this can be eliminated?
	/**
	 * A JOIN of {@link #TABLE_GOOGLE_CHECKOUT_PURCHASES} and
	 * {@link #VIEW_CONSOLIDATED_GOOGLE_CHECKOUT_ITEMS} on
	 * {@link #KEY_CHILD_ITEM_ID}. It has been used for plotting, but to
	 * ill-effect. It is supposed to enable filtering by date range.
	 */
	public static final String VIEW_LABELED_TRANSACTIONS = "VIEW_LABELED_TRANSACTIONS";

	/**
	 * A JOIN of {@link #TABLE_GOOGLE_CHECKOUT_PURCHASES} and
	 * {@link #TABLE_GOOGLE_CHECKOUT_PRODUCTS} on {@link #KEY_MERCHANT_ITEM_ID}.
	 */
	public static final String VIEW_RAW_TRANSACTION_ITEMS = "VIEW_RAW_TRANSACTION_ITEMS";

	/**
	 * A helper view to provide events to the Calendar app with the correct
	 * column names
	 */
	public static final String VIEW_CALENDAR_SALES_EVENTS = "VIEW_CALENDAR_SALES_EVENTS";
	public static final String VIEW_CALENDAR_SALES_EVENTS_TRUNCATED_DAY = "VIEW_CALENDAR_SALES_EVENTS_TRUNCATED_DAY";
	
	
	/**
	 * A FULL OUTER JOIN on the Apps and Items table (linking Market and
	 * Checkout), using {@link #KEY_APP_ID} from
	 * {@link #VIEW_PAID_RATED_MARKET_APPS} and {@link #KEY_CANONICAL_ITEM_ID}
	 * from {@link #VIEW_AGGREGATE_PURCHASES}. It is used directly in
	 * ConsolidationActivity and as an intermediate step to building
	 * {@link #VIEW_AGGREGATE_CONSOLIDATED_MARKET_LINKED}
	 */
	public static final String VIEW_SIMPLE_MARKET_LINKED = "VIEW_SIMPLE_MARKET_LINKED";

	/**
	 * An aggregation of app counts, sale counts, and summed revenue from
	 * {@link #VIEW_SIMPLE_MARKET_LINKED}, grouped by KEY_APP_ID. It is used
	 * directly in {@link #ConsolidationActivity}.
	 */
	public static final String VIEW_AGGREGATE_CONSOLIDATED_MARKET_LINKED = "VIEW_AGGREGATE_CONSOLIDATED_MARKET_LINKED";

	// ========================================================================
	public static final String KEY_CONSOLIDATED_APP_COUNT = "KEY_CONSOLIDATED_APP_COUNT";
	public static final String KEY_AGGREGATE_REVENUE = "KEY_AGGREGATE_REVENUE";
	public static final String KEY_AGGREGATE_SALE_COUNT = "KEY_AGGREGATE_SALE_COUNT";
	public static final String KEY_EARLIEST_SALE_DATE = "KEY_EARLIEST_SALE_DATE";
	public static final String KEY_LATEST_SALE_DATE = "KEY_LATEST_SALE_DATE";

	public static final String KEY_SUMMED_RATINGS = "KEY_SUMMED_RATINGS";
	public static final String KEY_AGGREGATE_RATING_COUNT = "KEY_AGGREGATE_RATING_COUNT";
	public static final String KEY_EARLIEST_RATING = "KEY_EARLIEST_RATING";
	public static final String KEY_LATEST_RATING = "KEY_LATEST_RATING";
	public static final String KEY_UNREAD_COMMENT_COUNT = "KEY_UNREAD_COMMENT_COUNT";

	public static final String KEY_UNREAD_SUBTHRESHOLD_COMMENT_COUNT = "KEY_UNREAD_SUBTHRESHOLD_COMMENT_COUNT";
	public static final String KEY_AGGREGATE_SUBTHRESHOLD_RATING_COUNT = "KEY_AGGREGATE_SUBTHRESHOLD_RATING_COUNT";
	public static final String KEY_EARLIEST_SUBTHRESHOLD_RATING = "KEY_EARLIEST_SUBTHRESHOLD_RATING";
	public static final String KEY_LATEST_SUBTHRESHOLD_RATING = "KEY_LATEST_SUBTHRESHOLD_RATING";

	public static final String KEY_COMMENT_AUTHOR_NAME = "KEY_COMMENT_AUTHOR_NAME";
	public static final String KEY_COMMENT_AUTHOR_ID = "KEY_COMMENT_AUTHOR_ID";
	public static final String KEY_RATING_TIMESTAMP = "KEY_RATING_TIMESTAMP";
	public static final String KEY_RATING_VALUE = "KEY_RATING_VALUE";
	public static final String KEY_RATING_ALERT_THRESHOLD = "KEY_RATING_ALERT_THRESHOLD";
	public static final String KEY_COMMENT_TEXT = "KEY_COMMENT_TEXT";
	public static final String KEY_COMMENT_READ = "KEY_COMMENT_READ";

	public static final String KEY_RATING_INSIDE_THRESHOLD = "KEY_RATING_INSIDE_THRESHOLD";

	public static final String KEY_APP_ID = "KEY_APP_ID";
	public static final String KEY_APP_CREATOR_ID = "KEY_APP_CREATOR_ID";
	public static final String KEY_APP_CREATOR_NAME = "KEY_APP_CREATOR_NAME";
	
	public static final String KEY_PUBLISHER_ID = "KEY_PUBLISHER_ID";
	public static final String KEY_PUBLISHER_NAME = "KEY_PUBLISHER_NAME";
	

	public static final String KEY_APP_RATING_COUNT = "KEY_APP_RATING_COUNT";
	public static final String KEY_APP_OVERALL_RATING = "KEY_APP_AGGREGATE_RATING";
	public static final String KEY_APP_PRICE_MICROS = "KEY_APP_PRICE_MICROS";
	public static final String KEY_APP_PACKAGE_NAME = "KEY_APP_PACKAGE_NAME";
	public static final String KEY_APP_VERSION_CODE = "KEY_APP_VERSION_CODE";
	public static final String KEY_APP_TITLE = "KEY_APP_TITLE";
	public static final String KEY_GOT_EARLIEST_COMMENT = "KEY_GOT_EARLIEST_COMMENT"; // Boolean
	public static final String KEY_GOT_MAX_COMMENTS = "KEY_GOT_MAX_COMMENTS"; // Boolean
	public static final String KEY_LAST_COMMENTS_UPDATE_TIMESTAMP = "KEY_LAST_COMMENTS_UPDATE_TIMESTAMP";

	public static final String KEY_APP_CURRENT_PRICE = "KEY_APP_CURRENT_PRICE";

	public static final String KEY_ORDER_NUMBER = "KEY_ORDER_NUMBER";
	public static final String KEY_ORDER_TIMESTAMP = "KEY_ORDER_TIMESTAMP";
	public static final String KEY_REVENUE_CENTS = "KEY_REVENUE_CENTS";
	public static final String KEY_MERCHANT_ITEM_ID = "KEY_MERCHANT_ITEM_ID";
	public static final String KEY_CHILD_ITEM_ID = "KEY_CHILD_ITEM_ID";

	// TODO
	// New App deployment comes with a new APP_ID. If the old App ID in the
	// Market Apps table gets wiped, we'll have some orphaned items;
	// We should automatically transfer the Canonical IDs when a new Market
	// app with the same package name as an old one is detected.
	public static final String KEY_CANONICAL_ITEM_ID = "KEY_CANONICAL_ITEM_ID";
	public static final String KEY_ITEM_NAME = "KEY_ITEM_NAME";
	public static final String KEY_ITEM_DESCRIPTION = "KEY_ITEM_DESCRIPTION";

	public static final String KEY_SPAN_ID = "KEY_SPAN_ID";
	public static final String KEY_SPAN_START_MILLISECONDS = "KEY_SPAN_START_MILLISECONDS";
	public static final String KEY_SPAN_END_MILLISECONDS = "KEY_SPAN_END_MILLISECONDS";

	// ========================================================================
	final static String SQL_CREATE_PURCHASES_TABLE = "create table "
			+ TABLE_GOOGLE_CHECKOUT_PURCHASES + " (" + KEY_ORDER_NUMBER
			+ " integer primary key ON CONFLICT IGNORE, " + KEY_REVENUE_CENTS
			+ " integer, " + KEY_MERCHANT_ITEM_ID + " integer, "
			+ KEY_ORDER_TIMESTAMP + " integer);";

	// Since (1) the KEY_CANONICAL_ITEM_ID is initially set to the
	// KEY_MERCHANT_ITEM_ID,
	// (2) KEY_CANONICAL_ITEM_ID is only ever set to an existing
	// KEY_MERCHANT_ITEM_ID, and
	// (3) rows of TABLE_ITEMS are never deleted except all at once,
	// the FOREIGN KEY constraint should never end up being violated (although
	// SQLite
	// doesn't enforce it anyway)
	final static String SQL_CREATE_ITEMS_TABLE = "create table "
			+ TABLE_GOOGLE_CHECKOUT_PRODUCTS + " (" + KEY_MERCHANT_ITEM_ID
			+ " integer primary key ON CONFLICT IGNORE, " + KEY_ITEM_NAME
			+ " text, " + KEY_CANONICAL_ITEM_ID + " integer, "
			+ KEY_ITEM_DESCRIPTION + " text, " + "FOREIGN KEY ("
			+ KEY_CANONICAL_ITEM_ID + ") REFERENCES " + TABLE_MARKET_APPS + "("
			+ KEY_APP_ID + ")" + ");";

	final static String SQL_CREATE_RECORD_SPAN_TABLE = "create table "
			+ TABLE_CACHE_SPANS + " (" + KEY_SPAN_ID
			+ " integer primary key autoincrement, "
			+ KEY_SPAN_START_MILLISECONDS + " integer, "
			+ KEY_SPAN_END_MILLISECONDS + " integer);";

	final static String SQL_CREATE_RATINGS_TABLE = "create table "
			+ TABLE_MARKET_COMMENTS
			+ " ("
			+ KEY_APP_ID
			+ " integer, "
			// XXX Author ID uses unsigned long values, requires BigInteger
			// conversion to signed
			+ KEY_COMMENT_AUTHOR_ID + " integer, " + KEY_COMMENT_AUTHOR_NAME
			+ " text, " + KEY_RATING_VALUE + " integer, " + KEY_COMMENT_TEXT
			+ " text, " + KEY_RATING_TIMESTAMP + " integer, "
			+ KEY_COMMENT_READ + " integer default " + FALSE
			+ ", " // Boolean
			+ "PRIMARY KEY(" + KEY_APP_ID + ", " + KEY_COMMENT_AUTHOR_ID
			+ ") ON CONFLICT REPLACE);";

	final static String SQL_MARKET_APPS_TABLE = "create table "
			+ TABLE_MARKET_APPS
			+ " ("
			+ KEY_APP_ID
			+ " integer primary key ON CONFLICT IGNORE, "
			+ KEY_APP_CREATOR_NAME
			+ " text, "
			+ KEY_APP_CREATOR_ID
			+ " text, " // Only difference is that the "Creator ID" has quote
						// marks.
			+ KEY_PUBLISHER_ID + " integer, "	// Foreign key to TABLE_PUBLISHERS
			+ KEY_RATING_ALERT_THRESHOLD + " integer default "
			+ AppsOverviewActivity.DEFAULT_RATING_ALERT_THRESHOLD + ", "
			+ KEY_APP_PRICE_MICROS + " integer, " + KEY_APP_VERSION_CODE
			+ " integer, " + KEY_APP_PACKAGE_NAME + " text, " + KEY_APP_TITLE
			+ " text, " + KEY_APP_RATING_COUNT + " integer, "
			+ KEY_GOT_EARLIEST_COMMENT + " integer default " + FALSE + ", " // Boolean
			+ KEY_GOT_MAX_COMMENTS + " integer default " + FALSE + ", " // Boolean
			+ KEY_LAST_COMMENTS_UPDATE_TIMESTAMP + " integer, " // Timestamp
			+ KEY_APP_OVERALL_RATING + " real);";

	final static String SQL_CREATE_PUBLISHERS_TABLE = "create table "
		+ TABLE_PUBLISHERS + " (" + KEY_PUBLISHER_ID
		+ " integer primary key autoincrement, "
		+ KEY_PUBLISHER_NAME + " text);";

	// ========================================================================
	final static String SQL_CREATE_RATED_MARKET_APPS_VIEW = "create view "
			+ VIEW_RATED_MARKET_APPS + " AS "
			+ ViewQueries.buildRatedMarketAppsQuery();

	final static String SQL_CREATE_RATED_MARKET_APPS_COMMENTS_VIEW = "create view "
			+ VIEW_SEMIAGGREGATED_SUBTHRESHOLD_MARKET_APP_COMMENTS
			+ " AS "
			+ ViewQueries.buildSemiaggregatedMarketAppCommentsQuery();

	final static String SQL_CREATE_PARTIAL_AND_FULL_LINKED_COMMENT_AGGREGATION_VIEW = "create view "
			+ PARTIAL_AND_FULL_LINKED_COMMENT_AGGREGATION
			+ " AS "
			+ ViewQueries.buildPartialAndFullLinkedCommentsAggregationQuery();

	final static String SQL_CREATE_PAID_RATED_MARKET_APPS_VIEW = "create view "
			+ VIEW_PAID_RATED_MARKET_APPS + " AS "
			+ ViewQueries.buildPaidRatedAppsQuery();

	final static String SQL_CREATE_CONSOLIDATED_ITEMS_VIEW = "create view "
			+ VIEW_CONSOLIDATED_GOOGLE_CHECKOUT_ITEMS + " AS "
			+ ViewQueries.buildConsolidatedItemsQuery();

	final static String SQL_CREATE_LABELED_TRANSACTIONS_VIEW = "create view "
			+ VIEW_LABELED_TRANSACTIONS + " AS "
			+ ViewQueries.buildLabeledTransactionQuery();

	final static String SQL_CREATE_RAW_TRANSACTION_ITEMS_VIEW = "create view "
			+ VIEW_RAW_TRANSACTION_ITEMS + " AS "
			+ ViewQueries.buildRawAssociatedTransactionQuery();

	final static String SQL_CREATE_CALENDAR_SALES_EVENTS_VIEW = "create view "
		+ VIEW_CALENDAR_SALES_EVENTS + " AS "
		+ ViewQueries.buildCalendarSalesEventsQuery();

	final static String SQL_CREATE_CALENDAR_SALES_EVENTS_TRUNCATED_DAY_VIEW = "create view "
		+ VIEW_CALENDAR_SALES_EVENTS_TRUNCATED_DAY + " AS "
		+ ViewQueries.buildCalendarSalesEventsQueryTruncatedDay();
	
	
	final static String SQL_CREATE_MARKET_COMMENTS_LINKED_VIEW = "create view "
			+ VIEW_MARKET_COMMENTS_LINKED + " AS "
			+ ViewQueries.buildLinkedMarketCommentsQuery();

	final static String SQL_CREATE_SIMPLE_MARKET_LINKED_VIEW = "create view "
			+ VIEW_SIMPLE_MARKET_LINKED + " AS "
			+ ViewQueries.buildSimpleMarketLinkedQuery();

	final static String SQL_CREATE_AGGREGATE_CONSOLIDATED_MARKET_LINKED_VIEW = "create view "
			+ VIEW_AGGREGATE_CONSOLIDATED_MARKET_LINKED
			+ " AS "
			+ ViewQueries.buildAggregatedConsolidatedMarketLinkedQuery();

	final static String SQL_CREATE_AGGREGATE_PURCHASES_VIEW = "create view "
			+ VIEW_AGGREGATE_PURCHASES + " AS "
			+ ViewQueries.buildAggregatePaidAppsQuery();

	final static String SQL_CREATE_AGGREGATE_RATINGS_VIEW = "create view "
			+ VIEW_AGGREGATE_RATINGS + " AS "
			+ ViewQueries.buildAggregateRatingsQuery();

	final static String SQL_LATEST_MARKET_APPS_VIEW = "create view "
			+ VIEW_LATEST_MARKET_APPS + " AS "
			+ ViewQueries.buildLatestMarketAppsQuery();

	final static String SQL_LATEST_RATED_MARKET_APPS_VIEW = "create view "
			+ VIEW_RATED_LATEST_MARKET_APPS + " AS "
			+ ViewQueries.buildRatedLatestMarketAppsQuery();

	// ========================================================================
	final static String[] table_list = {
			TABLE_MARKET_APPS,
			TABLE_MARKET_COMMENTS,
			TABLE_GOOGLE_CHECKOUT_PURCHASES,
			TABLE_GOOGLE_CHECKOUT_PRODUCTS,
			TABLE_CACHE_SPANS,
			TABLE_PUBLISHERS
	};

	final static String[] view_list = { VIEW_LATEST_MARKET_APPS,
			VIEW_RATED_LATEST_MARKET_APPS, VIEW_RATED_MARKET_APPS,
			VIEW_PAID_RATED_MARKET_APPS,
			VIEW_SEMIAGGREGATED_SUBTHRESHOLD_MARKET_APP_COMMENTS,
			PARTIAL_AND_FULL_LINKED_COMMENT_AGGREGATION,

			VIEW_CONSOLIDATED_GOOGLE_CHECKOUT_ITEMS, VIEW_LABELED_TRANSACTIONS,
			VIEW_RAW_TRANSACTION_ITEMS,
			VIEW_CALENDAR_SALES_EVENTS,
			VIEW_CALENDAR_SALES_EVENTS_TRUNCATED_DAY,
			VIEW_AGGREGATE_PURCHASES,
			VIEW_AGGREGATE_RATINGS, VIEW_MARKET_COMMENTS_LINKED,
			VIEW_SIMPLE_MARKET_LINKED,
			VIEW_AGGREGATE_CONSOLIDATED_MARKET_LINKED, };

	final static String[] table_creation_commands = {
			SQL_MARKET_APPS_TABLE,
			SQL_CREATE_RATINGS_TABLE,
			SQL_CREATE_PURCHASES_TABLE,
			SQL_CREATE_ITEMS_TABLE,
			SQL_CREATE_RECORD_SPAN_TABLE,
			SQL_CREATE_PUBLISHERS_TABLE,

			SQL_LATEST_MARKET_APPS_VIEW,
			SQL_CREATE_AGGREGATE_RATINGS_VIEW,
			SQL_CREATE_RATED_MARKET_APPS_VIEW,
			SQL_CREATE_PAID_RATED_MARKET_APPS_VIEW,

			SQL_LATEST_RATED_MARKET_APPS_VIEW,

			SQL_CREATE_CONSOLIDATED_ITEMS_VIEW, // This VIEW must be defined
												// before
												// SQL_CREATE_LABELED_TRANSACTIONS_VIEW!
			SQL_CREATE_LABELED_TRANSACTIONS_VIEW,

			SQL_CREATE_MARKET_COMMENTS_LINKED_VIEW, // This must precede the
													// next
			SQL_CREATE_RATED_MARKET_APPS_COMMENTS_VIEW,
			SQL_CREATE_PARTIAL_AND_FULL_LINKED_COMMENT_AGGREGATION_VIEW,

			SQL_CREATE_RAW_TRANSACTION_ITEMS_VIEW, // This must precede both
													// SQL_CREATE_AGGREGATE_PURCHASES_VIEW and SQL_CREATE_CALENDAR_SALES_EVENTS_VIEW
			
			SQL_CREATE_CALENDAR_SALES_EVENTS_VIEW,
			SQL_CREATE_CALENDAR_SALES_EVENTS_TRUNCATED_DAY_VIEW,
			
			SQL_CREATE_AGGREGATE_PURCHASES_VIEW, // This must precede
													// SQL_CREATE_SIMPLE_MARKET_LINKED_VIEW
			SQL_CREATE_SIMPLE_MARKET_LINKED_VIEW, // This must precede
													// SQL_CREATE_AGGREGATE_CONSOLIDATED_MARKET_LINKED_VIEW
			SQL_CREATE_AGGREGATE_CONSOLIDATED_MARKET_LINKED_VIEW };

	// ========================================================================
	public DatabaseRevenue(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	// ========================================================================
	public Cursor getCalendarSalesEvents(String[] projection,
			String selection, String[] selection_args, String order_by) {
		
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(VIEW_CALENDAR_SALES_EVENTS,
				projection, selection, selection_args,
				null, null, order_by);

		return cursor;
	}
	
	
	// ========================================================================
	public Cursor getCalendarSalesEventsGrouped(String[] projection,
			String selection, String[] selection_args, String order_by) {
		
		SQLiteDatabase db = getReadableDatabase();
		
		
		SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
		query_builder.setTables(VIEW_CALENDAR_SALES_EVENTS_TRUNCATED_DAY);

		Cursor cursor = query_builder.query(
				db,
				new String[] {
				BaseColumns._ID,
				CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.CALENDAR_ID,
				"SUM(" + SalesEventsContentProvider.COLUMN_QUANTITY0 + ") AS " + SalesEventsContentProvider.COLUMN_QUANTITY0,
				"COUNT(" + BaseColumns._ID + ") AS " + SalesEventsContentProvider.COLUMN_QUANTITY1,
				"'Aggregate Sales' AS " + CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.TITLE,
				CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.TIMESTAMP},
				selection, selection_args,
				// Group by:
				CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.TIMESTAMP,
				null, order_by, null);
		
		return cursor;
	}


	// ========================================================================
	private static class ViewQueries {

		// ========================================================================
		private static String buildConsolidatedItemsQuery() {

			SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
			query_builder.setTables(TABLE_GOOGLE_CHECKOUT_PRODUCTS + " AS A1"
					+ " LEFT JOIN " + TABLE_MARKET_APPS + " AS A2" + " ON (A1."
					+ KEY_CANONICAL_ITEM_ID + " = " + "A2." + KEY_APP_ID + ")");

			return query_builder.buildQuery(new String[] {
					"A1." + KEY_MERCHANT_ITEM_ID + " AS " + KEY_CHILD_ITEM_ID,
					"A2." + KEY_APP_ID + " AS " + KEY_APP_ID,
					"A2." + KEY_APP_TITLE + " AS " + KEY_ITEM_NAME,
					"A2." + KEY_APP_TITLE + " AS " + KEY_ITEM_DESCRIPTION, // FIXME
																			// I
																			// don't
																			// use
																			// this
																			// later
					"A2." + KEY_PUBLISHER_ID + " AS "
							+ KEY_PUBLISHER_ID

			}, null, null, null, null, null, null);
		}

		// ========================================================================
		private static String buildLabeledTransactionQuery() {

			SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
			query_builder.setTables(TABLE_GOOGLE_CHECKOUT_PURCHASES + " AS A1"
					+ " LEFT JOIN " + VIEW_CONSOLIDATED_GOOGLE_CHECKOUT_ITEMS
					+ " AS A2" + " ON (A1." + KEY_MERCHANT_ITEM_ID + " = "
					+ "A2." + KEY_CHILD_ITEM_ID + ")");

			return query_builder.buildQuery(new String[] { KEY_ORDER_NUMBER,
					KEY_REVENUE_CENTS,
					"A2." + KEY_APP_ID + " AS " + KEY_APP_ID,
					"A2." + KEY_CHILD_ITEM_ID + " AS " + KEY_CHILD_ITEM_ID,
					KEY_ORDER_TIMESTAMP, KEY_ITEM_NAME, KEY_ITEM_DESCRIPTION,
					KEY_PUBLISHER_ID }, null, null, null, null, null, null);
		}

		// ========================================================================
		private static String buildRawAssociatedTransactionQuery() {

			SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
			query_builder.setTables(TABLE_GOOGLE_CHECKOUT_PURCHASES + " AS A1"
					+ " LEFT JOIN " + TABLE_GOOGLE_CHECKOUT_PRODUCTS + " AS A2"
					+ " ON (A1." + KEY_MERCHANT_ITEM_ID + " = " + "A2."
					+ KEY_MERCHANT_ITEM_ID + ")");

			return query_builder.buildQuery(new String[] {
					"A1." + KEY_MERCHANT_ITEM_ID + " AS "
							+ KEY_MERCHANT_ITEM_ID, KEY_CANONICAL_ITEM_ID,
					KEY_ORDER_NUMBER, KEY_REVENUE_CENTS, KEY_ORDER_TIMESTAMP,
					KEY_ITEM_NAME, KEY_ITEM_DESCRIPTION }, null, null, null,
					null, null, null);
		}

		
		// ========================================================================
		private static String buildCalendarSalesEventsQuery() {

			SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
			query_builder.setTables(VIEW_RAW_TRANSACTION_ITEMS);

			return query_builder.buildQuery(
					new String[] {
							KEY_ORDER_NUMBER + " AS "
							+ BaseColumns._ID,
					KEY_CANONICAL_ITEM_ID + " AS " + CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.CALENDAR_ID,
					KEY_REVENUE_CENTS + "/CAST(100 AS REAL) AS " + SalesEventsContentProvider.COLUMN_QUANTITY0,
					KEY_ITEM_NAME + " AS " + CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.TITLE,
					KEY_ORDER_TIMESTAMP + " AS " + CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.TIMESTAMP},
					null, null, null, null, null, null);
		}
		
		// ========================================================================
		private static String buildCalendarSalesEventsQueryTruncatedDay() {

			SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
			query_builder.setTables(VIEW_CALENDAR_SALES_EVENTS);

			return query_builder.buildQuery(
					new String[] {
					BaseColumns._ID,
					CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.CALENDAR_ID,
					SalesEventsContentProvider.COLUMN_QUANTITY0,
					CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.TITLE,
					"CAST((CAST(" + CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.TIMESTAMP + "/" + TimescaleTier.DAYS.millis + " AS INTEGER)*" + TimescaleTier.DAYS.millis + ") AS INTEGER) " + CalendarPickerConstants.CalendarEventPicker.ContentProviderColumns.TIMESTAMP},
					null, null, null, null, null, null);
		}

		// ========================================================================
		static String buildLinkedMarketCommentsQuery() {

			SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
			query_builder.setTables(TABLE_MARKET_APPS + " AS A1"
					+ " LEFT JOIN " + TABLE_MARKET_COMMENTS + " AS A2"
					+ " ON (A1." + KEY_APP_ID + " = " + "A2." + KEY_APP_ID
					+ ")");

			// Returns every field from both tables.
			return query_builder.buildQuery(new String[] {
					"A1." + KEY_APP_ID + " AS " + KEY_APP_ID,
					KEY_PUBLISHER_ID,
					KEY_APP_CREATOR_NAME,
					KEY_APP_CREATOR_ID,
					KEY_RATING_ALERT_THRESHOLD,
					KEY_APP_PRICE_MICROS,
					KEY_APP_VERSION_CODE,
					KEY_APP_PACKAGE_NAME,
					KEY_APP_TITLE,
					KEY_APP_RATING_COUNT,
					KEY_GOT_EARLIEST_COMMENT,
					KEY_GOT_MAX_COMMENTS,
					KEY_LAST_COMMENTS_UPDATE_TIMESTAMP,
					KEY_APP_OVERALL_RATING,

					"A2.ROWID AS " + BaseColumns._ID,
					KEY_COMMENT_AUTHOR_ID,
					KEY_COMMENT_AUTHOR_NAME,
					KEY_RATING_VALUE,
					KEY_COMMENT_TEXT,
					KEY_RATING_TIMESTAMP,
					KEY_COMMENT_READ,
					KEY_RATING_VALUE + "<=" + KEY_RATING_ALERT_THRESHOLD
							+ " AS " + KEY_RATING_INSIDE_THRESHOLD }, null,
					null, null, null, null, null);
		}

		// ========================================================================
		/**
		 * Simulates a FULL OUTER JOIN
		 */
		private static String buildFullOuterJoin(String table1, String table2,
				String on_clause, String[] selection, String order) {

			SQLiteQueryBuilder query_builder1 = new SQLiteQueryBuilder();
			query_builder1.setTables(table1 + " LEFT JOIN " + table2
					+ on_clause);
			String query1 = query_builder1.buildQuery(selection, null, null,
					null, null, null, null);

			SQLiteQueryBuilder query_builder2 = new SQLiteQueryBuilder();
			query_builder2.setTables(table2 + " LEFT JOIN " + table1
					+ on_clause);
			String query2 = query_builder2.buildQuery(selection, null, null,
					null, null, null, null);

			SQLiteQueryBuilder query_builder3 = new SQLiteQueryBuilder();
			query_builder3.setDistinct(true);
			return query_builder3.buildUnionQuery(
					new String[] { query1, query2 }, order, null);
		}

		// ========================================================================
		private static String buildAggregatedConsolidatedMarketLinkedQuery() {

			SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
			query_builder.setTables(VIEW_SIMPLE_MARKET_LINKED);

			return query_builder.buildQuery(

			new String[] {
					KEY_APP_ID,
					KEY_APP_TITLE,
					"COUNT(" + KEY_MERCHANT_ITEM_ID + ") AS "
							+ KEY_CONSOLIDATED_APP_COUNT,
					"SUM(" + KEY_AGGREGATE_REVENUE + ") AS "
							+ KEY_AGGREGATE_REVENUE,
					"SUM(" + KEY_AGGREGATE_SALE_COUNT + ") AS "
							+ KEY_AGGREGATE_SALE_COUNT,
					KEY_AGGREGATE_RATING_COUNT, KEY_APP_RATING_COUNT,
					KEY_APP_OVERALL_RATING, KEY_GOT_EARLIEST_COMMENT,
					KEY_GOT_MAX_COMMENTS, KEY_APP_CURRENT_PRICE,
					KEY_PUBLISHER_ID, // NEW
			}, null, null, KEY_APP_ID, null, null, null);
		}

		// ========================================================================
		/**
		 * Performs a FULL OUTER JOIN on the Apps and Items table (from Market
		 * and Checkout)
		 */
		private static String buildSimpleMarketLinkedQuery() {

			// String t1 = VIEW_PAID_RATED_MARKET_APPS;
			// XXX This was changed so as to include formerly paid apps that
			// were made free
			String t1 = VIEW_RATED_MARKET_APPS;
			String t2 = VIEW_AGGREGATE_PURCHASES;

			String[] selection = new String[] {
					t1 + "." + KEY_APP_ID + " AS " + KEY_APP_ID,
					t1 + "." + KEY_APP_TITLE + " AS " + KEY_APP_TITLE,
					t1 + "." + KEY_APP_RATING_COUNT + " AS "
							+ KEY_APP_RATING_COUNT,
					t1 + "." + KEY_AGGREGATE_RATING_COUNT + " AS "
							+ KEY_AGGREGATE_RATING_COUNT,
					t1 + "." + KEY_GOT_EARLIEST_COMMENT + " AS "
							+ KEY_GOT_EARLIEST_COMMENT,
					t1 + "." + KEY_GOT_MAX_COMMENTS + " AS "
							+ KEY_GOT_MAX_COMMENTS,
					t1 + "." + KEY_APP_OVERALL_RATING + " AS "
							+ KEY_APP_OVERALL_RATING,
					t1 + "." + KEY_APP_PRICE_MICROS + " AS "
							+ KEY_APP_CURRENT_PRICE,
					t1 + "." + KEY_PUBLISHER_ID + " AS "
							+ KEY_PUBLISHER_ID, // NEW

					t2 + "." + KEY_MERCHANT_ITEM_ID + " AS "
							+ KEY_MERCHANT_ITEM_ID,
					t2 + "." + KEY_CANONICAL_ITEM_ID + " AS "
							+ KEY_CANONICAL_ITEM_ID,
					t2 + "." + KEY_ITEM_NAME + " AS " + KEY_ITEM_NAME,
					t2 + "." + KEY_EARLIEST_SALE_DATE + " AS "
							+ KEY_EARLIEST_SALE_DATE,
					t2 + "." + KEY_LATEST_SALE_DATE + " AS "
							+ KEY_LATEST_SALE_DATE,
					t2 + "." + KEY_REVENUE_CENTS + " AS " + KEY_REVENUE_CENTS,
					t2 + "." + KEY_AGGREGATE_REVENUE + " AS "
							+ KEY_AGGREGATE_REVENUE,
					t2 + "." + KEY_AGGREGATE_SALE_COUNT + " AS "
							+ KEY_AGGREGATE_SALE_COUNT };

			String on_clause = " ON (" + t2 + "." + KEY_CANONICAL_ITEM_ID
					+ " = " + t1 + "." + KEY_APP_ID + ")";

			return buildFullOuterJoin(t1, t2, on_clause, selection,
					KEY_APP_TITLE + " ASC");
		}

		// ========================================================================
		private static String buildRatedLatestMarketAppsQuery() {

			SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
			query_builder.setTables(VIEW_LATEST_MARKET_APPS + " AS A1"
					+ " LEFT JOIN " + VIEW_AGGREGATE_RATINGS + " AS A2"
					+ " ON (A1." + KEY_APP_ID + " = " + "A2." + KEY_APP_ID
					+ ")");

			return query_builder.buildQuery(null, null, null, null, null, null,
					null);
		}

		// ========================================================================
		private static String buildRatedMarketAppsQuery() {

			SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
			query_builder.setTables(TABLE_MARKET_APPS + " AS A1"
					+ " LEFT JOIN " + VIEW_AGGREGATE_RATINGS + " AS A2"
					+ " ON (A1." + KEY_APP_ID + " = " + "A2." + KEY_APP_ID
					+ ")");

			return query_builder.buildQuery(null, null, null, null, null, null,
					null);
		}

		// ========================================================================
		private static String buildPartialAndFullLinkedCommentsAggregationQuery() {

			SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
			query_builder
					.setTables(VIEW_SEMIAGGREGATED_SUBTHRESHOLD_MARKET_APP_COMMENTS
							+ " AS A1"
							+ " LEFT JOIN "
							+ VIEW_AGGREGATE_RATINGS
							+ " AS A2"
							+ " ON (A1."
							+ KEY_APP_ID
							+ " = "
							+ "A2." + KEY_APP_ID + ")");

			// Returns every field from both tables.
			return query_builder.buildQuery(new String[] {
					"A1." + KEY_APP_ID + " AS " + KEY_APP_ID,
					KEY_PUBLISHER_ID,
					KEY_APP_CREATOR_NAME,
					KEY_APP_CREATOR_ID,
					KEY_RATING_ALERT_THRESHOLD, KEY_APP_PRICE_MICROS,
					KEY_APP_VERSION_CODE, KEY_APP_PACKAGE_NAME, KEY_APP_TITLE,
					KEY_APP_RATING_COUNT, KEY_GOT_EARLIEST_COMMENT,
					KEY_GOT_MAX_COMMENTS, KEY_LAST_COMMENTS_UPDATE_TIMESTAMP,
					KEY_APP_OVERALL_RATING,

					KEY_SUMMED_RATINGS, KEY_UNREAD_COMMENT_COUNT,
					KEY_AGGREGATE_RATING_COUNT, KEY_EARLIEST_RATING,
					KEY_LATEST_RATING,

					KEY_UNREAD_SUBTHRESHOLD_COMMENT_COUNT,
					KEY_AGGREGATE_SUBTHRESHOLD_RATING_COUNT,
					KEY_EARLIEST_SUBTHRESHOLD_RATING,
					KEY_LATEST_SUBTHRESHOLD_RATING }, null, null, null, null,
					null, null);
		}

		// ========================================================================
		private static String buildSemiaggregatedMarketAppCommentsQuery() {

			SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
			query_builder.setTables(VIEW_MARKET_COMMENTS_LINKED);

			// Here we have an extra column for aggregates
			return query_builder.buildQuery(

			new String[] {
					KEY_APP_ID,
					KEY_PUBLISHER_ID,
					KEY_APP_CREATOR_NAME,
					KEY_APP_CREATOR_ID,
					KEY_RATING_ALERT_THRESHOLD,
					KEY_APP_PRICE_MICROS,
					KEY_APP_VERSION_CODE,
					KEY_APP_PACKAGE_NAME,
					KEY_APP_TITLE,
					KEY_APP_RATING_COUNT,
					KEY_GOT_EARLIEST_COMMENT,
					KEY_GOT_MAX_COMMENTS,
					KEY_LAST_COMMENTS_UPDATE_TIMESTAMP,
					KEY_APP_OVERALL_RATING,
					/*
					 * KEY_COMMENT_AUTHOR_ID, KEY_COMMENT_AUTHOR_NAME,
					 * KEY_RATING_VALUE, KEY_COMMENT_TEXT, KEY_RATING_TIMESTAMP,
					 * KEY_COMMENT_READ,
					 */

					// "SUM(" + KEY_RATING_VALUE + ") AS " + KEY_SUMMED_RATINGS,
					"SUM(CAST(NOT CAST(" + KEY_COMMENT_READ
							+ " AS BOOLEAN) AS INTEGER)) AS "
							+ KEY_UNREAD_SUBTHRESHOLD_COMMENT_COUNT,
					"COUNT(*) AS " + KEY_AGGREGATE_SUBTHRESHOLD_RATING_COUNT,
					"MIN(" + KEY_RATING_TIMESTAMP + ") AS "
							+ KEY_EARLIEST_SUBTHRESHOLD_RATING,
					"MAX(" + KEY_RATING_TIMESTAMP + ") AS "
							+ KEY_LATEST_SUBTHRESHOLD_RATING },
					KEY_RATING_INSIDE_THRESHOLD, null, KEY_APP_ID, null, null,
					null);
		}

		// ========================================================================
		private static String buildPaidRatedAppsQuery() {

			SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
			query_builder.setTables(VIEW_RATED_MARKET_APPS);

			return query_builder.buildQuery(null,
					getFilterString(ViewType.PAID), null, null, null, null,
					null);
		}

		// ========================================================================
		private static String buildLatestMarketAppsQuery() {

			SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
			query_builder.setTables(TABLE_MARKET_APPS);

			// Should retain only the "latest" version of an app.
			return query_builder.buildQuery(new String[] { KEY_APP_ID,
					KEY_PUBLISHER_ID,
					KEY_APP_CREATOR_NAME,
					KEY_APP_CREATOR_ID,
					KEY_RATING_ALERT_THRESHOLD, KEY_APP_PRICE_MICROS,
					KEY_APP_VERSION_CODE, KEY_APP_PACKAGE_NAME, KEY_APP_TITLE,
					KEY_APP_RATING_COUNT, KEY_GOT_EARLIEST_COMMENT,
					KEY_GOT_MAX_COMMENTS, KEY_LAST_COMMENTS_UPDATE_TIMESTAMP,
					KEY_APP_OVERALL_RATING }, null, null, KEY_APP_PACKAGE_NAME,
					null, KEY_APP_VERSION_CODE + " DESC", null);
		}

		// ========================================================================
		private static String buildAggregateRatingsQuery() {

			SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
			query_builder.setTables(TABLE_MARKET_COMMENTS);

			return query_builder.buildQuery(
					new String[] {
							KEY_APP_ID,
							"SUM(" + KEY_RATING_VALUE + ") AS "
									+ KEY_SUMMED_RATINGS,
							"SUM(CAST(NOT CAST(" + KEY_COMMENT_READ
									+ " AS BOOLEAN) AS INTEGER)) AS "
									+ KEY_UNREAD_COMMENT_COUNT,
							"COUNT(*) AS " + KEY_AGGREGATE_RATING_COUNT,
							"MIN(" + KEY_RATING_TIMESTAMP + ") AS "
									+ KEY_EARLIEST_RATING,
							"MAX(" + KEY_RATING_TIMESTAMP + ") AS "
									+ KEY_LATEST_RATING }, null, null,
					KEY_APP_ID, null, null, null);
		}

		// ========================================================================
		private static String buildAggregatePaidAppsQuery() {

			SQLiteQueryBuilder query_builder = new SQLiteQueryBuilder();
			query_builder.setTables(VIEW_RAW_TRANSACTION_ITEMS);

			return query_builder.buildQuery(new String[] {
					KEY_MERCHANT_ITEM_ID,
					KEY_CANONICAL_ITEM_ID,
					KEY_REVENUE_CENTS,
					"SUM(" + KEY_REVENUE_CENTS + ") AS "
							+ KEY_AGGREGATE_REVENUE,
					"COUNT(" + KEY_ORDER_NUMBER + ") AS "
							+ KEY_AGGREGATE_SALE_COUNT,
					"MIN(" + KEY_ORDER_TIMESTAMP + ") AS "
							+ KEY_EARLIEST_SALE_DATE,
					"MAX(" + KEY_ORDER_TIMESTAMP + ") AS "
							+ KEY_LATEST_SALE_DATE, KEY_ITEM_NAME,
					KEY_ITEM_DESCRIPTION }, null, null, KEY_MERCHANT_ITEM_ID,
					null, null, null);
		}
	}

	// ========================================================================
	public class Debug {

		void listAllCacheRanges(SQLiteDatabase db) {
			Cursor cursor = db.query(TABLE_CACHE_SPANS, new String[] {
					KEY_SPAN_START_MILLISECONDS, KEY_SPAN_END_MILLISECONDS },
					null, null, null, null, null);

			Log.e(TAG, "Listing all cache ranges:");
			while (cursor.moveToNext()) {
				DateRange date_range = new DateRange(
						new Date(cursor.getLong(0)),
						new Date(cursor.getLong(1)));
				Log.w(TAG, ":: " + date_range);
			}

			cursor.close();
		}

		// ========================================================================
		public void dumpSimpleMarketLinkedView() {

			SQLiteDatabase db = getReadableDatabase();

			Cursor c = db.query(VIEW_SIMPLE_MARKET_LINKED, new String[] {
					KEY_APP_ID, KEY_APP_TITLE, KEY_MERCHANT_ITEM_ID,
					KEY_CANONICAL_ITEM_ID, KEY_ITEM_NAME }, null, null, null,
					null, null);

			Log.e(TAG, "Dumping MASTER linked view:");
			while (c.moveToNext()) {
				long app_id = c.getLong(0);
				String app_title = c.getString(1);
				long merchant_item_id = c.getLong(2);
				long canonical_id = c.getLong(3);
				String item_name = c.getString(4);
				Log.i(TAG, "App ID: " + app_id + "; App title: " + app_title
						+ "; Item ID: " + merchant_item_id + "; canonical id: "
						+ canonical_id + "; Item name: " + item_name);
			}

			c.close();
			db.close();
		}

		// ========================================================================
		public void dumpCommentsTable() {

			SQLiteDatabase db = getReadableDatabase();
			Cursor cursor = db.query(TABLE_MARKET_COMMENTS, new String[] {
					KEY_APP_ID, KEY_COMMENT_AUTHOR_ID, KEY_COMMENT_AUTHOR_NAME,
					KEY_RATING_VALUE, KEY_COMMENT_TEXT, KEY_RATING_TIMESTAMP,
					KEY_COMMENT_READ }, null, null, null, null, null);

			Log.e(TAG, "Dumping " + TABLE_MARKET_COMMENTS);
			while (cursor.moveToNext()) {
				long app_id = cursor.getLong(cursor.getColumnIndex(KEY_APP_ID));
				String author = cursor.getString(cursor
						.getColumnIndex(KEY_COMMENT_AUTHOR_NAME));
				int read = cursor.getInt(cursor
						.getColumnIndex(KEY_COMMENT_READ));
				int rating = cursor.getInt(cursor
						.getColumnIndex(KEY_RATING_VALUE));
				String text = cursor.getString(cursor
						.getColumnIndex(KEY_COMMENT_TEXT));
				Log.d(TAG, "App ID: " + app_id + "; Author: " + author
						+ "; Read: " + read + "; rating: " + rating
						+ "; Text: " + text);
			}
			cursor.close();
			db.close();
		}

		// ========================================================================
		public void dumpUniqueRawMerchantIds(DateRange date_range) {

			Log
					.d(TAG, "Dumping partial range of "
							+ TABLE_GOOGLE_CHECKOUT_PURCHASES + "("
							+ date_range + "):");

			SQLiteDatabase db = getReadableDatabase();

			Cursor cursor = db.query(TABLE_GOOGLE_CHECKOUT_PURCHASES,
					new String[] { KEY_ORDER_NUMBER, KEY_REVENUE_CENTS,
							KEY_MERCHANT_ITEM_ID, KEY_ORDER_TIMESTAMP },
					KEY_ORDER_TIMESTAMP + ">=? AND " + KEY_ORDER_TIMESTAMP
							+ "<=?", date_range.getRangeAsStringArray(),
					KEY_MERCHANT_ITEM_ID, // Group by
					null, null);

			String column_names = TextUtils.join(", ", cursor.getColumnNames());
			Log.e(TAG, "Column names: " + column_names);

			int order_number_column_index = cursor
					.getColumnIndex(KEY_ORDER_NUMBER);
			int revenue_cents_column_index = cursor
					.getColumnIndex(KEY_REVENUE_CENTS);
			int merchant_item_id_column_index = cursor
					.getColumnIndex(KEY_MERCHANT_ITEM_ID);
			int order_timestamp_column_index = cursor
					.getColumnIndex(KEY_ORDER_TIMESTAMP);

			if (cursor.moveToFirst()) {
				do {
					long merchant_item_id = cursor
							.getLong(merchant_item_id_column_index);
					long order_number = cursor
							.getLong(order_number_column_index);
					int revenue_cents = cursor
							.getInt(revenue_cents_column_index);
					long timestamp = cursor
							.getLong(order_timestamp_column_index);
					Log.i(TAG, "Merchant Item ID: " + merchant_item_id
							+ "; Order #: " + order_number + "; Cents: "
							+ revenue_cents + "; timestamp: " + timestamp);
				} while (cursor.moveToNext());
			}

			cursor.close();
			db.close();
		}

		// ========================================================================
		public List<Long> dumpRawTransactionItems() {

			Log.d(TAG, "Dumping raw labeled transaction items...");
			SQLiteDatabase db = getReadableDatabase();
			Cursor c = db.query(VIEW_RAW_TRANSACTION_ITEMS, new String[] {
					KEY_ORDER_NUMBER, KEY_MERCHANT_ITEM_ID, KEY_ITEM_NAME,
					KEY_CANONICAL_ITEM_ID }, null, null, KEY_MERCHANT_ITEM_ID,
					null, KEY_MERCHANT_ITEM_ID + " ASC");

			List<Long> unlabeled_merchant_items = new ArrayList<Long>();
			while (c.moveToNext()) {
				long order_number = c.getLong(0);
				long merchant_item_id = c.getLong(1);
				String merchant_item_name = c.getString(2);
				long canonical_id = c.getLong(3);

				// Log.i(TAG, "Order number: " + order_number +
				// "; merchant item ID: " + merchant_item_id + "; Label: " +
				// merchant_item_name + "; Canonical ID: " + canonical_id);
				unlabeled_merchant_items.add(order_number);
			}

			c.close();
			db.close();
			return unlabeled_merchant_items;
		}

		// ========================================================================
		public void dumpParentsAndChildrenJoined() {

			Log.d(TAG, "Dumping parents and children from "
					+ VIEW_RAW_TRANSACTION_ITEMS + "...");

			List<Long> parent_list = new ArrayList<Long>();

			SQLiteDatabase db = getReadableDatabase();

			Cursor cursor = db
					.query(VIEW_RAW_TRANSACTION_ITEMS,
							new String[] { KEY_CANONICAL_ITEM_ID }, null, null,
							KEY_CANONICAL_ITEM_ID, null, KEY_CANONICAL_ITEM_ID
									+ " ASC");

			int canonical_item_id_index = cursor
					.getColumnIndex(KEY_CANONICAL_ITEM_ID);
			if (cursor.moveToFirst()) {
				do {
					long canonical_item_id = cursor
							.getLong(canonical_item_id_index);
					parent_list.add(canonical_item_id);

				} while (cursor.moveToNext());
			}

			cursor.close();

			for (long canonical_id : parent_list) {

				Log.i(TAG, "Canonical Item ID: " + canonical_id);

				Cursor cursor2 = db.query(VIEW_RAW_TRANSACTION_ITEMS,
						new String[] { KEY_MERCHANT_ITEM_ID, KEY_ITEM_NAME, },
						KEY_MERCHANT_ITEM_ID + "=?", new String[] { Long
								.toString(canonical_id) },
						KEY_MERCHANT_ITEM_ID, null, KEY_MERCHANT_ITEM_ID
								+ " ASC");

				int merchant_item_id_column_index = cursor2
						.getColumnIndex(KEY_MERCHANT_ITEM_ID);
				int item_name_column_index = cursor2
						.getColumnIndex(KEY_ITEM_NAME);

				if (cursor2.moveToFirst()) {
					do {
						long merchant_item_id = cursor2
								.getLong(merchant_item_id_column_index);
						String title = cursor2
								.getString(item_name_column_index);

						Log.d(TAG, "    Merchant Item ID: " + merchant_item_id
								+ "; Title: " + title);

					} while (cursor2.moveToNext());
				} else {
					Log.e(TAG, "    No children.");
				}
				cursor2.close();
			}

			db.close();
		}

		// ========================================================================
		public void dumpParentsAndChildrenRaw() {

			Log.d(TAG, "Dumping parents and children (raw)...");

			List<Long> parent_list = new ArrayList<Long>();

			SQLiteDatabase db = getReadableDatabase();

			Cursor cursor = db
					.query(TABLE_GOOGLE_CHECKOUT_PRODUCTS,
							new String[] { KEY_CANONICAL_ITEM_ID }, null, null,
							KEY_CANONICAL_ITEM_ID, null, KEY_CANONICAL_ITEM_ID
									+ " ASC");

			int canonical_item_id_index = cursor
					.getColumnIndex(KEY_CANONICAL_ITEM_ID);
			if (cursor.moveToFirst()) {
				do {
					long canonical_item_id = cursor
							.getLong(canonical_item_id_index);
					parent_list.add(canonical_item_id);

				} while (cursor.moveToNext());
			}

			cursor.close();

			for (long canonical_id : parent_list) {

				Log.i(TAG, "Canonical Item ID: " + canonical_id);

				Cursor cursor2 = db.query(TABLE_GOOGLE_CHECKOUT_PRODUCTS,
						new String[] { KEY_MERCHANT_ITEM_ID, KEY_ITEM_NAME, },
						KEY_MERCHANT_ITEM_ID + "=?", new String[] { Long
								.toString(canonical_id) },
						KEY_MERCHANT_ITEM_ID, null, KEY_MERCHANT_ITEM_ID
								+ " ASC");

				int merchant_item_id_column_index = cursor2
						.getColumnIndex(KEY_MERCHANT_ITEM_ID);
				int item_name_column_index = cursor2
						.getColumnIndex(KEY_ITEM_NAME);

				if (cursor2.moveToFirst()) {
					do {
						long merchant_item_id = cursor2
								.getLong(merchant_item_id_column_index);
						String title = cursor2
								.getString(item_name_column_index);

						Log.d(TAG, "    Merchant Item ID: " + merchant_item_id
								+ "; Title: " + title);

					} while (cursor2.moveToNext());
				} else {
					Log.e(TAG, "    No children.");
				}
				cursor2.close();
			}

			db.close();
		}

		// ========================================================================
		public void dumpUniqueRawMerchantItemIds() {

			Log.d(TAG, "Dumping FULL range of "
					+ TABLE_GOOGLE_CHECKOUT_PURCHASES + ":");

			SQLiteDatabase db = getReadableDatabase();

			Cursor cursor = db.query(TABLE_GOOGLE_CHECKOUT_PURCHASES,
					new String[] { KEY_ORDER_NUMBER, KEY_REVENUE_CENTS,
							KEY_MERCHANT_ITEM_ID, KEY_ORDER_TIMESTAMP }, null,
					null, KEY_MERCHANT_ITEM_ID, // Group by
					null, null);

			String column_names = TextUtils.join(", ", cursor.getColumnNames());
			Log.e(TAG, "Column names: " + column_names);

			int order_number_column_index = cursor
					.getColumnIndex(KEY_ORDER_NUMBER);
			int revenue_cents_column_index = cursor
					.getColumnIndex(KEY_REVENUE_CENTS);
			int merchant_item_id_column_index = cursor
					.getColumnIndex(KEY_MERCHANT_ITEM_ID);
			int order_timestamp_column_index = cursor
					.getColumnIndex(KEY_ORDER_TIMESTAMP);

			if (cursor.moveToFirst()) {
				do {
					long merchant_item_id = cursor
							.getLong(merchant_item_id_column_index);
					long order_number = cursor
							.getLong(order_number_column_index);
					int revenue_cents = cursor
							.getInt(revenue_cents_column_index);
					long timestamp = cursor
							.getLong(order_timestamp_column_index);
					Log.i(TAG, "Merchant Item ID: " + merchant_item_id
							+ "; Order #: " + order_number + "; Cents: "
							+ revenue_cents + "; timestamp: " + timestamp);
				} while (cursor.moveToNext());
			}

			cursor.close();
			db.close();
		}

		// ========================================================================
		public List<Long> queryOrdersByItemId(DateRange date_range, long item_id) {

			SQLiteDatabase db = getReadableDatabase();

			Cursor c = db.query(TABLE_GOOGLE_CHECKOUT_PURCHASES,
					new String[] { KEY_ORDER_NUMBER }, KEY_MERCHANT_ITEM_ID
							+ "=?" + " AND " + KEY_ORDER_TIMESTAMP + ">=?"
							+ " AND " + KEY_ORDER_TIMESTAMP + "<=?",
					new String[] { Long.toString(item_id),
							Long.toString(date_range.start.getTime()),
							Long.toString(date_range.end.getTime()) }, null,
					null, null);

			List<Long> order_numbers = new ArrayList<Long>();
			while (c.moveToNext()) {
				order_numbers.add(c.getLong(0));
			}

			c.close();
			db.close();
			return order_numbers;
		}

		// ========================================================================
		// Queries on the closed interval [start, end]
		public List<Long> queryOrdersWithZeroItemIds(DateRange date_range) {

			SQLiteDatabase db = getReadableDatabase();

			Cursor c = db.query(TABLE_GOOGLE_CHECKOUT_PURCHASES,
					new String[] { KEY_ORDER_NUMBER }, KEY_MERCHANT_ITEM_ID
							+ "=" + 0 + " AND " + KEY_ORDER_TIMESTAMP + ">=?"
							+ " AND " + KEY_ORDER_TIMESTAMP + "<=?", date_range
							.getRangeAsStringArray(), null, null, null);

			List<Long> order_numbers = new ArrayList<Long>();
			while (c.moveToNext()) {
				order_numbers.add(c.getLong(0));
			}

			c.close();
			db.close();
			return order_numbers;
		}

		// ====================================================================
		public Cursor getCachedMarketApps() {

			SQLiteDatabase db = getReadableDatabase();

			Cursor cursor = db.query(TABLE_MARKET_APPS, new String[] {
					KEY_APP_ID + " AS " + BaseColumns._ID,
					KEY_RATING_ALERT_THRESHOLD, KEY_APP_TITLE,
					KEY_APP_PRICE_MICROS, }, null, null, null, null, null);

			return cursor;
		}

		// ========================================================================
		// Preserve for debugging.
		public void dumpConsolidatedItemsView() {

			Log.d(TAG, "Dumping contents of "
					+ VIEW_CONSOLIDATED_GOOGLE_CHECKOUT_ITEMS + ":");

			SQLiteDatabase db = getReadableDatabase();

			Cursor cursor = db.query(VIEW_CONSOLIDATED_GOOGLE_CHECKOUT_ITEMS,
			// new String[] {KEY_MERCHANT_ITEM_ID, KEY_ITEM_NAME,
			// KEY_ITEM_DESCRIPTION},
					null, // XXX
					null, null, null, null, null);

			String column_names = TextUtils.join(", ", cursor.getColumnNames());
			Log.e(TAG, "Column names: " + column_names);

			int child_id_column_index = cursor
					.getColumnIndex(KEY_CHILD_ITEM_ID);
			int app_id_column_index = cursor.getColumnIndex(KEY_APP_ID);
			int description_column_index = cursor
					.getColumnIndex(KEY_ITEM_DESCRIPTION);
			int title_column_index = cursor.getColumnIndex(KEY_ITEM_NAME);

			if (cursor.moveToFirst()) {
				do {
					long id = cursor.getLong(app_id_column_index);
					long child_id = cursor.getLong(child_id_column_index);
					String title = cursor.getString(title_column_index);
					Log.i(TAG, "Child ID: " + child_id + "; Merchant ID: " + id
							+ "; Title: " + title);
				} while (cursor.moveToNext());
			}

			cursor.close();
			db.close();
		}

		// ========================================================================
		// Preserve for debugging.
		public void dumpMarketAppsTable() {

			Log.d(TAG, "Dumping contents of " + TABLE_MARKET_APPS + ":");

			SQLiteDatabase db = getReadableDatabase();

			Cursor cursor = db.query(TABLE_MARKET_APPS, null, null, null, null,
					null, null);

			String column_names = TextUtils.join(", ", cursor.getColumnNames());
			Log.e(TAG, "Column names: " + column_names);

			int KEY_APP_ID_column_index = cursor.getColumnIndex(KEY_APP_ID);
			int KEY_RATING_ALERT_THRESHOLD_column_index = cursor
					.getColumnIndex(KEY_RATING_ALERT_THRESHOLD);
			int KEY_APP_PRICE_MICROS_column_index = cursor
					.getColumnIndex(KEY_APP_PRICE_MICROS);
			int KEY_APP_VERSION_CODE_column_index = cursor
					.getColumnIndex(KEY_APP_VERSION_CODE);
			int title_column_index = cursor.getColumnIndex(KEY_APP_TITLE);
			int KEY_APP_RATING_COUNT_column_index = cursor
					.getColumnIndex(KEY_APP_RATING_COUNT);
			int KEY_GOT_EARLIEST_COMMENT_column_index = cursor
					.getColumnIndex(KEY_GOT_EARLIEST_COMMENT);
			int KEY_GOT_MAX_COMMENTS_column_index = cursor
					.getColumnIndex(KEY_GOT_MAX_COMMENTS);
			int KEY_LAST_COMMENTS_UPDATE_TIMESTAMP_column_index = cursor
					.getColumnIndex(KEY_LAST_COMMENTS_UPDATE_TIMESTAMP);
			int KEY_APP_AGGREGATE_RATING_column_index = cursor
					.getColumnIndex(KEY_APP_OVERALL_RATING);

			int KEY_PUBLISHER_ID_column_index = cursor
			.getColumnIndex(KEY_PUBLISHER_ID);
			
			
			if (cursor.moveToFirst()) {
				do {
					long id = cursor.getLong(KEY_APP_ID_column_index);
					String title = cursor.getString(title_column_index);
					int price_micros = cursor
							.getInt(KEY_APP_PRICE_MICROS_column_index);
					
					long pub_id = cursor.getLong(KEY_PUBLISHER_ID_column_index);
					Log.i(TAG, "ID: " + id + "; Title: " + title
							+ "; Price (micros): " + price_micros + "; Pub ID: " + pub_id);

				} while (cursor.moveToNext());
			}

			cursor.close();
			db.close();
		}

		// ========================================================================
		public void dumpCheckoutItemsTable() {

			Log.d(TAG, "Dumping contents of " + TABLE_GOOGLE_CHECKOUT_PRODUCTS
					+ ":");

			SQLiteDatabase db = getReadableDatabase();

			Cursor cursor = db.query(TABLE_GOOGLE_CHECKOUT_PRODUCTS,
			// new String[] {KEY_MERCHANT_ITEM_ID, KEY_CANONICAL_ITEM_ID,
			// KEY_ITEM_NAME, KEY_ITEM_DESCRIPTION},
					null, // XXX
					null, null, null, null, null);

			String column_names = TextUtils.join(", ", cursor.getColumnNames());
			Log.e(TAG, "Column names: " + column_names);

			int merchant_id_column_index = cursor
					.getColumnIndex(KEY_MERCHANT_ITEM_ID);
			int canonical_id_column_index = cursor
					.getColumnIndex(KEY_CANONICAL_ITEM_ID);
			int description_column_index = cursor
					.getColumnIndex(KEY_ITEM_DESCRIPTION);
			int title_column_index = cursor.getColumnIndex(KEY_ITEM_NAME);

			if (cursor.moveToFirst()) {
				do {
					long id = cursor.getLong(merchant_id_column_index);
					long canonical_id = cursor
							.getLong(canonical_id_column_index);
					String title = cursor.getString(title_column_index);
					Log.i(TAG, "ID: " + id + "; Parent: " + canonical_id
							+ "; Title: " + title);
				} while (cursor.moveToNext());
			}

			cursor.close();
			db.close();
		}
	}

	// ========================================================================
	public boolean hasUngroupedAppDeployments(SQLiteDatabase db) {

		Cursor cursor = db.query(VIEW_AGGREGATE_CONSOLIDATED_MARKET_LINKED,
				null, KEY_APP_TITLE + " ISNULL", null, null, null, null,
				Integer.toString(1));
		boolean has = cursor.moveToFirst();

		cursor.close();
		return has;
	}

	// ========================================================================
	public Cursor getCacheRanges() {

		SQLiteDatabase db = getReadableDatabase();

		Cursor cursor = db.query(TABLE_CACHE_SPANS, new String[] {
				KEY_SPAN_ID + " AS " + BaseColumns._ID,
				KEY_SPAN_START_MILLISECONDS, KEY_SPAN_END_MILLISECONDS }, null,
				null, null, null, KEY_SPAN_START_MILLISECONDS + " ASC");

		return cursor;
	}

	// ========================================================================
	public int getPurchaseCountInRange(DateRange date_range) {

		SQLiteDatabase db = getReadableDatabase();

		Cursor c = db.query(TABLE_GOOGLE_CHECKOUT_PURCHASES,
				new String[] { "COUNT(" + KEY_ORDER_TIMESTAMP + ")" },
				KEY_ORDER_TIMESTAMP + ">=? AND " + KEY_ORDER_TIMESTAMP + "<=?",
				date_range.getRangeAsStringArray(), null, null, null);

		int count = 0;
		if (c.moveToFirst())
			count = c.getInt(0);

		c.close();
		db.close();
		return count;
	}

	// ========================================================================
	public Date getLatestSaleDateWithin(DateRange date_range) {

		SQLiteDatabase db = getReadableDatabase();

		Cursor c = db.query(TABLE_GOOGLE_CHECKOUT_PURCHASES,
				new String[] { "MAX(" + KEY_ORDER_TIMESTAMP + ")" },
				KEY_ORDER_TIMESTAMP + ">=? AND " + KEY_ORDER_TIMESTAMP + "<=?",
				date_range.getRangeAsStringArray(), null, null, null);

		Date last_date = null;
		if (c.moveToFirst())
			last_date = new Date(c.getLong(0));

		c.close();
		db.close();
		return last_date;
	}

	// ========================================================================
	public String getMarketAppPackageName(long app_id) {

		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(TABLE_MARKET_APPS,
				new String[] { KEY_APP_PACKAGE_NAME }, KEY_APP_ID + "=?",
				new String[] { Long.toString(app_id) }, null, null, null);

		String package_name = null;
		if (c.moveToFirst())
			package_name = c.getString(0);

		c.close();
		db.close();

		return package_name;
	}

	// ========================================================================
	public int unConsolidateItem(long child_id) {

		Log.e(TAG, "Child ID: " + child_id);

		SQLiteDatabase db = getWritableDatabase();

		ContentValues cv = new ContentValues();
		cv.put(KEY_CANONICAL_ITEM_ID, child_id);
		int update_count = db.update(TABLE_GOOGLE_CHECKOUT_PRODUCTS, cv,
				KEY_MERCHANT_ITEM_ID + "=?", new String[] { Long
						.toString(child_id) });

		db.close();

		Log.d(TAG, "Ungrouped " + update_count + " item.");
		return update_count;
	}

	// ========================================================================
	static class AppIdAndTitle {
		public AppIdAndTitle(long id, String title) {
			this.id = id;
			this.title = title;
		}

		long id;
		String title;
	}

	// ========================================================================
	public int autoGroupMerchantItems() {

		// Algorithm
		// First, get the names and IDs of the Android Market apps (parents).
		// Then do an update() for the CANONICAL_ITEM_IDs in TABLE_ITEMS where
		// the "title" field matches that of our parent.

		SQLiteDatabase db2 = getReadableDatabase();
		Cursor c2 = db2.query(VIEW_RATED_MARKET_APPS, new String[] {
				KEY_APP_ID, KEY_APP_TITLE }, null, null, null, null, null);

		List<AppIdAndTitle> natural_parents = new ArrayList<AppIdAndTitle>();
		while (c2.moveToNext())
			natural_parents.add(new AppIdAndTitle(c2.getLong(0), c2
					.getString(1)));

		c2.close();
		db2.close();

		Log.e(TAG, "Found " + natural_parents.size() + " parents.");

		// Now assign the correct parent to all the children
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();

		ContentValues cv = new ContentValues();
		int total_update_count = 0;
		for (AppIdAndTitle parent : natural_parents) {

			cv.put(KEY_CANONICAL_ITEM_ID, parent.id);
			int update_count = db.update(TABLE_GOOGLE_CHECKOUT_PRODUCTS, cv,
					KEY_ITEM_NAME + " LIKE ?", // Performs a case-insensitive
												// match
					new String[] { parent.title });

			Log.d(TAG, "Updated " + update_count + " entries for "
					+ parent.title);
			total_update_count += update_count;
		}

		try {
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		db.close();

		return total_update_count;
	}

	// ========================================================================
	public int consolidateItems(long parent, List<Long> children) {

		// First we need to check whether any of the selected children are
		// already parents.
		// If so, adopt their children.
		SQLiteDatabase db2 = getReadableDatabase();
		List<Long> subchildren = new ArrayList<Long>();
		for (long child_id : children)
			subchildren.addAll(getSimpleConsolidatedItemIDs(db2, child_id));
		db2.close();
		children.addAll(subchildren);

		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();

		ContentValues cv = new ContentValues();

		int update_count = 0;
		for (long child_id : new HashSet<Long>(children)) { // Duplicates
															// removed

			cv.put(KEY_CANONICAL_ITEM_ID, parent);
			update_count += db.update(TABLE_GOOGLE_CHECKOUT_PRODUCTS, cv,
					KEY_MERCHANT_ITEM_ID + "=?", new String[] { Long
							.toString(child_id) });

			cv.clear();
		}

		try {
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		db.close();

		return update_count;
	}

	// ========================================================================
	public void populateMarketApps(List<App> apps) {

		SQLiteDatabase db = getWritableDatabase();
		
		// Load the PUBLISHER table
		Map<String, Long> publishers = new HashMap<String, Long>();
		Cursor cursor = db.query(TABLE_PUBLISHERS,
				new String[] {KEY_PUBLISHER_ID, KEY_PUBLISHER_NAME},
				null, null, null, null, null);
		while (cursor.moveToNext())
			publishers.put(cursor.getString(1), cursor.getLong(0));
		cursor.close();
		
		db.beginTransaction();

		ContentValues cv = new ContentValues();
		for (App app : apps) {

			cv.put(KEY_APP_ID, Long.parseLong(app.getId()));

			// Inserts a new entry into the PUBLISHER table if necessary
			String creator = app.getCreator();
			cv.put(KEY_APP_CREATOR_NAME, creator);
			long publisher_id;
			if (publishers.containsKey(creator)) {
				publisher_id = publishers.get(creator);
			} else {
				ContentValues cv2 = new ContentValues();
				cv2.put(KEY_PUBLISHER_NAME, creator);
				publisher_id = db.insert(TABLE_PUBLISHERS, null, cv2);
				publishers.put(creator, publisher_id);
			}
			cv.put(KEY_PUBLISHER_ID, publisher_id);
			
			cv.put(KEY_APP_CREATOR_ID, app.getCreatorId());
			// Log.d(TAG, "Creator ID: " + app.getCreatorId());

			cv.put(KEY_APP_PRICE_MICROS, app.getPriceMicros());
			cv.put(KEY_APP_VERSION_CODE, app.getVersionCode());
			cv.put(KEY_APP_TITLE, app.getTitle());
			cv.put(KEY_APP_RATING_COUNT, app.getRatingsCount());
			cv.put(KEY_APP_OVERALL_RATING, Float.parseFloat(app.getRating()));
			cv.put(KEY_APP_PACKAGE_NAME, app.getPackageName());

			long rowid = db.insert(TABLE_MARKET_APPS, null, cv);

			cv.clear();
		}

		try {
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		db.close();
	}

	// ========================================================================
	public long getPublisherId(String publisher_name) {

		if (publisher_name == null) return INVALID_PUBLISHER_ID;
			
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(TABLE_PUBLISHERS,
				new String[] {KEY_PUBLISHER_ID},
				KEY_PUBLISHER_NAME + "=?",
				new String[] {publisher_name},
				null, null, null);
		
		long publisher_id = INVALID_PUBLISHER_ID;
		if (cursor.moveToFirst())
			publisher_id = cursor.getLong(0);
		
		cursor.close();
		db.close();
		
		return publisher_id;
	}

	// ========================================================================
	/**
	 * Converts an unsigned 64-bit integer (as a String) to a signed 64-bit
	 * integer (as a long), for use in the database.
	 */
	static long normalizeAuthorIdString(String author_id_string) {

		// The Author ID string is an unsigned 64-bit integer,
		// so we must wrap with a BigInteger and offset by 2^63 before casting
		// as a long.
		BigInteger bi = new BigInteger(author_id_string);
		// Log.e(TAG, "Original authorID: " + comment.getAuthorId());
		bi = bi.add(new BigInteger(Long.toString(Long.MIN_VALUE)));
		// Log.d(TAG, "Adding long min value: " +
		// Long.toString(Long.MIN_VALUE));
		long reinterpreted_author_id = bi.longValue();
		// Log.i(TAG, "Reinterpreted authorID: " + reinterpreted_author_id);

		return reinterpreted_author_id;
	}

	// ========================================================================
	public static class CommentAbortEarlyData {
		Map<Long, Date> authoring_dates = new HashMap<Long, Date>();
		boolean has_earliest = false;
		boolean has_max = false;

		public boolean shouldAbortEarly() {
			return this.has_earliest || this.has_max;
		}

		public boolean checkAlreadyHasCommentInBatch(
				Collection<Comment> comments) {

			for (Comment comment : comments) {
				long author_id = normalizeAuthorIdString(comment.getAuthorId());
				if (this.authoring_dates.containsKey(author_id)) {
					// Log.d(TAG,
					// "Encountered a previously stored comment from " +
					// this.authoring_dates.get(author_id));
					return true;
				}
			}

			return false;
		}
	}

	// ========================================================================
	public CommentAbortEarlyData getCommentAbortEarlyData(long app_id) {

		CommentAbortEarlyData abort_early_data = new CommentAbortEarlyData();

		SQLiteDatabase db = getReadableDatabase();

		// First, we must check whether the comments have been retrieved
		// all the way to the beginning.
		Cursor c1 = db.query(TABLE_MARKET_APPS, new String[] {
				KEY_GOT_EARLIEST_COMMENT, KEY_GOT_MAX_COMMENTS }, KEY_APP_ID
				+ "=?", new String[] { Long.toString(app_id) }, null, null,
				null);

		if (c1.moveToFirst()) {
			abort_early_data.has_earliest = c1.getInt(0) != FALSE;
			abort_early_data.has_max = c1.getInt(1) != FALSE;
		}
		c1.close();

		Cursor cursor = db.query(TABLE_MARKET_COMMENTS, new String[] {
				KEY_COMMENT_AUTHOR_ID, KEY_RATING_TIMESTAMP }, KEY_APP_ID
				+ "=?", new String[] { Long.toString(app_id) }, null, null,
				null);

		while (cursor.moveToNext()) {
			abort_early_data.authoring_dates.put(cursor.getLong(0), new Date(
					cursor.getLong(1)));
		}
		cursor.close();
		db.close();

		return abort_early_data;
	}

	// ========================================================================
	public int markAllCommentsAsRead(boolean is_read) {

		SQLiteDatabase db = getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(KEY_COMMENT_READ, is_read ? TRUE : FALSE);
		int update_count = db.update(TABLE_MARKET_COMMENTS, cv,
				KEY_COMMENT_READ + "=" + (is_read ? FALSE : TRUE), null);
		db.close();

		return update_count;
	}

	// ========================================================================
	public int markAllAppCommentsAsRead(long app_id) {

		int threshold = getRatingAlertThreshold(app_id);
		Log.d(TAG, "Marking all comments at or below " + threshold
				+ " as read.");

		SQLiteDatabase db = getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(KEY_COMMENT_READ, TRUE);
		int update_count = db.update(TABLE_MARKET_COMMENTS, cv, KEY_APP_ID
				+ "=? AND " + KEY_RATING_VALUE + "<=? AND " + KEY_COMMENT_READ
				+ "=" + FALSE, new String[] { Long.toString(app_id),
				Integer.toString(threshold) });
		db.close();

		return update_count;
	}

	// ========================================================================
	public int markCommentAsRead(long comment_id) {
		String column = KEY_COMMENT_READ;

		// Log.d(TAG, "Setting " + column + " to True: " + comment_id);

		SQLiteDatabase db = getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(column, TRUE);
		int update_count = db.update(TABLE_MARKET_COMMENTS, cv, "ROWID=? AND "
				+ column + "=" + FALSE, new String[] { Long
				.toString(comment_id) });
		db.close();

		return update_count;
	}

	// ========================================================================
	public int setTrueAppBoolean(String column, long app_id) {

		// Log.e(TAG, "Setting " + column + " to True: " + app_id);

		SQLiteDatabase db = getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(column, TRUE);
		int update_count = db.update(TABLE_MARKET_APPS, cv, KEY_APP_ID
				+ "=? AND " + column + "=" + FALSE, new String[] { Long
				.toString(app_id) });
		db.close();

		return update_count;
	}

	// ========================================================================
	public Map<Long, Date> getAllLastCommentSyncDates() {

		Map<Long, Date> last_app_sync_dates = new HashMap<Long, Date>();

		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(TABLE_MARKET_APPS, new String[] { KEY_APP_ID,
				KEY_LAST_COMMENTS_UPDATE_TIMESTAMP }, null, null, null, null,
				null);

		while (cursor.moveToNext()) {
			long app_id = cursor.getLong(0);
			Date last_sync_date = new Date(cursor.getLong(1));

			last_app_sync_dates.put(app_id, last_sync_date);
		}
		cursor.close();
		db.close();

		return last_app_sync_dates;
	}

	// ========================================================================
	public Date getLastCommentSyncDate(long app_id) {

		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(TABLE_MARKET_APPS,
				new String[] { KEY_LAST_COMMENTS_UPDATE_TIMESTAMP }, KEY_APP_ID
						+ "=?", new String[] { Long.toString(app_id) }, null,
				null, null);

		Date last_sync_date = new Date(0);
		if (cursor.moveToFirst()) {
			last_sync_date.setTime(cursor.getLong(0));
		}
		cursor.close();
		db.close();

		return last_sync_date;
	}

	// ========================================================================
	/**
	 * This also updates the last "sync date".
	 */
	public void populateComments(long app_id, List<Comment> comments) {

		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();

		ContentValues cv = new ContentValues();
		for (Comment comment : comments) {

			cv.put(KEY_APP_ID, app_id);

			// cv.put(KEY_COMMENT_AUTHOR_ID,
			// Long.parseLong(comment.getAuthorId()));
			long reinterpreted_author_id = normalizeAuthorIdString(comment
					.getAuthorId());
			cv.put(KEY_COMMENT_AUTHOR_ID, reinterpreted_author_id);
			cv.put(KEY_COMMENT_AUTHOR_NAME, comment.getAuthorName());
			cv.put(KEY_RATING_VALUE, comment.getRating());
			cv.put(KEY_COMMENT_TEXT, comment.getText());
			cv.put(KEY_RATING_TIMESTAMP, comment.getCreationTime());

			// So as not to overwrite the "read" flag, we update
			// the comment if possible, first.
			int update_count = db.update(TABLE_MARKET_COMMENTS, cv, KEY_APP_ID
					+ "=? AND " + KEY_COMMENT_AUTHOR_ID + "=?", new String[] {
					Long.toString(app_id),
					Long.toString(reinterpreted_author_id) });

			if (update_count == 0) {
				long rowid = db.insert(TABLE_MARKET_COMMENTS, null, cv);

				// Log.d(TAG, "Inserting new comment as row: " + rowid);

			} else {
				// Log.i(TAG, "Comment by " + reinterpreted_author_id +
				// " on app " + app_id + " already present; updating.");
			}

			cv.clear();
		}

		cv.put(KEY_LAST_COMMENTS_UPDATE_TIMESTAMP, new Date().getTime());
		db.update(TABLE_MARKET_APPS, cv, KEY_APP_ID + "=?", new String[] { Long
				.toString(app_id) });

		try {
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		db.close();
	}

	// ========================================================================
	private static String getFilterString(ViewType filter_mode) {
		String filter_string = null;
		switch (filter_mode) {
		case FREE:
			filter_string = KEY_APP_PRICE_MICROS + "=" + 0;
			break;
		case PAID:
			filter_string = KEY_APP_PRICE_MICROS + "!=" + 0;
			break;
		}

		return filter_string;
	}

	// ========================================================================
	public Cursor getCachedLatestMarketApps(ViewType filter_mode) {

		SQLiteDatabase db = getReadableDatabase();

		String filter_string = getFilterString(filter_mode);
		Cursor cursor = db.query(VIEW_RATED_LATEST_MARKET_APPS, new String[] {
				KEY_APP_ID + " AS " + BaseColumns._ID,
				KEY_RATING_ALERT_THRESHOLD, KEY_APP_TITLE,
				KEY_APP_PRICE_MICROS, KEY_AGGREGATE_RATING_COUNT,
				KEY_APP_RATING_COUNT, KEY_APP_OVERALL_RATING,
				KEY_LATEST_RATING, KEY_GOT_EARLIEST_COMMENT,
				KEY_GOT_MAX_COMMENTS }, filter_string, null, null, null,
				KEY_APP_TITLE + " ASC");

		return cursor;
	}

	// ========================================================================
	public String getAppTitle(long app_id) {

		SQLiteDatabase db = getReadableDatabase();

		Cursor cursor = db.query(TABLE_MARKET_APPS,
				new String[] { KEY_APP_TITLE }, KEY_APP_ID + "=?",
				new String[] { Long.toString(app_id) }, null, null, null);

		String app_title = null;
		if (cursor.moveToFirst()) {
			app_title = cursor.getString(0);
		}

		cursor.close();
		db.close();

		return app_title;
	}

	// ========================================================================
	public List<Long> getAppsWithNonzeroRatingThreshold() {

		SQLiteDatabase db = getReadableDatabase();

		List<Long> app_ids = new ArrayList<Long>();

		Cursor cursor = db.query(TABLE_MARKET_APPS,
				new String[] { KEY_APP_ID }, KEY_RATING_ALERT_THRESHOLD + "!="
						+ 0, null, null, null, null);

		while (cursor.moveToNext()) {
			app_ids.add(cursor.getLong(0));
		}

		cursor.close();
		db.close();

		return app_ids;
	}

	// ========================================================================
	public int getRatingAlertThreshold(long merchant_item_id) {

		SQLiteDatabase db = getReadableDatabase();

		Cursor cursor = db.query(TABLE_MARKET_APPS,
				new String[] { KEY_RATING_ALERT_THRESHOLD }, KEY_APP_ID + "=?",
				new String[] { Long.toString(merchant_item_id) }, null, null,
				null);

		int threshold = AppsOverviewActivity.DEFAULT_RATING_ALERT_THRESHOLD;
		if (cursor.moveToFirst())
			threshold = cursor.getInt(0);

		cursor.close();
		db.close();

		return threshold;
	}

	// ========================================================================
	public int countUnreadComments(long app_id) {

		return countComments(app_id, KEY_APP_ID + "=? AND " + KEY_COMMENT_READ
				+ "=" + FALSE, new String[] { Long.toString(app_id) });
	}

	// ========================================================================
	public int countAllComments(long app_id) {

		return countComments(app_id, KEY_APP_ID + "=?", new String[] { Long
				.toString(app_id), });
	}

	// ========================================================================
	public int countComments(long app_id, String whereClause, String[] whereArgs) {
		SQLiteDatabase db = getReadableDatabase();

		// Only fetches comments that have text attached.
		Cursor cursor = db.query(TABLE_MARKET_COMMENTS,
				new String[] { "COUNT(*)" }, whereClause, whereArgs, null,
				null, null);

		int comment_count = -1;
		if (cursor.moveToFirst()) {
			comment_count = cursor.getInt(0);
		}

		cursor.close();
		db.close();

		return comment_count;
	}

	// ========================================================================
	public List<Integer> getUnreadCommentsBelowThreshold(long merchant_item_id) {

		SQLiteDatabase db = getReadableDatabase();

		// Only fetches comments that have text attached.
		List<Integer> new_ratings = new ArrayList<Integer>();
		Cursor cursor = db.query(VIEW_MARKET_COMMENTS_LINKED,
				new String[] { KEY_RATING_VALUE }, KEY_APP_ID + "=?" + " AND "
						+ KEY_RATING_VALUE + "<=" + KEY_RATING_ALERT_THRESHOLD
						+ " AND " + KEY_COMMENT_READ + "=" + FALSE,
				new String[] { Long.toString(merchant_item_id) }, null, null,
				null);

		while (cursor.moveToNext()) {
			new_ratings.add(cursor.getInt(0));
		}

		cursor.close();
		db.close();

		return new_ratings;
	}

	// ========================================================================
	public int setRatingAlertThreshold(long merchant_item_id, int threshold) {

		SQLiteDatabase db = getWritableDatabase();

		ContentValues cv = new ContentValues();
		cv.put(KEY_RATING_ALERT_THRESHOLD, threshold);

		int update_count = db.update(TABLE_MARKET_APPS, cv, KEY_APP_ID + "=?",
				new String[] { Long.toString(merchant_item_id) });

		db.close();

		return update_count;
	}

	// ========================================================================
	public int setAllRatingAlertThresholds(int threshold, ViewType filter_mode) {

		SQLiteDatabase db = getWritableDatabase();

		ContentValues cv = new ContentValues();
		cv.put(KEY_RATING_ALERT_THRESHOLD, threshold);

		String filter_string = getFilterString(filter_mode);
		int update_count = db
				.update(TABLE_MARKET_APPS, cv, filter_string, null);

		db.close();

		return update_count;
	}

	// ========================================================================
	public void setMerchantItemNames(
			Map<Long, TitleDescription> merchant_item_label_map) {

		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();

		ContentValues cv = new ContentValues();

		for (Entry<Long, TitleDescription> order_pair : merchant_item_label_map
				.entrySet()) {

			cv.put(KEY_MERCHANT_ITEM_ID, order_pair.getKey());
			cv.put(KEY_CANONICAL_ITEM_ID, order_pair.getKey()); // Initially set
																// self as
																// parent
			cv.put(KEY_ITEM_NAME, order_pair.getValue().name);
			cv.put(KEY_ITEM_DESCRIPTION, order_pair.getValue().description);

			long rowid = db.insert(TABLE_GOOGLE_CHECKOUT_PRODUCTS, null, cv);

			cv.clear();
		}

		try {
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		db.close();

		Log.d(TAG, "Finished setting Merchant Item names");
	}

	// ========================================================================
	public void setMerchantItemIds(Map<Long, Long> ids_map)
			throws SQLiteDatabaseCorruptException {

		SQLiteDatabase db;
		try {
			db = getWritableDatabase();
		} catch (SQLiteDatabaseCorruptException e) {
			throw e;
		}

		db.beginTransaction();

		ContentValues cv = new ContentValues();

		for (Entry<Long, Long> order_pair : ids_map.entrySet()) {

			cv.put(KEY_MERCHANT_ITEM_ID, order_pair.getValue());
			int updated = db.update(TABLE_GOOGLE_CHECKOUT_PURCHASES, cv,
					KEY_ORDER_NUMBER + "=?", new String[] { Long
							.toString(order_pair.getKey()) });

			cv.clear();
		}

		try {
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		db.close();
	}

	// ========================================================================
	public long cacheMergeHelper(DateRange date_range) {

		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		long merged_span_id = mergeCacheSpan(db, date_range);

		try {
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		db.close();

		return merged_span_id;
	}

	// ========================================================================
	public long storePurchasesInTransaction(List<ChargeAmount> charges,
			DateRange date_range) {

		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();

		long merged_span_id = storePurchases(db, charges, date_range);

		try {
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		db.close();

		return merged_span_id;
	}

	// ========================================================================
	public long storePurchases(SQLiteDatabase db, List<ChargeAmount> charges,
			DateRange date_range) {

		ContentValues cv = new ContentValues();
		for (ChargeAmount charge : charges) {

			cv.put(KEY_ORDER_NUMBER, charge.google_order_number);
			cv.put(KEY_REVENUE_CENTS, charge.cents);
			cv.put(KEY_ORDER_TIMESTAMP, charge.date.getTime());
			// cv.put(KEY_MERCHANT_ITEM_ID, null); // Unknown at this time, gets
			// filled in later on demand

			long rowid = db.insert(TABLE_GOOGLE_CHECKOUT_PURCHASES, null, cv);
			cv.clear();
		}

		return mergeCacheSpan(db, date_range);
	}

	// ========================================================================
	class DateRangeRecord {

		DateRangeRecord(Cursor cursor) {
			this(cursor.getLong(0), cursor.getLong(1), cursor.getLong(2));
		}

		DateRangeRecord(long id, long start_ms, long end_ms) {
			this.id = id;
			this.range = new DateRange(new Date(start_ms), new Date(end_ms));
		}

		long id;
		DateRange range;
	}

	// ========================================================================
	Cursor querySpansEnclosingDate(SQLiteDatabase db, Date date,
			String order_clause) {
		String milliseconds_string = Long.toString(date.getTime());
		Cursor cursor = db.query(TABLE_CACHE_SPANS, new String[] { KEY_SPAN_ID,
				KEY_SPAN_START_MILLISECONDS, KEY_SPAN_END_MILLISECONDS },
				KEY_SPAN_START_MILLISECONDS + "<=? AND "
						+ KEY_SPAN_END_MILLISECONDS + ">=?", new String[] {
						milliseconds_string, milliseconds_string }, null, null,
				order_clause);

		return cursor;
	}

	// ========================================================================
	/**
	 * This routine requires a writable SQLite database. It should be wrapped in
	 * a Transaction.
	 */
	public long mergeCacheSpan(SQLiteDatabase db, DateRange date_range) {

		if (date_range == null || date_range.end == null
				|| date_range.start == null)
			return -1;

		// Log.d(TAG, "Merging span from " + date_range.start + " to " +
		// date_range.end);

		// Algorithm:
		// 0) If our new span completely straddles any spans, delete those.
		// 1) Query for the set of all spans (S) that straddle (in a closed
		// interval)
		// the start date. We're aiming for the largest interval, so order by
		// earliest
		// (ascending start timestamp).
		// 2) Query for the set of all spans (E) that enclose the end date (also
		// closed interval).
		// This time we order by descending end timestamp, since we're still
		// going for the
		// largest interval.
		// 3) If (S) and (E) are empty, simply insert our new span and return.
		// 4) If (S) is nonempty but (E) is empty,

		// Step 0: Delete straddled spans
		int deletion_count = db.delete(TABLE_CACHE_SPANS,
				KEY_SPAN_START_MILLISECONDS + ">? AND "
						+ KEY_SPAN_END_MILLISECONDS + "<?", new String[] {
						Long.toString(date_range.start.getTime()),
						Long.toString(date_range.end.getTime()) });
		// Log.d(TAG, "Deleted " + deletion_count +
		// " records straddled by the new span.");

		// Step 1: Query earliest intervals that straddle our start point.
		Cursor c0 = querySpansEnclosingDate(db, date_range.start,
				KEY_SPAN_START_MILLISECONDS + " ASC");
		DateRangeRecord earliest_starting_span = null;
		List<DateRangeRecord> later_starting_records = new ArrayList<DateRangeRecord>();
		if (c0.moveToFirst()) {
			earliest_starting_span = new DateRangeRecord(c0);
			while (c0.moveToNext())
				later_starting_records.add(new DateRangeRecord(c0));
		}
		c0.close();

		// Step 2: Query latest intervals that straddle our end point.
		Cursor c1 = querySpansEnclosingDate(db, date_range.end,
				KEY_SPAN_END_MILLISECONDS + " DESC");
		DateRangeRecord latest_ending_span = null;
		List<DateRangeRecord> earlier_ending_records = new ArrayList<DateRangeRecord>();
		if (c1.moveToFirst()) {
			latest_ending_span = new DateRangeRecord(c1);
			while (c1.moveToNext())
				earlier_ending_records.add(new DateRangeRecord(c1));
		}
		c1.close();

		// Step 3: If there is a span for neither the start point nor the end
		// point,
		// simply create a new span and return.
		if (earliest_starting_span == null && latest_ending_span == null) {

			// Log.d(TAG, "No overlaps; creating new span.");

			ContentValues cv = new ContentValues();
			cv.put(KEY_SPAN_START_MILLISECONDS, date_range.start.getTime());
			cv.put(KEY_SPAN_END_MILLISECONDS, date_range.end.getTime());
			long inserted_span_id = db.insert(TABLE_CACHE_SPANS, null, cv);

			return inserted_span_id;

		} else if (earliest_starting_span != null && latest_ending_span == null) {
			// Step 4: In this case, we found an overlapping start, but no
			// overlapping end.
			// We will extend the start to cover our new range, and delete any
			// intermediates.

			// Log.d(TAG, "Start span set, end span null.");

			int new_deletion_count = 0;
			for (DateRangeRecord record : later_starting_records) {
				new_deletion_count += db.delete(TABLE_CACHE_SPANS, KEY_SPAN_ID
						+ "=?", new String[] { Long.toString(record.id) });
			}

			ContentValues cv = new ContentValues();
			cv.put(KEY_SPAN_END_MILLISECONDS, date_range.end.getTime());
			int updated_count = db.update(TABLE_CACHE_SPANS, cv, KEY_SPAN_ID
					+ "=?", new String[] { Long
					.toString(earliest_starting_span.id) });

			// Log.d(TAG, "Updated " + updated_count + " span and deleted " +
			// new_deletion_count + " more.");

			return earliest_starting_span.id;

		} else if (earliest_starting_span == null && latest_ending_span != null) {
			// This is the dual case of Step 4.

			// Log.d(TAG, "Start span set, null span set.");

			int new_deletion_count = 0;
			for (DateRangeRecord record : earlier_ending_records) {
				new_deletion_count += db.delete(TABLE_CACHE_SPANS, KEY_SPAN_ID
						+ "=?", new String[] { Long.toString(record.id) });
			}

			ContentValues cv = new ContentValues();
			cv.put(KEY_SPAN_START_MILLISECONDS, date_range.start.getTime());
			int updated_count = db.update(TABLE_CACHE_SPANS, cv, KEY_SPAN_ID
					+ "=?",
					new String[] { Long.toString(latest_ending_span.id) });

			// Log.d(TAG, "Updated " + updated_count + " span and deleted " +
			// new_deletion_count + " more.");

			return latest_ending_span.id;

		} else {
			// Step 6: Neither of the sets are empty. It is possible that the
			// latest ending span may be the same span as the earliest starting
			// span. If that is the case, we don't need to insert anything, and
			// there shouldn't exist any spans inside it, so we needn't delete
			// anything
			// either.

			if (earliest_starting_span.id == latest_ending_span.id) {
				// Log.e(TAG,
				// "Range already covered; no insertion necessary. Covered range was: "
				// + earliest_starting_span.range.start + " to " +
				// earliest_starting_span.range.end);
				return earliest_starting_span.id;
			}

			// If we got here, then the two span extrema are not one and the
			// same.
			// We can safely delete everything on both of our "auxiliarry"
			// lists,
			// plus we extend our start span to cover the range of the end span,
			// and
			// delete the end span which becomes redundant.

			ContentValues cv = new ContentValues();
			cv.put(KEY_SPAN_END_MILLISECONDS, latest_ending_span.range.end
					.getTime());

			int updated_count = db.update(TABLE_CACHE_SPANS, cv, KEY_SPAN_ID
					+ "=?", new String[] { Long
					.toString(earliest_starting_span.id) });

			later_starting_records.addAll(earlier_ending_records);
			later_starting_records.add(latest_ending_span);
			int new_deletion_count = 0;
			for (DateRangeRecord record : later_starting_records) {
				new_deletion_count += db.delete(TABLE_CACHE_SPANS, KEY_SPAN_ID
						+ "=?", new String[] { Long.toString(record.id) });
			}

			// Log.d(TAG, "Updated " + updated_count + " record and deleted " +
			// new_deletion_count + " more.");

			return earliest_starting_span.id;
		}
	}

	// ========================================================================
	/** For use with the cache span gap finder only */
	DateRange baseSpanQuery(SQLiteDatabase db, String where_clause,
			String[] where_args, String order) {
		Cursor cursor = db.query(TABLE_CACHE_SPANS, new String[] {
				KEY_SPAN_START_MILLISECONDS, KEY_SPAN_END_MILLISECONDS },
				where_clause, where_args, null, null, order, "1");

		DateRange date_range = null;
		if (cursor.moveToFirst()) {
			date_range = new DateRange(new Date(cursor.getLong(0)), new Date(
					cursor.getLong(1)));
		}
		cursor.close();
		return date_range;
	}

	// ========================================================================
	// For use with the cache span gap finder only
	// Searches the half-open interval: (last_end, reqest_end]
	DateRange queryEarliestStartingSpanStartingBetween(SQLiteDatabase db,
			DateRange target_range) {
		return baseSpanQuery(db, KEY_SPAN_START_MILLISECONDS + ">? AND "
				+ KEY_SPAN_START_MILLISECONDS + "<=?", new String[] {
				Long.toString(target_range.start.getTime()),
				Long.toString(target_range.end.getTime()) },
				KEY_SPAN_START_MILLISECONDS + " ASC");
	}

	// ========================================================================
	/** For use with the cache span gap finder only */
	DateRange queryLatestEndingSpanStraddlingTime(SQLiteDatabase db,
			long starting_milliseconds) {
		String milliseconds_string = Long.toString(starting_milliseconds);
		return baseSpanQuery(db, KEY_SPAN_START_MILLISECONDS + "<=? AND "
				+ KEY_SPAN_END_MILLISECONDS + ">?", new String[] {
				milliseconds_string, milliseconds_string },
				KEY_SPAN_END_MILLISECONDS + " DESC");
	}

	// ========================================================================
	// Generates a list of all gaps in the cache within the given date range.
	// We have already established with isWithinCacheSpan() that there
	// is no single span that covers the whole date range.
	public List<DateRange> getUncachedDateRanges(DateRange requested_date_range) {

		SQLiteDatabase db = getReadableDatabase();
		// listAllCacheRanges(db);
		List<DateRange> cache_gaps = new ArrayList<DateRange>();

		// This is the "running position" within the timeline of our requested
		// range.
		long start_time_milliseconds = requested_date_range.start.getTime();

		// First, check whether there is a span that covers the start date.
		// There could be several, so we choose the one with the latest ending
		// date.

		// int i=0;
		while (true) {
			// Log.d(TAG, "Loop: " + i++);

			DateRange cached_span = queryLatestEndingSpanStraddlingTime(db,
					start_time_milliseconds);
			if (cached_span != null) {

				// Log.i(TAG, "Found a span (from " + cached_span.start + " to "
				// + cached_span.end + ") that straddles " + new
				// Date(start_time_milliseconds));

				// If we keep finding overlapping spans, we'll always stay
				// in this clause until we get to the end. Then there will
				// have been no gaps.

				if (!cached_span.end.before(requested_date_range.end))
					break;

				start_time_milliseconds = cached_span.end.getTime();

			} else {

				// Log.e(TAG, "Couldn't find a span that straddles " + new
				// Date(start_time_milliseconds));

				// If we got here, there were no spans overlapping the starting
				// point.
				// We declare a gap starting at this point, and potentially
				// ending at
				// the end of the requested range.
				// We then look for the earliest starting date that comes before
				// the end
				// of our requested range. If there is none, record the gap and
				// terminate.
				// Otherwise, modify the end point of the gap to be the found
				// start point,
				// record the gap, and continue.

				DateRange gap = new DateRange(
						new Date(start_time_milliseconds),
						requested_date_range.end);

				DateRange resuming_span = queryEarliestStartingSpanStartingBetween(
						db, gap);
				if (resuming_span != null) {

					// Log.i(TAG, "Found a RESUMING span (from " +
					// resuming_span.start + " to " + resuming_span.end +
					// ") that starts between " + gap.start + " and " +
					// gap.end);

					gap.end = resuming_span.start;
					cache_gaps.add(gap);

					if (!resuming_span.end.before(requested_date_range.end))
						break;
					start_time_milliseconds = resuming_span.end.getTime();

				} else {

					// Log.e(TAG,
					// "Couldn't find a RESUMING span. Will terminate.");

					cache_gaps.add(gap);
					break;
				}
			}

			// SystemClock.sleep(500);
		}

		db.close();

		// Log.e(TAG, "Gaps found were:");
		// int j=0;
		// for (DateRange gap : cache_gaps) {
		// Log.i(TAG, j + ": from " + gap.start + " to " + gap.end + ")");
		// j++;
		// }

		return cache_gaps;
	}

	// ========================================================================
	public Cursor getRevenueByItemPlottableCursor(SQLiteDatabase db) {
		return getRevenueByItemPlottableCursor(db, null, null, null);
	}

	// ========================================================================
	public Cursor getRevenueByItemPlottableCursor(SQLiteDatabase db,
			DateRange date_range, long app_id) {

		return getRevenueByItemPlottableCursor(db, KEY_ORDER_TIMESTAMP + ">=?"
				+ " AND " + KEY_ORDER_TIMESTAMP + "<=?" + " AND " + KEY_APP_ID
				+ "=?",
				new String[] { Long.toString(date_range.start.getTime()),
						Long.toString(date_range.end.getTime()),
						Long.toString(app_id) });
	}

	// ========================================================================
	public Cursor getRevenueByItemPlottableCursor(SQLiteDatabase db,
			long publisher_id, DateRange date_range) {

		String timestamp_range_query = KEY_ORDER_TIMESTAMP + ">=? AND "
				+ KEY_ORDER_TIMESTAMP + "<=?";

		return getRevenueByItemPlottableCursor(db, timestamp_range_query
				+ (publisher_id == INVALID_PUBLISHER_ID ? "" :
					" AND " + KEY_PUBLISHER_ID + "=?"),
					publisher_id == INVALID_PUBLISHER_ID ?
						date_range.getRangeAsStringArray() :
							new String[] {
								Long.toString(date_range.start.getTime()),
								Long.toString(date_range.end.getTime()),
								Long.toString(publisher_id) });
	}

	// ========================================================================
	// This method gets its content directly from TABLE_ITEMS. However, this
	// prevents us from indicating the date of each item's first sale.
	@Deprecated
	public Cursor getChildMerchantItemsForConsolidation(long parent_id) {

		SQLiteDatabase db = getReadableDatabase();

		Cursor c = db.query(TABLE_GOOGLE_CHECKOUT_PRODUCTS,
				new String[] { KEY_MERCHANT_ITEM_ID + " AS " + BaseColumns._ID,
						KEY_ITEM_NAME }, KEY_CANONICAL_ITEM_ID + "=? AND "
						+ KEY_MERCHANT_ITEM_ID + "!=" + KEY_CANONICAL_ITEM_ID,
				new String[] { Long.toString(parent_id) },
				KEY_MERCHANT_ITEM_ID, null, KEY_MERCHANT_ITEM_ID + " ASC");

		return c;
	}

	// ========================================================================
	// This method gets its content directly from TABLE_ITEMS. However, this
	// prevents us from indicating the date of each item's first sale.
	@Deprecated
	public Cursor getParentMerchantItemsForConsolidation() {

		SQLiteDatabase db = getReadableDatabase();

		Cursor c = db.query(TABLE_GOOGLE_CHECKOUT_PRODUCTS,
				new String[] { KEY_MERCHANT_ITEM_ID + " AS " + BaseColumns._ID,
						KEY_ITEM_NAME }, KEY_MERCHANT_ITEM_ID + "="
						+ KEY_CANONICAL_ITEM_ID, null, KEY_MERCHANT_ITEM_ID,
				null, KEY_MERCHANT_ITEM_ID + " ASC");

		return c;
	}

	// ========================================================================
	public boolean hasChildren(long parent_id) {

		SQLiteDatabase db = getReadableDatabase();

		Cursor c = db
				.query(TABLE_GOOGLE_CHECKOUT_PRODUCTS,
						new String[] { KEY_MERCHANT_ITEM_ID + " AS "
								+ BaseColumns._ID, }, KEY_CANONICAL_ITEM_ID
								+ "=? AND " + KEY_MERCHANT_ITEM_ID + "!="
								+ KEY_CANONICAL_ITEM_ID, new String[] { Long
								.toString(parent_id) }, KEY_MERCHANT_ITEM_ID,
						null, null, "1");

		boolean has_children = c.moveToFirst();
		c.close();
		db.close();

		return has_children;
	}

	// ========================================================================
	// This method and its "Parent" counterpart integrate the first sale date
	// with the other data.
	public Cursor getChildConsolidationItems(long parent_id) {

		SQLiteDatabase db = getReadableDatabase();

		Cursor c = db.query(VIEW_RAW_TRANSACTION_ITEMS, new String[] {
				KEY_MERCHANT_ITEM_ID + " AS " + BaseColumns._ID, KEY_ITEM_NAME,
				"MIN(" + KEY_ORDER_TIMESTAMP + ") AS " + KEY_ORDER_TIMESTAMP },
				KEY_CANONICAL_ITEM_ID + "=? AND " + KEY_MERCHANT_ITEM_ID + "!="
						+ KEY_CANONICAL_ITEM_ID, new String[] { Long
						.toString(parent_id) }, KEY_MERCHANT_ITEM_ID, null,
				KEY_MERCHANT_ITEM_ID + " ASC");

		return c;
	}

	// ========================================================================
	public Cursor getMarketAppsAndUnparentedRow(
			long publisher_id) {

		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(VIEW_AGGREGATE_CONSOLIDATED_MARKET_LINKED,
				new String[] {
						KEY_APP_ID + " AS " + BaseColumns._ID,
						"IFNULL(" + KEY_APP_TITLE + ",'"
								+ ConsolidationActivity.UNGROUPED_APPS_LABEL
								+ "') AS " + KEY_APP_TITLE,
						KEY_CONSOLIDATED_APP_COUNT, KEY_AGGREGATE_REVENUE,
						KEY_AGGREGATE_SALE_COUNT, KEY_AGGREGATE_RATING_COUNT,
						KEY_APP_RATING_COUNT, KEY_APP_OVERALL_RATING,
						KEY_GOT_EARLIEST_COMMENT, KEY_GOT_MAX_COMMENTS,
						KEY_APP_CURRENT_PRICE,
						KEY_PUBLISHER_ID, // NEW
				}, KEY_CONSOLIDATED_APP_COUNT
						+ ">" + 0
						+ (publisher_id == INVALID_PUBLISHER_ID ? "" : " AND "
								+ KEY_PUBLISHER_ID + "=?"),
						publisher_id == INVALID_PUBLISHER_ID ? null
						: new String[] { Long.toString(publisher_id) },
				null, null, null);

		return c;
	}

	// ========================================================================
	public Cursor getMarketAppsForUnreadComments() {

		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(PARTIAL_AND_FULL_LINKED_COMMENT_AGGREGATION,
				new String[] { KEY_APP_ID + " AS " + BaseColumns._ID,
						KEY_APP_TITLE, KEY_APP_RATING_COUNT,
						KEY_APP_OVERALL_RATING, KEY_GOT_EARLIEST_COMMENT,
						KEY_GOT_MAX_COMMENTS,

						KEY_UNREAD_SUBTHRESHOLD_COMMENT_COUNT,
						KEY_AGGREGATE_SUBTHRESHOLD_RATING_COUNT,
						KEY_EARLIEST_SUBTHRESHOLD_RATING,
						KEY_LATEST_SUBTHRESHOLD_RATING,

						KEY_AGGREGATE_RATING_COUNT, KEY_LATEST_RATING,
						KEY_UNREAD_COMMENT_COUNT },
				KEY_UNREAD_SUBTHRESHOLD_COMMENT_COUNT + ">0", null, KEY_APP_ID,
				null, null);

		return c;
	}

	// ========================================================================
	// This method and its "Child" counterpart integrate the first sale date
	// with the other data.
	public Cursor getNewAppComments(long merchant_item_id) {

		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(VIEW_MARKET_COMMENTS_LINKED, new String[] {
				BaseColumns._ID, KEY_COMMENT_AUTHOR_NAME, KEY_RATING_VALUE,
				KEY_COMMENT_TEXT, KEY_RATING_TIMESTAMP,
				KEY_RATING_ALERT_THRESHOLD, KEY_COMMENT_READ }, KEY_APP_ID
				+ "=?"
				// + " AND " + KEY_RATING_TIMESTAMP + ">" +
				// KEY_LAST_COMMENTS_UPDATE_TIMESTAMP // XXX
				+ " AND " + KEY_RATING_VALUE + "<="
				+ KEY_RATING_ALERT_THRESHOLD + " AND " + KEY_COMMENT_READ + "="
				+ FALSE, new String[] { Long.toString(merchant_item_id) },
				null, null, KEY_RATING_TIMESTAMP + " DESC");

		return c;
	}

	// ========================================================================
	// This method and its "Child" counterpart integrate the first sale date
	// with the other data.
	public Cursor getMarketAppsChildren(long merchant_item_id) {

		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(VIEW_SIMPLE_MARKET_LINKED, new String[] {
				KEY_MERCHANT_ITEM_ID + " AS " + BaseColumns._ID, KEY_APP_ID,
				KEY_CANONICAL_ITEM_ID, KEY_ITEM_NAME, KEY_EARLIEST_SALE_DATE,
				KEY_LATEST_SALE_DATE, KEY_AGGREGATE_SALE_COUNT,
				KEY_AGGREGATE_REVENUE, KEY_REVENUE_CENTS },
				merchant_item_id != 0 ? KEY_CANONICAL_ITEM_ID + "=?"
						: KEY_APP_ID + " ISNULL",
				merchant_item_id != 0 ? new String[] { Long
						.toString(merchant_item_id) } : null,
				KEY_MERCHANT_ITEM_ID, null, KEY_LATEST_SALE_DATE + " DESC");

		return c;
	}

	// ========================================================================
	// We only want to list apps for which we have an entry stored in the Market
	// table.
	public Cursor getLinkedPaidMarketApps() {

		SQLiteDatabase db = getReadableDatabase();

		Cursor c = db.query(TABLE_MARKET_APPS, new String[] {
				KEY_APP_ID + " AS " + BaseColumns._ID, KEY_APP_TITLE },
				KEY_APP_PRICE_MICROS + "!=" + 0, null, null, null,
				KEY_APP_TITLE + " ASC");

		return c;
	}

	// ========================================================================
	// We only want to list apps for which we have an entry stored in the Market
	// table.
	public boolean hasLinkedPaidMarketApps(SQLiteDatabase db) {
		Cursor cursor = db.query(TABLE_MARKET_APPS, null, KEY_APP_PRICE_MICROS
				+ "!=" + 0, null, null, null, null, Integer.toString(1));

		boolean has = cursor.moveToFirst();
		cursor.close();
		return has;
	}

	// ========================================================================
	// We only want to list apps for which we have an entry stored in the Market
	// table.
	public int countLinkedPaidMarketApps() {

		SQLiteDatabase db = getReadableDatabase();

		Cursor cursor = db.query(TABLE_MARKET_APPS, new String[] { "COUNT("
				+ KEY_APP_ID + ")" }, KEY_APP_PRICE_MICROS + "!=" + 0, null,
				null, null, null);

		int count = 0;
		if (cursor.moveToFirst())
			count = cursor.getInt(0);

		cursor.close();
		db.close();
		return count;
	}

	// ========================================================================
	public List<Long> getPublishedAppIds() {

		SQLiteDatabase db = getReadableDatabase();

		List<Long> published_app_ids = new ArrayList<Long>();

		Cursor c = db.query(TABLE_MARKET_APPS, new String[] { KEY_APP_ID },
				null, null, KEY_APP_ID, null, null);

		while (c.moveToNext())
			published_app_ids.add(c.getLong(0));
		c.close();

		db.close();
		return published_app_ids;
	}

	// ========================================================================
	public List<Long> getRawCheckoutItemIds() {

		SQLiteDatabase db = getReadableDatabase();

		List<Long> item_ids = new ArrayList<Long>();

		Cursor c = db.query(VIEW_CONSOLIDATED_GOOGLE_CHECKOUT_ITEMS,
				new String[] { KEY_CHILD_ITEM_ID }, null, null,
				KEY_CHILD_ITEM_ID, null, null);

		while (c.moveToNext())
			item_ids.add(c.getLong(0));
		c.close();

		db.close();
		return item_ids;
	}

	// ========================================================================
	// Gets both parent and its children
	public List<Long> getSimpleConsolidatedItemIDs(SQLiteDatabase db,
			long parent_id) {

		List<Long> group_items = new ArrayList<Long>();

		Cursor c = db.query(TABLE_GOOGLE_CHECKOUT_PRODUCTS,
				new String[] { KEY_MERCHANT_ITEM_ID }, KEY_CANONICAL_ITEM_ID
						+ "=?", new String[] { Long.toString(parent_id) },
				KEY_MERCHANT_ITEM_ID, null, null);

		while (c.moveToNext())
			group_items.add(c.getLong(0));
		c.close();

		return group_items;
	}

	// ========================================================================
	public Cursor getSeriesLabelsForComments(long app_id) {

		SQLiteDatabase db = getReadableDatabase();

		return db.query(TABLE_MARKET_APPS, new String[] {
				KEY_APP_ID + " AS " + BaseColumns._ID,
				KEY_APP_TITLE + " AS "
						+ ColumnSchema.Aspect.Series.COLUMN_SERIES_LABEL },
				KEY_APP_ID + "=?", new String[] { Long.toString(app_id) },
				null, null, null);
	}

	// ========================================================================
	@Deprecated
	public Cursor getPublisherNamesCursorOLD() {

		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db
				.query(TABLE_MARKET_APPS, new String[] {
						"ROWID AS " + BaseColumns._ID,
						KEY_APP_CREATOR_NAME },
						null, null, KEY_APP_CREATOR_NAME, null,
						KEY_APP_CREATOR_NAME + " ASC");

		cursor.moveToFirst();

		db.close();
		return cursor;
	}
	
	// ========================================================================
	public Cursor getPublisherNamesCursor() {

		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(TABLE_PUBLISHERS,
				new String[] {
					"ROWID AS " + BaseColumns._ID,
					KEY_PUBLISHER_NAME},
				null,
				null,
				KEY_PUBLISHER_NAME, null, null);

		cursor.moveToFirst();

		db.close();
		return cursor;
	}

	// ========================================================================
	public List<String> getPublisherNames() {

		List<String> publisher_names = new ArrayList<String>();

		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(TABLE_PUBLISHERS,
				new String[] {KEY_PUBLISHER_NAME},
				null,
				null,
				KEY_PUBLISHER_NAME, null, KEY_PUBLISHER_NAME + " ASC");

		while (cursor.moveToNext())
			publisher_names.add(cursor.getString(0));

		cursor.close();
		db.close();
		return publisher_names;
	}

	
	// ========================================================================
	@Deprecated
	public List<String> getPublisherNamesOLD() {

		List<String> publisher_names = new ArrayList<String>();

		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(TABLE_MARKET_APPS,
				new String[] { KEY_APP_CREATOR_NAME }, null, null,
				KEY_APP_CREATOR_NAME, null, KEY_APP_CREATOR_NAME + " ASC");

		while (cursor.moveToNext())
			publisher_names.add(cursor.getString(0));

		cursor.close();
		db.close();
		return publisher_names;
	}

	// ========================================================================
	public Cursor getPrefixedPublisherNames(String prefix) {

		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(TABLE_PUBLISHERS,
				new String[] {
					"ROWID AS " + BaseColumns._ID,
					KEY_PUBLISHER_NAME
				},
				KEY_PUBLISHER_NAME + " LIKE ?",
				new String[] { prefix + "%" },
				KEY_PUBLISHER_NAME, null, KEY_PUBLISHER_NAME + " ASC");

		return cursor;
	}
	
	// ========================================================================
	@Deprecated
	public Cursor getPrefixedPublisherNamesOLD(String prefix) {

		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(TABLE_MARKET_APPS,
			new String[] {
				"ROWID AS " + BaseColumns._ID,
				KEY_APP_CREATOR_NAME
			},
			KEY_APP_CREATOR_NAME + " LIKE ?",
			new String[] { prefix + "%" }, KEY_APP_CREATOR_NAME, null,
			KEY_APP_CREATOR_NAME + " ASC");

		return cursor;
	}

	// ========================================================================
	public Cursor getSeriesLabelsForRevenueByItem(long publisher_id, DateRange date_range) {

		SQLiteDatabase db = getReadableDatabase();

		String timestamp_range_query = KEY_ORDER_TIMESTAMP + ">=? AND "
				+ KEY_ORDER_TIMESTAMP + "<=?";

		Cursor c = getRevenueByItemPlottableCursor(db, new String[] {
				KEY_APP_ID + " AS " + BaseColumns._ID,
				"IFNULL(" + KEY_ITEM_NAME + ",'"
						+ ConsolidationActivity.UNGROUPED_APPS_LABEL + "')"
						+ " AS "
						+ ColumnSchema.Aspect.Series.COLUMN_SERIES_LABEL },
				timestamp_range_query
						+ (publisher_id == INVALID_PUBLISHER_ID ? "" :
							" AND " + KEY_PUBLISHER_ID + "=?"),
								publisher_id == INVALID_PUBLISHER_ID ? date_range.getRangeAsStringArray()
						: new String[] {
								Long.toString(date_range.start.getTime()),
								Long.toString(date_range.end.getTime()),
								Long.toString(publisher_id) });

		// DEBUG ONLY:
		Log.d(TAG, "Listing series labels:");
		if (c.moveToFirst()) {
			do {
				Log.i(TAG, "App ID: " + c.getLong(0) + "; Item name: "
						+ c.getString(1));
			} while (c.moveToNext());
		}

		return c;
	}

	// ========================================================================
	public static String AXIS_X = "AXIS_X";
	public static String AXIS_Y = "AXIS_Y";

	public Cursor getRevenueByItemPlottableCursor(SQLiteDatabase db,
			String whereClause, String[] whereArgs) {

		return getRevenueByItemPlottableCursor(db,
				new String[] {
						KEY_APP_ID + " AS " + BaseColumns._ID,
						KEY_APP_ID + " AS "
								+ ColumnSchema.Aspect.Data.COLUMN_SERIES_INDEX,
						null + " AS "
								+ ColumnSchema.Aspect.Data.COLUMN_DATUM_LABEL,
						0 + " AS " + AXIS_X,
						"SUM(" + KEY_REVENUE_CENTS + ")/CAST(100 AS REAL) AS "
								+ AXIS_Y }, whereClause, whereArgs);
	}

	// ========================================================================
	public Cursor getRevenueOverallPlottableCursor(SQLiteDatabase db,
			DateRange date_range) {

		Cursor c = db.query(VIEW_LABELED_TRANSACTIONS,
				new String[] {
						0 + " AS " + BaseColumns._ID,
						0 + " AS "
								+ ColumnSchema.Aspect.Data.COLUMN_SERIES_INDEX,
						null + " AS "
								+ ColumnSchema.Aspect.Data.COLUMN_DATUM_LABEL,
						0 + " AS " + AXIS_X,
						"SUM(" + KEY_REVENUE_CENTS + ")/CAST(100 AS REAL) AS "
								+ AXIS_Y }, KEY_ORDER_TIMESTAMP + ">=?"
						+ " AND " + KEY_ORDER_TIMESTAMP + "<=?", new String[] {
						Long.toString(date_range.start.getTime()),
						Long.toString(date_range.end.getTime()) }, null, null,
				null);

		return c;
	}

	// ========================================================================
	public Cursor getRevenueByItemPlottableCursor(SQLiteDatabase db,
			String[] selection, String whereClause, String[] whereArgs) {

		Cursor c = db.query(VIEW_LABELED_TRANSACTIONS, selection, whereClause,
				whereArgs, KEY_APP_ID, null, KEY_APP_ID + " ASC");

		return c;
	}

	// ========================================================================
	public List<Long> getRepresentativeUnlabeledMerchantItemIds(
			DateRange date_range) {

		Log.d(TAG, "Getting representative unlabeled Merchant Item IDs...");
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(VIEW_RAW_TRANSACTION_ITEMS, new String[] {
				KEY_ORDER_NUMBER, KEY_MERCHANT_ITEM_ID, KEY_ITEM_NAME,
				KEY_CANONICAL_ITEM_ID }, KEY_ITEM_NAME + " ISNULL" + " AND "
				+ KEY_ORDER_TIMESTAMP + ">=?" + " AND " + KEY_ORDER_TIMESTAMP
				+ "<=?", date_range.getRangeAsStringArray(),
				KEY_MERCHANT_ITEM_ID, null, KEY_MERCHANT_ITEM_ID + " ASC");

		List<Long> unlabeled_merchant_items = new ArrayList<Long>();
		while (c.moveToNext()) {
			long order_number = c.getLong(0);
			long merchant_item_id = c.getLong(1);
			String merchant_item_name = c.getString(2);
			long canonical_id = c.getLong(3);

			// Log.i(TAG, "Order number: " + order_number +
			// "; merchant item ID: " + merchant_item_id + "; Label: " +
			// merchant_item_name + "; Canonical ID: " + canonical_id);
			unlabeled_merchant_items.add(order_number);
		}

		c.close();
		db.close();
		return unlabeled_merchant_items;
	}

	// ========================================================================
	public List<Long> getRepresentativeUnlabeledMerchantItemIds()
			throws SQLiteDatabaseCorruptException {
		Log.d(TAG, "Getting representative unlabeled Merchant Item IDs...");
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(VIEW_RAW_TRANSACTION_ITEMS, new String[] {
				KEY_ORDER_NUMBER, KEY_MERCHANT_ITEM_ID, KEY_ITEM_NAME,
				KEY_CANONICAL_ITEM_ID }, KEY_ITEM_NAME + " ISNULL", null,
				KEY_MERCHANT_ITEM_ID, null, KEY_MERCHANT_ITEM_ID + " ASC");

		List<Long> unlabeled_merchant_items = new ArrayList<Long>();

		while (c.moveToNext()) {
			long order_number = c.getLong(0);
			long merchant_item_id = c.getLong(1);
			String merchant_item_name = c.getString(2);
			long canonical_id = c.getLong(3);

			// Log.i(TAG, "Order number: " + order_number +
			// "; merchant item ID: " + merchant_item_id + "; Label: " +
			// merchant_item_name + "; Canonical ID: " + canonical_id);
			unlabeled_merchant_items.add(order_number);
		}

		c.close();
		db.close();
		return unlabeled_merchant_items;
	}

	// ========================================================================
	public List<Long> queryOrdersLackingItemIds() {
		return queryOrdersLackingItemIds(null);
	}

	// ========================================================================
	// Queries on the closed interval [start, end]
	public List<Long> queryOrdersLackingItemIds(DateRange date_range) {

		SQLiteDatabase db = getReadableDatabase();

		Cursor c = db.query(TABLE_GOOGLE_CHECKOUT_PURCHASES,
				new String[] { KEY_ORDER_NUMBER }, KEY_MERCHANT_ITEM_ID
						+ " ISNULL"
						+ (date_range != null ? " AND " + KEY_ORDER_TIMESTAMP
								+ ">=?" + " AND " + KEY_ORDER_TIMESTAMP + "<=?"
								: ""), (date_range != null ? date_range
						.getRangeAsStringArray() : null), null, null, null);

		List<Long> order_numbers = new ArrayList<Long>();
		while (c.moveToNext())
			order_numbers.add(c.getLong(0));

		c.close();
		db.close();
		return order_numbers;
	}

	// ========================================================================
	public static Date decrementDate(Date old_date, long milliseconds) {
		return new Date(old_date.getTime() - milliseconds);
	}

	// ========================================================================
	public static class DatedMultiSeriesValues {
		public List<SeriesValue> multi_series;
		public Date date;

		public DatedMultiSeriesValues(Date date, List<SeriesValue> multi_series) {
			this.multi_series = multi_series;
			this.date = date;
		}
	}

	// ========================================================================
	public static class SeriesValue {
		public long series;
		public Number value;

		public SeriesValue(long series, Number value) {
			this.series = series;
			this.value = value;
		}
	}

	// ========================================================================
	public static class DatedValue {
		public Date date;
		public Number value;

		public DatedValue(Date date, Number value) {
			this.date = date;
			this.value = value;
		}
	}

	// ========================================================================
	// This trick avoids having a "runt" bin at the end where new sales may
	// not have been recorded yet.
	public DateRange getTruncatedSalesDateRange(DateRange requested_range) {

		DateRange date_range = requested_range.clone();
		date_range.end = getLatestSaleDateWithin(date_range);
		Log.d(TAG, "Latest sale date with range: " + date_range.end);

		return date_range;
	}

	// ========================================================================
	public static class DatedRating {
		public Date date;
		public int rating;

		DatedRating(int rating, Date date) {
			this.date = date;
			this.rating = rating;
		}
	}

	// ========================================================================
	public static class DatedRatingsWindow {

		private List<DatedRating> ratings = new ArrayList<DatedRating>();

		DatedRatingsWindow(List<DatedRating> ratings) {
			this.ratings = ratings;
		}

		public float getAverage() {
			if (this.ratings.size() == 0)
				return 0;
			int sum = 0;
			for (DatedRating rating : this.ratings)
				sum += rating.rating;
			return sum / (float) this.ratings.size();
		}

		public float getMin() {
			return Collections.min(this.ratings, new Comparator<DatedRating>() {
				@Override
				public int compare(DatedRating object1, DatedRating object2) {
					return new Integer(object1.rating)
							.compareTo(object2.rating);
				}
			}).rating;
		}

		public float getValue(WindowEvaluatorMode evaluator) {
			switch (evaluator) {
			case AVERAGE:
				return getAverage();
			case MINIMUM:
				return getMin();
			}
			return 0;
		}

		/** Gets the most recent date in the window */
		public Date getDate() {
			if (this.ratings.size() == 0)
				return null;
			return this.ratings.get(this.ratings.size() - 1).date;
		}
	}

	// ========================================================================
	public static class PriceSalesSpan {
		public int price_cents;
		DateRange date_range = new DateRange();
		public int purchase_count = 0;

		PriceSalesSpan(int cents) {
			this.price_cents = cents;
		}

		public Date getStartDate() {
			return date_range.start;
		}

		public float getDailyRevenueDollars() {

			return this.purchase_count
					* (this.price_cents / 100f)
					/ (date_range.getMillisDelta() / (float) RevenueActivity.MILLIS_PER_DAY);
		}
	}

	// ========================================================================
	public List<PriceSalesSpan> getRevenuePriceCorrelation(long app_id,
			DateRange date_range) {
		SQLiteDatabase db = getReadableDatabase();

		// Split the revenue timeline at each price change.
		// There's no way to do this from with SQLite, so we fetch all of the
		// records.

		Log.d(TAG, "Querying for purchases of item " + app_id + " made "
				+ date_range);

		Cursor cursor = db.query(VIEW_RAW_TRANSACTION_ITEMS, new String[] {
				KEY_ORDER_TIMESTAMP, KEY_REVENUE_CENTS }, KEY_CANONICAL_ITEM_ID
				+ "=? AND " + KEY_ORDER_TIMESTAMP + ">=? AND "
				+ KEY_ORDER_TIMESTAMP + "<=?", new String[] {
				Long.toString(app_id),
				Long.toString(date_range.start.getTime()),
				Long.toString(date_range.end.getTime()) }, null, null,
				KEY_ORDER_TIMESTAMP + " ASC");

		Log
				.d(TAG, "Purchase record cursor has " + cursor.getCount()
						+ " rows.");

		List<PriceSalesSpan> sales_spans = new ArrayList<PriceSalesSpan>();

		PriceSalesSpan current_span = new PriceSalesSpan(0);

		// Whenever a new price is encountered, set the end_date on the previous
		// span, then create a new span.
		while (cursor.moveToNext()) {

			long timestamp = cursor.getLong(0);
			int price_cents = cursor.getInt(1);

			if (price_cents != current_span.price_cents) {
				Date changeover_date = new Date(timestamp);
				current_span.date_range.end = changeover_date;

				current_span = new PriceSalesSpan(price_cents);
				current_span.date_range.start = changeover_date;
				sales_spans.add(current_span);
			}

			current_span.purchase_count++;
		}

		// Close out the last span.
		current_span.date_range.end = date_range.end;

		Collections.sort(sales_spans, new Comparator<PriceSalesSpan>() {
			@Override
			public int compare(PriceSalesSpan object1, PriceSalesSpan object2) {
				return new Integer(object1.price_cents)
						.compareTo(object2.price_cents);
			}
		});

		return sales_spans;
	}

	// ========================================================================
	public static class PriceDate {
		public int price_cents;
		public Date date;

		public PriceDate(int price_cents, Date date) {
			this.price_cents = price_cents;
			this.date = date;
		}
	}

	// ========================================================================
	public List<PriceDate> getPriceTransitions(long app_id, DateRange date_range) {
		SQLiteDatabase db = getReadableDatabase();

		List<PriceDate> price_transition_points = new ArrayList<PriceDate>();

		// Split the revenue timeline at each price change.
		// There's no way to do this from with SQLite, so we fetch all of the
		// records.

		Log.d(TAG, "Querying for purchases of item " + app_id + " made "
				+ date_range);

		Cursor cursor = db.query(VIEW_RAW_TRANSACTION_ITEMS, new String[] {
				KEY_ORDER_TIMESTAMP, KEY_REVENUE_CENTS }, KEY_CANONICAL_ITEM_ID
				+ "=? AND " + KEY_ORDER_TIMESTAMP + ">=? AND "
				+ KEY_ORDER_TIMESTAMP + "<=?", new String[] {
				Long.toString(app_id),
				Long.toString(date_range.start.getTime()),
				Long.toString(date_range.end.getTime()) }, null, null,
				KEY_ORDER_TIMESTAMP + " ASC");

		Log
				.d(TAG, "Purchase record cursor has " + cursor.getCount()
						+ " rows.");

		// Whenever a new price is encountered, set the end_date on the previous
		// span, then create a new span.
		int last_price_cents = 0;
		Date last_sale_date = null;
		while (cursor.moveToNext()) {

			int price_cents = cursor.getInt(1);
			Date changeover_date = new Date(cursor.getLong(0));

			if (price_cents != last_price_cents) {
				if (last_sale_date != null)
					price_transition_points.add(new PriceDate(last_price_cents,
							last_sale_date));
				price_transition_points.add(new PriceDate(price_cents,
						changeover_date));
			}

			last_sale_date = changeover_date;
			last_price_cents = price_cents;
		}

		return price_transition_points;
	}

	// ========================================================================
	public static class TitleDescription {
		public TitleDescription(String name, String description) {
			this.name = name;
			this.description = description;
		}

		public String name, description;
	}

	// ========================================================================
	public static class RatingsSalesSpan {

		// Initially, only these members two will be used
		DateRange date_range = new DateRange();
		private float rating;

		RatingsSalesSpan(float rating) {
			this.rating = rating;
		}

		private int purchase_count;
		private int aggregate_revenue_cents;

		public int getPurchaseCount() {
			return purchase_count;
		}

		public long getDurationMillis() {
			return date_range.getMillisDelta();
		}

		public int getRevenueCents() {
			return aggregate_revenue_cents;
		}

		public float getRating() {
			return rating;
		}

		public Date getStartDate() {
			return date_range.start;
		}

		public float getDailyRevenueDollars() {
			return (this.aggregate_revenue_cents / 100f)
					/ (date_range.getMillisDelta() / (float) RevenueActivity.MILLIS_PER_DAY);
		}
	}

	// ========================================================================
	public static class RevenueDuration {
		int revenue_cents;
		long duration_millis;

		RevenueDuration(int revenue_cents, long duration_millis) {
			this.revenue_cents = revenue_cents;
			this.duration_millis = duration_millis;
		}
	}

	public static class RatedRevenueDuration extends RevenueDuration {
		float rating;

		RatedRevenueDuration(int revenueCents, long durationMillis, float rating) {
			super(revenueCents, durationMillis);
			this.rating = rating;
		}

		public float getRating() {
			return rating;
		}

		public float getDailyRevenueDollars() {
			return (this.revenue_cents / 100f)
					/ (duration_millis / (float) RevenueActivity.MILLIS_PER_DAY);
		}
	}

	// ========================================================================
	public List<RatedRevenueDuration> getRevenueRatingsCorrelation(long app_id,
			DateRange date_range, List<DatedRatingsWindow> ratings_windows,
			WindowEvaluatorMode evaluator_type) {

		// Iterate through the comment windows, creating a new span for each
		// change in average/min rating.

		List<RatingsSalesSpan> ratings_spans = new ArrayList<RatingsSalesSpan>();
		RatingsSalesSpan current_span = new RatingsSalesSpan(
				AppsOverviewActivity.INVALID_RATING);

		// Whenever a new price is encountered, set the end_date on the previous
		// span, then create a new span.
		for (DatedRatingsWindow ratings_window : ratings_windows) {

			float window_rating_value = ratings_window.getValue(evaluator_type);

			if (window_rating_value != current_span.rating) {
				Date changeover_date = ratings_window.getDate();
				current_span.date_range.end = changeover_date;

				current_span = new RatingsSalesSpan(window_rating_value);
				current_span.date_range.start = changeover_date;
				ratings_spans.add(current_span);
			}
		}

		// Close out the last span.
		current_span.date_range.end = date_range.end;

		SQLiteDatabase db = getReadableDatabase();

		for (RatingsSalesSpan sales_span : ratings_spans) {
			Cursor cursor = db.query(VIEW_RAW_TRANSACTION_ITEMS, new String[] {
					"SUM(" + KEY_REVENUE_CENTS + ") AS "
							+ KEY_AGGREGATE_REVENUE,
					"COUNT(" + KEY_REVENUE_CENTS + ") AS "
							+ KEY_AGGREGATE_SALE_COUNT }, KEY_CANONICAL_ITEM_ID
					+ "=? AND " + KEY_ORDER_TIMESTAMP + ">=? AND "
					+ KEY_ORDER_TIMESTAMP + "<=?", new String[] {
					Long.toString(app_id),
					Long.toString(sales_span.date_range.start.getTime()),
					Long.toString(sales_span.date_range.end.getTime()) },
					KEY_CANONICAL_ITEM_ID, null, null);

			if (cursor.moveToFirst()) {
				sales_span.aggregate_revenue_cents = cursor.getInt(0);
				sales_span.purchase_count = cursor.getInt(1);
			}

			cursor.close();
		}

		// Consolidate the spans by rating
		Map<Float, List<RevenueDuration>> ratings_consolidated_revenue_durations = new HashMap<Float, List<RevenueDuration>>();
		for (RatingsSalesSpan sales_span : ratings_spans) {

			if (sales_span.getPurchaseCount() > 0) {

				List<RevenueDuration> spans_for_rating_value;
				if (ratings_consolidated_revenue_durations
						.containsKey(sales_span.getRating())) {
					spans_for_rating_value = ratings_consolidated_revenue_durations
							.get(sales_span.getRating());
				} else {
					spans_for_rating_value = new ArrayList<RevenueDuration>();
					ratings_consolidated_revenue_durations.put(sales_span
							.getRating(), spans_for_rating_value);
				}
				spans_for_rating_value.add(new RevenueDuration(sales_span
						.getRevenueCents(), sales_span.getDurationMillis()));

			}
		}

		List<RatedRevenueDuration> consolidated_ratings_spans = new ArrayList<RatedRevenueDuration>();
		// Aggregate the lists into single Span objects
		for (Entry<Float, List<RevenueDuration>> entry : ratings_consolidated_revenue_durations
				.entrySet()) {
			int aggregate_cents = 0;
			long aggregate_milliseconds = 0;

			for (RevenueDuration revenue_duration : entry.getValue()) {
				aggregate_cents += revenue_duration.revenue_cents;
				aggregate_milliseconds += revenue_duration.duration_millis;
			}

			RatedRevenueDuration span = new RatedRevenueDuration(
					aggregate_cents, aggregate_milliseconds, entry.getKey());
			consolidated_ratings_spans.add(span);
		}

		Collections.sort(consolidated_ratings_spans,
				new Comparator<RatedRevenueDuration>() {
					@Override
					public int compare(RatedRevenueDuration object1,
							RatedRevenueDuration object2) {
						return new Float(object1.getRating()).compareTo(object2
								.getRating());
					}
				});
		return consolidated_ratings_spans;
	}

	// ========================================================================
	public Cursor getRatingHistogram(long app_id) {
		SQLiteDatabase db = getReadableDatabase();

		Cursor cursor = db.query(TABLE_MARKET_COMMENTS, new String[] {
				KEY_RATING_VALUE + " AS " + BaseColumns._ID,
				0 + " AS " + ColumnSchema.Aspect.Data.COLUMN_SERIES_INDEX,
				null + " AS " + ColumnSchema.Aspect.Data.COLUMN_DATUM_LABEL,
				KEY_RATING_VALUE + " AS " + DatabaseRevenue.AXIS_X,
				"COUNT(" + KEY_RATING_VALUE + ")" + " AS "
						+ DatabaseRevenue.AXIS_Y, }, KEY_APP_ID + "=?",
				new String[] { Long.toString(app_id) }, KEY_RATING_VALUE, null,
				KEY_RATING_VALUE + " ASC");

		return cursor;
	}

	// ========================================================================
	public DatedRating getLastRatingBefore(long app_id, Date date) {
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(DatabaseRevenue.TABLE_MARKET_COMMENTS,
				new String[] { KEY_RATING_VALUE, KEY_RATING_TIMESTAMP },
				DatabaseRevenue.KEY_APP_ID + "=? AND "
						+ DatabaseRevenue.KEY_RATING_TIMESTAMP + "<?",
				new String[] { Long.toString(app_id),
						Long.toString(date.getTime()) }, null, null,
				DatabaseRevenue.KEY_RATING_TIMESTAMP + " ASC");

		DatedRating dated_rating = null;
		if (cursor.moveToFirst()) {
			dated_rating = new DatedRating(cursor.getInt(0), new Date(cursor
					.getLong(1)));
		}

		cursor.close();
		db.close();

		return dated_rating;
	}

	// ========================================================================
	public List<DatedRatingsWindow> genWindowedRatingAverages(long app_id,
			int width, DateRange date_range) {
		SQLiteDatabase db = getReadableDatabase();

		List<DatedRatingsWindow> windows = new ArrayList<DatedRatingsWindow>();
		List<DatedRating> window = new ArrayList<DatedRating>();

		Cursor cursor = db.query(TABLE_MARKET_COMMENTS, new String[] {
				KEY_RATING_VALUE, KEY_RATING_TIMESTAMP }, KEY_APP_ID
				+ "=?"
				+ (date_range != null ? " AND " + KEY_RATING_TIMESTAMP
						+ ">=? AND " + KEY_RATING_TIMESTAMP + "<=?" : ""),
				(date_range != null ? new String[] { Long.toString(app_id),
						Long.toString(date_range.start.getTime()),
						Long.toString(date_range.end.getTime()) }
						: new String[] { Long.toString(app_id) }), null, null,
				KEY_RATING_TIMESTAMP + " ASC");

		while (cursor.moveToNext()) {
			displaceWindowItem(window, width, new DatedRating(cursor.getInt(0),
					new Date(cursor.getLong(1))));

			windows.add(new DatedRatingsWindow(new ArrayList<DatedRating>(
					window)));
		}

		cursor.close();
		db.close();
		return windows;
	}

	// ========================================================================
	/** Utility function for a moving average */
	public static <T> void displaceWindowItem(List<T> window, int width, T item) {

		window.add(item);

		// "shift" the list (i.e. "pop" from the front)
		if (window.size() > width)
			window.remove(0);
	}

	// ========================================================================
	public List<DatedMultiSeriesValues> generateSingleAppHistogram(
			final DateRange date_range, int bin_count, long app_id) {

		SQLiteDatabase db = getReadableDatabase();

		long millisecond_bin_duration = date_range.getMillisDelta() / bin_count;
		// float histogram_binwidth_days = millisecond_bin_duration / (float)
		// RevenueActivity.MILLIS_PER_DAY;

		Date bin_end_date = (Date) date_range.end.clone();
		List<DatedMultiSeriesValues> meta_bins = new ArrayList<DatedMultiSeriesValues>();
		for (int i = 0; i < bin_count; i++) {

			Date bin_start_date = decrementDate(bin_end_date,
					millisecond_bin_duration);
			DateRange bin_range = new DateRange(bin_start_date, bin_end_date);

			List<SeriesValue> series_bin = new ArrayList<SeriesValue>();

			Cursor cursor = getRevenueByItemPlottableCursor(db, bin_range,
					app_id);
			int column_index_series = cursor
					.getColumnIndex(ColumnSchema.Aspect.Data.COLUMN_SERIES_INDEX);
			int column_index_value = cursor
					.getColumnIndex(DatabaseRevenue.AXIS_Y);
			while (cursor.moveToNext())
				series_bin.add(new SeriesValue(cursor
						.getLong(column_index_series), cursor
						.getInt(column_index_value)));
			cursor.close();

			meta_bins.add(new DatedMultiSeriesValues(bin_end_date, series_bin));

			bin_end_date = bin_start_date;
		}

		db.close();

		// Although the bins will be sorted in increasing chronological order
		// automatically by
		// the SQLite database, this function may be used elsewhere, so we
		// ensure increasing
		// order here.
		Collections.reverse(meta_bins);

		return meta_bins;
	}

	// ========================================================================
	public List<DatedValue> generateOverallHistogram(
			final DateRange date_range, int bin_count) {

		SQLiteDatabase db = getReadableDatabase();

		long millisecond_bin_duration = date_range.getMillisDelta() / bin_count;
		// float histogram_binwidth_days = millisecond_bin_duration / (float)
		// RevenueActivity.MILLIS_PER_DAY;

		Date bin_end_date = (Date) date_range.end.clone();

		List<DatedValue> series_bin = new ArrayList<DatedValue>();
		for (int i = 0; i < bin_count; i++) {

			Date bin_start_date = decrementDate(bin_end_date,
					millisecond_bin_duration);
			DateRange bin_range = new DateRange(bin_start_date, bin_end_date);

			Cursor cursor = getRevenueOverallPlottableCursor(db, bin_range);
			Log.e(TAG, "ROW COUNT: " + cursor.getCount());

			int column_index_value = cursor
					.getColumnIndex(DatabaseRevenue.AXIS_Y);
			if (cursor.moveToFirst())
				series_bin.add(new DatedValue(bin_end_date, cursor
						.getInt(column_index_value)));
			cursor.close();

			bin_end_date = bin_start_date;
		}

		db.close();

		return series_bin;
	}

	// ========================================================================
	public List<DatedMultiSeriesValues> generateHistogram(
			final long publisher_id, final DateRange date_range, int bin_count) {

		SQLiteDatabase db = getReadableDatabase();

		long millisecond_bin_duration = date_range.getMillisDelta() / bin_count;
		// float histogram_binwidth_days = millisecond_bin_duration / (float)
		// RevenueActivity.MILLIS_PER_DAY;

		Date bin_end_date = (Date) date_range.end.clone();
		List<DatedMultiSeriesValues> meta_bins = new ArrayList<DatedMultiSeriesValues>();
		for (int i = 0; i < bin_count; i++) {

			Date bin_start_date = decrementDate(bin_end_date,
					millisecond_bin_duration);
			DateRange bin_range = new DateRange(bin_start_date, bin_end_date);

			List<SeriesValue> series_bin = new ArrayList<SeriesValue>();
			Cursor cursor = getRevenueByItemPlottableCursor(db, publisher_id, bin_range);
			int column_index_series = cursor
					.getColumnIndex(ColumnSchema.Aspect.Data.COLUMN_SERIES_INDEX);
			int column_index_value = cursor
					.getColumnIndex(DatabaseRevenue.AXIS_Y);
			while (cursor.moveToNext())
				series_bin.add(new SeriesValue(cursor
						.getLong(column_index_series), cursor
						.getInt(column_index_value)));
			cursor.close();

			meta_bins.add(new DatedMultiSeriesValues(bin_end_date, series_bin));

			bin_end_date = bin_start_date;
		}

		db.close();

		// Although the bins will be sorted in increasing chronological order
		// automatically by
		// the SQLite database, this function may be used elsewhere, so we
		// ensure increasing
		// order here.
		Collections.reverse(meta_bins);

		return meta_bins;
	}

	// ========================================================================
	@Override
	public void onCreate(SQLiteDatabase db) {

		for (String sql : table_creation_commands) {
			Log.d(TAG, sql);
			db.execSQL(sql);
		}
	}

	// ========================================================================
	// Deletes contents of specified tables.
	public int clearCache(String[] tables) {

		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();

		int deletion_count = 0;
		for (String table : tables)
			deletion_count += db.delete(table, "1", null);

		try {
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		db.close();

		return deletion_count;
	}

	// ========================================================================
	// Deletes contents of all tables.
	public int clearCache() {
		return clearCache(table_list);
	}

	// ========================================================================
	public void drop_all_tables(SQLiteDatabase db) {

		for (String view : view_list)
			db.execSQL("DROP VIEW IF EXISTS " + view);

		for (String table : table_list)
			db.execSQL("DROP TABLE IF EXISTS " + table);
	}

	// ========================================================================
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
				+ newVersion + ", which will destroy all old data");

		drop_all_tables(db);

		onCreate(db);
	}
}
