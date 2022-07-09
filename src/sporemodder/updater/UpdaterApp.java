package sporemodder.updater;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * Main class of the updater, generates the user interface and concurrent task,
 * and defines all actions that must be taken to update the program.
 */
public class UpdaterApp extends Application {
	
	/** How many milliseconds the program should wait for SporeModderFX.jar to have closed. */
	private static final int MAX_WAIT_TIME = 15000;
	
	/**
	 * Most important function of the updater, declares all the actions that must be taken to update the program.
	 * This does not execute them, it only adds them to the concurrent task.
	 * @param task
	 */
	private void setupTask(UpdateTask task) {
		task.addFile("SporeModderFX.jar");
		task.addFile("SporeModderFX.exe");
		task.addFile("ModCreatorKit.sporemod");
		
		task.addOptionalFile("EffectsEditor/main.pfx", buildPath("Effect Editor", "main.pfx"));
		task.addOptionalFile("EffectsEditor/main.effdir", buildPath("Effect Editor", "main.effdir"));
		
		task.addFile("Documentation/type_names.txt", buildPath("Documentation", "type_names.txt"));
		task.addFile("Styles/syntax.css", buildPath("Styles", "Default", "syntax.css"));
		task.addFile("Styles/basic.css", buildPath("Styles", "Default", "basic.css"));
		task.addFile("Styles/ribbonstyle.css", buildPath("Styles", "Default", "ribbonstyle.css"));
		task.addFile("Styles/color-swatch.css", buildPath("Styles", "Default", "color-swatch.css"));
		task.addFile("Styles/spui-duplicate.png", buildPath("Styles", "Default", "spui-duplicate.png"));
		task.addFile("Styles/spui-import.png", buildPath("Styles", "Default", "spui-import.png"));
		task.addFile("Styles/spui-export.png", buildPath("Styles", "Default", "spui-export.png"));
		task.addFile("Styles/anim-icon.png", buildPath("Styles", "Default", "anim-icon.png"));
		task.addFile("UIEditor/SporeUIDesignerProjectCustom.xml", buildPath("UI Editor", "SporeUIDesignerProjectCustom.xml"));
		
		String[] names = new String[] {
			"anchor-bottom.png", "anchor-fill-h.png", "anchor-fill-v.png", "anchor-left.png", "anchor-right.png", "anchor-top.png", "arrow-down.png", 
			"arrow-left.png", "arrow-right.png", "basic.css", "cancel.png", "color-swatch.css", "compare.png", "config.png", "debug-pack.png", "dialog-error.png", 
			"dialog-information.png", "dialog-warning.png", "duplicate-item.png", "explore-mod.png", "explore-source.png", "find-down.png", "find-up.png", 
			"import-external.png", "item-icon-effects.png", "item-icon-folder.png", "item-icon-spui.png", "item-icon-xml.png", "maximize.png", "modify-item.png", 
			"new-file.png", "new-folder.png", "number-converter-type.png", "pack-and-run.png", "pack.png", "program-icon-old.png", "program-icon.png", "redo.png", 
			"refresh.png", "remove-item.png", "rename-item.png", "ribbon-expand.png", "ribbon-minimize.png", "ribbonstyle.css", "run-without-pack.png", "save.png", 
			"search-fast.png", "search.png", "spui-duplicate.png", "spui-export.png", "spui-import.png", "spui-preview.png", "syntax.css", "undo.png", "unpack.png",
			"anim-icon.png"
		};
		for (String file : names) {
			task.addFile("Styles/Dark/" + file, buildPath("Styles", "Dark", file));
		}
		
		/*try {
			Path darkThemePath = new File(getClass().getResource("/sporemodder/updater/resources/Styles/Dark/basic.css").toURI()).getParentFile().toPath();
			List<String> files = Files.walk(darkThemePath).map(Path::getFileName).map(Path::toString).collect(Collectors.toList());
			files.remove("Dark");
			for (String file : files) {
				task.addFile("Styles/Dark/" + file, "Styles" + File.separatorChar + "Dark" + File.separatorChar + file);
			}
		}
		catch (URISyntaxException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		task.modifyRegistry("reg_file.txt", "reg_file.txt");
		task.modifyRegistry("reg_property.txt", "reg_property.txt");
		task.forcedModifyRegistry("reg_type.txt", "reg_type.txt");
		task.modifyRegistry("reg_type_noforce.txt", "reg_type.txt");
		
		task.addFile("reg_cnv.txt");
	}
	
	/**
	 * Builds a relative path joining the given strings, using the appropriate file separator character.
	 * Not necessary for internal paths.
	 * @param strings
	 * @return
	 */
	private String buildPath(String ... strings) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < strings.length; i++) {
			if (i != 0) sb.append(File.separatorChar);
			sb.append(strings[i]);
		}
		return sb.toString();
	}
	
	private void showErrorAlert() {
		Alert alert = new Alert(AlertType.ERROR, "Updater failed, original program may still be running.", ButtonType.OK);
		alert.showAndWait();
	}
	
	private static boolean waitForProgram(File programJar) {
		Path path = programJar.toPath();
		long waitStart = System.currentTimeMillis();
		while (!Files.isWritable(path)) {
			if (System.currentTimeMillis() - waitStart > MAX_WAIT_TIME) {
				return false;
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return true;
	}

	public static void main(String[] args) {
		launch(args);
	}
	
	@Override public void start(Stage primaryStage) throws Exception {
		List<String> params = getParameters().getRaw();
		File folder;
		if (params.isEmpty()) {
			DirectoryChooser chooser = new DirectoryChooser();
			chooser.setTitle("Find the SporeModder FX folder");
			folder = chooser.showDialog(null);
		} else {
			folder = new File(params.get(0));
		}
		
		if (folder == null) {
			Platform.exit();
			return;
		}
		File jarFile = new File(folder, "SporeModderFX.jar");
		
		if (waitForProgram(jarFile)) {
			
			UpdateTask task = new UpdateTask(folder);
			setupTask(task);
			
			ProgressBar progressBar = new ProgressBar();
			progressBar.setPrefWidth(400);
			progressBar.setPrefHeight(35);
			progressBar.setProgress(-1);
			progressBar.progressProperty().bind(task.progressProperty());
			
			Pane pane = new Pane(progressBar);
			progressBar.setPadding(new Insets(10));
			
			Scene scene = new Scene(pane);
			
			primaryStage.setOnShowing(event -> {
				new Thread(task).start();
			});
			
			task.setOnSucceeded(event -> {
				try {
					File executable = new File(folder, "SporeModderFX.exe");
					if (executable.exists()) {
						Runtime.getRuntime().exec(new String[] {"cmd", "/c", executable.getAbsolutePath()});
					} else {
						Runtime.getRuntime().exec(new String[] {"java", "-jar", jarFile.getAbsolutePath()});
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				Platform.exit();
			});
			
			task.setOnFailed(event -> {
				showErrorAlert();
				Platform.exit();
			});
			
			primaryStage.setScene(scene);
			primaryStage.setTitle("Updating SporeModder FX...");
			primaryStage.show();
		}
		else {
			showErrorAlert();
		}
	}
}
