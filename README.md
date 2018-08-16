MobMuPlat
=========

MobMuPlat is an iOS and Android app for audio software made with PureData, with user-created interfaces.

more info at http://www.mobmuplat.com

### Building for iOS, with XCode:

This project is built upon multiple submodules. Clone the project to your computer, then cd into the MobMuPlat folder. Then run (assumes recent version of git):

```
$ cd MobMuPlat
$ git submodule update --init --recursive
```
This should populate the libpd folder, and, within libpd, the pure-data folder. It should also populate PdParty and ZipArchive submodules.
Then open MobMuPlat.xcodeproj. Select the MobMuPlat target and run.

### Building for Android, with Android Studio:

This project uses gradle, with its libpd/pure-data dependency handled by jcenter. No git submodules are needed. Clone the project to your computer. Open Android Studio, "Import from existing Android Studio project", and select the MobMuPlat-Android folder. Note that you'll need SDK 27 installed.

