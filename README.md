MobMuPlat
=========

MobMuPlat is an iOS and Android app for audio software made with PureData, with user-created interfaces.

more info at http://www.mobmuplat.com

This project is built upon multiple submodules. Clone the project to your computer, then cd into the MobMuPlat folder. Then run:

```
$ cd MobMuPlat
$ git submodule init
$ git submodule update
```

to populate the libpd and pd-for-android folders.
Then run the same submodule init + update in both the libpd and pd-android folders.


To build for iOS, open MobMuPlat.xcodeproj. Select the MobMuPlat target and run.

To build for Android (with Eclipse):
- Make sure you have all the Android development tools installed.
- "File -> Import". Choose "Android->Existing Android Code Into Workspace". Select the MobMuPlat-Android Folder.
- "File -> Import". Choose "General->Existing Projects Into Workspace". Select the pd-for-android/PdCore Folder.
- "File -> Import". Choose "General->Existing Projects Into Workspace". Select the pd-for-android/midi Folder.

At this point you should have "MobMuPlat", "PdCore" and "AndroidMidi" projects in your workspace, with no errors.
Open "MainActivity.java" in MobMuPlat package "com.iglesiaintermedia.mobmuplat". Run.
See https://github.com/libpd/pd-for-android/wiki/eclipse for details/troubleshooting.
