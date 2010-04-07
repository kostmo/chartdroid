package org.achartengine.consumer;

import android.database.Cursor;


@SuppressWarnings("serial")
public class DualNumber extends Number {

	long mLong;
	double mDouble;
	
	DualNumber(long mLong, double mDouble) {
		this.mLong = mLong;
		this.mDouble = mDouble;
	}
	
	DualNumber(Cursor cursor, int column) {
		this(cursor.getLong(column), cursor.getDouble(column));
	}
	
	@Override
	public double doubleValue() {
		return mDouble;
	}

	@Override
	public float floatValue() {
		return (float) mDouble;
	}

	@Override
	public int intValue() {
		return (int) mLong;
	}

	@Override
	public long longValue() {
		return mLong;
	}
}
