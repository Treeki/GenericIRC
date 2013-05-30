package xtreeki.irc.event;

import xtreeki.irc.Connection;

/**
 * Created with IntelliJ IDEA.
 * User: me
 * Date: 5/24/13
 * Time: 2:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class NewConnectionEvent {
	public NewConnectionEvent(Connection connection) {
		this.connection = connection;
	}

	public Connection connection;
}
