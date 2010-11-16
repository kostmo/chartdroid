package org.crittr.track.containers;

import org.crittr.shared.browser.Constants;
import org.crittr.track.DatabaseSightings;

import android.content.Intent;
import android.view.View;



public class OrphanLabeler extends IntentDependentRunnable {
	private ViewHolderTaxon view_holder;
	public OrphanLabeler(ViewHolderTaxon view_holder) {
		super(new Intent());
		this.view_holder = view_holder;
	}
	public void run() {
		if (getIntent().getLongExtra(Constants.INTENT_EXTRA_TSN, DatabaseSightings.INVALID_TSN) == DatabaseSightings.ORPHAN_PARENT_ID)
			view_holder.orphan_textview.setVisibility(View.VISIBLE);
	}
}