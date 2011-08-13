package org.openrtms.android;

import android.os.Bundle;
import android.preference.PreferenceActivity;
 
public class PrefView extends PreferenceActivity {
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);
        //  setContentView(R.layout.splash);


    }
    
    
}
