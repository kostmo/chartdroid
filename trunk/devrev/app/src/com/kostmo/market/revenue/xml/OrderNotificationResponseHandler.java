package com.kostmo.market.revenue.xml;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.text.format.Time;

import com.kostmo.market.revenue.container.NotificationsApiSchema.NotificationHistoryResponse.NewOrderNotification;
import com.kostmo.market.revenue.xml.CheckoutXmlUtils.NewOrder;
import com.kostmo.market.revenue.xml.CheckoutXmlUtils.NewOrderBatch;

public class OrderNotificationResponseHandler extends NotificationResponseHandler {

	final static String TAG = "OrderNotificationResponseHandler";
	
	public OrderNotificationResponseHandler(boolean populate_names) {

		/*
		if (!populate_names) {
			// If we don't want to populate the names, we just ignore the
			// MERCHANT_ITEM_NAME and MERCHANT_ITEM_DESCRIPTION tags,
			// which are the last 2 on the list.
			this.ORDER_FIELD_TAGS_LIST = this.ORDER_FIELD_TAGS_LIST.subList(
				OrderField.GOOGLE_ORDER_NUMBER.ordinal(),
				OrderField.MERCHANT_ITEM_NAME.ordinal()
			);
		}
		*/
	}

	enum OrderField {
		GOOGLE_ORDER_NUMBER,
		MERCHANT_ITEM_ID,
		MERCHANT_ITEM_NAME,
		MERCHANT_ITEM_DESCRIPTION,
		ORDER_TIMESTAMP	// Used only for debugging
	}

	final static String[] ORDER_FIELD_TAGS = {
			NewOrderNotification.TAG_GOOGLE_ORDER_NUMBER,
			NewOrderNotification.ShoppingCart.TAG_MERCHANT_ITEM_ID,
			NewOrderNotification.ShoppingCart.TAG_ITEM_NAME,
			NewOrderNotification.ShoppingCart.TAG_ITEM_DESCRIPTION,
			NewOrderNotification.TAG_TIMESTAMP
	};
	List<String> order_field_tags_list = Arrays.asList(ORDER_FIELD_TAGS);
	
	
	private NewOrder current_order;

	private NewOrderBatch new_order_batch = new NewOrderBatch();
	public NewOrderBatch getNewOrderBatch() {
//		Log.e(TAG, "Number of parsed NewOrder's: " + this.new_order_batch.orders.size());
		return this.new_order_batch;
	}
	
	private boolean in_new_order_notification = false;

	// ========================================================================
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {

		if (this.in_new_order_notification) {
			int order_field_index = order_field_tags_list.indexOf(localName);
			if (order_field_index >= 0) {
				this.buffer = new StringBuilder();
			}
		} else if (localName.equals(NewOrderNotification.TAG_NEW_ORDER_NOTIFICATION)) {
			this.in_new_order_notification = true;
			this.current_order = new NewOrder();
			
		} else {
			super.startElement(namespaceURI, localName, qName, atts);
		}
	}

	// ========================================================================
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
	throws SAXException {

//		Log.d(TAG, "Local name: " + localName);

		if (localName.equals(NewOrderNotification.TAG_NEW_ORDER_NOTIFICATION)) {
			
			this.new_order_batch.orders.add(this.current_order);
			this.in_new_order_notification = false;

		} else if (this.in_new_order_notification) {
			int order_field_index = order_field_tags_list.indexOf(localName);
			if (order_field_index >= 0) {
				
				String body = this.buffer.toString();
				
				switch (OrderField.values()[order_field_index]) {
				case GOOGLE_ORDER_NUMBER:
					try {
						this.current_order.google_order_number = Long.parseLong( body );
					} catch (NumberFormatException e) {
						throw new SAXException("Unparseable Order Number: " + body);
					}
					break;
				case MERCHANT_ITEM_ID:
					try {
						this.current_order.merchant_item_id = Long.parseLong( body );
					} catch (NumberFormatException e) {
						throw new SAXException("Unparseable Merchant Item ID: " + body);
					}
					break;
				case MERCHANT_ITEM_NAME:
					this.current_order.merchant_item_name = body;
					break;
				case MERCHANT_ITEM_DESCRIPTION:
					this.current_order.merchant_item_description = body;
					break;
				case ORDER_TIMESTAMP:
					Time t = new Time();
					t.parse3339( body );
					this.current_order.date = new Date( t.toMillis(true) );
					break;
				default:
					break;
				}
			}
		} else {
			super.endElement(namespaceURI, localName, qName);
		}
	}

	// ========================================================================
	@Override
	public String getNextPageToken() {
		return this.new_order_batch.getNextPageToken();
	}

	@Override
	public void setNextPageToken(String token) {
		this.new_order_batch.setNextPageToken(token);
	}
}
