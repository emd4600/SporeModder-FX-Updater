# SporeModder FX Updater

Automatic updater for SporeModder FX. To make a new SMFX release:
 - Change the version code variable `versionInfo`, file `src/sporemodder/UpdateManager.java` in the main SMFX project.
 - Export the SMFX project as an executable jar named `SporeModderFX.jar`.
 - Add the modified files (which include the exported jar) into `src/sporemodder/updater/resources`, in the Updater project.
 - If there are new files or something needs special treatment, change `UpdaterApp.java` accordingly.
 - Export the updater as `SporeModderFX Updater.jar`.
 - Publish a new release in `https://github.com/emd4600/SporeModder-FX/releases`, creating a new tag with the appropiate version code (follow the format, it is used to check if there are updates!). In the release files, add the updater jar and a folder with a clean SMFX installation to the latest version.
 - Edit `index.md` in SMFX branch `gh-pages`, changing the main url to link to the new zipped SMFX.
