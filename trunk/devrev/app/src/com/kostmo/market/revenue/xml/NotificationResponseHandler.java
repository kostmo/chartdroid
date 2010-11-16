package com.kostmo.market.revenue.xml;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.kostmo.market.revenue.container.NotificationsApiSchema;
import com.kostmo.market.revenue.container.NotificationsApiSchema.NotificationHistoryResponse;

public abstract class NotificationResponseHandler extends DefaultHandler implements GoogleCheckoutXmlResponse {

	private String error_message;
	public String getErrorMessage() {
		return this.error_message;
	}

	private List<Long> invalid_order_numbers;
	List<Long> getInvalidOrderNumbers() {
		return this.invalid_order_numbers;
	}
	
	protected boolean in_notification_history_response = false;
	protected boolean in_notifications = false;
	protected boolean in_tag_next_page_token = false;

	private boolean in_invalid_order_numbers = false;
	private boolean in_invalid_order_number = false;
	
	protected boolean in_error = false;
	protected boolean in_error_message = false;
	
	protected StringBuilder buffer = new StringBuilder();

	// ========================================================================
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {

		if (localName.equals( NotificationHistoryResponse.TAG_NEXT_PAGE_TOKEN )) {
			this.buffer = new StringBuilder();
			this.in_tag_next_page_token = true;

		} else if (localName.equals( NotificationHistoryResponse.TAG_NOTIFICATION_HISTORY_RESPONSE )) {
			this.in_notification_history_response = true;

		} else if (localName.equals( NotificationHistoryResponse.TAG_NOTIFICATIONS )) {
			this.in_notifications = true;
			
		} else if (localName.equals(NotificationHistoryResponse.TAG_INVALID_ORDER_NUMBERS)) {
			this.invalid_order_numbers = new ArrayList<Long>();
			this.in_invalid_order_numbers = true;

		} else if (this.in_invalid_order_numbers && localName.equals(NotificationHistoryResponse.TAG_GOOGLE_ORDER_NUMBER)) {

			this.buffer = new StringBuilder();
			this.in_invalid_order_number = true;

		} else if (localName.equals(NotificationsApiSchema.TAG_ERROR)) {
			this.in_error = true;
			
		} else if (localName.equals(NotificationsApiSchema.TAG_ERROR_MESSAGE)) {
			
			this.buffer = new StringBuilder();
			this.in_error_message = true;
		}
	}
	
	// ========================================================================
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
	throws SAXException {

		if (localName.equals( NotificationHistoryResponse.TAG_NOTIFICATION_HISTORY_RESPONSE )) {
			this.in_notification_history_response = false;

		} else if (localName.equals( NotificationHistoryResponse.TAG_NEXT_PAGE_TOKEN )) {
			setNextPageToken(this.buffer.toString());
			this.in_tag_next_page_token = false;
			
		} else if (localName.equals(NotificationHistoryResponse.TAG_INVALID_ORDER_NUMBERS)) {
			this.in_invalid_order_numbers = false;
			
		} else if (this.in_invalid_order_numbers && localName.equals(NotificationHistoryResponse.TAG_GOOGLE_ORDER_NUMBER)) {
			
			long invalid_order_number = Long.parseLong( this.buffer.toString() );
			this.invalid_order_numbers.add( invalid_order_number );
			this.in_invalid_order_number = false;
			
		} else if (localName.equals( NotificationHistoryResponse.TAG_NOTIFICATIONS )) {
			this.in_notifications = false;

		} else if (localName.equals(NotificationsApiSchema.TAG_ERROR)) {
			this.in_error = false;
			
		} else if (this.in_error && localName.equals(NotificationsApiSchema.TAG_ERROR_MESSAGE)) {
			
			this.error_message = this.buffer.toString();
			this.in_error_message = false;
		}
	}
	
	// ========================================================================
	@Override
	public void characters(char ch[], int start, int length) {
		this.buffer.append(ch, start, length);
	}
}
