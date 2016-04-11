package com.iglesiaintermedia.mobmuplatandroidwear;
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//package com.example.android.wearable.gridviewpager;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Color;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.FragmentGridPagerAdapter;
//import android.support.wearable.view.ImageReference;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Constructs fragments as requested by the GridViewPager. For each row a
 * different background is provided.
 */
public class SampleGridPagerAdapter extends FragmentGridPagerAdapter {

    private Context mContext;
    //private MMPWearFragment[] mFrags;
    private List<MMPWearFragment> mFragsList;
    private Map<String, List<MMPWearFragment>> addressToFragmentListMap;
    public SampleGridPagerAdapter(Context ctx, FragmentManager fm) {
        super(fm);
        mContext = ctx;
        mFragsList = new ArrayList<MMPWearFragment>();
        addressToFragmentListMap = new HashMap<String, List<MMPWearFragment>>(); 
    }
    
   public void passMessage(String address, List<Object>messageList) {
	   List<MMPWearFragment> fragList = addressToFragmentListMap.get(address);
       if (fragList !=null) {
           for (MMPWearFragment frag : fragList) {
               if (frag.control != null) {
                   frag.control.receiveList(messageList);
               }
           }
       }
   }
   public void loadGUI(String dataString) {
	   addressToFragmentListMap.clear();
       mFragsList.clear();
		try {
			JsonParser parser = new JsonParser();
			JsonObject topDict = parser.parse(dataString).getAsJsonObject();//top dict=wear - exception on bad JSON

			if(topDict.get("wearGui")!=null) {
				JsonArray pagesArray = topDict.get("wearGui").getAsJsonArray();
				for (int i=0; i<pagesArray.size();i++) {
					JsonObject pageDict = pagesArray.get(i).getAsJsonObject(); //page dict (title, paageui)
					if (pageDict.get("pageGui")==null) {
                        showAlert();
                        break;
                    }
					JsonObject pageGuiDict = pageDict.get("pageGui").getAsJsonObject(); //control dict
					if (pageGuiDict.get("address")==null){
                        showAlert();
                        break;
                    }
					String address = pageGuiDict.get("address").getAsString();

                    MMPWearFragment wearFragment = new MMPWearFragment();
					wearFragment.setPageDict(pageDict);
                    mFragsList.add(wearFragment);

					// put in list of fragments with that address
					List<MMPWearFragment> fragList = addressToFragmentListMap.get(address);
					if (fragList == null){
						fragList = new ArrayList<MMPWearFragment>();
						addressToFragmentListMap.put(address, fragList);
					}
					fragList.add(wearFragment);
				}
			} else { //no wear gui
                showAlert();
            }
		} catch(JsonParseException e) {
			showAlert();
		}
   }

    private void showAlert() {
        Toast.makeText(mContext, "Unable to parse interface file", Toast.LENGTH_LONG).show();;
    }

    @Override
    public Fragment getFragment(int row, int col) {
        if (mFragsList.size() > col) {
            return mFragsList.get(col);
        } else {
            return new MMPWearFragment(); //set dummy fragment
        }
    }

    /*@Override
    public ImageReference getBackground(int row, int column) {
        return ImageReference.forDrawable(BG_IMAGES[row % BG_IMAGES.length]);
    }*/

    @Override
    public int getRowCount() {
        if (mFragsList.size() > 0) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public int getColumnCount(int rowNum) {
        return mFragsList.size();
    }
    
}
