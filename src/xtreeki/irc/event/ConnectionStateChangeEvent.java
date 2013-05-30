package xtreeki.irc.event;

import xtreeki.irc.Connection;

/**
 * Created with IntelliJ IDEA.
 * User: me
 * Date: 5/24/13
 * Time: 2:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConnectionStateChangeEvent {
	public Connection connection;
	public Connection.State oldState, newState;
}
