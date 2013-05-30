package xtreeki.irc;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created with IntelliJ IDEA.
 * User: me
 * Date: 5/19/13
 * Time: 11:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class NetworkConfig {
	public NetworkConfig() { }
	public NetworkConfig(NetworkConfig other) {
		databaseID = other.databaseID;

		networkName = other.networkName;

		useGlobalUserDetails = other.useGlobalUserDetails;

		nick = other.nick;
		altNick = other.altNick;
		userName = other.userName;
		realName = other.realName;

		hostname = other.hostname;
		port = other.port;
		useSSL = other.useSSL;
		trustAllCertificates = other.trustAllCertificates;
		serverPassword = other.serverPassword;
	}

	public long databaseID = -1;

	public String networkName = "New Network";

	public boolean useGlobalUserDetails = true;
	public String nick = "ircUser";
	public String altNick = "ircUser__";
	public String userName = "ircUser", realName = "IRC User";

	public String hostname;
	public int port = 6667;
	public boolean useSSL = false, trustAllCertificates = false;
	public String serverPassword = null;


	public void writeToDatabase(SQLiteDatabase db) {
		ContentValues cv = new ContentValues();
		if (databaseID != -1)
			cv.put("id", databaseID);
		else
			cv.putNull("id");
		cv.put("networkName", networkName);
		cv.put("useGlobalUserDetails", useGlobalUserDetails ? 1 : 0);
		cv.put("nick", nick);
		cv.put("altNick", altNick);
		cv.put("username", userName);
		cv.put("realName", realName);
		cv.put("hostname", hostname);
		cv.put("port", port);
		cv.put("useSSL", useSSL ? 1 : 0);
		cv.put("trustAllCertificates", trustAllCertificates ? 1 : 0);
		cv.put("serverPassword", serverPassword);
		long id = db.insertWithOnConflict("servers", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
		databaseID = id;
	}
}
