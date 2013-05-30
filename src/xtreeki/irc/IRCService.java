package xtreeki.irc;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import de.greenrobot.event.EventBus;
import xtreeki.irc.event.ConnectionNewBufferEvent;
import xtreeki.irc.event.ConnectionStateChangeEvent;
import xtreeki.irc.event.NewConnectionEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: me
 * Date: 5/16/13
 * Time: 6:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class IRCService extends Service {
	SQLiteOpenHelper mDBHelper;
	public SQLiteDatabase getDatabase() { return mDatabase; }
	SQLiteDatabase mDatabase;

	private class DatabaseLoader implements Runnable {
		@Override
		public void run() {
			mDatabase = mDBHelper.getWritableDatabase();

			Cursor c = mDatabase.query("servers", null, null, null, null, null, null);
			// This is so ugly
			int idID = c.getColumnIndex("id");
			int networkNameID = c.getColumnIndex("networkName");
			int useGlobalUserDetailsID = c.getColumnIndex("useGlobalUserDetails");
			int nickID = c.getColumnIndex("nick");
			int altNickID = c.getColumnIndex("altNick");
			int usernameID = c.getColumnIndex("username");
			int realNameID = c.getColumnIndex("realName");
			int hostnameID = c.getColumnIndex("hostname");
			int portID = c.getColumnIndex("port");
			int useSSLID = c.getColumnIndex("useSSL");
			int trustAllCertificatesID = c.getColumnIndex("trustAllCertificates");
			int serverPasswordID = c.getColumnIndex("serverPassword");

			if (c.moveToFirst()) {
				while (true) {
					NetworkConfig conf = new NetworkConfig();
					conf.databaseID = c.getLong(idID);
					conf.networkName = c.getString(networkNameID);
					conf.useGlobalUserDetails = (c.getInt(useGlobalUserDetailsID) == 1);
					conf.nick = c.getString(nickID);
					conf.altNick = c.getString(altNickID);
					conf.userName = c.getString(usernameID);
					conf.realName = c.getString(realNameID);
					conf.hostname = c.getString(hostnameID);
					conf.port = c.getInt(portID);
					conf.useSSL = (c.getInt(useSSLID) == 1);
					conf.trustAllCertificates = (c.getInt(trustAllCertificatesID) == 1);
					conf.serverPassword = c.getString(serverPasswordID);

					createNewConnection(conf);

					if (!c.moveToNext())
						break;
				}
			}

			c.close();
		}
	}
    private NotificationManager mNM;
	private MainActivity mActivity;
	private boolean mInForeground = false;

	public void setOwnedActivity(MainActivity a) {
		mActivity = a;
	}

	public class LocalBinder extends Binder {
        IRCService getService() {
            return IRCService.this;
        }
    }
    private final IBinder mBinder = new LocalBinder();
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
	    EventBus.getDefault().register(this);

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	    mDBHelper = new ServerDBHelper(getApplicationContext());
	    new Thread(new DatabaseLoader()).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
	    for (Connection conn : mConnections) {
		    conn.disconnect();
	    }
	    mConnections.clear();
	    stopForeground(true);

	    EventBus.getDefault().unregister(this);
    }

    private void enableForeground() {
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);
        Notification n = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Running.")
		        .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(intent)
                .setOngoing(true)
                .setDefaults(0)
                .build();

	    startForeground(1, n);
	    mInForeground = true;
    }
	private void disableForeground() {
		stopForeground(true);
		mInForeground = false;
	}


	private ArrayList<Connection> mConnections = new ArrayList<Connection>();
	public void getConnections(List<Connection> output) {
		synchronized (this) {
			output.clear();
			output.addAll(mConnections);
		}
	}

	public Connection createNewConnection() {
		return createNewConnection(new NetworkConfig());
	}
	public Connection createNewConnection(NetworkConfig conf) {
		Connection conn = new Connection(conf);
		mConnections.add(conn);

		EventBus.getDefault().post(new NewConnectionEvent(conn));

		return conn;
	}
	public Connection findConnectionByDatabaseID(long id) {
		for (Connection c : mConnections) {
			if (c.getConfig().databaseID == id)
				return c;
		}
		return null;
	}



	// Buffers and stuff.
	private int mNextBufferKey = 0;
	public int assignUniqueBufferKey() {
		synchronized (this) {
			mNextBufferKey++;
			return mNextBufferKey;
		}
	}


	private ArrayList<IRCBuffer> mBuffers = new ArrayList<IRCBuffer>();
	public ArrayList<IRCBuffer> getBuffers() {
		return mBuffers;
	}

	public void onEvent(ConnectionStateChangeEvent event) {
		boolean anyActive = false;
		for (Connection iter : mConnections) {
			if (iter.getState() != Connection.State.DISCONNECTED) {
				anyActive = true;
				break;
			}
		}

		if (anyActive) {
			if (!mInForeground)
				enableForeground();
		} else {
			if (mInForeground)
				disableForeground();
		}
	}

	public void onEvent(ConnectionNewBufferEvent event) {
		event.buffer.uniqueKey = assignUniqueBufferKey();
		mBuffers.add(event.buffer);

		MainActivity ac = mActivity;
		if (ac != null)
			ac.onBufferAdded(event.buffer);
	}
}

