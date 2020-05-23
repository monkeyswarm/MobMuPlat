package com.iglesiaintermedia.mobmuplat;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class HIDFragment extends Fragment{
	private static final String TAG = "HID";
	private SparseArray<InputDeviceState> mInputDeviceStates;
    private ListView mSummaryList;
    private SummaryAdapter mSummaryAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_hid, container,
				false);
		
		mInputDeviceStates = new SparseArray<InputDeviceState>();
        mSummaryAdapter = new SummaryAdapter(getResources());
        
		mSummaryList = (ListView) rootView.findViewById(R.id.summary);
        mSummaryList.setAdapter(mSummaryAdapter);
        mSummaryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSummaryAdapter.onItemClick(position);
            }
        });
        
		return rootView;
	}
	
	public void show(InputDeviceState state) {
		mSummaryAdapter.show(state);
	}
	

    /**
     * A list adapter that displays a summary of the device state.
     */
    private static class SummaryAdapter extends BaseAdapter {
        private static final int BASE_ID_HEADING = 1 << 10;
        private static final int BASE_ID_DEVICE_ITEM = 2 << 10;
        private static final int BASE_ID_AXIS_ITEM = 3 << 10;
        private static final int BASE_ID_KEY_ITEM = 4 << 10;

        //private final Context mContext;
        private final Resources mResources;

        private final SparseArray<Item> mDataItems = new SparseArray<Item>();
        private final ArrayList<Item> mVisibleItems = new ArrayList<Item>();

        private final Heading mDeviceHeading;
        private final TextColumn mDeviceNameTextColumn;

        private final Heading mAxesHeading;
        private final Heading mKeysHeading;

        //private InputDeviceState mState;

        public SummaryAdapter(Resources resources) {
            //mContext = context;
            mResources = resources;

            mDeviceHeading = new Heading(BASE_ID_HEADING | 0,
                    mResources.getString(R.string.game_controller_input_heading_device));
            mDeviceNameTextColumn = new TextColumn(BASE_ID_DEVICE_ITEM | 0,
                    mResources.getString(R.string.game_controller_input_label_device_name));

            mAxesHeading = new Heading(BASE_ID_HEADING | 1,
                    mResources.getString(R.string.game_controller_input_heading_axes));
            mKeysHeading = new Heading(BASE_ID_HEADING | 2,
                    mResources.getString(R.string.game_controller_input_heading_keys));
        }

        public void onItemClick(int position) {
            /*if (mState != null) {
                Toast toast = Toast.makeText(
                        mContext, mState.getDevice().toString(), Toast.LENGTH_LONG);
                toast.show();
            }*/
        }

        public void show(InputDeviceState state) {
            //mState = state;
            mVisibleItems.clear();

            // Populate device information.
            mVisibleItems.add(mDeviceHeading);
            mDeviceNameTextColumn.setContent(state.getDevice().getName());
            mVisibleItems.add(mDeviceNameTextColumn);

            // Populate axes.
            mVisibleItems.add(mAxesHeading);
            final int axisCount = state.getAxisCount();
            for (int i = 0; i < axisCount; i++) {
                final int axis = state.getAxis(i);
                final int id = BASE_ID_AXIS_ITEM | axis;
                TextColumn column = (TextColumn) mDataItems.get(id);
                if (column == null) {
                    column = new TextColumn(id, MotionEvent.axisToString(axis));
                    mDataItems.put(id, column);
                }
                column.setContent(Float.toString(state.getAxisValue(i)));
                mVisibleItems.add(column);
            }

            // Populate keys.
            mVisibleItems.add(mKeysHeading);
            final int keyCount = state.getKeyCount();
            for (int i = 0; i < keyCount; i++) {
                final int keyCode = state.getKeyCode(i);
                final int id = BASE_ID_KEY_ITEM | keyCode;
                TextColumn column = (TextColumn) mDataItems.get(id);
                if (column == null) {
                    column = new TextColumn(id, KeyEvent.keyCodeToString(keyCode));
                    mDataItems.put(id, column);
                }
                column.setContent(mResources.getString(state.isKeyPressed(i)
                        ? R.string.game_controller_input_key_pressed
                        : R.string.game_controller_input_key_released));
                mVisibleItems.add(column);
            }

            notifyDataSetChanged();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public int getCount() {
            return mVisibleItems.size();
        }

        @Override
        public Item getItem(int position) {
            return mVisibleItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getItemId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getItem(position).getView(convertView, parent);
        }

        private static abstract class Item {
            private final int mItemId;
            private final int mLayoutResourceId;
            private View mView;

            public Item(int itemId, int layoutResourceId) {
                mItemId = itemId;
                mLayoutResourceId = layoutResourceId;
            }

            public long getItemId() {
                return mItemId;
            }

            public View getView(View convertView, ViewGroup parent) {
                if (mView == null) {
                    LayoutInflater inflater = (LayoutInflater)
                            parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    mView = inflater.inflate(mLayoutResourceId, parent, false);
                    initView(mView);
                }
                updateView(mView);
                return mView;
            }

            protected void initView(View view) {
            }

            protected void updateView(View view) {
            }
        }

        private static class Heading extends Item {
            private final String mLabel;

            public Heading(int itemId, String label) {
                super(itemId, R.layout.game_controller_input_heading);
                mLabel = label;
            }

            @Override
            public void initView(View view) {
                TextView textView = (TextView) view;
                textView.setText(mLabel);
            }
        }

        private static class TextColumn extends Item {
            private final String mLabel;

            private String mContent;
            private TextView mContentView;

            public TextColumn(int itemId, String label) {
                super(itemId, R.layout.game_controller_input_text_column);
                mLabel = label;
            }

            public void setContent(String content) {
                mContent = content;
            }

            @Override
            public void initView(View view) {
                TextView textView = (TextView) view.findViewById(R.id.label);
                textView.setText(mLabel);

                mContentView = (TextView) view.findViewById(R.id.content);
            }

            @Override
            public void updateView(View view) {
                mContentView.setText(mContent);
            }
        }
    }
}
