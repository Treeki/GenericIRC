package xtreeki.irc;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: me
 * Date: 5/23/13
 * Time: 5:13 AM
 * To change this template use File | Settings | File Templates.
 */
public class ServerDBHelper extends SQLiteOpenHelper {
	public ServerDBHelper(Context c) {
		super(c, "xtreeki.irc", null, 1);
		Log.i("irc", "ServerDBHelper ctor");
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i("irc", "Running onCreate");
		db.execSQL("CREATE TABLE servers (id INTEGER PRIMARY KEY," +
				"networkName TEXT," +
				"useGlobalUserDetails INTEGER," +
				"nick TEXT, altNick TEXT, username TEXT, realName TEXT," +
				"hostname TEXT, port INTEGER, useSSL INTEGER, trustAllCertificates INTEGER," +
				"serverPassword TEXT);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
		Log.i("irc", "Running onUpgrade");
	}

}
