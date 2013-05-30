package xtreeki.irc;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import de.greenrobot.event.EventBus;
import xtreeki.irc.event.BufferDetailsChangedEvent;

import java.util.ArrayList;

public class MainActivity extends SherlockFragmentActivity {
	private IRCService mBoundService;
	private boolean mIsBound = false;
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			mBoundService = ((IRCService.LocalBinder)iBinder).getService();
			mBoundService.setOwnedActivity(MainActivity.this);

			setupBufferSystem();
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBoundService = null;
		}
	};

	void doBindService() {
		startService(new Intent(this, IRCService.class));
		bindService(new Intent(this, IRCService.class),
				mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}
	void doUnbindService() {
		if (mIsBound) {
			mBoundService.setOwnedActivity(null);
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mDrawerLayout = (DrawerLayout)findViewById(R.id.drawerLayout);

		mBufferPager = (ViewPager)findViewById(R.id.viewPager);
		mBufferList = (ListView)findViewById(R.id.bufferList);


		mDrawerToggle = new ActionBarDrawerToggle(this,
				mDrawerLayout, R.drawable.ic_drawer,
				R.string.drawer_open, R.string.drawer_close) {
			@Override
			public void onDrawerOpened(View drawerView) {
				updateActionBarForPage();
				invalidateOptionsMenu();
			}

			@Override
			public void onDrawerClosed(View drawerView) {
				updateActionBarForPage();
				invalidateOptionsMenu();
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);


		EventBus.getDefault().register(this);

		doBindService();
	}

	@Override
	protected void onDestroy() {
		EventBus.getDefault().unregister(this);

		super.onDestroy();
		doUnbindService();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if (mDrawerToggle != null)
			mDrawerToggle.syncState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mVisibleBuffer != null)
			mVisibleBuffer.becomeVisible();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mVisibleBuffer != null)
			mVisibleBuffer.becomeInvisible();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// This is just copied from ActionBarDrawerToggle.. because its own version
		// doesn't work with the Sherlock menus.
		if (item != null && item.getItemId() == 0x0102002C && mDrawerToggle.isDrawerIndicatorEnabled()) {
			if (mDrawerLayout.isDrawerVisible(GravityCompat.START)) {
				mDrawerLayout.closeDrawer(GravityCompat.START);
			} else {
				mDrawerLayout.openDrawer(GravityCompat.START);
			}
			return true;
		}

		return super.onOptionsItemSelected(item);    //To change body of overridden methods use File | Settings | File Templates.
	}


	class BufferPagerAdapter extends FragmentPagerAdapter {
		BufferPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			if (position == 0)
				return new HomeFragment(mBoundService);
			return mBuffers.get(position - 1).createFragment();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			if (position == 0)
				return "Home";
			else
				return mBuffers.get(position - 1).getTitle();
		}

		@Override
		public long getItemId(int position) {
			if (position == 0)
				return -500;
			else
				return mBuffers.get(position - 1).uniqueKey;
		}

		@Override
		public int getCount() {
			return mBuffers.size() + 1;
		}
	}
	class BufferListAdapter extends BaseAdapter implements ListAdapter {
		@Override
		public int getCount() {
			return mBuffers.size() + 1;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			if (position == 0)
				return -500;
			return mBuffers.get(position - 1).uniqueKey;
		}

		@Override
		public View getView(int position, View view, ViewGroup viewGroup) {
			String title;
			int unreadCount;
			if (position == 0) {
				title = "Home";
				unreadCount = 0;
			} else {
				IRCBuffer buffer = mBuffers.get(position - 1);
				title = buffer.getTitle();
				unreadCount = buffer.unreadCount;
				Log.i("irc", "Unread: " + unreadCount + " for " + title);
			}

			if (view == null)
				view = getLayoutInflater().inflate(R.layout.drawer_list_item, viewGroup, false);

			// TODO: use ViewHolder thing for this
			((TextView)view.findViewById(R.id.bufferName)).setText(title);

			TextView unreadView = (TextView)view.findViewById(R.id.unreadCount);
			unreadView.setVisibility((unreadCount > 0) ? View.VISIBLE : View.INVISIBLE);
			if (unreadCount > 0)
				unreadView.setText(String.valueOf(unreadCount));

			return view;
		}
	}
	private ArrayList<IRCBuffer> mBuffers = new ArrayList<IRCBuffer>();
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	private ListView mBufferList;
	private ViewPager mBufferPager;
	private BufferPagerAdapter mBufferPagerAdapter;
	private BufferListAdapter mBufferListAdapter;

	private IRCBuffer mVisibleBuffer = null;

	private void setupBufferSystem() {
		mBuffers.addAll(mBoundService.getBuffers());

		mBufferPagerAdapter = new BufferPagerAdapter(getSupportFragmentManager());
		mBufferListAdapter = new BufferListAdapter();

		mBufferPager.setAdapter(mBufferPagerAdapter);
		mBufferList.setAdapter(mBufferListAdapter);

		mBufferPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				if (mVisibleBuffer != null)
					mVisibleBuffer.becomeInvisible();

				if (position == 0) {
					mVisibleBuffer = null;
				} else {
					mVisibleBuffer = mBuffers.get(position - 1);
					mVisibleBuffer.becomeVisible();
				}
				mBufferList.setItemChecked(position, true);
				updateActionBarForPage();
			}
		});
		mBufferList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				mBufferPager.setCurrentItem(position, true);
				mDrawerLayout.closeDrawers();
			}
		});
	}

	public void onBufferAdded(IRCBuffer buf) {
		final IRCBuffer saveMe = buf;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mBuffers.add(saveMe);
				mBufferPagerAdapter.notifyDataSetChanged();
				mBufferListAdapter.notifyDataSetChanged();
			}
		});
	}

	public void onEventMainThread(BufferDetailsChangedEvent event) {
		// Fix up unread counts
		Log.i("irc", "BufferDetailsChangedEvent");
		mBufferListAdapter.notifyDataSetChanged();
	}


	private void updateActionBarForPage() {
		ActionBar aBar = getSupportActionBar();
		if (mDrawerLayout.isDrawerVisible(GravityCompat.START)) {
			// Show the generic actionbar for the app
			aBar.setTitle(R.string.app_name);
			aBar.setSubtitle(null);

		} else {
			// Show the stuff for the page
			int num = mBufferPager.getCurrentItem();
			if (num == 0) {
				aBar.setTitle(R.string.app_name);
				aBar.setSubtitle(null);
			} else {
				IRCBuffer buf = mBuffers.get(num - 1);
				aBar.setTitle(buf.getActionBarTitle());
				aBar.setSubtitle(buf.getActionBarSubtitle());
			}
		}
	}
}
