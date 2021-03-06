package com.maksl5.bl_hunt.custom_ui.fragment;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.maksl5.bl_hunt.BlueHunter;
import com.maksl5.bl_hunt.LevelSystem;
import com.maksl5.bl_hunt.R;
import com.maksl5.bl_hunt.custom_ui.FragmentLayoutManager;
import com.maksl5.bl_hunt.net.AuthentificationSecure;

/**
 * @author Maksl5
 */
public class LeaderboardLayout {

	public static final int ARRAY_INDEX_NAME = 0;
	public static final int ARRAY_INDEX_LEVEL = 1;
	public static final int ARRAY_INDEX_PROGRESS_MAX = 2;
	public static final int ARRAY_INDEX_PROGRESS_VALUE = 3;
	public static final int ARRAY_INDEX_DEV_NUMBER = 4;
	public static final int ARRAY_INDEX_EXP = 5;
	public static final int ARRAY_INDEX_ID = 6;

	private volatile static ArrayList<LBAdapterData> showedFdList =
			new ArrayList<LBAdapterData>();
	public volatile static ArrayList<LBAdapterData> completeFdList =
			new ArrayList<LBAdapterData>();

	private static ThreadManager threadManager = null;

	public static HashMap<Integer, Integer> changeList = new HashMap<Integer, Integer>();

	public static void refreshLeaderboard(final BlueHunter bhApp) {

		refreshLeaderboard(bhApp, false);
	}

	public static void refreshLeaderboard(	final BlueHunter bhApp,
											boolean orientationChanged) {

		if (orientationChanged) {

			ListView listView;
			LeaderboardAdapter ldAdapter;

			if (bhApp.mainActivity.mViewPager == null) {
				bhApp.mainActivity.mViewPager =
						(ViewPager) bhApp.mainActivity.findViewById(R.id.pager);
			}

			ViewPager pager = bhApp.mainActivity.mViewPager;
			View pageView = pager.getChildAt(FragmentLayoutManager.PAGE_LEADERBOARD + 1);

			if (pageView == null) {
				listView = (ListView) pager.findViewById(R.id.listView1);
			}
			else {
				listView = (ListView) pageView.findViewById(R.id.listView1);
			}

			if (listView == null) {
				listView = (ListView) bhApp.mainActivity.findViewById(R.id.listView1);
			}

			if (listView == null) { return; }

			ldAdapter = (LeaderboardAdapter) listView.getAdapter();
			if (ldAdapter == null || ldAdapter.isEmpty()) {
				ldAdapter =
						new LeaderboardLayout().new LeaderboardAdapter(bhApp.mainActivity, R.layout.act_page_leaderboard_row, showedFdList);
				listView.setAdapter(ldAdapter);
			}

			ldAdapter.notifyDataSetChanged();
			return;

		}

		if (threadManager == null) {
			threadManager = new LeaderboardLayout().new ThreadManager();
		}

		RefreshThread refreshThread =
				new LeaderboardLayout().new RefreshThread(bhApp, threadManager);
		if (refreshThread.canRun()) {
			showedFdList.clear();
			bhApp.actionBarHandler.getMenuItem(R.id.menu_search).collapseActionView();
			refreshThread.execute(1, 5);
		}

	}

	public static void filterLeaderboard(	String text,
											BlueHunter bhApp) {

		if (threadManager.running) return;

		List<String> searchedList = new ArrayList<String>();

		if (bhApp.mainActivity.mViewPager == null) {
			bhApp.mainActivity.mViewPager =
					(ViewPager) bhApp.mainActivity.findViewById(R.id.pager);
		}

		ViewPager pager = bhApp.mainActivity.mViewPager;
		View pageView = pager.getChildAt(FragmentLayoutManager.PAGE_LEADERBOARD + 1);
		ListView lv;
		
		
		if (pageView == null) {
			lv = (ListView) pager.findViewById(R.id.listView1);
		}
		else {
			lv = (ListView) pageView.findViewById(R.id.listView1);
		}

		if (lv == null) {
			lv = (ListView) bhApp.mainActivity.findViewById(R.id.listView1);
		}

		if (lv == null) {
			return;
		}
		
		LeaderboardAdapter lbAdapter = (LeaderboardAdapter) lv.getAdapter();
		
		if (lbAdapter == null || lbAdapter.isEmpty()) {
			lbAdapter =
					new LeaderboardLayout().new LeaderboardAdapter(bhApp.mainActivity, R.layout.act_page_leaderboard_row, showedFdList);
			lv.setAdapter(lbAdapter);
		}
		
		text = text.toLowerCase();
		
		if (text.length() == 0) {
			if (!showedFdList.equals(completeFdList)) {
				showedFdList = new ArrayList<LBAdapterData>(completeFdList);
				lbAdapter.refreshList(showedFdList);
			}
		}
		else {

				ArrayList<LBAdapterData> filterList =
						new ArrayList<LBAdapterData>(completeFdList);

				final int count = filterList.size();
				final ArrayList<LBAdapterData> newValues = new ArrayList<LBAdapterData>();

				for (int i = 0; i < count; i++) {
					final LBAdapterData data = filterList.get(i);

					if (data.getName().toLowerCase().contains(text))
						if (!newValues.contains(data)) newValues.add(data);

					if (("" + data.getDevNum()).toLowerCase().contains(text))
						if (!newValues.contains(data)) newValues.add(data);

					if (("" + data.getExp()).toLowerCase().contains(text))
						if (!newValues.contains(data)) newValues.add(data);

					if (("" + data.getLevel()).toLowerCase().contains(text))
						if (!newValues.contains(data)) newValues.add(data);

				}

				showedFdList = newValues;
				lbAdapter.refreshList(showedFdList);

		}

	}

	private class RefreshThread extends AsyncTask<Integer, Void, String> {

		private BlueHunter bhApp;
		private ListView listView;

		private LeaderboardAdapter ldAdapter;

		private ThreadManager threadManager;

		private boolean canRun = true;

		private int scrollIndex;
		private int scrollTop;

		private int startIndex;
		private int length;

		private RefreshThread(BlueHunter app,
				ThreadManager threadManager) {

			super();
			this.bhApp = app;

			if (bhApp.mainActivity.mViewPager == null) {
				bhApp.mainActivity.mViewPager =
						(ViewPager) bhApp.mainActivity.findViewById(R.id.pager);
			}

			ViewPager pager = bhApp.mainActivity.mViewPager;
			View pageView = pager.getChildAt(FragmentLayoutManager.PAGE_LEADERBOARD + 1);

			if (pageView == null) {
				listView = (ListView) pager.findViewById(R.id.listView1);
			}
			else {
				listView = (ListView) pageView.findViewById(R.id.listView1);
			}

			if (listView == null) {
				listView = (ListView) bhApp.mainActivity.findViewById(R.id.listView1);
			}

			if (listView == null) {
				canRun = false;
				return;
			}

			scrollIndex = listView.getFirstVisiblePosition();
			View v = listView.getChildAt(0);
			scrollTop = (v == null) ? 0 : v.getTop();

			this.ldAdapter = (LeaderboardAdapter) listView.getAdapter();
			if (this.ldAdapter == null || this.ldAdapter.isEmpty()) {
				this.ldAdapter =
						new LeaderboardAdapter(bhApp.mainActivity, R.layout.act_page_leaderboard_row, showedFdList);
				this.listView.setAdapter(ldAdapter);
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
		protected String doInBackground(Integer... params) {

			startIndex = params[0];
			length = params[1];

			try {

				List<NameValuePair> postValues = new ArrayList<NameValuePair>();

				
				
				URI httpUri =
						URI.create(AuthentificationSecure.SERVER_GET_LEADERBOARD + "?s=" + startIndex + "&l=" + length);

				HttpClient httpClient;

				httpClient = new DefaultHttpClient();

				HttpPost postRequest = new HttpPost(httpUri);

				postRequest.setEntity(new UrlEncodedFormEntity(postValues));

				HttpResponse httpResponse = httpClient.execute(postRequest);

				String result = EntityUtils.toString(httpResponse.getEntity());

				if (!String.valueOf(httpResponse.getStatusLine().getStatusCode()).startsWith("2")) { return "Error=" + httpResponse.getStatusLine().getStatusCode(); }

				return result;
			}
			catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "Error=5\n" + e.getMessage();
			}
			catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "Error=4\n" + e.getMessage();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "Error=1\n" + e.getMessage();
			}

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(String result) {

			try {

				Pattern pattern = Pattern.compile("Error=(\\d+)");
				Matcher matcher = pattern.matcher(result);
				if (matcher.find()) {
					int errorCode = Integer.parseInt(matcher.group(1));

					LBAdapterData data =
							new LBAdapterData("Error " + errorCode, 0, 100, 0, 0, 0, 0);
					showedFdList.add(data);

					if (ldAdapter != null) ldAdapter.refreshList(showedFdList);

					listView.setSelectionFromTop(scrollIndex, scrollTop);

					threadManager.finished(this);

					return;
				}

				DocumentBuilder docBuilder;
				Document document = null;

				try {
					docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					document = docBuilder.parse(new InputSource(new StringReader(result)));
				}
				catch (Exception e) {

				}

				document.getDocumentElement().normalize();

				NodeList nodes = document.getElementsByTagName("user");

				boolean last = false;

				for (int i = 0; i < nodes.getLength(); i++) {

					Node node = nodes.item(i);
					if (node.getNodeType() == Node.ELEMENT_NODE) {

						Element element = (Element) node;

						int id = Integer.parseInt(element.getAttribute("id"));
						int rank = Integer.parseInt(element.getAttribute("rank"));

						String name =
								element.getElementsByTagName("name").item(0).getTextContent();
						int exp =
								Integer.parseInt(element.getElementsByTagName("exp").item(0).getTextContent());
						int num =
								Integer.parseInt(element.getElementsByTagName("number").item(0).getTextContent());

						int level = LevelSystem.getLevel(exp);
						int progressMax =
								LevelSystem.getLevelEndExp(level) - LevelSystem.getLevelStartExp(level);
						int progressValue = exp - LevelSystem.getLevelStartExp(level);

						last = element.getAttribute("last").equals("1");

						LBAdapterData data =
								new LBAdapterData(name, level, progressMax, progressValue, num, exp, id);

						completeFdList.add(data);
						completeFdList.set(rank - 1, data);
						showedFdList = completeFdList;

					}

				}

				threadManager.finished(this);

				if (last) {
					showedFdList = completeFdList;
					ldAdapter.notifyDataSetChanged();

					listView.setSelectionFromTop(scrollIndex, scrollTop);
				}
				else {

					ldAdapter.notifyDataSetChanged();
					new RefreshThread(bhApp, threadManager).execute(startIndex + length, length);

				}

			}
			catch (NullPointerException e) {
				if (bhApp != null && threadManager != null) {
					new RefreshThread(bhApp, threadManager).execute(startIndex, length);
				}

			}

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
		 */
		@Override
		protected void onProgressUpdate(Void... values) {

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
			setRunning(true);
			return true;
		}

		public boolean finished(RefreshThread refreshThread) {

			if (this.refreshThread.equals(refreshThread)) {
				setRunning(false);
				return true;
			}
			return false;
		}

		private void setRunning(boolean running) {

			this.running = running;

			if (!running) {
				if (!refreshThread.bhApp.netMananger.areThreadsRunning()) {
					MenuItem progressBar =
							refreshThread.bhApp.actionBarHandler.getMenuItem(R.id.menu_progress);
					progressBar.setVisible(false);
				}
			}
			else {
				MenuItem progressBar =
						refreshThread.bhApp.actionBarHandler.getMenuItem(R.id.menu_progress);
				progressBar.setVisible(true);
			}

		}
	}

	public class LBAdapterData {

		private String name;
		private int level;
		private int progressMax;
		private int progressValue;
		private int devNum;
		private int exp;
		private int id;

		public LBAdapterData(String name,
				int level,
				int progressMax,
				int progressValue,
				int devNum,
				int exp,
				int id) {

			this.name = name;
			this.level = level;
			this.progressMax = progressMax;
			this.progressValue = progressValue;
			this.devNum = devNum;
			this.exp = exp;
			this.id = id;
		}

		public String getName() {

			return name;
		}

		public void setName(String name) {

			this.name = name;
		}

		public int getLevel() {

			return level;
		}

		public void setLevel(int level) {

			this.level = level;
		}

		public int getProgressMax() {

			return progressMax;
		}

		public void setProgressMax(int progressMax) {

			this.progressMax = progressMax;
		}

		public int getProgressValue() {

			return progressValue;
		}

		public void setProgressValue(int progressValue) {

			this.progressValue = progressValue;
		}

		public int getDevNum() {

			return devNum;
		}

		public void setDevNum(int devNum) {

			this.devNum = devNum;
		}

		public int getExp() {

			return exp;
		}

		public void setExp(int exp) {

			this.exp = exp;
		}

		public int getId() {

			return id;
		}

		public void setId(int id) {

			this.id = id;
		}

		@Override
		public boolean equals(Object o) {

			// TODO Auto-generated method stub
			return id == ((LBAdapterData) (o)).id;
		}

	}

	public class LeaderboardAdapter extends ArrayAdapter<LBAdapterData> {

		private ArrayList<LBAdapterData> dataList;
		private ArrayList<LBAdapterData> originalDataList;

		public LeaderboardAdapter(Context context,
				int textViewResourceId,
				ArrayList<LBAdapterData> newLbData) {

			super(context, textViewResourceId, showedFdList);

			dataList = newLbData;
			originalDataList = newLbData;

		}

		@Override
		public View getView(int position,
							View convertView,
							ViewGroup parent) {

			View rowView = convertView;
			if (rowView == null) {
				LayoutInflater inflater =
						(LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.act_page_leaderboard_row, parent, false);

				LeaderboardLayout.ViewHolder viewHolder = new ViewHolder();

				viewHolder.rank = (TextView) rowView.findViewById(R.id.rankTxtView);
				viewHolder.name = (TextView) rowView.findViewById(R.id.nameTxtView);
				viewHolder.level = (TextView) rowView.findViewById(R.id.levelTxtView);
				viewHolder.levelPrg = (ProgressBar) rowView.findViewById(R.id.levelPrgBar);
				viewHolder.devices = (TextView) rowView.findViewById(R.id.devTxtView);
				viewHolder.exp = (TextView) rowView.findViewById(R.id.expTxtView);
				viewHolder.changeImg = (ImageView) rowView.findViewById(R.id.changeImgView);
				viewHolder.changeTxt = (TextView) rowView.findViewById(R.id.changeTxtView);

				rowView.setTag(viewHolder);
			}

			LeaderboardLayout.ViewHolder holder = (LeaderboardLayout.ViewHolder) rowView.getTag();

			if (holder != null && dataList != null && dataList.size() > position) {

				LBAdapterData user = dataList.get(position);

				String nameString = user.getName();

				holder.rank.setText("" + (completeFdList.indexOf(user) + 1) + ".");
				holder.name.setText(nameString);
				holder.name.setTag(user.getId());
				holder.level.setText("" + user.getLevel());
				holder.levelPrg.setMax(user.getProgressMax());
				holder.levelPrg.setProgress(user.getProgressValue());
				holder.devices.setText(new DecimalFormat(",###").format(user.getDevNum()) + " Devices");
				holder.exp.setText(new DecimalFormat(",###").format(user.getExp()) + " " + getContext().getString(R.string.str_foundDevices_exp_abbreviation));

				int rankNow = position + 1;
				Integer rankBefore = changeList.get(user.getId());

				if (rankBefore == null) rankBefore = position + 1;

				holder.changeTxt.setText("" + Math.abs(rankBefore - rankNow));

				if ((rankBefore - rankNow) > 0) {

					holder.changeImg.setImageResource(R.drawable.ic_change_up);

				}
				else if ((rankBefore - rankNow) < 0) {

					holder.changeImg.setImageResource(R.drawable.ic_change_down);

				}
				else if ((rankBefore - rankNow) == 0) {

					holder.changeImg.setImageResource(0);
					holder.changeTxt.setText("");
				}

			}
			return rowView;
		}

		public void refreshList(ArrayList<LBAdapterData> data) {
			
			clear();
			addAll(data);
			notifyDataSetChanged();
			
		}

	}

	static class ViewHolder {

		TextView rank;
		TextView name;
		TextView level;
		ProgressBar levelPrg;
		TextView devices;
		TextView exp;
		ImageView changeImg;
		TextView changeTxt;
	}

}