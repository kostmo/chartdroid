package com.kostmo.market.revenue.container;

public class NotificationsApiSchema {

	public static final String NAMESPACE_PREFIX = "http://checkout.google.com/schema/2";
	
	
	public static final String TAG_ERROR = "error";
	public static final String TAG_ERROR_MESSAGE = "error-message";
	
	
	public static class NotificationHistoryResponse {
		
		public static final String TAG_NOTIFICATION_HISTORY_RESPONSE = "notification-history-response";
		public static final String TAG_NOTIFICATIONS = "notifications";
		public static final String TAG_INVALID_ORDER_NUMBERS = "invalid-order-numbers";
		public static final String TAG_GOOGLE_ORDER_NUMBER = "google-order-number";
		

		public static final String TAG_NEXT_PAGE_TOKEN = NotificationHistoryRequest.TAG_NEXT_PAGE_TOKEN;
		
		public static class ChargeAmountNotification {
			public static final String TAG_CHARGE_AMOUNT_NOTIFICATION = "charge-amount-notification";
			
			public static final String TAG_TIMESTAMP = "timestamp";
			
			
			public static final String TAG_TOTAL_CHARGE_AMOUNT = "total-charge-amount";
			public static final String TAG_GOOGLE_ORDER_NUMBER = NotificationHistoryResponse.TAG_GOOGLE_ORDER_NUMBER;
		}

		public static class NewOrderNotification {
			public static final String TAG_NEW_ORDER_NOTIFICATION = "new-order-notification";
			
			public static final String TAG_TIMESTAMP = ChargeAmountNotification.TAG_TIMESTAMP;
			
			
			public static final String TAG_TOTAL_CHARGE_AMOUNT = "total-charge-amount";
			public static final String TAG_GOOGLE_ORDER_NUMBER = NotificationHistoryResponse.TAG_GOOGLE_ORDER_NUMBER;
			

			public static final String TAG_ORDER_TOTAL = "order-total";
			public static final String TAG_FULFILLMENT_ORDER_STATE = "fulfillment-order-state";
			public static final String TAG_FINANCIAL_ORDER_STATE = "financial-order-state";

			public static final String TAG_SHOPPING_CART = "shopping-cart";
			
			
			public static class BuyerBillingAddress {
				public static final String TAG_BUYER_BILLING_ADDRESS = "buyer-billing-address";
				
				public static final String TAG_EMAIL = "email";
				public static final String TAG_COMPANY_NAME = "company-name";
				public static final String TAG_CONTACT_NAME = "contact-name";
				public static final String TAG_ADDRESS1 = "address1";
				public static final String TAG_ADDRESS2 = "address2";
				public static final String TAG_PHONE = "phone";
				public static final String TAG_FAX = "fax";
				public static final String TAG_COUNTRY_CODE = "country-code";
				public static final String TAG_POSTAL_CODE = "postal-code";
				public static final String TAG_CITY = "city";
				public static final String TAG_REGION = "region";
			}
			
			public static class ShoppingCart {

				public static final String TAG_ITEMS = "items";
				public static final String TAG_ITEM = "item";
				public static final String TAG_DIGITAL_CONTENT = "digital-content";
				public static final String TAG_DESCRIPTION = "description";
				public static final String TAG_ITEM_NAME = "item-name";
				public static final String TAG_ITEM_DESCRIPTION = "item-description";

				public static final String TAG_UNIT_PRICE = "unit-price";
				public static final String TAG_QUANTITY = "quantity";
				public static final String TAG_MERCHANT_ITEM_ID = "merchant-item-id";
			}
		}
	}

	// ========================================================================
	public static class NotificationHistoryRequest {
		
		public final static String NOTIFICATION_HISTORY_REQUEST = "notification-history-request";

		public final static String TAG_START_TIME = "start-time";
		public final static String TAG_END_TIME = "end-time";


		public final static String TAG_NOTIFICATION_TYPES = "notification-types";
		public final static String TAG_NOTIFICATION_TYPE = "notification-type";

		public static final String TAG_ORDER_NUMBERS = "order-numbers";
		public static final String TAG_GOOGLE_ORDER_NUMBER = "google-order-number";
		
		public static final String TAG_NEXT_PAGE_TOKEN = "next-page-token";
		
		
		public enum NotificationType {
			CHARGE_AMOUNT, NEW_ORDER
		}
		public final static String TAG_CHARGE_AMOUNT = "charge-amount";
		public final static String TAG_NEW_ORDER = "new-order";
		public final static String[] NOTIFICATION_TYPE_TAGS = {TAG_CHARGE_AMOUNT, TAG_NEW_ORDER};
	}
}
