package com.maksl5.bl_hunt;



import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.TextView;



/**
 * 
 * 
 * 
 * Manages the complete device discovery. Easily construct the class with
 * <p>
 * {@code DiscoveryManager disMan = new DiscoveryManager(Activity);}
 * </p>
 * 
 * Supply the discovery state {@link TextView} with the {@link #supplyTextView(TextView)} method.
 * 
 * @author Maksl5[Markus Bensing]
 */

public class DiscoveryManager {

	private Activity parentActivity;
	private TextView stateTextView;
	private BluetoothDiscoveryHandler btHandler;

	public DiscoveryManager(Activity activity) {

		parentActivity = activity;

	}

	/**
	 * Starts the {@link DiscoveryManager}, constructing all its subclasses and initializing the BluetoothDiscovery.
	 * 
	 * @return <b>false</b> - if the given TextView is null.
	 */
	public boolean startDiscoveryManager() {

		if (stateTextView == null) return false;

		if (btHandler == null) {
			btHandler = new BluetoothDiscoveryHandler(new DiscoveryState(stateTextView, parentActivity));
			registerReceiver();
		}
		else {
			btHandler.forceSetStateText();
		}
		return true;
	}

	/**
	 * Supplies a new {@link TextView} to use for the discovery state.
	 * 
	 * @param txtView
	 *            - Discovery state {@link TextView}
	 * @return <b>false</b> - if the {@link TextView} is null.
	 */
	public boolean supplyTextView(TextView txtView) {

		if (txtView == null) return false;

		stateTextView = txtView;
		return true;

	}

	public boolean passEnableBTActivityResult(int result) {

		if (btHandler == null) return false;

		btHandler.enableBluetoothResult(result);
		return true;
	}

	public void registerReceiver() {

		IntentFilter filter = new IntentFilter();

		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

		parentActivity.registerReceiver(btHandler, filter);
	}

	public void unregisterReceiver() {

		parentActivity.unregisterReceiver(btHandler);

	}

	/**
	 * 
	 * This class handles the discovery state of the device discovery. Construct with
	 * {@code new DiscoveryState(TextView, Context);}
	 * 
	 * @author Maksl5[Markus Bensing]
	 * 
	 */

	public static class DiscoveryState {

		public static final int DISCOVERY_STATE_RUNNING = 1;
		public static final int DISCOVERY_STATE_STOPPED = 3;
		public static final int DISCOVERY_STATE_BT_OFF = -1;
		public static final int DISCOVERY_STATE_FINISHED = 2;
		public static final int DISCOVERY_STATE_BT_ENABLING = 4;
		public static final int DISCOVERY_STATE_ERROR = 10;
		public static final int DISCOVERY_STATE_OFF = 0;
		public static final int DISCOVERY_STATE_BT_DISABLING = 5;

		private int curDiscoveryState = DISCOVERY_STATE_OFF;
		private Context context;
		private TextView stateTextView;

		/**
		 * 
		 * @param stateTextView
		 *            The {@link TextView}, in which the discovery state should be shown.
		 * @param con
		 *            The {@link Context} of the base package or activity.
		 */

		public DiscoveryState(TextView stateTextView,
				Context con) {

			this.stateTextView = stateTextView;
			context = con;

			setDiscoveryStateTextView();
		}

		/**
		 * @param state
		 * @param context
		 * @return
		 */
		public static String getDiscoveryState(	int state,
												Context context) {

			switch (state) {
			case DISCOVERY_STATE_RUNNING:
				return formatStateText(context.getString(R.string.str_discoveryState_running));
			case DISCOVERY_STATE_STOPPED:
				return formatStateText(context.getString(R.string.str_discoveryState_stopped));
			case DISCOVERY_STATE_BT_OFF:
				return formatStateText(context.getString(R.string.str_discoveryState_btOff));
			case DISCOVERY_STATE_FINISHED:
				return formatStateText(context.getString(R.string.str_discoveryState_finished));
			case DISCOVERY_STATE_BT_ENABLING:
				return formatStateText(context.getString(R.string.str_discoveryState_btEnabling));
			case DISCOVERY_STATE_BT_DISABLING:
				return formatStateText(context.getString(R.string.str_discoveryState_btDisabling));
			case DISCOVERY_STATE_ERROR:
				return formatStateText(context.getString(R.string.str_discoveryState_error));
			case DISCOVERY_STATE_OFF:
				return formatStateText(context.getString(R.string.str_discoveryState_off));
			}

			return "";
		}

		private String getDiscoveryState() {

			return getDiscoveryState(curDiscoveryState, context);
		}

		/**
		 * @return
		 */
		public int getCurDiscoveryState() {

			return curDiscoveryState;
		}

		/**
		 * @return
		 */
		public String getCurDiscoveryStateText() {

			return getDiscoveryState();
		}

		private static String formatStateText(String stateText) {

			char[] splittedText = stateText.toCharArray();

			String newText = "";

			for (int i = 0; i < splittedText.length; i++) {
				newText = newText + String.valueOf(splittedText[i]) + " ";
			}

			return newText.trim().toUpperCase();
		}

		/**
		 * @param state
		 * @return
		 */
		public boolean setDiscoveryStateTextView(int state) {

			if (stateTextView == null) return false;

			stateTextView.setText(getDiscoveryState(state, context));
			return true;
		}

		/**
		 * @return
		 */
		public boolean setDiscoveryStateTextView() {

			if (stateTextView == null) return false;

			stateTextView.setText(formatStateText(getDiscoveryState()));
			return true;
		}

		/**
		 * @param state
		 * @return
		 */
		public boolean setDiscoveryState(int state) {

			// if (state != (DISCOVERY_STATE_BT_OFF | DISCOVERY_STATE_ERROR | DISCOVERY_STATE_FINISHED | DISCOVERY_STATE_OFF | DISCOVERY_STATE_RUNNING | DISCOVERY_STATE_STOPPED | DISCOVERY_STATE_BT_ENABLING | DISCOVERY_STATE_BT_DISABLING))
			//	return false;

			curDiscoveryState = state;
			return true;
		}

	}

	/**
	 * Handles the native Bluetooth events and manages Bluetooth related user input.
	 * 
	 * @author Maksl5[Markus Bensing]
	 * 
	 */
	private class BluetoothDiscoveryHandler extends BroadcastReceiver {

		private DiscoveryState disState;
		private BluetoothAdapter btAdapter;

		private BluetoothDiscoveryHandler(DiscoveryState state) {

			disState = state;
			btAdapter = BluetoothAdapter.getDefaultAdapter();

			if (isBluetoothSupported()) {
				if (!isBluetoothEnabled()) {
					parentActivity.startActivityForResult(new Intent(parentActivity, EnableBluetoothActivity.class), 64);
				}
			}

		}

		private boolean isBluetoothSupported() {

			if (btAdapter == null) {
				return false;
			}
			else {
				return true;
			}
		}

		private boolean isBluetoothEnabled() {

			if (isBluetoothSupported()) if (btAdapter.isEnabled()) return true;

			return false;

		}

		private boolean forceSetStateText() {

			if (disState.setDiscoveryStateTextView()) {
				return true;
			}
			else {
				return false;
			}
		}

		private void enableBluetoothResult(int result) {

			switch (result) {
			case 1:
				enableBluetooth();
				break;
			case -1:

				break;
			default:
				break;
			}
		}

		private boolean enableBluetooth() {

			if (btAdapter.enable()) return true;

			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
		 */
		@Override
		public void onReceive(	Context context,
								Intent intent) {

			String action = intent.getAction();

			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

				switch (newState) {
				case BluetoothAdapter.STATE_OFF:
					disState.setDiscoveryState(DiscoveryState.DISCOVERY_STATE_BT_OFF);
					break;
				case BluetoothAdapter.STATE_TURNING_ON:
					disState.setDiscoveryState(DiscoveryState.DISCOVERY_STATE_BT_ENABLING);
					break;
				case BluetoothAdapter.STATE_TURNING_OFF:
					disState.setDiscoveryState(DiscoveryState.DISCOVERY_STATE_BT_DISABLING);
					break;
				case BluetoothAdapter.STATE_ON:
					disState.setDiscoveryState(DiscoveryState.DISCOVERY_STATE_OFF);
					break;
				default:
					break;
				}

				disState.setDiscoveryStateTextView();
			}

		}

	}

}
