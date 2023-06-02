package com.iglesiaintermedia.mobmuplat;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;

public class DocumentsFragment extends Fragment implements OnClickListener{

	private RecyclerView _recyclerView;
	private ArrayList<String> _filenamesList;
	private DocumentsRecyclerViewAdapter _adapter;
	private boolean _isListFiltered;
	private Button _showAllFilesButton, _infoButton, _importButton;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_documents, container,
				false);
		_showAllFilesButton = (Button)rootView.findViewById(R.id.button1);
		_showAllFilesButton.setOnClickListener(this);
		
		_infoButton = (Button)rootView.findViewById(R.id.button2);
		_infoButton.setOnClickListener(this);
		_importButton = (Button)rootView.findViewById(R.id.button3);
		_importButton.setOnClickListener(this);


		_recyclerView = (RecyclerView)rootView.findViewById(R.id.recyclerView1);
		_recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 1));

		_filenamesList = new ArrayList<String>();

        _isListFiltered = true;

        SharedPreferences sp = getActivity().getPreferences(Activity.MODE_PRIVATE);
        _isListFiltered = !sp.getBoolean("showAllFiles", false);

	    _adapter = new DocumentsRecyclerViewAdapter(getActivity(), _filenamesList);
		_adapter.setClickListener(new DocumentsRecyclerViewAdapter.ItemClickListener() {
			@Override
			public void onItemClick(View view, int index) {
				final String filename = _filenamesList.get(index);
				String suffix = filename.substring(filename.lastIndexOf(".")+1);
				if (suffix.equalsIgnoreCase("mmp")) {
					Toast.makeText(getActivity(), "Loading MobMuPlat document "+filename, Toast.LENGTH_SHORT).show();
    				((MainActivity)getActivity()).loadScene(filename);
	        		getActivity().getSupportFragmentManager().popBackStack();// remove(DocumentsFragment.this).commit();
				} else if (suffix.equalsIgnoreCase("pd")) {
					Toast.makeText(getActivity(), "Loading PureData patch "+filename, Toast.LENGTH_SHORT).show();
					((MainActivity)getActivity()).loadScenePatchOnly(filename);
					getActivity().getSupportFragmentManager().popBackStack();// remove(DocumentsFragment.this).commit();
				}
			}
		});
		_recyclerView.setAdapter(_adapter);
		refreshFileList();
		refreshShowFilesButton();

		//
		ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
			@Override
			public boolean onMove(
					 RecyclerView recyclerView,
					RecyclerView.ViewHolder viewHolder,
					RecyclerView.ViewHolder target) {
				// Not called.
				return false;
			}
			@Override
			public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
				// Delete file
				String filename = _filenamesList.get(viewHolder.getAdapterPosition());
				File dir = getActivity().getFilesDir();
				File file = new File(dir, filename);
				if (file.isDirectory()) {
					deleteDirectory(file);
					refreshFileList();
				} else {
					boolean deleted = file.delete();
					refreshFileList();
				}
			}
		});
		itemTouchHelper.attachToRecyclerView(_recyclerView);

	   //TODO zips!
	    //rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		return rootView;
	}

	private void deleteDirectory(File file) {
		Path directory = file.toPath();
		try {
			Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			Log.e("DOCUMENTS_FRAGMENT", "Could not delete directory");
		}
	}

	private boolean isMMP(String filename) {
		String suffix = filename.substring(filename.lastIndexOf(".")+1);
		if (suffix.equalsIgnoreCase("mmp")) return true;// || suffix.equalsIgnoreCase("pd")) return true;
		else return false;
	}
	
	private ArrayList<String> getFilenames() {
		File fileDir = new File(MainActivity.getDocumentsFolderPath(getActivity()));

		ArrayList<String> fileList = new ArrayList<String>();
	    File[] files = fileDir.listFiles();
		if (files == null)return null;
	    Arrays.sort(files); //listfiles does not have an order...
	    if (files.length == 0)
	        return null;

		for (File file : files) {
		    // if its a folder, look for a main.pd/mmp in it and add that
            if (file.isDirectory()) {
				File testFile1 = new File(file, "main.pd");
				if (testFile1.exists()) {
					fileList.add(file.getName() + "/main.pd"); // add "foo/main.pd" as item
				}
				File testFile2 = new File(file, "main.mmp");
				if (testFile2.exists()) {
					fileList.add(file.getName() + "/main.mmp"); // add "foo/main.mmp" as item
				}
			}
			fileList.add(file.getName());
		}

	    return fileList;
	}

	@Override
	public void onClick(View v) {
		if (v == _showAllFilesButton) {
			_isListFiltered = !_isListFiltered;
			_showAllFilesButton.setText(_isListFiltered ? "Show all files" : "Showing all files");
			refreshFileList();
            refreshShowFilesButton();
			// save to preferences
            SharedPreferences settings = getActivity().getPreferences(Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("showAllFiles", !_isListFiltered);
            editor.commit();
		} else if (v==_infoButton) {
			new AlertDialog.Builder(getActivity())
		    .setTitle("Getting files in and out of MobMuPlat")
		    .setMessage("MobMuPlat files are stored within the app file system. \nTo bring in files, use the 'import' button to select file(s) from the file picker. \nNote that selection of a zip file will unzip its contents; its contents should all be in the top level, without any subdirectories. \n\nMobMuPlat will also automatically import .mmp, .pd, and .zip files, so just tap on those files from any other application (email, web, Dropbox, Google Drive, etc), and MobMuPlat will copy the file(s) into this folder (and extract all files from a .zip).\n\nSwiping on a file will remove it from the app file storage.")
		    .setPositiveButton(android.R.string.yes, null)
		    .setIcon(R.drawable.ic_launcher)
		     .show();
		} else if (v==_importButton) {
			((MainActivity)getActivity()).requestImportFiles();
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
	
	public void refreshFileList() {
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
		_adapter.notifyDataSetChanged();
	}

	private static class DocumentsRecyclerViewAdapter extends RecyclerView.Adapter<DocumentsRecyclerViewAdapter.DocumentsRecyclerViewHolder> {

		private List<String> filenames;
		private LayoutInflater inflater;
		private ItemClickListener clickListener;

		// data is passed into the constructor
		DocumentsRecyclerViewAdapter(Context context, List<String> filenames) {
			this.inflater = LayoutInflater.from(context);
			this.filenames = filenames;
		}

		@Override
		public DocumentsRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = inflater.inflate(R.layout.list_item_documents, parent, false);
			return new DocumentsRecyclerViewHolder(view);
		}

		private boolean isOpenable(String filename) {
			String suffix = filename.substring(filename.lastIndexOf(".") + 1);
			if (suffix.equalsIgnoreCase("mmp") || suffix.equalsIgnoreCase("pd")) return true;
			else return false;
		}
		// binds the data to the TextView in each row
		@Override
		public void onBindViewHolder(DocumentsRecyclerViewHolder holder, int position) {
			String filename = filenames.get(position);
			holder.setEnabled(isOpenable(filename));
			holder.myTextView.setText(filename);
		}

		@Override
		public int getItemCount() {
			return filenames.size();
		}

		public class DocumentsRecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
			TextView myTextView;

			DocumentsRecyclerViewHolder(View itemView) {
				super(itemView);
				myTextView = itemView.findViewById(R.id.list_item_textView);
				itemView.setOnClickListener(this);
			}

			@Override
			public void onClick(View view) {
				if (clickListener != null) clickListener.onItemClick(view, getAdapterPosition());
			}

			void setEnabled(boolean isEnabled) {
				myTextView.setEnabled(isEnabled); // gray out text
				itemView.setEnabled(isEnabled); // disable tap
			}
		}

		void setClickListener(ItemClickListener itemClickListener) {
			this.clickListener = itemClickListener;
		}

		public interface ItemClickListener {
			void onItemClick(View view, int position);
		}
	}
}
