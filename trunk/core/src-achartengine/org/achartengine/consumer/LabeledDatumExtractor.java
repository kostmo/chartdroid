/**
 * Copyright (C) 2009 Karl Ostmo
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.achartengine.consumer;

import org.achartengine.consumer.DataCollector.LabeledDatum;

import android.database.Cursor;

public class LabeledDatumExtractor implements DatumExtractor<LabeledDatum> {

	@Override
	public LabeledDatum getDatum(Cursor cursor, int dataColumn, int labelColumn) {
		LabeledDatum labeled_datum = new LabeledDatum();
		
		labeled_datum.datum = new DualNumber(cursor, dataColumn);
		if (labelColumn >= 0)
			labeled_datum.label = cursor.getString(labelColumn);
		return labeled_datum;
	}
}
