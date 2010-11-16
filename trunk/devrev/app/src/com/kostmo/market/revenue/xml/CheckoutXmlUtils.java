package com.kostmo.market.revenue.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xmlpull.v1.XmlSerializer;

import android.net.Uri;
import android.text.format.Time;
import android.util.Log;
import android.util.Xml;

import com.kostmo.market.revenue.GoogleCheckoutUtils;
import com.kostmo.market.revenue.container.DateRange;
import com.kostmo.market.revenue.container.NotificationsApiSchema;
import com.kostmo.market.revenue.container.NotificationsApiSchema.NotificationHistoryRequest;
import com.kostmo.market.revenue.container.NotificationsApiSchema.NotificationHistoryRequest.NotificationType;
import com.kostmo.market.revenue.provider.DatabaseRevenue;
import com.kostmo.market.revenue.provider.DatabaseRevenue.TitleDescription;
import com.kostmo.market.revenue.task.CancellableProgressIncrementor;
import com.kostmo.market.revenue.task.CancellableProgressNotifier;
import com.kostmo.market.revenue.task.OrderProcessor;


public class CheckoutXmlUtils {

	public static final String TAG = "CheckoutXmlUtils";

	
	
	public static final int MAX_FILTER_ORDER_COUNT = 16;
	public static final int MAX_REQUEST_AGE_DAYS = 450;	// TODO Use me
	public static final int MAX_REPONSE_NOTIFICATION_COUNT = 50;
	// Requests must be for at least 5 minutes in the past.
	public static final int MINIMUM_REQUEST_AGE_MINUTES = 30;	// Using 30 rather than 5 minutes provides some leeway
	
	// ========================================================================
	static HttpRequestInterceptor preemptiveAuth = new HttpRequestInterceptor() {
	    public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
	        AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
	        CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(
	                ClientContext.CREDS_PROVIDER);
	        HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
	        
	        if (authState.getAuthScheme() == null) {
	            AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());
	            Credentials creds = credsProvider.getCredentials(authScope);
	            if (creds != null) {
	                authState.setAuthScheme(new BasicScheme());
	                authState.setCredentials(creds);
	            }
	        }
	    }    
	};
	
	// ========================================================================
	public static DateRange sanitizeDateRange(DateRange date_range) {
		
		DateRange sanitized_date_range = new DateRange(date_range.start, date_range.end);
		
		// Requested date range has to be at least five minutes old
		Calendar cal = new GregorianCalendar();	// Initialized to "now"
		Calendar five_minutes_ago = (Calendar) cal.clone();
		five_minutes_ago.add(Calendar.MINUTE, -MINIMUM_REQUEST_AGE_MINUTES);
		
		if (sanitized_date_range.end.after(five_minutes_ago.getTime()))
			sanitized_date_range.end = five_minutes_ago.getTime();

		// Make sure that start date is earlier than end date
		if (date_range.start.after(date_range.end))
			sanitized_date_range.start = sanitized_date_range.end;

		return sanitized_date_range;
	}

	// ========================================================================
	static void wrapTextWithTag(XmlSerializer serializer, String text, String tag) throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag("", tag);
		serializer.text( text );
		serializer.endTag("", tag);
	}
	
	// ========================================================================
	public static String generateRecordRequest(String next_token, DateRange date_range, List<Long> order_numbers, List<NotificationType> notification_types) {

		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();

		try {
			
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag("", NotificationsApiSchema.NotificationHistoryRequest.NOTIFICATION_HISTORY_REQUEST);
			serializer.attribute("", "xmlns", NotificationsApiSchema.NAMESPACE_PREFIX);

			// All other arguments are ignored if we have a "next_token".
			if (next_token != null) {
				
				wrapTextWithTag(serializer, next_token, NotificationHistoryRequest.TAG_NEXT_PAGE_TOKEN);

			} else {

				// Adds an optional date range query parameters
				if (date_range != null) {
					DateRange sanitized_date_range = sanitizeDateRange(date_range);
					
					Time start_time = new Time();
					start_time.set(sanitized_date_range.start.getTime());
					wrapTextWithTag(serializer, start_time.format3339(false), NotificationHistoryRequest.TAG_START_TIME);
					
					Time end_time = new Time();
					end_time.set(sanitized_date_range.end.getTime());
					wrapTextWithTag(serializer, end_time.format3339(false), NotificationHistoryRequest.TAG_END_TIME);
				}
				
				// Optionally queries for specific order numbers
				if (order_numbers != null) {
					serializer.startTag("", NotificationsApiSchema.NotificationHistoryRequest.TAG_ORDER_NUMBERS);
					for (long order_number : order_numbers)
						wrapTextWithTag(serializer, Long.toString(order_number), NotificationHistoryRequest.TAG_GOOGLE_ORDER_NUMBER);
					serializer.endTag("", NotificationsApiSchema.NotificationHistoryRequest.TAG_ORDER_NUMBERS);
				}

				// Specifies which types of notifications we're interested in
				serializer.startTag("", NotificationsApiSchema.NotificationHistoryRequest.TAG_NOTIFICATION_TYPES);
				for (NotificationType notification_type : notification_types)
					wrapTextWithTag(serializer, NotificationHistoryRequest.NOTIFICATION_TYPE_TAGS[notification_type.ordinal()], NotificationHistoryRequest.TAG_NOTIFICATION_TYPE);
				serializer.endTag("", NotificationsApiSchema.NotificationHistoryRequest.TAG_NOTIFICATION_TYPES);
			}

			serializer.endTag("", NotificationsApiSchema.NotificationHistoryRequest.NOTIFICATION_HISTORY_REQUEST);

			serializer.endDocument();
			return writer.toString();


		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	// ========================================================================	
	public static class ChargesRangeFetchResult {

		public GoogleCheckoutException google_exception = null;
		public IOException io_exception = null;

		public List<ChargeAmount> charges_list = new ArrayList<ChargeAmount>();
	}
	

	// ========================================================================
	public static <T> T getLast(List<T> list) {
		return list.get(list.size() - 1);
	}
	
	// ========================================================================
	public static ChargesRangeFetchResult fetchChargesRange(CancellableProgressIncrementor cancel_wrapper, UsernamePasswordCredentials credentials, DateRange date_range) {

		// Since the records are returned in order of ascending date,
		// use the user-requested start date as the opening span value, then
		// in each iteration widen that span to include the returned date.
		// When all of the records have been returned, close the span with the end date.
		
		ChargesRangeFetchResult charges_fetch_result = new ChargesRangeFetchResult();
		

		ChargeAmountBatch charge_batch;	// Deals with "continue" tokens
		String next_token = null;
		
		Date last_date = date_range.start;
			
		// We postpone throwing errors until we cache the records we've obtained.
		try {
			
			do {
				// If cancelled, must not *break*; we must *return* to avoid marking
				// the request range as fully cached.
				if (cancel_wrapper.isCancelled()) return charges_fetch_result;
				
				String charges_request_xml = generateRecordRequest(next_token, date_range, null, Arrays.asList(new NotificationType[] {NotificationType.CHARGE_AMOUNT}));
				InputStream is = getNotificationsStream(credentials, charges_request_xml);
				charge_batch = extractChargeBatch(is);
				is.close();
	
				
				charges_fetch_result.charges_list.addAll(charge_batch.item_list);
				
				if (charge_batch.item_list.size() > 0) {
					Date list_end = getLast(charge_batch.item_list).date;
					cancel_wrapper.notifyIncrementalProgress( new DateRange(last_date, list_end).getMillisDelta() );
					last_date = list_end;
				}
				
				next_token = charge_batch.getNextPageToken();

			} while (charge_batch.getNextPageToken() != null);

		} catch (GoogleCheckoutException e) {
			charges_fetch_result.google_exception = e;
			e.printStackTrace();
		} catch (IOException e) {
			charges_fetch_result.io_exception = e;
			e.printStackTrace();
		}

		return charges_fetch_result;
	}

	// ================================================================
	public static void fillInMerchantItemIds(CancellableProgressNotifier cancel_wrapper, int max_http_threads, DatabaseRevenue database, UsernamePasswordCredentials credentials) throws GoogleCheckoutException, IOException {
		
		List<Long> incomplete_order_numbers = database.queryOrdersLackingItemIds();
		Log.d(TAG, "Updated pending order numbers: " + incomplete_order_numbers.size());

		// Assigns Merchant Item IDs to all Order Numbers
		fetchNewOrders(
				cancel_wrapper,
				max_http_threads,
				credentials,
				incomplete_order_numbers,
				new MerchantItemIdAssociator(database),
				false);

		assignMerchantItemLabels(cancel_wrapper, max_http_threads,database, credentials);
	}

	// ================================================================
	/** Assigns labels to all Merchant Items that lack one. */
	static void assignMerchantItemLabels(
			CancellableProgressNotifier cancel_wrapper, 
    		int max_http_threads,
    		DatabaseRevenue database,
    		UsernamePasswordCredentials credentials) throws GoogleCheckoutException, IOException {

		List<Long> unlabeled_merchant_item_representatives = database.getRepresentativeUnlabeledMerchantItemIds();
//		Log.d(TAG, "Count of unlabeled representatives: " + unlabeled_merchant_item_representatives.size());

		List<NewOrder> representative_orders = fetchNewOrders(cancel_wrapper, max_http_threads, credentials, unlabeled_merchant_item_representatives, null, true);
		Map<Long, TitleDescription> merchant_item_label_map = new HashMap<Long, TitleDescription>();
		for (NewOrder order : representative_orders) {
//			Log.d(TAG, "Assigning label \"" + order.merchant_item_name + "\" to " + order.merchant_item_id);
			merchant_item_label_map.put(order.merchant_item_id, new TitleDescription(order.merchant_item_name, order.merchant_item_description));
		}

//		Log.d(TAG, "Item label map size: " + merchant_item_label_map.size());
		database.setMerchantItemNames(merchant_item_label_map);
	}

	// ================================================================
	static class OrderFetcherThread extends Thread {

		final CancellableProgressNotifier cancel_wrapper;
		final UsernamePasswordCredentials credentials;
		final List<Long> partial_order_numbers;
		final boolean populate_names;
		
		public OrderFetcherThread(
				CancellableProgressNotifier cancel_wrapper,
	    		UsernamePasswordCredentials credentials,
	    		List<Long> partial_order_numbers,
	    		boolean populate_names) {
			
			this.cancel_wrapper = cancel_wrapper;
			this.credentials = credentials;
			this.partial_order_numbers = partial_order_numbers;
			this.populate_names = populate_names;
		}
		
		IOException io_exception;
		GoogleCheckoutException google_exception;
		public List<NewOrder> all_orders_partial_intermediate;
		
		@Override
		public void run() {
			try {
				this.all_orders_partial_intermediate = obtainAndProcessRecords(credentials, cancel_wrapper, partial_order_numbers, populate_names);
			} catch (IOException e) {
				this.io_exception = e;
				e.printStackTrace();
			} catch (GoogleCheckoutException e) {
				this.google_exception = e;
				e.printStackTrace();
			}
		}
	};
	
	// ================================================================
	/** This is a dual-purpose function. It either can populate missing
	 * Merchant Item IDs for an order number, or fetch item names for
	 * Merchant Items without a label.  This depends on "populate_names". 
	 */
    static List<NewOrder> fetchNewOrders(
    		final CancellableProgressNotifier cancel_wrapper,
    		final int max_http_threads,
    		final UsernamePasswordCredentials credentials,
    		final List<Long> order_numbers,
    		final OrderProcessor order_processor,
    		final boolean populate_names) throws GoogleCheckoutException, IOException {

    	// Prepare stack
    	Stack<Long> order_numbers_stack = new Stack<Long>();
    	for (long order_number : order_numbers)
    		order_numbers_stack.push(order_number);
    	
    	
		List<NewOrder> all_obtained_orders = new ArrayList<NewOrder>();

		int total_count = 0;
		// Fetches the order list with batches of 20 concurrent chunks at a time,
		// with chunks of size 16
		while (!order_numbers_stack.isEmpty()) {

			if (cancel_wrapper.isCancelled()) break;
			cancel_wrapper.notifyProgress(total_count, order_numbers.size(), 0);

			// Run concurrent HTTP fetches
			List<OrderFetcherThread> fetcher_threads = new ArrayList<OrderFetcherThread>(); 
			final List<NewOrder> all_orders_intermediate = new ArrayList<NewOrder>();
			for (int i=0; i<max_http_threads && !order_numbers_stack.isEmpty(); i++) {
				
				// Skim off batches of sixteen
				final List<Long> partial_order_numbers = new ArrayList<Long>(); 
				for (int j=0; j<MAX_FILTER_ORDER_COUNT && !order_numbers_stack.isEmpty(); j++)
					partial_order_numbers.add(order_numbers_stack.pop());
				
				OrderFetcherThread fetcher_thread = new OrderFetcherThread(cancel_wrapper, credentials, partial_order_numbers, populate_names);
				fetcher_threads.add(fetcher_thread);
				fetcher_thread.start();
			}

			// Rejoin with this thread
			try {
				for (OrderFetcherThread thread : fetcher_threads) {
					thread.join();
					if (thread.io_exception != null)
						throw thread.io_exception;
					else if (thread.google_exception != null)
						throw thread.google_exception;
					else {
						all_orders_intermediate.addAll( thread.all_orders_partial_intermediate );
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Incrementally commit order associations to storage
			if (order_processor != null)
				order_processor.setOrdersAndRun(all_orders_intermediate);
			
			all_obtained_orders.addAll(all_orders_intermediate);
			total_count += all_orders_intermediate.size();
		}
		
		return all_obtained_orders;
	}

    // ========================================================================
    static List<NewOrder> obtainAndProcessRecords(UsernamePasswordCredentials credentials, CancellableProgressNotifier cancel_wrapper, List<Long> partial_order_numbers, boolean populate_names) throws IOException, GoogleCheckoutException {

		List<NewOrder> all_orders_intermediate = new ArrayList<NewOrder>();
    	
    	// Deals with "continue" tokens
		NewOrderBatch order_batch;
		String next_token = null;
		
		// Note, in current use cases, we won't have a continue token, so
		// there will be no sub-iterations.
		do {
			if (cancel_wrapper.isCancelled()) break;
			
			String merchant_items_request_xml = generateRecordRequest(next_token, null, partial_order_numbers, Arrays.asList(new NotificationType[] {NotificationType.NEW_ORDER}));
			InputStream is2 = getNotificationsStream(credentials, merchant_items_request_xml);
			order_batch = extractOrderBatch(is2, populate_names);
			next_token = order_batch.getNextPageToken();

			all_orders_intermediate.addAll(order_batch.orders);

		} while (order_batch.getNextPageToken() != null);
		
		return all_orders_intermediate;
    }
    
    // ========================================================================
	/** Assigns Merchant Item IDs to all Order Numbers */
    public static class MerchantItemIdAssociator extends OrderProcessor {

    	private DatabaseRevenue database;
    	public MerchantItemIdAssociator(DatabaseRevenue database) {
    		this.database = database;
    	}
    	
		@Override
		public void run() {

			Map<Long, Long> ids_map = new HashMap<Long, Long>();
			for (NewOrder order : this.orders)
				ids_map.put(order.google_order_number, order.merchant_item_id);

			this.database.setMerchantItemIds(ids_map);
		}
    }

    // ================================================================
    public static class ChargeAmount {
    	public int cents;
    	public Date date;
    	public long google_order_number;
    }
    
    // ================================================================
    public static class ChargeAmountBatch implements GoogleCheckoutXmlResponse {
    	final List<ChargeAmount> item_list = new ArrayList<ChargeAmount>();
    	private String next_page_token;
    	
		@Override
		public String getNextPageToken() {
			return next_page_token;
		}
		
		public void setNextPageToken(String next_page_token) {
			this.next_page_token = next_page_token;
		}
    }

	// ========================================================================
	public static ChargeAmountBatch extractChargeBatch(InputStream is) throws GoogleCheckoutException, IOException {

		try {
			XMLReader xr = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
			ChargeNotificationResponseHandler chargeHandler = new ChargeNotificationResponseHandler();
			xr.setContentHandler(chargeHandler);

			xr.parse(new InputSource(is)); 
			
			String error = chargeHandler.getErrorMessage();
			if (error != null) {
				if (error.contains("Malformed URL component: expected id:")) {
					error = "Bad Merchant ID.";
					throw new GoogleCheckoutLoginIdException(error);
				} else if (error.contains("Bad username and/or password for API Access.")) {
					throw new GoogleCheckoutLoginException(error);
				} else {
					throw new GoogleCheckoutException(error);					
				}
			}

			ChargeAmountBatch new_order_batch = chargeHandler.getChargeAmountBatch();
			return new_order_batch;

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		return null;
	}

    // ================================================================
    public static class NewOrder {
    	
    	public static class Address {
    		public String email, companyName, contactName, address1,
    			address2, phone, fax, countryCode, postalCode, city, region; 
    	}

    	public static class ShoppingCart {
    		public static class ShoppingCartItem {
    			long merchantItemId;
    			int quantity;
    			float unitPrice;
    			String itemName, itemDescription;
    			String digitalContentDescription;
    		}
    		
    		public List<ShoppingCartItem> items;
    	}

    	public Address buyer_billing_address, buyer_shipping_address;
    	public ShoppingCart shopping_cart;
    	
    	public Date date;

    	
    	public long google_order_number;
    	public long merchant_item_id;	// We're assuming that only one item is purchased per order.
    	public String merchant_item_name;
    	public String merchant_item_description;
    }

	// ========================================================================
    public static class NewOrderBatch implements GoogleCheckoutXmlResponse {
    	public final List<NewOrder> orders = new ArrayList<NewOrder>();
    	private String next_page_token;
    	
		@Override
		public String getNextPageToken() {
			return next_page_token;
		}
		
		public void setNextPageToken(String next_page_token) {
			this.next_page_token = next_page_token;
		}
    }

	// ========================================================================
    public static class GoogleCheckoutException extends Exception {
    	public GoogleCheckoutException(String error_message) {
    		super(error_message);
    	}
    }

	// ========================================================================
    public static class GoogleCheckoutLoginException extends GoogleCheckoutException {
    	public GoogleCheckoutLoginException(String error_message) {
    		super(error_message);
    	}
    }
    
	// ========================================================================
    public static class GoogleCheckoutLoginIdException extends GoogleCheckoutLoginException {
		public GoogleCheckoutLoginIdException(String errorMessage) {
			super(errorMessage);
		}
    }

	// ========================================================================
	public static NewOrderBatch extractOrderBatch(InputStream is, boolean populate_names) throws GoogleCheckoutException, IOException {

		try {
			XMLReader xr = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
			OrderNotificationResponseHandler orderHandler = new OrderNotificationResponseHandler(populate_names);
			xr.setContentHandler(orderHandler);

			xr.parse(new InputSource(is)); 
			
			String error = orderHandler.getErrorMessage();
			if (error != null) {
				throw new GoogleCheckoutException(error);
			}

			NewOrderBatch new_order_batch = orderHandler.getNewOrderBatch();
			return new_order_batch;

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		return null;
	}

	// ========================================================================
	public static InputStream getNotificationsStream(UsernamePasswordCredentials credentials, String request_xml) throws IOException {

		Uri notifications_api_uri = new Uri.Builder()
			.scheme(GoogleCheckoutUtils.SECURE_HTTP_SCHEME)
			.authority(GoogleCheckoutUtils.TARGET_CHECKOUT_PAGE.getAuthority())
			.path("api/checkout/v2/reports/Merchant/" + credentials.getUserName())
			.build();

		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpResponse response = null;
		HttpPost httppost = new HttpPost( notifications_api_uri.toString() );
		try {
			httppost.setEntity(new StringEntity(request_xml));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		httpclient.addRequestInterceptor(preemptiveAuth, 0);
		httpclient.getCredentialsProvider().setCredentials(
				new AuthScope(null, -1),
				credentials);

		try {
			response = httpclient.execute(httppost);
			return response.getEntity().getContent();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		}
		return null;
	}
}
