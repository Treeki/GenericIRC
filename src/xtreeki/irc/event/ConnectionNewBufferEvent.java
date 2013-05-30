package xtreeki.irc.event;

import xtreeki.irc.Connection;
import xtreeki.irc.IRCBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: me
 * Date: 5/24/13
 * Time: 1:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConnectionNewBufferEvent {
	public Connection connection;
	public IRCBuffer buffer;
}
