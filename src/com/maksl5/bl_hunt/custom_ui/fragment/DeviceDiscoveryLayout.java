package com.maksl5.bl_hunt.custom_ui.fragment;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Transformation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.maksl5.bl_hunt.LevelSystem;
import com.maksl5.bl_hunt.R;
import com.maksl5.bl_hunt.DiscoveryManager.DiscoveryState;
import com.maksl5.bl_hunt.activity.MainActivity;
import com.maksl5.bl_hunt.custom_ui.FoundDevice;
import com.maksl5.bl_hunt.custom_ui.PatternProgressBar;
import com.maksl5.bl_hunt.storage.DatabaseManager;
import com.maksl5.bl_hunt.storage.MacAddressAllocations;

/**
 * @author Maksl5
 */
public class DeviceDiscoveryLayout {

	public static DiscoveryAdapter dAdapter;
	public static ListView lv;

	private static ArrayList<FoundDevice> curList;

	public static void updateIndicatorViews(MainActivity mainActivity) {

		TextView expTextView = (TextView) mainActivity
				.findViewById(R.id.expIndicator);
		TextView lvlTextView = (TextView) mainActivity
				.findViewById(R.id.lvlIndicator);
		TextView devicesTextView = (TextView) mainActivity
				.findViewById(R.id.txt_devices);
		TextView devExpPerDayTxt = (TextView) mainActivity
				.findViewById(R.id.txt_devExpPerDay);
		TextView devExpTodayTxt = (TextView) mainActivity
				.findViewById(R.id.txt_devExpToday);

		PatternProgressBar progressBar = (PatternProgressBar) mainActivity
				.findViewById(R.id.progressBar1);

		int exp = LevelSystem.getUserExp(mainActivity.getBlueHunter());
		mainActivity.exp = exp;
		int level = LevelSystem.getLevel(exp);

		DecimalFormat df = new DecimalFormat(",###");

		expTextView
				.setText(String.format(
						"%s %s / %s %s",
						df.format(exp),
						mainActivity
								.getString(R.string.str_foundDevices_exp_abbreviation),
						df.format(LevelSystem.getLevelEndExp(level)),
						mainActivity
								.getString(R.string.str_foundDevices_exp_abbreviation)));
		lvlTextView.setText(String.format("%d", level));

		progressBar.setMax(LevelSystem.getLevelEndExp(level)
				- LevelSystem.getLevelStartExp(level));
		progressBar.setProgress(exp - LevelSystem.getLevelStartExp(level));

		int deviceNum = new DatabaseManager(mainActivity.getBlueHunter())
				.getDeviceNum();
		long firstTime = new DatabaseManager(mainActivity.getBlueHunter())
				.getDevice(
						DatabaseManager.DatabaseHelper.COLUMN_TIME + " != 0",
						DatabaseManager.DatabaseHelper.COLUMN_TIME + " DESC")
				.getTime();

		long now = System.currentTimeMillis();
		double devPerDay = (deviceNum / (double) (now - firstTime)) * 86400000;
		double expPerDay = (exp / (double) (now - firstTime)) * 86400000;

		devicesTextView.setText(df.format(deviceNum));
		devExpPerDayTxt.setText(String.format("%.2f / %.2f", devPerDay,
				expPerDay));

		List<FoundDevice> devicesToday = new DatabaseManager(
				mainActivity.getBlueHunter()).getDevices(
				DatabaseManager.DatabaseHelper.COLUMN_TIME + " < " + now
						+ " AND " + DatabaseManager.DatabaseHelper.COLUMN_TIME
						+ " > " + (now - 86400000), null);
		int devicesTodayNum = devicesToday.size();

		int expTodayNum = 0;

		for (Iterator iterator = devicesToday.iterator(); iterator.hasNext();) {
			FoundDevice deviceIt = (FoundDevice) iterator.next();
			String manufacturer = deviceIt.getManufacturer();

			float bonus = deviceIt.getBonus();

			expTodayNum += MacAddressAllocations.getExp(manufacturer)
					* (1 + bonus);
		}

		devExpTodayTxt.setText(String.format("%d / %d", devicesTodayNum,
				expTodayNum));

		curList = new ArrayList<FoundDevice>(
				mainActivity.getBlueHunter().disMan.getFDInCurDiscovery());

		TextView devInCurDisTxt = (TextView) mainActivity
				.findViewById(R.id.txt_discovery_devInCycle_value);
		devInCurDisTxt.setText("" + curList.size());

		if (dAdapter == null) {
			dAdapter = new DeviceDiscoveryLayout().new DiscoveryAdapter(
					mainActivity, curList);

			if (lv == null)
				lv = (ListView) mainActivity
						.findViewById(R.id.discoveryListView);

			lv.setAdapter(dAdapter);
		}

		if (lv != null
				&& mainActivity != null
				&& mainActivity.getBlueHunter() != null
				&& mainActivity.getBlueHunter().disMan != null
				&& mainActivity.getBlueHunter().disMan.getCurDiscoveryState() != -2) {

			if (mainActivity.getBlueHunter().disMan.getCurDiscoveryState() == DiscoveryState.DISCOVERY_STATE_RUNNING
					&& lv.getVisibility() != View.VISIBLE) {
				startShowLV(mainActivity);
			} else if (mainActivity.getBlueHunter().disMan
					.getCurDiscoveryState() != DiscoveryState.DISCOVERY_STATE_RUNNING
					&& mainActivity.getBlueHunter().disMan
							.getCurDiscoveryState() != DiscoveryState.DISCOVERY_STATE_FINISHED
					&& lv.getVisibility() == View.VISIBLE) {
				stopShowLV(mainActivity);
			}
		}

		if (dAdapter.values.equals(curList)) {
			dAdapter.notifyDataSetChanged();
		} else {
			dAdapter.clear();
			dAdapter.addAll(curList);
		}

	}

	public static void startShowLV(MainActivity main) {

		AlphaAnimation animation = new AlphaAnimation(1f, 0f);
		animation.setDuration(1000);

		TableLayout discoveryInfo = (TableLayout) main
				.findViewById(R.id.discoveryInfoTable);
		discoveryInfo.startAnimation(animation);

		lv.setVisibility(View.VISIBLE);

		animation = new AlphaAnimation(0f, 1f);

		animation.setDuration(1000);
		discoveryInfo.startAnimation(animation);
		lv.startAnimation(animation);

	}

	public static void stopShowLV(MainActivity main) {

		AlphaAnimation animation = new AlphaAnimation(1f, 0f);
		animation.setDuration(1000);

		TableLayout discoveryInfo = (TableLayout) main
				.findViewById(R.id.discoveryInfoTable);

		lv.startAnimation(animation);

		animation = new AlphaAnimation(0f, 1f);
		animation.setDuration(1000);

		discoveryInfo.startAnimation(animation);
		lv.setVisibility(View.GONE);
	}

	public class DiscoveryAdapter extends ArrayAdapter<FoundDevice> {

		private Activity context;
		private ArrayList<FoundDevice> values;

		public DiscoveryAdapter(Activity context, ArrayList<FoundDevice> curList) {

			super(context, R.layout.act_page_discovery_row, curList);
			this.values = curList;
			this.context = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View rowView = convertView;
			if (rowView == null) {
				LayoutInflater inflater = context.getLayoutInflater();
				rowView = inflater.inflate(R.layout.act_page_discovery_row,
						null);

				DeviceDiscoveryLayout.Holder holder = new Holder();
				holder.macAddress = (TextView) rowView
						.findViewById(R.id.macTxtView);
				holder.manufacturer = (TextView) rowView
						.findViewById(R.id.manufacturerTxtView);
				holder.exp = (TextView) rowView.findViewById(R.id.expTxtView);
				holder.rssi = (ImageView) rowView.findViewById(R.id.rssiView);

				rowView.setTag(holder);

			}

			DeviceDiscoveryLayout.Holder vHolder = (DeviceDiscoveryLayout.Holder) rowView
					.getTag();

			FoundDevice device = values.get(position);

			String mac = device.getMacAddress();
			String manufacturer = device.getManufacturer();

			int exp = MacAddressAllocations.getExp(manufacturer);

			int bonusExp = (int) Math.floor(MacAddressAllocations
					.getExp(manufacturer) * device.getBonus());

			exp = bonusExp + exp;

			int rssi = device.getRssi();
			int rssiRes = 0;

			// 0 bars
			if (rssi <= -102)
				rssiRes = 0;

			// 1 bar
			if (rssi >= -101 && rssi <= -93)
				rssiRes = R.drawable.rssi_1;

			// 2 bars
			if (rssi >= -92 && rssi <= -87)
				rssiRes = R.drawable.rssi_2;

			// 3 bars
			if (rssi >= -86 && rssi <= -78)
				rssiRes = R.drawable.rssi_3;

			// 4 bars
			if (rssi >= -77 && rssi <= -40)
				rssiRes = R.drawable.rssi_4;

			// 5 bars
			if (rssi >= -41)
				rssiRes = R.drawable.rssi_5;

			if (manufacturer.equals("Unknown")) {
				manufacturer = context
						.getString(R.string.str_foundDevices_manu_unkown);
			}

			String expString = context
					.getString(R.string.str_foundDevices_exp_abbreviation);

			vHolder.macAddress.setText(mac);
			vHolder.manufacturer.setText(manufacturer);
			vHolder.exp.setText("+" + exp + " " + expString);
			vHolder.rssi.setImageResource(rssiRes);

			return rowView;
		}

	}

	static class Holder {

		TextView macAddress;
		TextView manufacturer;
		TextView exp;
		ImageView rssi;

	}

}