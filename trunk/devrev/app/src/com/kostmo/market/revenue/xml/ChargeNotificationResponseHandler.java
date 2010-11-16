package com.kostmo.market.revenue.xml;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.text.format.Time;

import com.kostmo.market.revenue.container.NotificationsApiSchema.NotificationHistoryResponse.ChargeAmountNotification;
import com.kostmo.market.revenue.xml.CheckoutXmlUtils.ChargeAmount;
import com.kostmo.market.revenue.xml.CheckoutXmlUtils.ChargeAmountBatch;

public class ChargeNotificationResponseHandler extends NotificationResponseHandler {

	final static String TAG = "ChargeNotificationResponseHandler";

	enum ChargeField {
		GOOGLE_ORDER_NUMBER,
		TOTAL_CHARGE_AMOUNT,
		TIMESTAMP
	}

	String[] CHARGE_FIELD_TAGS = {
			ChargeAmountNotification.TAG_GOOGLE_ORDER_NUMBER,
			ChargeAmountNotification.TAG_TOTAL_CHARGE_AMOUNT,
			ChargeAmountNotification.TAG_TIMESTAMP
	};
	List<String> CHARGE_FIELD_TAGS_LIST = Arrays.asList(CHARGE_FIELD_TAGS);
	
	
	private ChargeAmount current_charge;

	private ChargeAmountBatch charge_amount_batch = new ChargeAmountBatch();
	public ChargeAmountBatch getChargeAmountBatch() {
//		Log.e(TAG, "Number of parsed ChargeAmount's: " + this.charge_amount_batch.item_list.size());
		return this.charge_amount_batch;
	}

	private boolean in_charge_notification = false;
	
	// ========================================================================
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {

		if (this.in_charge_notification) {
			int charge_field_index = CHARGE_FIELD_TAGS_LIST.indexOf(localName);
			if (charge_field_index >= 0) {
				this.buffer = new StringBuilder();
			}

		} else if (localName.equals(ChargeAmountNotification.TAG_CHARGE_AMOUNT_NOTIFICATION)) {

			this.in_charge_notification = true;
			this.current_charge = new ChargeAmount();
			
		} else {
			super.startElement(namespaceURI, localName, qName, atts);
		}
	}

	// ========================================================================
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
	throws SAXException {
		if (localName.equals(ChargeAmountNotification.TAG_CHARGE_AMOUNT_NOTIFICATION)) {
			
			this.charge_amount_batch.item_list.add(this.current_charge);
			this.in_charge_notification = false;

		} else if (this.in_charge_notification) {
			int order_field_index = CHARGE_FIELD_TAGS_LIST.indexOf(localName);
			if (order_field_index >= 0) {
				
				String body = this.buffer.toString();
				
				switch (ChargeField.values()[order_field_index]) {
				case GOOGLE_ORDER_NUMBER:
					this.current_charge.google_order_number = Long.parseLong( body );
					break;
				case TOTAL_CHARGE_AMOUNT:
					this.current_charge.cents = Math.round( 100 * Float.parseFloat( body ) );
					break;
				case TIMESTAMP:
					Time t = new Time();
					t.parse3339( body );
					this.current_charge.date = new Date( t.toMillis(true) );
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
		return this.charge_amount_batch.getNextPageToken();
	}

	@Override
	public void setNextPageToken(String token) {
		this.charge_amount_batch.setNextPageToken(token);
	}
}
