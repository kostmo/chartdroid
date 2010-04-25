package com.googlecode.chartdroid.market.sales.container;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// ========================================================================
public class HistogramBin extends DateRange {
	public List<SpreadsheetRow> rows;
	
	public HistogramBin() {
		super();
		this.rows = new ArrayList<SpreadsheetRow>();
	}
	
	HistogramBin(Date start, Date end, List<SpreadsheetRow> rows) {
		super(start, end);
		this.rows = rows;
	}
	
	public double getTotalIncome() {
		double total = 0;
		for (SpreadsheetRow row : rows)
			total += row.getIncome();
		return total;
	}
}