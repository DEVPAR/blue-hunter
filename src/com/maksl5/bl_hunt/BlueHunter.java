/**
 *  BlueHunter.java in com.maksl5.bl_hunt
 *  � Maksl5[Markus Bensing] 2012
 */
package com.maksl5.bl_hunt;



import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.app.Activity;
import android.app.Application;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import com.maksl5.bl_hunt.activity.ActionBarHandler;
import com.maksl5.bl_hunt.activity.MainActivity;
import com.maksl5.bl_hunt.net.Authentification;
import com.maksl5.bl_hunt.net.Authentification.LoginManager;
import com.maksl5.bl_hunt.net.NetworkManager;



/**
 * @author Maksl5[Markus Bensing]
 * 
 */
@ReportsCrashes(formKey = "dFpyWWtjQ1E3VV9EaklYbFZETmpQLVE6MQ")
public class BlueHunter extends Application {

	public Authentification authentification;
	public ActionBarHandler actionBarHandler;
	public DiscoveryManager disMan;
	public NetworkManager netMananger;
	public LoginManager loginManager;

	public MainActivity mainActivity;
	public Activity currentActivity;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {

		// TODO Auto-generated method stub
		super.onCreate();

		ACRA.init(this);
	}

	public int getVersionCode() {

		try {
			return getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionCode;
		}
		catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			return 0;
		}
	}

	public String getVersionName() {

		try {
			return getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionName;
		}
		catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}

}
