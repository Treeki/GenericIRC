package xtreeki.irc.event;

import xtreeki.irc.IRCBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: me
 * Date: 5/25/13
 * Time: 11:44 PM
 * To change this template use File | Settings | File Templates.
 */
// This event is posted when a buffer's title or unread count changes
public class BufferDetailsChangedEvent {
	public IRCBuffer buffer;

	public BufferDetailsChangedEvent(IRCBuffer buffer) {
		this.buffer = buffer;
	}
}
