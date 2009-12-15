package com.googlecode.chartdroid.pie;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.chartdroid.R;
import com.googlecode.chartdroid.R.id;
import com.googlecode.chartdroid.R.layout;

import android.content.Context;
import android.graphics.drawable.PaintDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;



public class ColorSwatchKeyAdapter extends BaseAdapter {


    final private LayoutInflater mInflater;
    
	public static class PieDataElement {
		public int datum;
		public int color;
		public String label;
	}
	
	List<PieDataElement> datum_list;

	Context context;
	
	public ColorSwatchKeyAdapter(Context context) {
		this.context= context;
		datum_list = new ArrayList<PieDataElement>();
		

        mInflater = LayoutInflater.from(context);
	}
	
	
	public void setData(List<PieDataElement> list) {
		
		datum_list = list;
		this.notifyDataSetInvalidated();
	}
	
	
	
	@Override
	public int getCount() {
		return datum_list.size();
	}

    /* Use the array-Positions as unique IDs */
	@Override
    public Object getItem(int position) { return position; }

	@Override
    public long getItemId(int position) { return position; }

	
	class ViewHolderPieDatum {

		ImageView swatch_holder;
		TextView label_holder;
		TextView datum_holder;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		
		
        ViewHolderPieDatum holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.key_list_item, null);
		

            holder = new ViewHolderPieDatum();
            holder.swatch_holder = (ImageView) convertView.findViewById(R.id.swatch_holder);
            holder.label_holder = (TextView) convertView.findViewById(R.id.label_holder);
            holder.datum_holder = (TextView) convertView.findViewById(R.id.datum_holder);
            

            convertView.setTag(holder);
      
        } else {
            holder = (ViewHolderPieDatum) convertView.getTag();
        }
        

	    int color = datum_list.get(position).color;
	    
    	holder.label_holder.setText( datum_list.get(position).label );
	    holder.datum_holder.setText( Integer.toString( datum_list.get(position).datum ) );
	    
        PaintDrawable p = new PaintDrawable(color);
        
        p.setIntrinsicHeight(32);
        p.setIntrinsicWidth(32);
        
        p.setCornerRadius(6);
        
        holder.swatch_holder.setImageDrawable( p );
	   
	   
        return convertView;
	}
}