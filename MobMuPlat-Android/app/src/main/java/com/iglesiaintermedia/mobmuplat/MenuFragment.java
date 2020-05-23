package com.iglesiaintermedia.mobmuplat;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.iglesiaintermedia.mobmuplat.controls.MMPMenu;

import java.util.List;

public class MenuFragment extends Fragment {

    private MMPMenu _menu;
    private int _bgColor;
    private ListView _listView;
    private ArrayAdapter<String> _adapter;

    //TODO clean this up so it has an empty constructor so it can be re-instantiated.
    /*public MenuFragment(MMPMenu menu, int bgColor) {
        super();
        _menu = menu;
        _bgColor = bgColor;
    }*/

    public void setMenuAndColor(MMPMenu menu, int bgColor) {
        _menu = menu;
        _bgColor = bgColor;
    }

    public MMPMenu getMenu() {
        return _menu;
    }

    public void refresh() {
        _adapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_menu, container,
                false);

        final int color = _menu.color;
        //highlightColor = intent.getIntExtra("highlightColor", Color.RED);
        List<String> stringList = _menu.stringList;

        _listView = (ListView)rootView.findViewById(R.id.listView1);
        FrameLayout frameLayout = (FrameLayout)rootView.findViewById(R.id.container);
        frameLayout.setBackgroundColor(_bgColor);

        _adapter = new ArrayAdapter<String>(getActivity(), R.layout.centered_text, stringList) {
            @Override
            public View getView(int position, View convertView,
                                ViewGroup parent) {
                View view =super.getView(position, convertView, parent);
                TextView textView=(TextView) view.findViewById(android.R.id.text1);
                textView.setTextColor(color);
                //textView.setHighlightColor(MenuActivity.this.highlightColor);//doesn't work...
                return view;
            }
        };
        _listView.setAdapter(_adapter);

        _listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parentAdapter, View view, int position,long id) {
                //TextView clickedView = (TextView) view;
                //Toast.makeText(MenuActivity.this, "Item with id ["+id+"] - Position ["+position+"] - ["+clickedView.getText()+"]", Toast.LENGTH_SHORT).show();
                _menu.didSelect(position);
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return rootView;
    }
}
