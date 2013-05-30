package xtreeki.irc.event;

import xtreeki.irc.Connection;

/**
 * Created with IntelliJ IDEA.
 * User: me
 * Date: 5/24/13
 * Time: 2:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConnectionConfigUpdatedEvent {
	public Connection connection;

	public ConnectionConfigUpdatedEvent(Connection conn) {
		connection = conn;
	}
}
