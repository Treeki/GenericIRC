package xtreeki.irc;

import android.support.v4.app.Fragment;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: me
 * Date: 5/18/13
 * Time: 2:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class IRCChannel extends IRCBuffer {
	public static class User {
		public String nick;
		public String mode;
	}
	public IRCChannel(Connection connection, String channelName) {
		super(connection);
		mChannelName = channelName;
	}

	private ArrayList<User> mUsers = new ArrayList<User>();
	void registerInitialUserList(String nicksList) {
		for (String rawNick : nicksList.split(" ")) {
			User user = new User();
			char firstChar = rawNick.charAt(0);
			if ((firstChar >= 'A' && firstChar <= 'Z') || (firstChar >= 'a' && firstChar <= 'z')) {
				user.mode = null;
				user.nick = rawNick;
			} else {
				user.mode = rawNick.substring(0, 1);
				user.nick = rawNick.substring(1);
			}
			mUsers.add(user);
			Log.i("irc", "Added user to channel : " + user.nick + " to " + mChannelName);
		}

		pushMessage("*** " + mUsers.size() + " user" + (mUsers.size() == 1 ? "" : "s") + " in the channel");
	}
	void userHasJoined(String nick) {
		User user = new User();
		user.mode = null;
		user.nick = nick;
		mUsers.add(user);

		pushMessage("*** " + nick + " has joined " + mChannelName);
	}
	void userChangedNick(String oldNick, String newNick, boolean isMe) {
		User u = null;
		for (User check : mUsers) {
			if (check.nick.equals(oldNick)) {
				u = check;
				break;
			}
		}

		if (u != null) {
			u.nick = newNick;
			if (isMe)
				pushMessage("*** You are now known as " + newNick);
			else
				pushMessage("*** " + oldNick + " is now known as " + newNick);
		}
	}

	boolean userWasKicked(String kicked, String kicker, String message) {
		return userHasLeft(kicked, LeaveType.Kick, message, kicker);
	}
	boolean userParted(String nick, String message) {
		return userHasLeft(nick, LeaveType.Part, message, null);
	}
	boolean userHasQuit(String nick, String message) {
		return userHasLeft(nick, LeaveType.Quit, message, null);
	}

	private enum LeaveType {
		Part, Quit, Kick
	}
	private boolean userHasLeft(String nick, LeaveType type, String message, String kicker) {
		User u = null;
		for (User check : mUsers) {
			if (check.nick.equals(nick)) {
				u = check;
				break;
			}
		}

		if (u != null) {
			String msg = null;
			switch (type) {
				case Quit:
					msg = nick + " has quit IRC";
					break;
				case Part:
					msg = nick + " has parted " + mChannelName;
					break;
				case Kick:
					msg = nick + " was kicked by " + kicker;
					break;
			}
			if (message != null)
				msg = msg + " (" + message + ")";
			pushMessage("*** " + msg);
			mUsers.remove(u);
			return true;
		}

		return false;
	}

	private String mChannelName;
	public String getChannelName() {
		return mChannelName;
	}

	private String mTopic;
	public void setTopic(String topic) {
		mTopic = topic;
		pushMessage("*** New topic: " + topic);
	}

	@Override
	public Fragment createFragment() {
		return new BufferFragment(this);
	}

	@Override
	public String getActionBarTitle() {
		return mChannelName;
	}

	@Override
	public String getActionBarSubtitle() {
		return mTopic;
	}

	@Override
	public String getTitle() {
		return mChannelName;
	}

	@Override
	public void writeText(CharSequence text) {
		mConnection.writeLine("PRIVMSG " + mChannelName + " :" + text);
		pushMessage("[" + mConnection.getNick() + "] " + text);
	}
}
