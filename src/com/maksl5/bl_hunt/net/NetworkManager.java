/**
 *  NetworkMananger.java in com.maksl5.bl_hunt
 *  © Maksl5[Markus Bensing] 2012
 */
package com.maksl5.bl_hunt.net;



import java.util.ArrayList;
import java.util.List;

import android.view.MenuItem;

import com.maksl5.bl_hunt.BlueHunter;
import com.maksl5.bl_hunt.R;
import com.maksl5.bl_hunt.activity.MainActivity;



/**
 * @author Maksl5[Markus Bensing]
 * 
 */
public class NetworkManager {

	private BlueHunter bhApp;
	private List<NetworkThread> curRunningThreads;

	public NetworkManager(BlueHunter app) {

		bhApp = app;
		curRunningThreads = new ArrayList<NetworkThread>();
	}

	public void addRunningThread(NetworkThread netThread) {

		curRunningThreads.add(netThread);
	}

	public synchronized void threadFinished(NetworkThread netThread) {

		curRunningThreads.remove(netThread);
		checkList();
	}

	public boolean areThreadsRunning() {

		if (curRunningThreads.size() == 0) return false;

		return true;
	}

	/**
	 * 
	 */
	private void checkList() {

		if (curRunningThreads.size() == 0) {
			if (!MainActivity.destroyed) {
				MenuItem progressBar = bhApp.actionBarHandler.getMenuItem(R.id.menu_progress);
				progressBar.setVisible(false);
			}
		}

	}

}
