package xtreeki.irc;

import android.util.Log;
import de.greenrobot.event.EventBus;
import xtreeki.irc.event.ConnectionNewBufferEvent;
import xtreeki.irc.event.ConnectionStateChangeEvent;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created with IntelliJ IDEA.
 * User: me
 * Date: 5/17/13
 * Time: 7:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class Connection implements Runnable {
	public enum State {
		DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING
	}
	private State mState = State.DISCONNECTED;
	public State getState() { return mState; }
	protected synchronized void setState(State newState) {
		ConnectionStateChangeEvent event = new ConnectionStateChangeEvent();
		event.connection = this;
		event.oldState = mState;
		event.newState = newState;

		mState = newState;
		EventBus.getDefault().post(event);

		if (newState == State.DISCONNECTED) {
			if (mSocket != null)
				try {
					mSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			mSocket = null;
			mBufWriter = null;
		}
	}

	private Socket mSocket;
	private BufferedWriter mBufWriter;
	private LinkedBlockingQueue<String> mWriteQueue =
			new LinkedBlockingQueue<String>();

	private String mMyNick;
	public String getNick() { return mMyNick; }

	private NetworkConfig mConfig;
	public NetworkConfig getConfig() { return mConfig; }

	private IRCStatusBuffer mStatusBuffer;
	public IRCStatusBuffer getStatusBuffer() { return mStatusBuffer; }

	private Map<String, IRCChannel> mChannels =
			new HashMap<String, IRCChannel>();

	public Connection(NetworkConfig config) {
		this.mConfig = new NetworkConfig(config);
		this.mStatusBuffer = new IRCStatusBuffer(this);
	}

	private class WriterThread implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					String line = mWriteQueue.take();
					mBufWriter.write(line, 0, line.length());
					mBufWriter.write(13);
					mBufWriter.write(10);
					mBufWriter.flush();
				} catch (IOException e) {
					Log.e("irc", "Exception while writing:");
					Log.e("irc", e.toString());
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}
	private Thread mMainThread, mWriterThread;
	public void connect() {
		synchronized (this) {
			if (mState == State.DISCONNECTED) {
				setState(State.CONNECTING);
				mMainThread = new Thread(this);
				mMainThread.start();
			}
		}
	}

	@Override
	public void run() {
		mStatusBuffer.pushMessage("Connecting...");

		try {
			if (mConfig.useSSL) {
				mStatusBuffer.pushMessage("Using SSL.");

				SocketFactory factory;

				if (mConfig.trustAllCertificates) {
					SSLContext ctx = SSLContext.getInstance("TLSv1");
					ctx.init(null, new TrustManager[] {
							new X509TrustManager() {
								@Override
								public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
									// NOTHING!
								}

								@Override
								public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
									// NOTHING!
								}

								@Override
								public X509Certificate[] getAcceptedIssuers() {
									return new X509Certificate[0];
								}
							}
					}, null);
					factory = ctx.getSocketFactory();
				} else {
					factory = SSLSocketFactory.getDefault();
				}
				SSLSocket ssl = (SSLSocket)factory.createSocket(mConfig.hostname, mConfig.port);
				ssl.startHandshake();
				mSocket = ssl;
			} else {
				mSocket = SocketFactory.getDefault().createSocket(mConfig.hostname, mConfig.port);
			}
		} catch (NoSuchAlgorithmException e) {
			mStatusBuffer.pushMessage(e.toString());
			setState(State.DISCONNECTED);
			return;
		} catch (KeyManagementException e) {
			mStatusBuffer.pushMessage(e.toString());
			setState(State.DISCONNECTED);
			return;
		} catch (IOException e) {
			mStatusBuffer.pushMessage(e.toString());
			setState(State.DISCONNECTED);
			return;
		}
		setState(State.CONNECTED);
		mStatusBuffer.pushMessage("Connected.");

		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(mSocket.getInputStream(), "US-ASCII"));
			mBufWriter = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream(), "US-ASCII"));
		} catch (IOException e) {
			mStatusBuffer.pushMessage(e.toString());
			setState(State.DISCONNECTED);
			return;
		}

		// Create the output thread
		mWriteQueue.clear();

		mWriterThread = new Thread(new WriterThread());
		mWriterThread.start();

		// Start logging in
		mMyNick = mConfig.nick;
		if (mConfig.serverPassword != null)
			writeLine("PASS "+mConfig.serverPassword);
		writeLine("NICK "+mConfig.nick);
		writeLine("USER "+mConfig.userName+" 0 * :"+mConfig.realName);

		while (true) {
			String line = null;
			try {
				line = reader.readLine();
			} catch (IOException e) {
				Log.e("irc", "readline failed:");
				Log.e("irc", e.toString());
				break;
			}
			if (line == null) {
				Log.e("irc", "Line returned is null");
				break;
			}

			if (line.isEmpty())
				continue;

			lineReceived(line);
		}

		for (IRCChannel chan : mChannels.values()) {
			chan.disconnected();
		}

		setState(State.DISCONNECTED);
		mStatusBuffer.pushMessage("Disconnected.");
	}


	protected void lineReceived(String line) {
		String targetNick = null, targetUser = null, targetHost = null;
		if (line.charAt(0) == ':') {
			// Strip off the prefix for now...
			int pfxEnd = line.indexOf(' ');

			String prefix = line.substring(1, pfxEnd);
			line = line.substring(pfxEnd + 1);
			if (line.isEmpty()) // wat?
				return;

			// Is it a user?
			int userStart = prefix.indexOf('!');
			int hostStart = prefix.indexOf('@');
			if (userStart >= 0 && hostStart >= 0 && hostStart > userStart) {
				targetNick = prefix.substring(0, userStart);
				targetUser = prefix.substring(userStart+1, hostStart);
				targetHost = prefix.substring(hostStart+1);
			}
		}

		// Parse.. everything else
		String cmd = null;
		List<String> bits = new ArrayList<String>();

		int curPos = 0;
		while (curPos < line.length()) {
			char curChar = line.charAt(curPos);
			if (curChar == ' ') {
				// Ignore whitespace at the connect of each param
				curPos++;
				continue;
			}

			if (curChar == ':' && cmd != null) {
				// This is the trailing bit
				bits.add(line.substring(curPos + 1));
				break;
			}

			// Find the end of this param
			int endWhere = line.indexOf(' ', curPos);
			if (endWhere == -1)
				endWhere = line.length();
			String bit = line.substring(curPos, endWhere);

			if (cmd == null)
				cmd = bit;
			else
				bits.add(bit);

			// Start right after this one
			curPos = endWhere + 1;
		}

		// Let's see.
		if (cmd.equals("PING")) {
			writeLine("PONG :" + bits.get(0));
			return;

		} else if (cmd.equals("JOIN")) {
			if (targetNick.equals(mMyNick)) {
				// yay, we're in a channel
				Log.i("irc", "Entering channel!");
				channelEntered(bits.get(0));
				return;
			} else {
				IRCChannel chan = findChannel(bits.get(0));
				if (chan != null) {
					chan.userHasJoined(targetNick);
					return;
				}
			}
		} else if (cmd.equals("PART")) {
			IRCChannel chan = findChannel(bits.get(0));
			String partMsg = null;
			if (bits.size() > 1)
				partMsg = bits.get(1);

			if (chan != null) {
				if (targetNick.equals(mMyNick))
					chan.iHaveParted(partMsg);
				else
					chan.userParted(targetNick, partMsg);
				return;
			}
		} else if (cmd.equals("QUIT")) {
			String quitMsg = null;
			if (!bits.isEmpty())
				quitMsg = bits.get(0);

			boolean isMe = targetNick.equals(mMyNick);

			for (IRCChannel chan : mChannels.values()) {
				if (isMe)
					chan.iHaveQuit(quitMsg);
				else
					chan.userHasQuit(targetNick, quitMsg);
			}
			return;
		} else if (cmd.equals("KICK")) {
			IRCChannel chan = findChannel(bits.get(0));
			String kickMsg = null;
			if (bits.size() > 2)
				kickMsg = bits.get(2);

			String kicked = bits.get(1);

			if (chan != null) {
				if (kicked.equals(mMyNick))
					chan.iWasKicked(targetNick, kickMsg);
				else
					chan.userWasKicked(kicked, targetNick, kickMsg);
				return;
			}

		} else if (cmd.equals("NICK")) {
			boolean isMe = targetNick.equals(mMyNick);
			String newNick = bits.get(0);
			if (isMe)
				mMyNick = newNick;

			for (IRCChannel chan : mChannels.values())
				chan.userChangedNick(targetNick, newNick, isMe);
			return;

		} else if (cmd.equals("PRIVMSG")) {
			String dest = bits.get(0);
			String msg = bits.get(1);

			IRCChannel chan = findChannel(dest);

			if (chan != null) {
				chan.pushMessage("[" + targetNick + "] " + msg, true);
				return;
			}

		} else if (cmd.equals("001")) {
			mMyNick = bits.get(0);
			Log.i("irc", "Setting mMyNick to: '" + mMyNick + "'");
			return;
		} else if (cmd.equals("353")) {
			// RPL_NAMREPLY
			String chanName = bits.get(bits.size() - 2);
			String theList = bits.get(bits.size() - 1);

			IRCChannel chan = findChannel(chanName);
			if (chan != null) {
				chan.registerInitialUserList(theList);
				return;
			}
		}

		mStatusBuffer.pushMessage("[Unhandled] " + line);
	}


	public IRCChannel findChannel(String chanName) {
		String normalised = normaliseChannelName(chanName);
		return mChannels.get(normalised);
	}

	public static String normaliseChannelName(String chanName) {
		// rfc says {}| are the lowercase equivalents of []\. does this
		// matter for channel names? I dunno.
		return chanName.toLowerCase();
	}

	private void channelEntered(String chanName) {
		String normalised = normaliseChannelName(chanName);
		if (!mChannels.containsKey(normalised)) {
			IRCChannel chan = new IRCChannel(this, chanName);
			mChannels.put(normalised, chan);

			ConnectionNewBufferEvent event = new ConnectionNewBufferEvent();
			event.connection = this;
			event.buffer = chan;
			EventBus.getDefault().post(event);
		}

		findChannel(chanName).makeActive();
	}


	public void writeLine(String line) {
		mWriteQueue.offer(line);
	}
	public synchronized void disconnect() {
		if (mState == State.CONNECTING || mState == State.CONNECTED) {
			final boolean wasStillConnecting = (mState == State.CONNECTING);
			setState(State.DISCONNECTING);

			// Goodbye.
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						if (!wasStillConnecting) {
							mBufWriter.write("QUIT :Disconnecting.");
							mBufWriter.flush();
						}
						mSocket.close();
					} catch (IOException e) {
						// aaa.
					}
				}
			}).start();
		}
	}
}
