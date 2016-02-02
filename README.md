MobMuPlat
=========

MobMuPlat is an iOS and Android app for audio software made with PureData, with user-created interfaces.

more info at http://www.mobmuplat.com

###Building for iOS, with XCode:

This project is built upon multiple submodules. Clone the project to your computer, then cd into the MobMuPlat folder. Then run:

```
$ cd MobMuPlat
$ git submodule init
$ git submodule update // populates the libpd folder.
$ cd libpd
$ git submodule update // populates the pure-data folder.
```

Then open MobMuPlat.xcodeproj. Select the MobMuPlat target and run.

###Build for Android, with Android Studio:

This project uses gradle, with its libpd/pure-data dependency handled by jcenter. No git submodules are needed. Clone the project to your computer. Open Android Studio, "Import from existing Android Studio project", and select the MobMuPlat-Android folder. Note that you'll need SDK 22 installed.

