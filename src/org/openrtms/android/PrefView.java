package org.openrtms.android;

import android.os.Bundle;
import android.preference.PreferenceActivity;
 
public class PrefView extends PreferenceActivity {
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      //  setContentView(R.layout.splash);
        addPreferencesFromResource(R.xml.pref);

    }
    
    
}
