package com.iglesiaintermedia.mobmuplat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class DocumentsFragment extends Fragment implements OnClickListener{

	private ListView _listView;
	private ArrayList<String> _filenamesList;
	private ArrayAdapter<String> _adapter;
	private boolean _isListFiltered;
	private Button _showAllFilesButton, _infoButton;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_documents, container,
				false);
		_showAllFilesButton = (Button)rootView.findViewById(R.id.button1);
		_showAllFilesButton.setOnClickListener(this);
		
		_infoButton = (Button)rootView.findViewById(R.id.button2);
		_infoButton.setOnClickListener(this);
		
		
		_listView = (ListView)rootView.findViewById(R.id.listView1);
		_filenamesList = new ArrayList<String>();

        _isListFiltered = true;

        SharedPreferences sp = getActivity().getPreferences(Activity.MODE_PRIVATE);
        _isListFiltered = !sp.getBoolean("showAllFiles", false);
		refreshFileList();
        refreshShowFilesButton();
		
	    _adapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_documents, R.id.list_item_textView, _filenamesList) {
	    	@Override
	    	public View getView(int position, View convertView, ViewGroup parent) {
	    		View view = super.getView(position, convertView, parent);
	    		String filename = _filenamesList.get(position);
				view.setEnabled(isOpenable(filename)); //when filtered, should always be true...
				return view;
	    	}
	    };
	    _listView.setAdapter(_adapter);
		
	    _listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parentAdapter, View view, int position,long id) {
				//TextView clickedView = (TextView) view;
				//final ProgressBar pb = (ProgressBar) view.findViewById(R.id.progressBar1);
	    		//pb.setVisibility(View.VISIBLE);
				final String filename = _filenamesList.get(position);
				String suffix = filename.substring(filename.lastIndexOf(".")+1);
				if (suffix.equalsIgnoreCase("mmp")) {
					Toast.makeText(getActivity(), "Loading MobMuPlat document "+filename, Toast.LENGTH_SHORT).show();
					
						/*	Handler handler = new Handler();
	        				handler.post(new Runnable() {
	        					@Override
	        					public void run() {*/
	        						((MainActivity)getActivity()).loadScene(filename);
	        						getActivity().getSupportFragmentManager().popBackStack();// remove(DocumentsFragment.this).commit();
	        						
	        						//	pb.setVisibility(View.INVISIBLE);
	        					/*}
	        				});*/

	        		
					
					
				} else if (suffix.equalsIgnoreCase("pd")) {
					Toast.makeText(getActivity(), "Loading PureData patch "+filename, Toast.LENGTH_SHORT).show();
					((MainActivity)getActivity()).loadScenePatchOnly(filename);
					getActivity().getSupportFragmentManager().popBackStack();// remove(DocumentsFragment.this).commit();
				}
				
			}	
		});
	   //TODO zips!
	    //rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		return rootView;
	}
	
	private boolean isMMP(String filename) {
		String suffix = filename.substring(filename.lastIndexOf(".")+1);
		if (suffix.equalsIgnoreCase("mmp")) return true;// || suffix.equalsIgnoreCase("pd")) return true;
		else return false;
	}
	
	private boolean isOpenable(String filename) {
		String suffix = filename.substring(filename.lastIndexOf(".") + 1);
		if (suffix.equalsIgnoreCase("mmp") || suffix.equalsIgnoreCase("pd")) return true;
		else return false;
	}
	
	private ArrayList<String> getFilenames() {
		String state = Environment.getExternalStorageState();
		Log.i("DOC state", state);
		if (!(Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))) return null;
		
		File fileDir = new File(MainActivity.getDocumentsFolderPath());//device/sdcard

		ArrayList<String> fileList = new ArrayList<String>();
	    File[] files = fileDir.listFiles();
		if (files == null)return null;
	    Arrays.sort(files); //listfiles does not have an order...
	    if (files.length == 0)
	        return null;
	    else {
	        for (int i=0; i<files.length; i++) 
	        	fileList.add(files[i].getName());
	    }

	    return fileList;
	}

	@Override
	public void onClick(View v) {
		if (v == _showAllFilesButton) {
			_isListFiltered = !_isListFiltered;
			_showAllFilesButton.setText(_isListFiltered ? "Show all files" : "Showing all files");
			refreshFileList();
			_adapter.notifyDataSetChanged();
            refreshShowFilesButton();
			// save to preferences
            SharedPreferences settings = getActivity().getPreferences(Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("showAllFiles", !_isListFiltered);
            editor.commit();
		} else if (v==_infoButton) {
			new AlertDialog.Builder(getActivity())
		    .setTitle("Getting files in and out of MobMuPlat")
		    .setMessage("MobMuPlat files are stored in \n"+MainActivity.getDocumentsFolderPath()+"\n(Some file system apps may also show this as /sdcard/MobMuPlat).\nYou can use a file system app to manually copy files in and out of that folder.\n\nMobMuPlat will also automatically import .mmp, .pd, and .zip files, so just tap on those files from any other application (email, web, Dropbox, Google Drive, etc), and MobMuPlat will copy the file(s) into this folder (and extract all files from a .zip).")
		    .setPositiveButton(android.R.string.yes, null)
		    .setIcon(R.drawable.ic_launcher)
		     .show();
		}
	}

    private void refreshShowFilesButton() {
        //Drawable background = getResources().getDrawable(android.R.drawable.btn_default);
		Drawable background = _showAllFilesButton.getBackground();
		background.mutate();
        if (!_isListFiltered) {
            background.setColorFilter(0x55FFFFFF, PorterDuff.Mode.ADD);
        } else {
            background.setColorFilter(0x00000000, PorterDuff.Mode.ADD); //setting to null wasn't working on some devices.
        }
        if ( Build.VERSION.SDK_INT >= 16) {
            _showAllFilesButton.setBackground(background);
        } else {
            // api 14,15
            _showAllFilesButton.setBackgroundDrawable(background);
        }
		// need to always set these for some reason...
        //_infoButton.getBackground().setColorFilter(null);
    }

    private void refreshAutoLoadButton() {
        //DEI
    }
	
	private void refreshFileList() {
		ArrayList<String> files = getFilenames();
		_filenamesList.clear();
		if (files == null) return;
		for (String filename : files) {
			if (_isListFiltered){
				if (isMMP(filename)) _filenamesList.add(filename);
			} else {//add all files
				_filenamesList.add(filename);
			}
		}
	}
}
