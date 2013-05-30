package xtreeki.irc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import de.greenrobot.event.EventBus;
import org.jraf.android.backport.switchwidget.Switch;
import xtreeki.irc.event.ConnectionConfigUpdatedEvent;
import xtreeki.irc.event.ConnectionStateChangeEvent;
import xtreeki.irc.event.NewConnectionEvent;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: me
 * Date: 5/21/13
 * Time: 1:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class HomeFragment extends SherlockFragment {
	LayoutInflater mLayoutInflater;
	IRCService mService;
	public HomeFragment(IRCService service) {
		Log.i("irc", "HomeFragment created with " + service);
		mService = service;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);

		EventBus.getDefault().register(this);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mLayoutInflater = ((LayoutInflater)getSherlockActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE));

		mService.getConnections(mServers);

		ListView serversList = (ListView)(getView().findViewById(R.id.serversList));
		serversList.setAdapter(mServersAdapter);
		serversList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				Connection conn = mServers.get(i);
				Intent intent = new Intent(getSherlockActivity(), NetworkEditorActivity.class);
				intent.putExtra(NetworkEditorActivity.EXTRA_SERVER_ID, conn.getConfig().databaseID);
				startActivity(intent);
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.irc_home, container, false);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.irc_home, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_add_network:
				Intent intent = new Intent(getSherlockActivity(), NetworkEditorActivity.class);
				intent.putExtra(NetworkEditorActivity.EXTRA_SERVER_ID, -1);
				startActivity(intent);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDestroyView() {
		mLayoutInflater = null;
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		EventBus.getDefault().unregister(this);
		super.onDestroy();    //To change body of overridden methods use File | Settings | File Templates.
	}

	private ArrayList<Connection> mServers = new ArrayList<Connection>();
	private ServersAdapter mServersAdapter = new ServersAdapter();

	public void onEventMainThread(NewConnectionEvent event) {
		mServers.add(event.connection);
		mServersAdapter.notifyDataSetChanged();
	}
	public void onEventMainThread(ConnectionConfigUpdatedEvent event) {
		mServersAdapter.notifyDataSetChanged();
	}
	public void onEventMainThread(ConnectionStateChangeEvent event) {
		mServersAdapter.notifyDataSetChanged();
	}

	private static class ViewHolder {
		public TextView nameView, statusView;
		public Switch connSwitch;
	}
	class ServersAdapter extends BaseAdapter implements ListAdapter, Switch.OnCheckedChangeListener {
		@Override
		public int getCount() {
			return mServers.size();
		}

		@Override
		public Object getItem(int i) {
			return null;
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			ViewHolder holder;
			if (view == null) {
				view = mLayoutInflater.inflate(R.layout.server_list_item, viewGroup, false);

				holder = new ViewHolder();
				holder.nameView = ((TextView)view.findViewById(R.id.serverName));
				holder.statusView = ((TextView)view.findViewById(R.id.serverStatus));
				holder.connSwitch = ((Switch)view.findViewById(R.id.toggleConnection));
				view.setTag(holder);
			} else {
				holder = (ViewHolder)view.getTag();
			}

			Connection c = mServers.get(i);
			String desc = "";
			switch (c.getState()) {
				case CONNECTED: desc = "Connected"; break;
				case CONNECTING: desc = "Connecting"; break;
				case DISCONNECTED: desc = "Disconnected"; break;
				case DISCONNECTING: desc = "Disconnecting"; break;
			}
			holder.nameView.setText(c.getConfig().networkName);
			holder.statusView.setText(desc);

			holder.connSwitch.setOnCheckedChangeListener(null);
			holder.connSwitch.setChecked(c.getState() != Connection.State.DISCONNECTED);
			holder.connSwitch.setOnCheckedChangeListener(this);
			holder.connSwitch.setTag(c);

			return view;
		}

		@Override
		public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
			Connection c = (Connection)compoundButton.getTag();
			if (b)
				c.connect();
			else
				c.disconnect();
		}
	}
}
