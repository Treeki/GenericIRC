package xtreeki.irc;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import de.greenrobot.event.EventBus;
import org.jraf.android.backport.switchwidget.Switch;
import xtreeki.irc.event.ConnectionConfigUpdatedEvent;

/**
 * Created with IntelliJ IDEA.
 * User: me
 * Date: 5/21/13
 * Time: 3:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class NetworkEditorActivity extends SherlockFragmentActivity {
	public final static String EXTRA_SERVER_ID = "xtreeki.irc.SERVER_ID";

	private Switch mConnectionSwitch;
	private boolean mConnectionSwitchHasBeenSetup = false;
	private void setupConnectionSwitch() {
		// Is a lock and all overkill for this? I.. don't know.
		synchronized (mConnectionSwitch) {
			if (mConnectionSwitchHasBeenSetup)
				return;
			mConnectionSwitchHasBeenSetup = true;

			mConnectionSwitch.setEnabled(true);
			mConnectionSwitch.setChecked(mServer.getState() != Connection.State.DISCONNECTED);
			mConnectionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (b)
						mServer.connect();
					else
						mServer.disconnect();
				}
			});
		}
	}

	private IRCService mBoundService;
	private boolean mIsBound = false;
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			mBoundService = ((IRCService.LocalBinder)iBinder).getService();

			if (mIsNewServer) {
				mServer = mBoundService.createNewConnection();
				mServer.getConfig().writeToDatabase(mBoundService.getDatabase());
			} else {
				mServer = mBoundService.findConnectionByDatabaseID(mServerID);
			}
			NetworkConfig config = mServer.getConfig();

			getSupportFragmentManager().beginTransaction().
					add(R.id.statusTab, mServer.getStatusBuffer().createFragment()).
					commit();

			getSupportActionBar().setTitle(config.networkName);

			if (mConnectionSwitch != null)
				setupConnectionSwitch();

			// Set up all our things
			if (mServer.getState() == Connection.State.DISCONNECTED)
				((TabHost) findViewById(R.id.tabHost)).setCurrentTab(1);

			((EditText)findViewById(R.id.networkName)).setText(config.networkName);

			((RadioGroup)findViewById(R.id.userDetailsMode)).check(config.useGlobalUserDetails ? R.id.globalUserDetails : R.id.localUserDetails);

			((EditText)findViewById(R.id.nickname)).setText(config.nick);
			((EditText)findViewById(R.id.nickname2)).setText(config.altNick);
			((EditText)findViewById(R.id.username)).setText(config.userName);
			((EditText)findViewById(R.id.realName)).setText(config.realName);

			((EditText)findViewById(R.id.address)).setText(config.hostname);
			((EditText)findViewById(R.id.portNumber)).setText(String.valueOf(config.port));

			((CheckBox)findViewById(R.id.useSSL)).setChecked(config.useSSL);
			((CheckBox)findViewById(R.id.trustAllCertificates)).setChecked(config.trustAllCertificates);

			// Deal with the password
			final EditText pwView = (EditText)findViewById(R.id.serverPassword);
			pwView.setEnabled(config.serverPassword != null);
			if (config.serverPassword != null)
				pwView.setText(config.serverPassword);

			CheckBox pwCheck = (CheckBox)findViewById(R.id.useServerPassword);
			pwCheck.setChecked(config.serverPassword != null);
			pwCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					pwView.setEnabled(b);
				}
			});
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBoundService = null;
		}
	};

	void doBindService() {
		bindService(new Intent(this, IRCService.class),
				mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}
	void doUnbindService() {
		if (mIsBound) {
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	private void saveStuff() {
		String networkName = ((EditText)findViewById(R.id.networkName)).getText().toString();
		if (networkName.isEmpty()) {
			Toast.makeText(this, "Enter a network name.", Toast.LENGTH_SHORT).show();
			return;
		}

		NetworkConfig config = mServer.getConfig();
		config.networkName = networkName;
		getSupportActionBar().setTitle(config.networkName);

		config.useGlobalUserDetails =
				((RadioGroup)findViewById(R.id.userDetailsMode)).getCheckedRadioButtonId() == R.id.globalUserDetails;

		config.nick = ((EditText)findViewById(R.id.nickname)).getText().toString();
		config.altNick = ((EditText)findViewById(R.id.nickname2)).getText().toString();
		config.userName = ((EditText)findViewById(R.id.username)).getText().toString();
		config.realName = ((EditText)findViewById(R.id.realName)).getText().toString();

		config.hostname = ((EditText)findViewById(R.id.address)).getText().toString();
		config.port = Integer.parseInt(((EditText) findViewById(R.id.portNumber)).getText().toString());
		config.useSSL = ((CheckBox)findViewById(R.id.useSSL)).isChecked();
		config.trustAllCertificates = ((CheckBox)findViewById(R.id.trustAllCertificates)).isChecked();

		if (((CheckBox)findViewById(R.id.trustAllCertificates)).isChecked())
			config.serverPassword = ((EditText)findViewById(R.id.serverPassword)).getText().toString();
		else
			config.serverPassword = null;

		if (mServer.getState() == Connection.State.DISCONNECTED)
			Toast.makeText(this, "Settings have been saved.", Toast.LENGTH_SHORT).show();
		else
			Toast.makeText(this, "Settings have been saved. They will take effect the next time you connect.", Toast.LENGTH_LONG).show();
		Log.i("irc", "Settings saved");

		config.writeToDatabase(mBoundService.getDatabase());

		EventBus.getDefault().post(new ConnectionConfigUpdatedEvent(mServer));
	}

	private void deleteServer() {
	}


	private boolean mIsNewServer;
	private long mServerID;
	private Connection mServer;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.server_editor);

		mServerID = getIntent().getLongExtra(EXTRA_SERVER_ID, -1);
		mIsNewServer = (mServerID == -1);

		final TabHost tabs = (TabHost)findViewById(R.id.tabHost);
		tabs.setup();
		tabs.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
			@Override
			public void onTabChanged(String s) {
				boolean showingSettings = (tabs.getCurrentTab() == 1);
				if (mSwitchItem != null) {
					mSwitchItem.setVisible(!showingSettings);
					mSaveItem.setVisible(showingSettings);
					mDeleteItem.setVisible(showingSettings);
				}
			}
		});
		TabHost.TabSpec spec;

		spec = tabs.newTabSpec("statusTab")
				.setIndicator("Status")
				.setContent(R.id.statusTab);
		tabs.addTab(spec);

		spec = tabs.newTabSpec("configTab")
				.setIndicator("Settings")
				.setContent(R.id.settingsTab);
		tabs.addTab(spec);

		doBindService();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.server_editor, menu);

		boolean showingSettings = ((TabHost)findViewById(R.id.tabHost)).getCurrentTab() == 1;
		mSwitchItem = menu.findItem(R.id.menu_toggle_connection);
		mSaveItem = menu.findItem(R.id.menu_save);
		mDeleteItem = menu.findItem(R.id.menu_delete);

		mSwitchItem.setVisible(!showingSettings);
		mSaveItem.setVisible(showingSettings);
		mDeleteItem.setVisible(showingSettings);

		mConnectionSwitch = (Switch)mSwitchItem.getActionView();
		mConnectionSwitch.setEnabled(false);
		if (mServer != null)
			setupConnectionSwitch();

		return super.onCreateOptionsMenu(menu);
	}
	MenuItem mSwitchItem, mSaveItem, mDeleteItem;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				//NavUtils.navigateUpFromSameTask(this);
				finish();
				return true;
			case R.id.menu_save:
				saveStuff();
				return true;
			case R.id.menu_delete:
				deleteServer();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}