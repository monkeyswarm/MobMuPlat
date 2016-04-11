package com.iglesiaintermedia.mobmuplatandroidwear;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MMPMenu extends MMPControl {

    private WearableListView _listView;
    //private ArrayAdapter<String> _adapter;
    private MenuAdapter _adapter;
    private Handler mHandler = new Handler();
    List<String> _stringList;


    public MMPMenu(Context context, float screenRatio) {
        super(context, screenRatio);
        //
        /*this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(2*screenRatio);
        this.paint.setColor(this.color);*/
        //
        _stringList = new ArrayList<String>();//_menu.stringList;
        /*_stringList.add("fooasdflkjasdk");
        _stringList.add("bar/as/asdfasdfasd/asfas/asdfsadfasd/");
        _stringList.add("foo");
        _stringList.add("bar");
        _stringList.add("foo");
        _stringList.add("bar");
        _stringList.add("foo");
        _stringList.add("bar");*/

        _listView = new WearableListView(context);
        _listView.setGreedyTouchMode(true);
        //_testView = new View(context);
        //_testView.setBackgroundColor(0xFFFFFFFF);

        /*_adapter = new WearableListView.Adapter(context, R.layout.centered_text, stringList) {
            @Override
            public View getView(int position, View convertView,
                                ViewGroup parent) {
                View view =super.getView(position, convertView, parent);
                TextView textView=(TextView) view.findViewById(android.R.id.text1);
                textView.setTextColor(color);
                //textView.setHighlightColor(MenuActivity.this.highlightColor);//doesn't work...
                return view;
            }
        };*/
        _adapter = new MenuAdapter(context, _stringList);
        _listView.setAdapter(_adapter);

        _listView.setClickListener(new WearableListView.ClickListener() {
            public void onClick(WearableListView.ViewHolder viewHolder) {
                int index = viewHolder.getAdapterPosition();
                final View view = viewHolder.itemView;
                //Log.i("MENU", "tap "+index);
                sendValue(index);
                view.setBackgroundColor(highlightColor & 0x55FFFFFF);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        view.setBackgroundColor(0x00000000);
                    }
                }, 200);

                //_menu.didSelect(position);
                //getActivity().getSupportFragmentManager().popBackStack();
            }
            public void onTopEmptyRegionClick(){}
        });

        _listView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        addView(_listView);//, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    /*public void refresh() {
        _adapter.notifyDataSetChanged();
    }*/
    /*protected void onDraw(Canvas canvas) {

        //border
        //this.paint.setStyle(Paint.Style.FILL);
        //canvas.drawRect(_myRect,paint);

        //lines
    }*/

    public void setColor(int color) {
        super.setColor(color);
        _adapter.color = color;
    }

    public void setHighlightColor(int highlightColor) {
        super.setHighlightColor(highlightColor);
        _adapter.highlightColor = highlightColor;
    }

    private void sendValue(int index) {
        if (index<0 || index >= _stringList.size() ) return;
        List<Object> args = new ArrayList<Object>();
        args.add(this.address);
        args.add(Integer.valueOf(index));
        args.add(_stringList.get(index));
        this.controlDelegate.sendGUIMessageArray(args);
    }

    public void receiveList(List<Object> messageArray){
        super.receiveList(messageArray);
        //ignore enable message
        if (messageArray.size()>=2 &&
                (messageArray.get(0) instanceof String) &&
                messageArray.get(0).equals("enable") &&
                (messageArray.get(1) instanceof Float)) {
            return;
        }

        List<String> newDataArray = new ArrayList<String>();

        //put all elements in list into a string array

        for(Object ob: messageArray){
            if(ob instanceof String) newDataArray.add((String)ob);
            else if(ob instanceof Float) { // format
                float val = ((Float)ob).floatValue();
                if (val % 1 == 0) { //round number, display as integer
                    newDataArray.add(String.format("%d", (int)val));
                } else {
                    newDataArray.add(String.format("%.3f", val));
                }
            }
            else if(ob instanceof Integer) newDataArray.add(String.format("%d",((Integer)(ob)).intValue()) );
        }

        _stringList = newDataArray;
        _adapter.setItems(_stringList);
        _adapter.notifyDataSetChanged();
    }

    private class MenuAdapter extends WearableListView.Adapter {

        public int color;
        public int highlightColor;
        private final Context context;
        private List<String> items;

        public MenuAdapter(Context context, List<String> items) {
            this.context = context;
            this.items = items;
        }


        public void setItems(List<String> items) {
            this.items = items;
        }
        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new WearableListView.ViewHolder(new SettingsItemView(context));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder viewHolder, final int position) {
            SettingsItemView SettingsItemView = (SettingsItemView) viewHolder.itemView;
            final String item = items.get(position);

            TextView textView = (TextView) SettingsItemView.findViewById(R.id.text);
            textView.setText(item);
            textView.setTextColor(color);

            //final ImageView imageView = (ImageView) SettingsItemView.findViewById(R.id.image);
            //imageView.setImageResource(item.iconRes);

        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    public final class SettingsItemView extends FrameLayout implements WearableListView.OnCenterProximityListener {

        //final ImageView image;
        final TextView textView;

        public SettingsItemView(Context context) {
            super(context);
            View.inflate(context, R.layout.wearablelistview_item, this);
            //image = (ImageView) findViewById(R.id.image);
            textView = (TextView) findViewById(R.id.text);
            textView.setSingleLine(true);

        }


        @Override
        public void onCenterPosition(boolean b) {

            //Animation example to be ran when the view becomes the centered one
            //image.animate().scaleX(1f).scaleY(1f).alpha(1);
            textView.animate().scaleX(1f).scaleY(1f).alpha(1);
            //textView.animate().alpha(1);

        }

        @Override
        public void onNonCenterPosition(boolean b) {

            //Animation example to be ran when the view is not the centered one anymore
            //image.animate().scaleX(0.8f).scaleY(0.8f).alpha(0.6f);
            textView.animate().scaleX(0.9f).scaleY(0.9f).alpha(0.8f);
            //textView.animate().alpha(0.8f);

        }
    }
}
