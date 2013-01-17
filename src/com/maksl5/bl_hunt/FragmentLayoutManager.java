package com.maksl5.bl_hunt;



import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.maksl5.bl_hunt.activity.MainActivity;
import com.maksl5.bl_hunt.activity.MainActivity.CustomSectionFragment;
import com.maksl5.bl_hunt.custom_ui.AdjustedEditText;
import com.maksl5.bl_hunt.custom_ui.AdjustedEditText.OnBackKeyClickedListener;
import com.maksl5.bl_hunt.custom_ui.PatternProgressBar;
import com.maksl5.bl_hunt.storage.DatabaseManager;
import com.maksl5.bl_hunt.storage.DatabaseManager.DatabaseHelper;
import com.maksl5.bl_hunt.storage.MacAddressAllocations;



/**
 * 
 * @author Maksl5[Markus Bensing]
 * 
 */

public class FragmentLayoutManager {

	public static final int PAGE_DEVICE_DISCOVERY = 0;
	public static final int PAGE_LEADERBOARD = 1;
	public static final int PAGE_FOUND_DEVICES = 2;
	public static final int PAGE_ACHIEVEMENTS = 3;
	public static final int PAGE_PROFILE = 4;

	public static View getSpecificView(	Bundle params,
										LayoutInflater parentInflater,
										ViewGroup rootContainer,
										Context context) {

		int sectionNumber = params.getInt(CustomSectionFragment.ARG_SECTION_NUMBER);

		switch (sectionNumber) {
		case PAGE_DEVICE_DISCOVERY:
			return parentInflater.inflate(R.layout.act_page_discovery, rootContainer, false);
		case PAGE_LEADERBOARD:
			return parentInflater.inflate(R.layout.act_page_leaderboard, rootContainer, false);
		case PAGE_FOUND_DEVICES:
			return parentInflater.inflate(R.layout.act_page_founddevices, rootContainer, false);
		case PAGE_ACHIEVEMENTS:
			break;
		case PAGE_PROFILE:
			return parentInflater.inflate(R.layout.act_page_profile, rootContainer, false);

		}

		return new View(context);
	}

	/**
	 * @author Maksl5
	 * 
	 */
	public static class FoundDevicesLayout {

		public static final int ARRAY_INDEX_MAC_ADDRESS = 0;
		public static final int ARRAY_INDEX_NAME = 1;
		public static final int ARRAY_INDEX_RSSI = 2;
		public static final int ARRAY_INDEX_MANUFACTURER = 3;
		public static final int ARRAY_INDEX_EXP = 4;
		public static final int ARRAY_INDEX_TIME = 5;

		private volatile static List<String> showedFdList = new ArrayList<String>();
		private volatile static List<String> completeFdList = new ArrayList<String>();

		private static ThreadManager threadManager = null;

		public static int selectedItem = -1;

		private static OnItemLongClickListener onLongClickListener = new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(	AdapterView<?> parent,
											View view,
											int position,
											long id) {

				if (view.getContext() instanceof BlueHunter) {

					BlueHunter bhApp = (BlueHunter) view.getContext();

					selectedItem = position;

					bhApp.mainActivity.startActionMode(bhApp.actionBarHandler.actionModeCallback);
					view.setSelected(true);

					return true;
				}
				else {
					return false;
				}
			}

		};

		public static void refreshFoundDevicesList(final BlueHunter bhApp) {

			if (threadManager == null) {
				threadManager = new FoundDevicesLayout().new ThreadManager();
			}

			RefreshThread refreshThread =
					new FoundDevicesLayout().new RefreshThread(bhApp, threadManager);
			if (refreshThread.canRun()) {
				refreshThread.execute();
			}

		}

		public static void filterFoundDevices(	String text,
												BlueHunter bhApp) {

			List<String> searchedList = new ArrayList<String>();

			ListView lv =
					(ListView) bhApp.mainActivity.mViewPager.getChildAt(3).findViewById(R.id.listView2);
			FoundDevicesAdapter fdAdapter = (FoundDevicesAdapter) lv.getAdapter();
			if (fdAdapter == null || fdAdapter.isEmpty()) {
				fdAdapter =
						new FoundDevicesLayout().new FoundDevicesAdapter(bhApp.mainActivity, R.layout.act_page_founddevices_row, showedFdList);
				lv.setAdapter(fdAdapter);
			}

			if (text.equalsIgnoreCase("[unknown]")) {

				String unknownString = bhApp.getString(R.string.str_foundDevices_manu_unkown);

				for (String deviceAsString : completeFdList) {

					String[] device = deviceAsString.split(String.valueOf((char) 30));

					if (device[ARRAY_INDEX_MANUFACTURER].equals(unknownString)) {
						searchedList.add(deviceAsString);
					}
				}
				showedFdList = searchedList;
				fdAdapter.refill(showedFdList);

			}
			else if (text.length() == 0) {
				if (showedFdList != completeFdList) {
					showedFdList = completeFdList;
					fdAdapter.refill(showedFdList);
				}
			}
			else {
				fdAdapter.getFilter().filter(text);
			}

		}

		/**
		 * @param mainActivity
		 * @return
		 */
		public static String getSelectedMac() {

			if (selectedItem == -1) return null;

			String macString =
					showedFdList.get(selectedItem).split(String.valueOf((char) 30))[ARRAY_INDEX_MAC_ADDRESS];

			return macString;

		}

		private class RefreshThread extends AsyncTask<Void, List<String>, List<String>> {

			private BlueHunter bhApp;
			private ListView listView;

			private FoundDevicesAdapter fdAdapter;

			private ThreadManager threadManager;

			private boolean canRun = true;

			private int scrollIndex;
			private int scrollTop;

			private RefreshThread(BlueHunter app,
					ThreadManager threadManager) {

				super();
				this.bhApp = app;
				this.listView =
						(ListView) bhApp.mainActivity.mViewPager.getChildAt(PAGE_FOUND_DEVICES + 1).findViewById(R.id.listView2);

				listView.setOnItemLongClickListener(onLongClickListener);

				scrollIndex = listView.getFirstVisiblePosition();
				View v = listView.getChildAt(0);
				scrollTop = (v == null) ? 0 : v.getTop();

				this.fdAdapter = (FoundDevicesAdapter) listView.getAdapter();
				if (this.fdAdapter == null || this.fdAdapter.isEmpty()) {
					this.fdAdapter =
							new FoundDevicesAdapter(bhApp.mainActivity, R.layout.act_page_founddevices_row, showedFdList);
					this.listView.setAdapter(fdAdapter);
				}

				this.threadManager = threadManager;

				if (!this.threadManager.setThread(this)) {
					canRun = false;
				}

			}

			public boolean canRun() {

				return canRun;
			}

			@Override
			protected List<String> doInBackground(Void... params) {

				List<HashMap<String, String>> devices =
						new DatabaseManager(bhApp, bhApp.getVersionCode()).getAllDevices();
				List<String> listViewList = new ArrayList<String>();

				String expString = bhApp.getString(R.string.str_foundDevices_exp_abbreviation);
				DateFormat dateFormat = DateFormat.getDateTimeInstance();

				String tempString;

				for (int i = 0; i < devices.size(); i++) {

					HashMap<String, String> device = devices.get(i);

					String deviceMac = device.get(DatabaseHelper.COLUMN_MAC_ADDRESS);
					String manufacturer = device.get(DatabaseHelper.COLUMN_MANUFACTURER);
					String deviceTime = device.get(DatabaseHelper.COLUMN_TIME);

					if (manufacturer == null || manufacturer.equals("Unknown") || manufacturer.equals("")) {
						manufacturer = MacAddressAllocations.getManufacturer(deviceMac);
						new DatabaseManager(bhApp, bhApp.getVersionCode()).addManufacturerToDevice(deviceMac, manufacturer);

						if (manufacturer.equals("Unknown")) {
							manufacturer = bhApp.getString(R.string.str_foundDevices_manu_unkown);
						}
					}

					Long time =
							(deviceTime == null || deviceTime.equals("null")) ? 0 : Long.parseLong(deviceTime);

					tempString =
							deviceMac + (char) 30 + device.get(DatabaseHelper.COLUMN_NAME) + (char) 30 + "RSSI: " + device.get(DatabaseHelper.COLUMN_RSSI) + (char) 30 + manufacturer + (char) 30 + "+" + MacAddressAllocations.getExp(manufacturer.replace(" ", "_")) + " " + expString + (char) 30 + dateFormat.format(new Date(time));

					listViewList.add(tempString);

					publishProgress(listViewList);

				}

				return listViewList;

				// ListenerClass listenerClass = new FragmentLayoutManager().new ListenerClass();

				// lv.setOnHierarchyChangeListener(listenerClass);

			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
			 */
			@Override
			protected void onPostExecute(List<String> result) {

				if (!completeFdList.equals(result)) {
					completeFdList = result;
				}

				fdAdapter.refill(showedFdList);

				listView.setSelectionFromTop(scrollIndex, scrollTop);

				threadManager.finished(this);

			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
			 */
			@Override
			protected void onProgressUpdate(List<String>... values) {

				showedFdList = values[0];

				// fdAdapter.refill(showedFdList);

				listView.setSelectionFromTop(scrollIndex, scrollTop);

			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.os.AsyncTask#onPreExecute()
			 */
			@Override
			protected void onPreExecute() {

			}
		}

		private class ThreadManager {

			RefreshThread refreshThread;
			boolean running;

			/**
			 * @param refreshThread2
			 * @return
			 */
			public boolean setThread(RefreshThread refreshThread) {

				if (running) { return false; }

				this.refreshThread = refreshThread;
				running = true;
				return true;
			}

			public boolean finished(RefreshThread refreshThread) {

				if (this.refreshThread.equals(refreshThread)) {
					running = false;
					return true;
				}
				return false;
			}
		}

		public class FoundDevicesAdapter extends ArrayAdapter<String> {

			private final Object lock = new Object();

			private Context context;

			private FoundDevicesFilter filter;

			private List<String> devices;

			private ArrayList<String> originalValues;

			private boolean notifyOnChange = true;

			private LayoutInflater inflater;

			public FoundDevicesAdapter(Context context,
					int textViewResourceId,
					List<String> objects) {

				super(context, textViewResourceId, objects);
				init(context, textViewResourceId, 0, objects);

				devices = objects;

			}

			private void init(	Context context,
								int resource,
								int textViewResourceId,
								List<String> objects) {

				this.context = context;
				inflater =
						(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				this.devices = objects;

			}

			@Override
			public View getView(int position,
								View convertView,
								ViewGroup parent) {

				if (devices == null || devices.get(position) == null) { return new View(context); }

				View rowView = convertView;
				if (rowView == null) {
					rowView = inflater.inflate(R.layout.act_page_founddevices_row, parent, false);

					ViewHolder viewHolder = new ViewHolder();

					viewHolder.macAddress = (TextView) rowView.findViewById(R.id.macTxtView);
					viewHolder.name = (TextView) rowView.findViewById(R.id.nameTxtView);
					viewHolder.manufacturer =
							(TextView) rowView.findViewById(R.id.manufacturerTxtView);
					viewHolder.rssi = (TextView) rowView.findViewById(R.id.rssiTxtView);
					viewHolder.time = (TextView) rowView.findViewById(R.id.timeTxtView);
					viewHolder.exp = (TextView) rowView.findViewById(R.id.expTxtView);
					viewHolder.nameTableRow = (TableRow) rowView.findViewById(R.id.tableRow1);

					rowView.setTag(viewHolder);
				}

				ViewHolder holder = (ViewHolder) rowView.getTag();

				if (holder != null) {

					String deviceAsString = devices.get(position);
					String[] device = deviceAsString.split(String.valueOf((char) 30));

					String nameString = device[ARRAY_INDEX_NAME];
					if (nameString == null || nameString.equals("null")) {
						nameString = "";
						holder.nameTableRow.setVisibility(View.GONE);
					}
					else {
						holder.nameTableRow.setVisibility(View.VISIBLE);
					}

					holder.macAddress.setText(device[ARRAY_INDEX_MAC_ADDRESS]);
					holder.name.setText(nameString);
					holder.manufacturer.setText(device[ARRAY_INDEX_MANUFACTURER]);
					holder.rssi.setText(device[ARRAY_INDEX_RSSI]);
					holder.time.setText(device[ARRAY_INDEX_TIME]);
					holder.exp.setText(device[ARRAY_INDEX_EXP]);

				}
				return rowView;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.widget.ArrayAdapter#getFilter()
			 */
			@Override
			public Filter getFilter() {

				if (filter == null) {
					filter = new FoundDevicesFilter();
				}

				return filter;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.widget.ArrayAdapter#add(java.lang.Object)
			 */
			@Override
			public void add(String object) {

				synchronized (lock) {
					if (originalValues != null) {
						originalValues.add(object);
					}
					else {
						devices.add(object);
					}
				}
				if (notifyOnChange) notifyDataSetChanged();
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.widget.ArrayAdapter#addAll(java.util.Collection)
			 */
			@Override
			public void addAll(Collection<? extends String> collection) {

				synchronized (lock) {
					if (originalValues != null) {
						originalValues.addAll(collection);
					}
					else {
						devices.addAll(collection);
					}
				}
				if (notifyOnChange) notifyDataSetChanged();
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.widget.ArrayAdapter#addAll(T[])
			 */
			@Override
			public void addAll(String... items) {

				synchronized (lock) {
					if (originalValues != null) {
						Collections.addAll(originalValues, items);
					}
					else {
						Collections.addAll(devices, items);
					}
				}
				if (notifyOnChange) notifyDataSetChanged();
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.widget.ArrayAdapter#clear()
			 */
			@Override
			public void clear() {

				synchronized (lock) {
					if (originalValues != null) {
						originalValues.clear();
					}
					else {
						devices.clear();
					}
				}
				if (notifyOnChange) notifyDataSetChanged();
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.widget.ArrayAdapter#getCount()
			 */
			@Override
			public int getCount() {

				return devices.size();
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.widget.ArrayAdapter#getItem(int)
			 */
			@Override
			public String getItem(int position) {

				return devices.get(position);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.widget.ArrayAdapter#getPosition(java.lang.Object)
			 */
			@Override
			public int getPosition(String item) {

				return devices.indexOf(item);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.widget.ArrayAdapter#insert(java.lang.Object, int)
			 */
			@Override
			public void insert(	String object,
								int index) {

				synchronized (lock) {
					if (originalValues != null) {
						originalValues.add(index, object);
					}
					else {
						devices.add(index, object);
					}
				}
				if (notifyOnChange) notifyDataSetChanged();
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.widget.ArrayAdapter#remove(java.lang.Object)
			 */
			@Override
			public void remove(String object) {

				synchronized (lock) {
					if (originalValues != null) {
						originalValues.remove(object);
					}
					else {
						devices.remove(object);
					}
				}
				if (notifyOnChange) notifyDataSetChanged();
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.widget.ArrayAdapter#sort(java.util.Comparator)
			 */
			@Override
			public void sort(Comparator<? super String> comparator) {

				synchronized (lock) {
					if (originalValues != null) {
						Collections.sort(originalValues, comparator);
					}
					else {
						Collections.sort(devices, comparator);
					}
				}
				if (notifyOnChange) notifyDataSetChanged();
			}

			@Override
			public void notifyDataSetChanged() {

				super.notifyDataSetChanged();
				notifyOnChange = true;
			}

			@Override
			public void setNotifyOnChange(boolean notifyOnChange) {

				this.notifyOnChange = notifyOnChange;
			}

			public void refill(List<String> devices) {

				this.devices.clear();
				this.devices.addAll(devices);
				notifyDataSetChanged();
			}

			/**
			 * @author Maksl5[Markus Bensing]
			 * 
			 */
			private class FoundDevicesFilter extends Filter {

				/*
				 * (non-Javadoc)
				 * 
				 * @see android.widget.Filter#performFiltering(java.lang.CharSequence)
				 */
				@Override
				protected FilterResults performFiltering(CharSequence filterSequence) {

					FilterResults results = new FilterResults();

					if (originalValues == null) {
						synchronized (lock) {
							originalValues = new ArrayList<String>(devices);
						}
					}

					if (filterSequence == null || filterSequence.length() == 0) {
						ArrayList<String> list;
						synchronized (lock) {
							list = new ArrayList<String>(originalValues);
						}
						results.values = list;
						results.count = list.size();
					}
					else {
						String filterString = filterSequence.toString().toLowerCase();

						ArrayList<String> devicesList;
						synchronized (lock) {
							devicesList = new ArrayList<String>(originalValues);
						}

						final int count = devicesList.size();
						final ArrayList<String> newValues = new ArrayList<String>();

						for (int i = 0; i < count; i++) {
							final String device = devicesList.get(i);
							final String deviceString = device.toString().toLowerCase();

							final String[] deviceAsArray =
									deviceString.split(String.valueOf((char) 30));

							for (String property : deviceAsArray) {
								if (property.contains(filterString)) {
									if (!newValues.contains(device)) newValues.add(device);
								}
							}

						}

						results.values = newValues;
						results.count = newValues.size();
					}

					return results;
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see android.widget.Filter#publishResults(java.lang.CharSequence,
				 * android.widget.Filter.FilterResults)
				 */
				@Override
				protected void publishResults(	CharSequence constraint,
												FilterResults results) {

					devices = (List<String>) results.values;
					showedFdList = devices;
					if (results.count > 0) {
						notifyDataSetChanged();
					}
					else {
						notifyDataSetInvalidated();
					}

				}

			}

		}

		static class ViewHolder {

			TextView macAddress;
			TextView name;
			TextView manufacturer;
			TextView rssi;
			TextView time;
			TextView exp;
			TableRow nameTableRow;
		}

	}

	/**
	 * @author Maksl5
	 * 
	 */
	public static class DeviceDiscoveryLayout {

		public static void updateIndicatorViews(MainActivity mainActivity) {

			TextView expTextView = (TextView) mainActivity.findViewById(R.id.expIndicator);
			TextView lvlTextView = (TextView) mainActivity.findViewById(R.id.lvlIndicator);
			PatternProgressBar progressBar =
					(PatternProgressBar) mainActivity.findViewById(R.id.progressBar1);

			int exp = LevelSystem.getUserExp((BlueHunter) mainActivity.getApplication());
			mainActivity.exp = exp;
			int level = LevelSystem.getLevel(exp);

			expTextView.setText(String.format("%d %s / %d %s", exp, mainActivity.getString(R.string.str_foundDevices_exp_abbreviation), LevelSystem.getLevelEndExp(level), mainActivity.getString(R.string.str_foundDevices_exp_abbreviation)));
			lvlTextView.setText(String.format("%s %d", mainActivity.getString(R.string.str_foundDevices_level), level));

			progressBar.setMax(LevelSystem.getLevelEndExp(level) - LevelSystem.getLevelStartExp(level));
			progressBar.setProgress(exp - LevelSystem.getLevelStartExp(level));
		}

	}

	/**
	 * @author Maksl5
	 * 
	 */
	public static class StatisticLayout {

		public static void initializeView(final MainActivity mainActivity) {

			View parentContainer = mainActivity.mViewPager.getChildAt(PAGE_PROFILE + 1);

			final TextView nameTextView =
					(TextView) parentContainer.findViewById(R.id.nameTextView);
			final AdjustedEditText nameEditText =
					(AdjustedEditText) parentContainer.findViewById(R.id.nameEditText);

			// Listener
			nameTextView.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {

					nameEditText.setText(nameTextView.getText());

					nameTextView.animate().setDuration(500).alpha(0f);
					nameTextView.setVisibility(TextView.GONE);

					nameEditText.setAlpha(0f);
					nameEditText.setVisibility(EditText.VISIBLE);
					nameEditText.animate().setDuration(500).alpha(1f);

					InputMethodManager imm =
							(InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

					return true;
				}
			});

			nameEditText.setOnEditorActionListener(new OnEditorActionListener() {

				@Override
				public boolean onEditorAction(	TextView v,
												int actionId,
												KeyEvent event) {

					InputMethodManager imm =
							(InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);

					if (actionId == EditorInfo.IME_ACTION_DONE && nameEditText.isShown()) {

						imm.hideSoftInputFromWindow(nameEditText.getWindowToken(), 0);

						// submit();
						nameTextView.setText(nameEditText.getText());

						nameEditText.animate().setDuration(500).alpha(1f);
						nameEditText.setVisibility(EditText.GONE);

						nameTextView.setAlpha(0f);
						nameTextView.setVisibility(TextView.VISIBLE);
						nameTextView.animate().setDuration(500).alpha(1f);

						return true;
					}
					return false;
				}
			});

			nameEditText.setOnBackKeyClickListener(new OnBackKeyClickedListener() {

				@Override
				public void onBackKeyClicked() {

					InputMethodManager imm =
							(InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);

					if (nameEditText.isShown() && imm.isActive(nameEditText)) {

						imm.hideSoftInputFromWindow(nameEditText.getWindowToken(), 0);

						nameEditText.animate().setDuration(500).alpha(0f);
						nameEditText.setVisibility(EditText.GONE);

						nameTextView.setAlpha(0f);
						nameTextView.setVisibility(TextView.VISIBLE);
						nameTextView.animate().setDuration(500).alpha(1f);
					}
				}
			});

		}

	}

	private class ListenerClass implements OnHierarchyChangeListener {

		List<TextWatcherClass> listenerList;

		private ListenerClass() {

			listenerList = new ArrayList<TextWatcherClass>();

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.view.ViewGroup.OnHierarchyChangeListener#onChildViewAdded(android.view.View, android.view.View)
		 */
		@Override
		public void onChildViewAdded(	View parent,
										View child) {

			TextView nameTxtView = (TextView) child.findViewById(R.id.nameTxtView);
			if (nameTxtView.getText() == null || nameTxtView.getText().toString().equals("") || nameTxtView.toString().trim().equals("")) {
				((TableRow) child.findViewById(R.id.TableRow01)).setVisibility(View.GONE);

				TextWatcherClass txtWatcherClass = new TextWatcherClass(nameTxtView, child);
				nameTxtView.addTextChangedListener(txtWatcherClass);
				listenerList.add(txtWatcherClass);

			}
			else {
				((TableRow) child.findViewById(R.id.tableRow1)).setVisibility(View.VISIBLE);
			}

			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.view.ViewGroup.OnHierarchyChangeListener#onChildViewRemoved(android.view.View,
		 * android.view.View)
		 */
		@Override
		public void onChildViewRemoved(	View parent,
										View child) {

			TextView nameTxtView = (TextView) child.findViewById(R.id.nameTxtView);
			for (TextWatcherClass textWatcher : new ArrayList<TextWatcherClass>(listenerList)) {
				if (textWatcher.child.equals(child)) {
					textWatcher.nameTxtView.removeTextChangedListener(textWatcher);
					listenerList.remove(textWatcher);
				}
			}

		}

		private class TextWatcherClass implements TextWatcher {

			public TextView nameTxtView;
			View child;

			private TextWatcherClass(TextView nameTxtView,
					View child) {

				this.nameTxtView = nameTxtView;
				this.child = child;

			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.text.TextWatcher#afterTextChanged(android.text.Editable)
			 */
			@Override
			public void afterTextChanged(Editable s) {

				// TODO Auto-generated method stub

			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.text.TextWatcher#beforeTextChanged(java.lang.CharSequence, int, int, int)
			 */
			@Override
			public void beforeTextChanged(	CharSequence s,
											int start,
											int count,
											int after) {

				// TODO Auto-generated method stub

			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see android.text.TextWatcher#onTextChanged(java.lang.CharSequence, int, int, int)
			 */
			@Override
			public void onTextChanged(	CharSequence s,
										int start,
										int before,
										int count) {

				if (nameTxtView.getText() == null || nameTxtView.getText().toString().equals("") || nameTxtView.toString().trim().equals("")) {
					((TableRow) child.findViewById(R.id.tableRow1)).setVisibility(View.GONE);
				}
				else {
					((TableRow) child.findViewById(R.id.tableRow1)).setVisibility(View.VISIBLE);
				}

			}

		}
	}

}
