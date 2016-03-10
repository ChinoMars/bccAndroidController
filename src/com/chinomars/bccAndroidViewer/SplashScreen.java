package com.chinomars.bccAndroidViewer;

//import java.security.acl.AclNotFoundException;
//import com.chinomars.bccAndroidViewer.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
/**
 * Created by Chino on 3/8/16.
 */
public class SplashScreen extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.splash);

        Thread splashTread = new Thread() {
            @Override
            public void run() {
                try {
                    int waited = 0;
                    while(waited < 5000) {
                        sleep(100);
                        waited += 100;
                    }
                } catch(InterruptedException e) {
                    // do nothing
                } finally {
                    startActivity(new Intent("com.chinomars.bccAndroidViewer.BccController"));
                    finish();
                }
            }
        };
        splashTread.start();
    }

}
