package xtreeki.irc;

import android.support.v4.app.Fragment;
import android.util.Log;
import de.greenrobot.event.EventBus;
import xtreeki.irc.event.BufferDetailsChangedEvent;
import xtreeki.irc.event.BufferNewMessageEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: me
 * Date: 5/18/13
 * Time: 2:46 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class IRCBuffer {
	protected IRCBuffer(Connection connection) {
		this.mConnection = connection;
	}

	public int uniqueKey;
	public int unreadCount = 0;

	public abstract String getActionBarTitle();
	public String getActionBarSubtitle() { return null; }
	public abstract String getTitle();
	public abstract Fragment createFragment();

	public abstract void writeText(CharSequence text);

	private boolean mVisible;
	public void becomeVisible() {
		boolean postEvent = false;
		synchronized (this) {
			mVisible = true;
			if (unreadCount > 0) {
				unreadCount = 0;
				postEvent = true;
			}
		}

		if (postEvent)
			EventBus.getDefault().post(new BufferDetailsChangedEvent(this));

		Log.i("irc", getTitle() + " has become visible");
	}
	public void becomeInvisible() {
		mVisible = false;
		Log.i("irc", getTitle() + " has become invisible");
	}

	private List<String> mMessages = new ArrayList<String>();
	public void getMessages(List<String> output) {
		synchronized (this) {
			output.clear();
			output.addAll(mMessages);
		}
	}

	protected Connection mConnection;


	public void pushMessage(String msg) {
		pushMessage(msg, false);
	}
	public void pushMessage(String msg, boolean important) {
		synchronized (this) {
			mMessages.add(msg);

			if (important && !mVisible) {
				unreadCount++;
				Log.i("irc", "Unread count of " + getTitle() + " going up to " + unreadCount);
				EventBus.getDefault().post(new BufferDetailsChangedEvent(this));
			}

			BufferNewMessageEvent event = new BufferNewMessageEvent();
			event.buffer = this;
			event.message = msg;
			EventBus.getDefault().post(event);

		}
	}
}
