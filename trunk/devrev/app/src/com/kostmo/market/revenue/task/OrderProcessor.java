package com.kostmo.market.revenue.task;

import java.util.List;

import com.kostmo.market.revenue.xml.CheckoutXmlUtils.NewOrder;


// ================================================================
abstract public class OrderProcessor implements Runnable {
	protected List<NewOrder> orders;
	public void setOrdersAndRun(List<NewOrder> orders) {
		setOrders(orders);
		run();
	}
	
	public void setOrders(List<NewOrder> orders) {
		this.orders = orders;
	}
}