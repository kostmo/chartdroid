package org.crittr.task;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.crittr.browse.Market;
import org.crittr.browse.R;
import org.crittr.browse.activity.TabActivityTaxonExtendedInfo;
import org.crittr.shared.browser.containers.TaxonInfo;
import org.crittr.shared.browser.utilities.MediawikiSearchResponseParser;
import org.crittr.shared.browser.utilities.MediawikiSearchResponseParser.TitleDescription;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class WikiBlurbGetterTask extends AsyncTask<Void, Void, TitleDescription> {

	final static String TAG = Market.DEBUG_TAG;
	public final static String WIKIPEDIA_MOBILE_BASE = "http://en.m.wikipedia.org/wiki/";
	final static int MAX_BLURB_CHARACTERS = 1000;
	
	ProgressDialog wait_dialog;
	long tsn;
	TaxonInfo ti;
	String taxon_name;
	
	Context context;
	public WikiBlurbGetterTask(Context context, TaxonInfo ti, long tsn) {
		this.context = context;
		this.tsn = tsn;
		this.ti = ti;
	}
	
	
	void instantiate_latent_wait_dialog() {

		wait_dialog = new ProgressDialog(context);
		wait_dialog.setMessage("Fetching info...");
		wait_dialog.setIndeterminate(true);
		wait_dialog.setCancelable(false);
		wait_dialog.show();
	}

    @Override
    public void onPreExecute() {
		instantiate_latent_wait_dialog();
    }
	
	@Override
    protected TitleDescription doInBackground(Void... nothing) {

		this.taxon_name = TabActivityTaxonExtendedInfo.get_taxon_name_now(this.context, this.ti, this.tsn);
		try {

			TitleDescription blurb_xml = MediawikiSearchResponseParser.getWikipediaBlurb(taxon_name);
//			Log.e(TAG, "blurb xml:");
//			Log.d(TAG, blurb_xml.description);

			if (blurb_xml.description == null)
				return blurb_xml;
			
			String extracted_wikitext = MediawikiSearchResponseParser.extractBlurbContent(blurb_xml.description);

//			Log.e(TAG, "extracted wikitext:");
//			Log.i(TAG, extracted_wikitext);
			
			String partial = extracted_wikitext.trim();
			String chopped_blurb = partial.substring(0, Math.min(partial.length(), MAX_BLURB_CHARACTERS));
			

//			Log.e(TAG, "chopped blurb:");
//			Log.i(TAG, chopped_blurb);
			
			String rendered_wikitext = MediawikiSearchResponseParser.renderWikipediaBlurb(chopped_blurb);
			
//			Log.e(TAG, "Rendered wikitext:");
//			Log.d(TAG, rendered_wikitext);
			
			
			// Strip <p> tags
	    	String paragraph_tag_matcher = "</?p>";
	    	Pattern paragraph_tag_pattern = Pattern.compile(paragraph_tag_matcher, Pattern.CASE_INSENSITIVE);
	        Matcher classMatcher = paragraph_tag_pattern.matcher(rendered_wikitext);
	        String stripped_html = classMatcher.replaceAll("");
			
			blurb_xml.description = stripped_html + "...";

			return blurb_xml;
			
		} catch (NetworkUnavailableException e) {
			e.printStackTrace();
		}
		return null;
	}


     @Override
     protected void onPostExecute(TitleDescription td) {


		 wait_dialog.dismiss();
		 
		 if (td == null) {
			Toast.makeText(context, "Network unavailable.", Toast.LENGTH_SHORT).show();
			return;
		 }

        LayoutInflater factory = LayoutInflater.from(this.context);
        View dialog_wiki_blurb = factory.inflate(R.layout.dialog_wiki_blurb, null);

        TextView tv = (TextView) dialog_wiki_blurb.findViewById(R.id.blurb_text);
		tv.setText(Html.fromHtml(td.description), TextView.BufferType.SPANNABLE);
		
		
		
        String wikipedia_url = WIKIPEDIA_MOBILE_BASE + this.taxon_name;
        String html = "<a href=\"" + wikipedia_url + "\">Full article</a>";
		TextView tv2 = (TextView) dialog_wiki_blurb.findViewById(R.id.blurb_article_link);
		tv2.setText(Html.fromHtml(html), TextView.BufferType.SPANNABLE);
		tv2.setMovementMethod(LinkMovementMethod.getInstance());
		
		new AlertDialog.Builder(this.context)
		.setTitle(td.title)
		.setView(dialog_wiki_blurb)
		.create().show();
     }
 }