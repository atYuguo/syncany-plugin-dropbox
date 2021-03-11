Syncany Dropbox Plugin
----------------------
This plugin uses a **Dropbox folder** as a storage backend for [Syncany](http://www.syncany.org). The plugin can be installed in Syncany using the `sy plugin install` command. For further information about the usage, please refer to the central **[wiki page](https://github.com/binwiederhier/syncany/wiki)**.

For plugin development, please refer to the [plugin development wiki page](https://github.com/binwiederhier/syncany/wiki/Plugin-development).
	

### Preparation

You need to set up your own [Dropbox application](https://www.dropbox.com/developers). Replace with your `APP_KEY` and `APP_SECRET` in 

````
syncany-plugin-dropbox/src/main/java/org/syncany/plugins/dropbox/DropboxTransferPlugin.java
````

, and compile this plugin according to the [plugin development wiki page](https://github.com/binwiederhier/syncany/wiki/Plugin-development):

````
./gradlew pluginJar
````

.

### Usage

The destination folder in Dropbox needs to be created beforehand, this plugin can't create it for you yet. If your application is restricted to `App folder`, then *the relative path* should omit `Apps/Your-App-Name/` when asking by `sy init`.

