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

import android.database.Cursor;


public class DoubleDatumExtractor implements DatumExtractor<Double> {

	@Override
	public Double getDatum(Cursor cursor, int dataColumn, int labelColumn) {
		// TODO Auto-generated method stub
		return cursor.getDouble(dataColumn);
	}
}
