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

import org.achartengine.activity.GraphicalActivity.LabeledDatum;

import android.database.Cursor;

public class LabeledDoubleDatumExtractor implements DatumExtractor<LabeledDatum> {

  public LabeledDatum getDatum(Cursor cursor, int data_column, int label_column) {
    
    LabeledDatum labeled_datum = new LabeledDatum();
    double datum = cursor.getDouble(data_column);
    labeled_datum.datum = datum;
    labeled_datum.label = cursor.getString(label_column);
    return labeled_datum;
  }
}
