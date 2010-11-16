package org.crittr.track.activity;


import org.crittr.track.R;
import org.crittr.track.R.drawable;
import org.crittr.track.R.id;
import org.crittr.track.R.layout;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;


public class HelpGlossary extends Activity {
	
    
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        /*
        // Blurring is not a good idea when we animate the bird
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        WindowManager.LayoutParams old_attributes = getWindow().getAttributes();
        old_attributes.dimAmount = 0.85f;
        getWindow().setAttributes(old_attributes);
        */
        
        
        // Inflate our UI from its XML layout description.
        setContentView(R.layout.help_glossary);
        this.getWindow().setBackgroundDrawableResource(R.drawable.translucent_background);


        ((TextView) findViewById(R.id.links_test)).setMovementMethod(LinkMovementMethod.getInstance());
        
        
        // Run on startup
//    	this.startActivity(new Intent().setClass(this, AnimateFlyby.class));

    }
}