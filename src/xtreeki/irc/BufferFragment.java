package xtreeki.irc;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import de.greenrobot.event.EventBus;
import xtreeki.irc.event.BufferNewMessageEvent;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: me
 * Date: 5/19/13
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class BufferFragment extends Fragment {
	public BufferFragment(IRCBuffer buffer) {
		mBuffer = buffer;
	}

	IRCBuffer mBuffer;
	ArrayList<String> mMessages = new ArrayList<String>();
	MessagesAdapter mMessagesAdapter = new MessagesAdapter();

	ListView mMessagesList;
	EditText mTextInput;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mMessagesList = (ListView)getView().findViewById(R.id.messagesList);
		mMessagesList.setDivider(null);
		mMessagesList.setDividerHeight(0);
		mMessagesList.setAdapter(mMessagesAdapter);

		mTextInput = (EditText)getView().findViewById(R.id.textInput);
		mTextInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
				if (i == EditorInfo.IME_ACTION_SEND) {
					mBuffer.writeText(textView.getText());
					textView.setText("");
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		EventBus.getDefault().register(this);
		mBuffer.getMessages(mMessages);
	}

	@Override
	public void onDestroy() {
		EventBus.getDefault().unregister(this);

		super.onDestroy();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.buffer_fragment, container, false);
	}


	public void onEventMainThread(BufferNewMessageEvent event) {
		if (event.buffer == mBuffer) {
			mMessages.add(event.message);
			mMessagesAdapter.notifyDataSetChanged();
		}
	}

	public class MessagesAdapter extends BaseAdapter implements ListAdapter {
		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			if (view != null && view instanceof TextView) {
				TextView tv = (TextView)view;
				tv.setText(mMessages.get(i));
				return tv;
			} else {
				TextView tv = new TextView(viewGroup.getContext());
				tv.setText(mMessages.get(i));
				return tv;
			}
		}

		@Override
		public Object getItem(int i) {
			return mMessages.get(i);
		}

		@Override
		public int getCount() {
			return mMessages.size();
		}

		@Override
		public long getItemId(int i) {
			return i;
		}
	}

}
