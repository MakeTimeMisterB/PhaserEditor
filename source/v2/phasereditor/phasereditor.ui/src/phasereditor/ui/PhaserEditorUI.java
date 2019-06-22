// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.internal.ide.IDEInternalPreferences;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.misc.StringMatcher;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.statushandlers.StatusManager;

import phasereditor.ui.ZoomCanvas.ZoomCalculator;
import phasereditor.ui.views.PreviewView;

public class PhaserEditorUI {

	public static final String PREF_PROP_ALIEN_EDITOR_ENABLED = "phasereditor.ui.alieneditor.enabled";
	public static final String PREF_PROP_ALIEN_EDITOR_PROGRAM = "phasereditor.ui.alieneditor.program";
	public static final String PREF_PROP_ALIEN_EDITOR_COMMON_ARGS = "phasereditor.ui.alieneditor.commonArgs";
	public static final String PREF_PROP_ALIEN_EDITOR_PROJECT_ARGS = "phasereditor.ui.alieneditor.projectArgs";
	public static final String PREF_PROP_ALIEN_EDITOR_FILE_ARGS = "phasereditor.ui.alieneditor.fileArgs";
	public static final String PREF_PROP_ALIEN_EDITOR_FILE_LINE_ARGS = "phasereditor.ui.alieneditor.fileLineArgs";

	public static final String PREF_PROP_BROWSER_TYPE = "phasereditor.ui.browser";
	public static final String PREF_VALUE_DEFAULT_BROWSER = "phasereditor.ui.browser.default";
	public static final String PREF_VALUE_CHROMIUM_BROWSER = "phasereditor.ui.browser.chromium";

	public static final String PREF_PROP_COLOR_DIALOG_TYPE = "phasereditor.ui.dialogs.colorDialogType";
	public static final String PREF_VALUE_COLOR_DIALOG_JAVA = "java";
	public static final String PREF_VALUE_COLOR_DIALOG_NATIVE = "native";

	public static final String PREF_PROP_PREVIEW_ANTIALIAS = "phasereditor.ui.preview.antialiasing";
	public static final String PREF_PROP_PREVIEW_IMG_PAINT_BG_TYPE = "phasereditor.ui.preview.imageBackgroundType";
	public static final String PREF_VALUE_PREVIEW_IMG_PAINT_BG_TYPE_TRANSPARENT = "0";
	public static final String PREF_VALUE_PREVIEW_IMG_PAINT_BG_TYPE_ONE_COLOR = "1";
	public static final String PREF_VALUE_PREVIEW_IMG_PAINT_BG_TYPE_TWO_COLORS = "2";
	private static String _PREF_PROP_PREVIEW_IMG_PAINT_BG_TYPE = PREF_VALUE_PREVIEW_IMG_PAINT_BG_TYPE_TWO_COLORS;

	public static final String PREF_PROP_PREVIEW_IMG_PAINT_BG_SOLID_COLOR = "phasereditor.ui.preview.imageBackgroundSolidColor";
	public static final String PREF_PROP_PREVIEW_IMG_PAINT_BG_COLOR_1 = "phasereditor.ui.preview.imageBackgroundColor1";
	public static Color _PREF_PROP_PREVIEW_IMG_PAINT_BG_SOLID_COLOR;
	public static Color _PREF_PROP_PREVIEW_IMG_PAINT_BG_COLOR_1;

	public static final String PREF_PROP_PREVIEW_SPRITESHEET_PAINT_FRAMES = "phasereditor.ui.preview.spritesheetPaintFrames";
	public static final String PREF_PROP_PREVIEW_SPRITESHEET_FRAMES_BORDER_COLOR = "phasereditor.ui.preview.spritesheetBorderColor";
	public static final String PREF_PROP_PREVIEW_SPRITESHEET_PAINT_LABELS = "phasereditor.ui.preview.spritesheetPaintLabels";
	public static final String PREF_PROP_PREVIEW_SPRITESHEET_LABELS_COLOR = "phasereditor.ui.preview.spritesheetLabelsColor";
	private static boolean _PREF_PROP_PREVIEW_SPRITESHEET_PAINT_FRAMES = true;
	public static Color _PREF_PROP_PREVIEW_SPRITESHEET_FRAMES_BORDER_COLOR;
	private static boolean _PREF_PROP_PREVIEW_SPRITESHEET_PAINT_LABELS = true;
	public static Color _PREF_PROP_PREVIEW_SPRITESHEET_LABELS_COLOR;

	public static final String PREF_PROP_PREVIEW_TILEMAP_OVER_TILE_BORDER_COLOR = "phasereditor.ui.preview.tilemapOverTileBorderColor";
	public static final String PREF_PROP_PREVIEW_TILEMAP_LABELS_COLOR = "phasereditor.ui.preview.tilemapLabelsColor";
	public static final String PREF_PROP_PREVIEW_TILEMAP_SELECTION_BG_COLOR = "phasereditor.ui.preview.tilemapSelectionColor";

	public static Color _PREF_PROP_PREVIEW_TILEMAP_OVER_TILE_BORDER_COLOR;
	public static Color _PREF_PROP_PREVIEW_TILEMAP_LABELS_COLOR;
	public static Color _PREF_PROP_PREVIEW_TILEMAP_SELECTION_BG_COLOR;

	public static final String PREF_PROP_PREVIEW_TILEMAP_TILE_WIDTH = "phasereditor.ui.preview.tilemapTileWidth";
	public static final String PREF_PROP_PREVIEW_TILEMAP_TILE_HEIGHT = "phasereditor.ui.preview.tilemapTileHeight";
	private static final String PLUGIN_ID = "phasereditor.ui";

	private static Set<Object> _supportedImageExts = new HashSet<>(Arrays.asList("png", "bmp", "jpg", "gif", "ico"));
	private static boolean _isCocoaPlatform = Util.isMac();
	private static boolean _isWindowsPlatform = Util.isWindows();
	private static boolean _isLinux = Util.isLinux();

	private static final String VARIABLE_RESOURCE = "${selected_resource_loc}"; //$NON-NLS-1$
	private static final String VARIABLE_RESOURCE_URI = "${selected_resource_uri}"; //$NON-NLS-1$
	private static final String VARIABLE_FOLDER = "${selected_resource_parent_loc}"; //$NON-NLS-1$

	private PhaserEditorUI() {
	}

	private static String formShowInSytemExplorerCommand(File path) throws IOException {
		String command = IDEWorkbenchPlugin.getDefault().getPreferenceStore()
				.getString(IDEInternalPreferences.WORKBENCH_SYSTEM_EXPLORER);

		command = Util.replaceAll(command, VARIABLE_RESOURCE, quotePath(path.getCanonicalPath()));
		command = Util.replaceAll(command, VARIABLE_RESOURCE_URI, path.getCanonicalFile().toURI().toString());
		File parent = path.getParentFile();
		if (parent != null) {
			command = Util.replaceAll(command, VARIABLE_FOLDER, quotePath(parent.getCanonicalPath()));
		}
		return command;
	}

	private static String quotePath(String path) {
		var path2 = path;

		if (Util.isLinux() || Util.isMac()) {
			// Quote for usage inside "", man sh, topic QUOTING:
			path2 = path.replaceAll("[\"$`]", "\\\\$0"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		// Windows: Can't quote, since explorer.exe has a very special command line
		// parsing strategy.
		return path2;
	}

	public static void showInSystemExplorer(File canonicalPath) {
		Job job = Job.create(IDEWorkbenchMessages.ShowInSystemExplorerHandler_jobTitle, monitor -> {
			try {

				String launchCmd = formShowInSytemExplorerCommand(canonicalPath);

				if ("".equals(launchCmd)) { //$NON-NLS-1$
					return Status.OK_STATUS;
				}

				File dir = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
				Process p;
				if (Util.isLinux() || Util.isMac()) {
					p = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", launchCmd }, null, dir); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					p = Runtime.getRuntime().exec(launchCmd, null, dir);
				}
				int retCode = p.waitFor();
				if (retCode != 0 && !Util.isWindows()) {
					return Status.OK_STATUS;
				}
			} catch (IOException | InterruptedException e2) {
				return Status.OK_STATUS;
			}
			return Status.OK_STATUS;
		});
		job.schedule();
	}

	public static void logError(Exception e) {
		e.printStackTrace();
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

	public static void logError(String msg) {
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, msg, null));
	}

	public static void initPreferences() {

		RGB _RED = new RGB(255, 0, 0);
		RGB _BLUE = new RGB(0, 0, 255);
		RGB _YELLOW = new RGB(255, 255, 0);
		// RGB _GRAY = new RGB(192, 192, 192);

		IPreferenceStore store = PhaserEditorUI.getPreferenceStore();

		store.setDefault(PREF_PROP_BROWSER_TYPE, PREF_VALUE_DEFAULT_BROWSER);

		store.setDefault(PhaserEditorUI.PREF_PROP_COLOR_DIALOG_TYPE, PhaserEditorUI.PREF_VALUE_COLOR_DIALOG_NATIVE);

		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_ANTIALIAS, false);
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_IMG_PAINT_BG_TYPE,
				PhaserEditorUI.PREF_VALUE_PREVIEW_IMG_PAINT_BG_TYPE_TRANSPARENT);
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_IMG_PAINT_BG_SOLID_COLOR,
				StringConverter.asString(new RGB(180, 180, 180)));

		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_IMG_PAINT_BG_COLOR_1,
				StringConverter.asString(PhaserEditorUI.getListTextColor().getRGB()));

		// spritesheet

		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_SPRITESHEET_PAINT_FRAMES, true);
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_SPRITESHEET_PAINT_LABELS, true);
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_SPRITESHEET_FRAMES_BORDER_COLOR,
				StringConverter.asString(_RED));
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_SPRITESHEET_LABELS_COLOR, StringConverter.asString(_YELLOW));

		// tilemap

		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_TILEMAP_OVER_TILE_BORDER_COLOR,
				StringConverter.asString(_RED));
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_TILEMAP_LABELS_COLOR, StringConverter.asString(_YELLOW));
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_TILEMAP_SELECTION_BG_COLOR, StringConverter.asString(_BLUE));
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_TILEMAP_TILE_WIDTH, 32);
		store.setDefault(PhaserEditorUI.PREF_PROP_PREVIEW_TILEMAP_TILE_HEIGHT, 32);

		_PREF_PROP_PREVIEW_IMG_PAINT_BG_TYPE = getPreferenceStore().getString(PREF_PROP_PREVIEW_IMG_PAINT_BG_TYPE);

		{
			RGB rgb = StringConverter.asRGB(getPreferenceStore().getString(PREF_PROP_PREVIEW_IMG_PAINT_BG_SOLID_COLOR));
			_PREF_PROP_PREVIEW_IMG_PAINT_BG_SOLID_COLOR = SwtRM.getColor(rgb);
			rgb = StringConverter.asRGB(getPreferenceStore().getString(PREF_PROP_PREVIEW_IMG_PAINT_BG_COLOR_1));
			_PREF_PROP_PREVIEW_IMG_PAINT_BG_COLOR_1 = SwtRM.getColor(rgb);
		}

		{
			_PREF_PROP_PREVIEW_SPRITESHEET_PAINT_FRAMES = getPreferenceStore()
					.getBoolean(PREF_PROP_PREVIEW_SPRITESHEET_PAINT_FRAMES);
			_PREF_PROP_PREVIEW_SPRITESHEET_PAINT_LABELS = getPreferenceStore()
					.getBoolean(PREF_PROP_PREVIEW_SPRITESHEET_PAINT_LABELS);
		}

		{
			RGB rgb = StringConverter
					.asRGB(getPreferenceStore().getString(PREF_PROP_PREVIEW_SPRITESHEET_FRAMES_BORDER_COLOR));
			_PREF_PROP_PREVIEW_SPRITESHEET_FRAMES_BORDER_COLOR = SwtRM.getColor(rgb);
			rgb = StringConverter.asRGB(getPreferenceStore().getString(PREF_PROP_PREVIEW_SPRITESHEET_LABELS_COLOR));
			_PREF_PROP_PREVIEW_SPRITESHEET_LABELS_COLOR = SwtRM.getColor(rgb);
		}

		{
			RGB rgb = StringConverter
					.asRGB(getPreferenceStore().getString(PREF_PROP_PREVIEW_TILEMAP_OVER_TILE_BORDER_COLOR));
			_PREF_PROP_PREVIEW_TILEMAP_OVER_TILE_BORDER_COLOR = SwtRM.getColor(rgb);
			rgb = StringConverter.asRGB(getPreferenceStore().getString(PREF_PROP_PREVIEW_TILEMAP_LABELS_COLOR));
			_PREF_PROP_PREVIEW_TILEMAP_LABELS_COLOR = SwtRM.getColor(rgb);
			rgb = StringConverter.asRGB(getPreferenceStore().getString(PREF_PROP_PREVIEW_TILEMAP_SELECTION_BG_COLOR));
			_PREF_PROP_PREVIEW_TILEMAP_SELECTION_BG_COLOR = SwtRM.getColor(rgb);
		}

		{
			// external editor
			getPreferenceStore().setDefault(PREF_PROP_ALIEN_EDITOR_ENABLED, false);
			getPreferenceStore().setDefault(PREF_PROP_ALIEN_EDITOR_PROGRAM, "code");
			getPreferenceStore().setDefault(PREF_PROP_ALIEN_EDITOR_COMMON_ARGS, "-r");
			getPreferenceStore().setDefault(PREF_PROP_ALIEN_EDITOR_PROJECT_ARGS, "${project}");
			getPreferenceStore().setDefault(PREF_PROP_ALIEN_EDITOR_FILE_ARGS, "${file}");
			getPreferenceStore().setDefault(PREF_PROP_ALIEN_EDITOR_FILE_LINE_ARGS, "-g ${file}:${line}");
		}

		getPreferenceStore().addPropertyChangeListener(event -> {

			String prop = event.getProperty();

			switch (prop) {
			case PREF_PROP_PREVIEW_IMG_PAINT_BG_TYPE:
				_PREF_PROP_PREVIEW_IMG_PAINT_BG_TYPE = (String) event.getNewValue();
				break;
			case PREF_PROP_PREVIEW_IMG_PAINT_BG_SOLID_COLOR:
				_PREF_PROP_PREVIEW_IMG_PAINT_BG_SOLID_COLOR = SwtRM.getColor(getRGBFromPrefEvent(event));
				break;
			case PREF_PROP_PREVIEW_IMG_PAINT_BG_COLOR_1:
				_PREF_PROP_PREVIEW_IMG_PAINT_BG_COLOR_1 = SwtRM.getColor(getRGBFromPrefEvent(event));
				break;

			// spritesheet

			case PREF_PROP_PREVIEW_SPRITESHEET_PAINT_FRAMES:
				_PREF_PROP_PREVIEW_SPRITESHEET_PAINT_FRAMES = getPreferenceStore()
						.getBoolean(PREF_PROP_PREVIEW_SPRITESHEET_PAINT_FRAMES);
				break;
			case PREF_PROP_PREVIEW_SPRITESHEET_PAINT_LABELS:
				_PREF_PROP_PREVIEW_SPRITESHEET_PAINT_LABELS = getPreferenceStore()
						.getBoolean(PREF_PROP_PREVIEW_SPRITESHEET_PAINT_LABELS);
				break;
			case PREF_PROP_PREVIEW_SPRITESHEET_FRAMES_BORDER_COLOR:
				_PREF_PROP_PREVIEW_SPRITESHEET_FRAMES_BORDER_COLOR = SwtRM.getColor(getRGBFromPrefEvent(event));
				break;
			case PREF_PROP_PREVIEW_SPRITESHEET_LABELS_COLOR:
				_PREF_PROP_PREVIEW_SPRITESHEET_LABELS_COLOR = SwtRM.getColor(getRGBFromPrefEvent(event));
				break;

			// tilemap

			case PREF_PROP_PREVIEW_TILEMAP_OVER_TILE_BORDER_COLOR:
				_PREF_PROP_PREVIEW_TILEMAP_OVER_TILE_BORDER_COLOR = SwtRM.getColor(getRGBFromPrefEvent(event));
				break;
			case PREF_PROP_PREVIEW_TILEMAP_LABELS_COLOR:
				_PREF_PROP_PREVIEW_TILEMAP_LABELS_COLOR = SwtRM.getColor(getRGBFromPrefEvent(event));
				break;
			case PREF_PROP_PREVIEW_TILEMAP_SELECTION_BG_COLOR:
				_PREF_PROP_PREVIEW_TILEMAP_SELECTION_BG_COLOR = SwtRM.getColor(getRGBFromPrefEvent(event));
				break;
			default:
				break;
			}
		});
	}

	public static boolean externalEditor_enabled() {
		return getPreferenceStore().getBoolean(PREF_PROP_ALIEN_EDITOR_ENABLED);
	}

	public static void externalEditor_openProject(IResource res) {
		var prog = getPreferenceStore().getString(PREF_PROP_ALIEN_EDITOR_PROGRAM);
		var common = getPreferenceStore().getString(PREF_PROP_ALIEN_EDITOR_COMMON_ARGS);
		var project = getPreferenceStore().getString(PREF_PROP_ALIEN_EDITOR_PROJECT_ARGS);
		project = project.replace("${project}", res.getProject().getLocation().toOSString());

		var pb = new ProcessBuilder(prog, common, project);
		try {
			pb.start();
		} catch (IOException e) {
			logError(e);
			throw new RuntimeException(e);
		}
	}

	public static void externalEditor_openFile(IResource res) {
		var prog = getPreferenceStore().getString(PREF_PROP_ALIEN_EDITOR_PROGRAM);
		var common = getPreferenceStore().getString(PREF_PROP_ALIEN_EDITOR_COMMON_ARGS);
		var file = getPreferenceStore().getString(PREF_PROP_ALIEN_EDITOR_FILE_ARGS);
		file = file.replace("${file}", res.getLocation().toOSString());
		var pb = new ProcessBuilder(prog, common, file);
		try {
			pb.start();
		} catch (IOException e) {
			logError(e);
			throw new RuntimeException(e);
		}
	}

	public static void externalEditor_openFileLine(IResource res, int line) {
		var list = new ArrayList<String>();

		{
			var progArgs = getPreferenceStore().getString(PREF_PROP_ALIEN_EDITOR_PROGRAM);
			list.add(progArgs);
		}
		{
			var commonArgs = getPreferenceStore().getString(PREF_PROP_ALIEN_EDITOR_COMMON_ARGS);
			list.addAll(Arrays.asList(commonArgs.split(" ")));
		}
		{
			var filepath = res.getLocation().toOSString();
			var filelineArgs = getPreferenceStore().getString(PREF_PROP_ALIEN_EDITOR_FILE_LINE_ARGS);
			for (var str : filelineArgs.split(" ")) {
				str = str.replace("${file}", filepath);
				str = str.replace("${line}", Integer.toString(line));
				list.add(str);
			}
		}

		var pb = new ProcessBuilder(list);
		try {
			pb.start();
		} catch (IOException e) {
			logError(e);
			throw new RuntimeException(e);
		}
	}

	public static boolean useChromuiumBrowser() {
		return getPreferenceStore().getString(PREF_PROP_BROWSER_TYPE).equals(PREF_VALUE_CHROMIUM_BROWSER);
	}

	public static RGB getRGBFromPrefEvent(PropertyChangeEvent event) {
		Object newValue = event.getNewValue();
		if (newValue instanceof RGB) {
			return (RGB) newValue;

		}
		return StringConverter.asRGB((String) newValue);
	}

	public static boolean get_pref_Dialog_Color_Java() {
		return getPreferenceStore().getString(PREF_PROP_COLOR_DIALOG_TYPE).equals(PREF_VALUE_COLOR_DIALOG_JAVA);
	}

	public static boolean get_pref_Preview_Anitialias() {
		return getPreferenceStore().getBoolean(PREF_PROP_PREVIEW_ANTIALIAS);
	}

	public static boolean get_pref_Preview_Spritesheet_paintFramesBorder() {
		return _PREF_PROP_PREVIEW_SPRITESHEET_PAINT_FRAMES;
	}

	public static boolean get_pref_Preview_Spritesheet_paintFramesLabels() {
		return _PREF_PROP_PREVIEW_SPRITESHEET_PAINT_LABELS;
	}

	public static Color get_pref_Preview_Spritesheet_borderColor() {
		return _PREF_PROP_PREVIEW_SPRITESHEET_FRAMES_BORDER_COLOR;
	}

	public static Color get_pref_Preview_Spritesheet_labelsColor() {
		return _PREF_PROP_PREVIEW_SPRITESHEET_LABELS_COLOR;
	}

	public static Color get_pref_Preview_Tilemap_overTileBorderColor() {
		return _PREF_PROP_PREVIEW_TILEMAP_OVER_TILE_BORDER_COLOR;
	}

	public static Color get_pref_Preview_Tilemap_labelsColor() {
		return _PREF_PROP_PREVIEW_TILEMAP_LABELS_COLOR;
	}

	public static Color get_pref_Preview_Tilemap_selectionBgColor() {
		return _PREF_PROP_PREVIEW_TILEMAP_SELECTION_BG_COLOR;
	}

	public static Point get_pref_Preview_Tilemap_size() {
		IPreferenceStore store = getPreferenceStore();
		return new Point(store.getInt(PREF_PROP_PREVIEW_TILEMAP_TILE_WIDTH),
				store.getInt(PREF_PROP_PREVIEW_TILEMAP_TILE_HEIGHT));
	}

	public static boolean isMacPlatform() {
		return _isCocoaPlatform;
	}

	public static boolean isWindowsPlaform() {
		return _isWindowsPlatform;
	}

	public static boolean isLinux() {
		return _isLinux;
	}

	public static void applyThemeStyle(Object widget) {
		IThemeEngine engine = getThemeEngine();
		if (engine == null) {
			return;
		}
		if (widget instanceof Shell) {
			((Shell) widget).setBackgroundMode(SWT.INHERIT_DEFAULT);
		}
		engine.applyStyles(widget, true);
	}

	public static Color getListSelectionColor() {
		return Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION);
	}

	public static Color getListSelectionTextColor() {
		return Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
	}

	public static Color getListTextColor() {
		return Display.getDefault().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
	}

	public static Color getListBackgroundColor() {
		return Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
	}

	public static void setThemeClass(Object widget, String className) {
		IStylingEngine styledEngine = PlatformUI.getWorkbench().getService(IStylingEngine.class);
		styledEngine.setClassname(widget, className);
	}

	public static IThemeEngine getThemeEngine() {
		MApplication application = PlatformUI.getWorkbench().getService(MApplication.class);
		IEclipseContext context = application.getContext();
		IThemeEngine engine = context.get(IThemeEngine.class);
		return engine;
	}

	public static void forEachEditor(Consumer<IEditorPart> visitor) {
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference editorRef : page.getEditorReferences()) {
					IEditorPart editor = editorRef.getEditor(false);

					if (editor == null) {
						continue;
					}

					visitor.accept(editor);
				}
			}
		}
	}

	public static List<IEditorPart> findOpenFileEditors(IFile file) {
		List<IEditorPart> list = new ArrayList<>();
		forEachOpenFileEditor(file, e -> {
			list.add(e);
		});
		return list;
	}

	public static boolean forEachOpenFileEditor(IFile file, Consumer<IEditorPart> visitor) {
		boolean found = false;
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference editorRef : page.getEditorReferences()) {
					IEditorPart editor = editorRef.getEditor(false);

					if (editor == null) {
						continue;
					}

					IEditorInput input = editor.getEditorInput();

					if (input instanceof FileEditorInput) {
						IFile editorFile = ((FileEditorInput) input).getFile();
						if (editorFile.equals(file)) {
							found = true;
							visitor.accept(editor);
						}
					}
				}
			}
		}
		return found;
	}

	public static boolean isEditorSupportedImage(IFile file) {
		return _supportedImageExts.contains(file.getFileExtension().toLowerCase());
	}

	public static IFile pickFileWithoutExtension(List<IFile> files, String... exts) {
		if (files.isEmpty()) {
			return null;
		}
		Set<String> set = new HashSet<>();
		for (String ext : exts) {
			set.add(ext.toLowerCase());
		}
		for (IFile file : files) {
			String ext = file.getFileExtension().toLowerCase();
			if (!set.contains(ext)) {
				return file;
			}
		}
		return files.get(0);
	}

	public static Path eclipseFileToJavaPath(IFile file) {
		return Paths.get(file.getLocation().toPortableString());
	}

	public interface ISearchFilter {
		public void setSearchText(String str);
	}

	public static void initSearchText(Text text, StructuredViewer viewer, ViewerFilter filter) {
		String msg = "type filter text";
		text.setText(msg);

		viewer.setFilters(new ViewerFilter[] { filter });

		text.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if (text.getText().equals(msg)) {
					text.setText("");
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (text.getText().trim().length() == 0) {
					text.setText(msg);
				}
			}
		});

		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				String str = text.getText();
				((ISearchFilter) filter).setSearchText(str);
				viewer.refresh();
				if (viewer instanceof TreeViewer) {
					if (str.trim().length() > 0) {
						((TreeViewer) viewer).expandAll();
					}
				}
			}
		});
	}

	public static class SimpleSearchFilter extends ViewerFilter implements ISearchFilter {

		private String _searchString;
		private ILabelProvider _labelProvider;
		private Map<Object, Boolean> _cache;

		public SimpleSearchFilter(ILabelProvider labelProvider) {
			super();
			_labelProvider = labelProvider;
			_cache = new HashMap<>();
		}

		@Override
		public void setSearchText(String s) {
			this._searchString = ".*" + s.toLowerCase().replace("*", ".*") + ".*";
			_cache = new HashMap<>();
		}

		@Override
		public boolean select(Viewer viewer, Object parent, Object element) {
			if (_cache.containsKey(element)) {
				return _cache.get(element).booleanValue();
			}

			boolean b = select2(viewer, parent, element);

			_cache.put(element, Boolean.valueOf(b));

			return b;
		}

		private boolean select2(Viewer viewer, Object parent, Object element) {
			if (_searchString == null || _searchString.length() == 4) {
				return true;
			}

			{
				// if it is not final, then match it if any children match.
				IContentProvider provider = ((StructuredViewer) viewer).getContentProvider();
				if (provider instanceof ITreeContentProvider) {
					Object[] children = ((ITreeContentProvider) provider).getChildren(element);
					if (children.length > 0) {
						for (Object child : children) {
							if (select(viewer, element, child)) {
								return true;
							}
						}
					}
				}
			}

			if (matches(parent)) {
				return true;
			}

			return matches(element);
		}

		private boolean matches(Object element) {
			String text = _labelProvider.getText(element).toLowerCase();

			if (text.matches(_searchString)) {
				return true;
			}

			return false;
		}
	}

	public static void initSearchText(Text text, StructuredViewer viewer, ILabelProvider labelProvider) {
		initSearchText(text, viewer, new SimpleSearchFilter(labelProvider));
	}

	/**
	 * <p>
	 * Searches "searchTerm" in "content" and returns an array of int pairs (index,
	 * length) for each occurrence. The search is case-sensitive. The consecutive
	 * occurrences are merged together.
	 * </p>
	 * <p>
	 * Examples:
	 * </p>
	 * 
	 * <pre>
	 * content = "123123x123"
	 * searchTerm = "1"
	 * --> [0, 1, 3, 1, 7, 1]
	 * content = "123123x123"
	 * searchTerm = "123"
	 * --> [0, 6, 7, 3]
	 * </pre>
	 * 
	 * http://www.vogella.com/tutorials/EclipseJFaceTableAdvanced/article.html
	 * 
	 * @param searchTerm
	 *            can be null or empty. int[0] is returned in this case!
	 * @param content
	 *            a not-null string (can be empty!)
	 * @return an array of int pairs (index, length)
	 */

	public static int[] getSearchTermOccurrences(final String searchTerm, final String content) {
		if (searchTerm == null || searchTerm.length() == 0) {
			return new int[0];
		}
		if (content == null) {
			throw new IllegalArgumentException("content is null");
		}
		final List<Integer> list = new ArrayList<>();
		int searchTermLength = searchTerm.length();
		int index;
		int fromIndex = 0;
		int lastIndex = -1;
		int lastLength = 0;
		while (true) {
			index = content.indexOf(searchTerm, fromIndex);
			if (index == -1) {
				// no occurrence of "searchTerm" in "content" starting from
				// index "fromIndex"
				if (lastIndex != -1) {
					// but there was a previous occurrence
					list.add(Integer.valueOf(lastIndex));
					list.add(Integer.valueOf(lastLength));
				}
				break;
			}
			if (lastIndex == -1) {
				// the first occurrence of "searchTerm" in "content"
				lastIndex = index;
				lastLength = searchTermLength;
			} else {
				if (lastIndex + lastLength == index) {
					// the current occurrence is right after the previous
					// occurrence
					lastLength += searchTermLength;
				} else {
					// there is at least one character between the current
					// occurrence and the previous one
					list.add(Integer.valueOf(lastIndex));
					list.add(Integer.valueOf(lastLength));
					lastIndex = index;
					lastLength = searchTermLength;
				}
			}
			fromIndex = index + searchTermLength;
		}
		final int n = list.size();
		final int[] result = new int[n];
		for (int i = 0; i != n; i++) {
			result[i] = list.get(i).intValue();
		}
		return result;
	}

	public static StyledCellLabelProvider createSearchLabelProvider(Supplier<String> getStr,
			Function<Object, String> toStr) {
		return new StyledCellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				String search = getStr.get();
				String cellText = toStr.apply(cell.getElement());
				cell.setText(cellText);
				if (search != null && search.length() > 0) {
					int intRangesCorrectSize[] = getSearchTermOccurrences(search, cellText);
					List<StyleRange> styleRange = new ArrayList<>();
					for (int i = 0; i < intRangesCorrectSize.length / 2; i++) {
						int start = intRangesCorrectSize[i];
						int length = intRangesCorrectSize[++i];
						StyleRange myStyledRange = new StyleRange(start, length, null, null);
						myStyledRange.font = SwtRM.getBoldFont(cell.getFont());
						styleRange.add(myStyledRange);
					}
					cell.setStyleRanges(styleRange.toArray(new StyleRange[styleRange.size()]));
				} else {
					cell.setStyleRanges(null);
				}

				super.update(cell);

			}
		};
	}

	public static void paintPreviewBackground(GC gc, Rectangle rect) {
		paintPreviewBackground(gc, rect, 32);
	}

	public static boolean isPreviewBackgroundSolidColor() {
		return _PREF_PROP_PREVIEW_IMG_PAINT_BG_TYPE.equals(PREF_VALUE_PREVIEW_IMG_PAINT_BG_TYPE_ONE_COLOR);
	}

	public static boolean isPreviewBackgroundTransparent() {
		return _PREF_PROP_PREVIEW_IMG_PAINT_BG_TYPE.equals(PREF_VALUE_PREVIEW_IMG_PAINT_BG_TYPE_TRANSPARENT);
	}

	public static boolean isPreviewBackgroundPattern() {
		return _PREF_PROP_PREVIEW_IMG_PAINT_BG_TYPE.equals(PREF_VALUE_PREVIEW_IMG_PAINT_BG_TYPE_TWO_COLORS);
	}

	public static Color get_pref_Preview_PatternColor() {
		return _PREF_PROP_PREVIEW_IMG_PAINT_BG_COLOR_1;
	}

	public static void paintPreviewBackground(GC gc, Rectangle bounds, int space) {
		switch (_PREF_PROP_PREVIEW_IMG_PAINT_BG_TYPE) {
		case PREF_VALUE_PREVIEW_IMG_PAINT_BG_TYPE_TRANSPARENT:
			break;
		case PREF_VALUE_PREVIEW_IMG_PAINT_BG_TYPE_ONE_COLOR:
			gc.setBackground(_PREF_PROP_PREVIEW_IMG_PAINT_BG_SOLID_COLOR);
			gc.fillRectangle(bounds);
			break;
		case PREF_VALUE_PREVIEW_IMG_PAINT_BG_TYPE_TWO_COLORS:
			Rectangle oldClip = gc.getClipping();
			gc.setClipping(bounds);
			gc.setBackground(_PREF_PROP_PREVIEW_IMG_PAINT_BG_COLOR_1);

			int nx = bounds.width / space + 2;
			int ny = bounds.height / space + 2;

			gc.setAlpha(60);

			for (int x = -1; x < nx; x++) {
				for (int y = 0; y < ny; y++) {
					int fx = bounds.x / space * space + x * space;
					int fy = bounds.y / space * space + y * space;
					if ((fx / space + fy / space) % 2 == 0) {
						gc.fillRectangle(fx, fy, space, space);
					}
				}
			}

			gc.setAlpha(255);
			gc.setClipping(oldClip);
			break;
		default:
			break;
		}
	}

	public static void paintPreviewMessage(GC gc, Rectangle canvasBounds, String msg) {
		Point msgSize = gc.stringExtent(msg);
		int x = canvasBounds.width / 2 - msgSize.x / 2;
		if (x < 0) {
			x = 5;
		}
		int y = canvasBounds.height / 2 - msgSize.y / 2;
		// gc.setBackground(PREVIEW_BG_DARK);
		// gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		gc.drawText(msg, x, y);
	}

	public static void swtRunRedraw(Control c) {
		swtRun(c, (c2) -> {
			c2.redraw();
		});
	}

	public static void swtRun(long delayMillis, Runnable run) {
		_runLaterExecutor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(delayMillis);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				swtRun(run);
			}
		});
	}

	private static ExecutorService _runLaterExecutor = Executors.newCachedThreadPool();

	public static void swtRun(Runnable run) {
		try {

			if (PlatformUI.getWorkbench().isClosing()) {
				return;
			}

			Display display = Display.getDefault();
			display.asyncExec(new Runnable() {

				@Override
				public void run() {
					try {
						run.run();
					} catch (SWTException e) {
						// nothing
					}
				}
			});
		} catch (SWTException e) {
			// nothing
		}
	}

	public static <T extends Control> void swtRun(final T c, Consumer<T> action) {
		Display display = Display.getDefault();

		if (display.isDisposed()) {
			return;
		}

		if (Thread.currentThread() == display.getThread()) {
			if (c.isDisposed()) {
				return;
			}

			action.accept(c);
			return;
		}

		try {
			display.asyncExec(new Runnable() {

				@Override
				public void run() {
					if (!c.isDisposed()) {
						action.accept(c);
					}
				}
			});
		} catch (SWTException e) {
			//
		}
	}

	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// e.printStackTrace();
		}
	}

	public static void openPreview(Object obj) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			PreviewView view = (PreviewView) page.showView(PreviewView.ID);
			view.preview(obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
	private static final String[] Q = new String[] { "", "k", "m", "g", "t", "P", "E" };

	public static String getFileHumanSize(long bytes) {
		for (int i = 6; i > 0; i--) {
			double step = Math.pow(1024, i);
			if (bytes > step)
				return String.format("%3.1f %sb", Double.valueOf(bytes / step), Q[i]);
		}
		return Long.toString(bytes);
	}

	public static Rectangle computeImageZoom(Rectangle srcBounds, Rectangle dstBounds) {
		int srcW = srcBounds.width;
		int srcH = srcBounds.height;
		int dstW = srcW;
		int dstH = srcH;
		double ratio = srcW / (double) srcH;
		int controlHeight = dstBounds.height;
		int controlWidth = dstBounds.width;

		if (srcW >= dstW || srcH >= dstH) {
			if (srcW > srcH && srcW > controlWidth) {
				dstW = controlWidth;
				dstH = (int) (dstW / ratio);
			} else if (srcH > controlHeight) {
				dstH = controlHeight;
				dstW = (int) (dstH * ratio);
			}

			if (dstW > controlWidth) {
				dstW = controlWidth;
				dstH = (int) (dstW / ratio);
			}

			if (dstH > controlHeight) {
				dstH = controlHeight;
				dstW = (int) (dstH * ratio);
			}
		}

		int dstX = (controlWidth - dstW) / 2;
		int dstY = (controlHeight - dstH) / 2;

		return new Rectangle(dstX, dstY, dstW, dstH);
	}

	public static void set_DND_Image(DragSourceEvent e, Image image) {
		if (image != null) {
			set_DND_Image(e, image, image.getBounds());
		}
	}

	public static void set_DND_Image(DragSourceEvent e, Image image, Rectangle src) {
		if (image == null) {
			return;
		}

		Image image2 = scaleImage_DND(image, src);

		e.image = image2;

		if (image2 != null) {
			e.offsetX = src.width / 2;
			e.offsetY = src.height / 2;
		}
	}

	public static Image scaleImage_DND(Image image, Rectangle src) {
		int maxSize = 256;
		int minSize = 64;

		int srcSize = Math.max(src.width, src.height);

		int newSize = Math.min(maxSize, srcSize);
		newSize = Math.max(newSize, minSize);

		return scaleImage(image, src, newSize, 200);
	}

	public static Image scaleImage(String filepath, Rectangle src, int newSize, BufferedImage overlay) {
		try (FileInputStream input = new FileInputStream(new File(filepath))) {
			return scaleImage(input, src, newSize, overlay);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Image scaleImage(Image image, Rectangle src, int newSize, int alpha) {
		var data1 = image.getImageData();

		var data2 = new ImageData(newSize, newSize, data1.depth, data1.palette);
		data2.alphaData = new byte[newSize * newSize];

		var scaled = new Image(Display.getDefault(), data2);

		GC gc = new GC(scaled);

		if (PhaserEditorUI.get_pref_Preview_Anitialias()) {
			gc.setAntialias(SWT.ON);
			gc.setInterpolation(SWT.ON);
		} else {
			gc.setAntialias(SWT.OFF);
			gc.setInterpolation(SWT.OFF);
		}

		Rectangle src2 = src == null ? image.getBounds() : src;

		ZoomCalculator calc = new ZoomCalculator(src2.width, src2.height);
		calc.fit(newSize, newSize);

		Rectangle z = calc.modelToView(0, 0, src2.width, src2.height);

		gc.setAlpha(alpha);
		gc.drawImage(image, src2.x, src2.y, src2.width, src2.height, z.x, z.y, z.width, z.height);

		gc.dispose();

		return scaled;
	}

	public static Image scaleImage(InputStream input, Rectangle src, int newSize, BufferedImage overlay) {
		try {
			BufferedImage swingimg = ImageIO.read(input);
			BufferedImage swingimg2 = new BufferedImage(newSize, newSize, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = swingimg2.createGraphics();
			Rectangle src2 = src == null ? new Rectangle(0, 0, swingimg.getWidth(), swingimg.getHeight()) : src;

			ZoomCalculator calc = new ZoomCalculator(src2.width, src2.height);
			calc.fit(newSize, newSize);

			Rectangle z = calc.modelToView(0, 0, src2.width, src2.height);

			g2.drawImage(swingimg, z.x, z.y, z.x + z.width, z.y + z.height, src2.x, src2.y, src2.x + src2.width,
					src2.y + src2.height, null);

			if (overlay != null) {
				g2.drawImage(overlay, 0, 0, null);
			}

			g2.dispose();

			var img = image_Swing_To_SWT(swingimg2);

			return img;
		} catch (IOException e) {
			// e.printStackTrace();
			return null;
		}
	}

	public static Image createImageFromCellRenderer(ICanvasCellRenderer renderer, int width, int height) {
		var img = createTransparentSWTImage(Display.getDefault(), width, height);
		var gc = new GC(img);
		BaseCanvas.prepareGC(gc);
		renderer.render(null, gc, 0, 0, width, height);
		gc.dispose();
		return img;
	}

	public static Image image_Swing_To_SWT(BufferedImage img) throws IOException {
		ByteArrayOutputStream memory = new ByteArrayOutputStream();
		ImageIO.write(img, "png", memory);
		Image result = new Image(Display.getCurrent(), new ByteArrayInputStream(memory.toByteArray()));
		return result;
	}

	public static String getNameFromFilename(String name) {
		String name2 = name;
		name2 = Paths.get(name).getFileName().toString();
		int i = name2.lastIndexOf(".");
		if (i > 0) {
			name2 = name2.substring(0, i);
		}
		return name2;
	}

	public static String getExtensionFromFilename(String name) {
		int i = name.lastIndexOf(".");
		if (i > 0) {
			return name.substring(i + 1, name.length());
		}
		return "";
	}

	public static void setStyle(String style, Control... controls) {
		for (Control control : controls) {
			control.setData("org.eclipse.e4.ui.css.CssClassName", style);
		}
	}

	public static Image getFileImage(IFile file) {
		try (InputStream contents = file.getContents();) {
			Image img = getStreamImage(contents);
			return img;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Image getFileImage(Path file) {
		try (InputStream contents = Files.newInputStream(file)) {
			Image img = getStreamImage(contents);
			return img;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Image getStreamImage(InputStream stream) throws IOException {
		try {
			Display display = Display.getCurrent();
			ImageData data = new ImageData(stream);
			if (data.transparentPixel > 0) {
				return new Image(display, data, data.getTransparencyMask());
			}
			return new Image(display, data);
		} finally {
			stream.close();
		}
	}

	public static Rectangle getImageBounds(String filepath) {

		if (filepath == null) {
			return new Rectangle(0, 0, 1, 1);
		}

		try (FileImageInputStream input = new FileImageInputStream(new File(filepath))) {
			ImageReader reader = ImageIO.getImageReaders(input).next();
			reader.setInput(input);
			int w = reader.getWidth(0);
			int h = reader.getHeight(0);
			return new Rectangle(0, 0, w, h);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Rectangle getImageBounds(IFile file) {
		return getImageBounds(file == null ? null : file.getLocation().toPortableString());
	}

	public static String getUIIconURL(String icon) {
		return "platform:/plugin/phasereditor.ui/" + icon;
	}

	public static Image makeColorIcon(RGB rgb) {
		RGB black = new RGB(0, 0, 0);
		PaletteData dataPalette = new PaletteData(new RGB[] { black, black, rgb });
		ImageData data = new ImageData(16, 16, 4, dataPalette);
		data.transparentPixel = 0;

		int start = 3;
		int end = 16 - start;
		for (int y = start; y <= end; y++) {
			for (int x = start; x <= end; x++) {
				if (x == start || y == start || x == end || y == end) {
					data.setPixel(x, y, 1);
				} else {
					data.setPixel(x, y, 2);
				}
			}
		}

		Image img = new Image(Display.getCurrent(), data);

		return img;
	}

	public static void setTreeBackgroundColor(Color bgColor, Control control) {
		control.setBackground(bgColor);
		if (control instanceof Composite) {
			Control[] list = ((Composite) control).getChildren();
			for (Control c : list) {
				setTreeBackgroundColor(bgColor, c);
			}
		}
	}

	public static Color getMainWindowColor() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getBackground();
	}

	private static Map<Class<?>, RGB> _clsStyleMap = new HashMap<>();

	@SuppressWarnings("boxing")
	public static void forceApplyControlStyle(Composite target, Class<?> templateClass) {
		var fg = _clsStyleMap.get(templateClass);

		if (fg != null) {

			target.setForeground(SwtRM.getColor(fg));

			return;
		}

		target.getDisplay().asyncExec(() -> {
			Shell shell = new Shell();

			try {

				var ctr = templateClass.getConstructor(Composite.class, int.class);
				var temp = (Control) ctr.newInstance(shell, target.getStyle());

				PhaserEditorUI.applyThemeStyle(shell);

				var fgColor = temp.getForeground();

				_clsStyleMap.put(templateClass, fgColor.getRGB());

				if (!target.isDisposed()) {
					target.setForeground(fgColor);
					target.redraw();
				}

				temp.dispose();

			} catch (Exception e) {
				e.printStackTrace();
			}

			shell.close();
			shell.dispose();
		});
	}

	public static ListSelectionDialog createFilteredListSelectionDialog(Shell shell, Object input,
			IStructuredContentProvider contentProvider, ILabelProvider labelProvider, String message) {
		ListSelectionDialog dlg = new ListSelectionDialog(shell, input, contentProvider, labelProvider, message) {

			@Override
			protected Label createMessageArea(Composite composite) {
				Label area = super.createMessageArea(composite);
				Text text = new Text(composite, SWT.SEARCH);
				text.setText("type filter text");
				text.selectAll();
				GridData data = new GridData(GridData.FILL_HORIZONTAL);
				text.setLayoutData(data);
				text.addModifyListener(new ModifyListener() {

					@SuppressWarnings({ "synthetic-access" })
					@Override
					public void modifyText(ModifyEvent e) {
						String query = "*" + text.getText().toLowerCase().trim() + "*";
						StringMatcher matcher = new StringMatcher(query, true, false);
						CheckboxTableViewer tableViewer = getViewer();
						tableViewer.setFilters(new ViewerFilter() {

							@Override
							public boolean select(Viewer viewer, Object parentElement, Object element) {
								if (query.length() == 0) {
									return true;
								}
								return matcher.match((String) element);
							}
						});
						tableViewer.setCheckedElements(_checkedElements.toArray());
					}
				});

				return area;
			}

			LinkedHashSet<Object> _checkedElements = new LinkedHashSet<>();

			@Override
			protected Control createDialogArea(Composite parent) {
				Composite area = (Composite) super.createDialogArea(parent);

				Control[] children = area.getChildren();
				Control selectButtonsComp = children[children.length - 1];
				selectButtonsComp.dispose();

				getViewer().addCheckStateListener(new ICheckStateListener() {

					@Override
					public void checkStateChanged(CheckStateChangedEvent event) {
						Object element = event.getElement();
						if (event.getChecked()) {
							_checkedElements.add(element);
						} else {
							_checkedElements.remove(element);
						}
					}
				});

				return area;
			}

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void setInitialElementSelections(List selectedElements) {
				super.setInitialElementSelections(selectedElements);
				_checkedElements.addAll(selectedElements);
			}

			@Override
			protected void okPressed() {
				// sort checked elements
				List<?> list = ((List<?>) getViewer().getInput()).stream().filter(e -> _checkedElements.contains(e))
						.collect(Collectors.toList());
				setResult(list);
				setReturnCode(OK);
				close();
			}

		};
		return dlg;
	}

	public static IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	public static void redrawCanvasWhenPreferencesChange(Canvas canvas) {
		IPropertyChangeListener listener = e -> canvas.redraw();
		PhaserEditorUI.getPreferenceStore().addPropertyChangeListener(listener);
		canvas.addDisposeListener(e -> {
			PhaserEditorUI.getPreferenceStore().removePropertyChangeListener(listener);
		});
	}

	public static void refreshViewerWhenPreferencesChange(IPreferenceStore store, ContentViewer... viewers) {
		IPropertyChangeListener listener = e -> {
			for (ContentViewer viewer : viewers) {
				viewer.refresh();
			}
		};
		store.addPropertyChangeListener(listener);
		viewers[0].getControl().addDisposeListener(e -> {
			store.removePropertyChangeListener(listener);
		});
	}

	public static String imageToBase64(Image image) {
		if (image == null) {
			return "";
		}

		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {

			ImageLoader loader = new ImageLoader();

			loader.data = new ImageData[] { image.getImageData() };

			loader.save(output, SWT.IMAGE_PNG);

			String base64 = Base64.getEncoder().encodeToString(output.toByteArray());

			return base64;
		} catch (Exception e) {
			return "";
		}
	}

	public static int[] getImageSize(File file) throws IOException {
		String[] split = file.getName().split("\\.");
		String ext = split[split.length - 1];

		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(ext.toUpperCase());

		while (readers.hasNext()) {
			var reader = readers.next();
			try (FileImageInputStream fis = new FileImageInputStream(file)) {

				reader.setInput(fis);

				int width = reader.getWidth(reader.getMinIndex());
				int height = reader.getHeight(reader.getMinIndex());

				return new int[] { width, height };
			}
		}

		return null;
	}

	public static void paintListItemBackground(GC gc, int i, Rectangle rect) {
		paintListItemBackground(gc, i, rect.x, rect.y, rect.width, rect.height);
	}

	public static void paintListItemBackground(GC gc, int i, int x, int y, int w, int h) {
		if (i % 2 == 0) {
			gc.setBackground(PhaserEditorUI.getListTextColor());
			// gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
		} else {
			gc.setBackground(PhaserEditorUI.getListBackgroundColor());
			// gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
		}

		gc.setAlpha(10);
		gc.fillRectangle(x, y, w, h);
		gc.setAlpha(255);

	}

	public static void paintScaledImageInArea(GC gc, Image image, FrameData fd, Rectangle renderArea) {
		paintScaledImageInArea(gc, image, fd, renderArea, true, true);
	}

	public static void paintScaledImageInArea(GC gc, Image image, FrameData fd, Rectangle renderArea,
			boolean blankSpaces, boolean center) {

		int frameHeight = renderArea.height;
		int frameWidth = renderArea.width;

		double imgW = blankSpaces ? fd.srcSize.x : fd.src.width;
		double imgH = blankSpaces ? fd.srcSize.y : fd.src.height;

		// compute the right width
		imgW = imgW * (frameHeight / imgH);
		imgH = frameHeight;

		// fix width if it goes beyond the area
		if (imgW > frameWidth) {
			imgH = imgH * (frameWidth / imgW);
			imgW = frameWidth;
		}

		double scaleX = imgW / (blankSpaces ? fd.srcSize.x : fd.src.width);
		double scaleY = imgH / (blankSpaces ? fd.srcSize.y : fd.src.height);

		var imgX = renderArea.x + (center ? frameWidth / 2 - imgW / 2 : 0) + (blankSpaces ? fd.dst.x : 0) * scaleX;
		var imgY = renderArea.y + frameHeight / 2 - imgH / 2 + (blankSpaces ? fd.dst.y : 0) * scaleY;

		double imgDstW = (blankSpaces ? fd.dst.width : fd.src.width) * scaleX;
		double imgDstH = (blankSpaces ? fd.dst.height : fd.src.height) * scaleY;

		if (imgDstW > 0 && imgDstH > 0) {
			gc.drawImage(image, fd.src.x, fd.src.y, fd.src.width, fd.src.height, (int) imgX, (int) imgY, (int) imgDstW,
					(int) imgDstH);
		}
	}

	public static void paintIconHoverBackground(GC gc, Canvas canvas, int btnSize, Rectangle btnArea) {
		gc.setAlpha(20);
		gc.setBackground(canvas.getForeground());
		gc.fillRoundRectangle(btnArea.x, btnArea.y, btnArea.width, btnArea.height, btnSize / 2, btnSize / 2);
		gc.setAlpha(40);
		gc.drawRoundRectangle(btnArea.x, btnArea.y, btnArea.width, btnArea.height, btnSize / 2, btnSize / 2);
		gc.setAlpha(255);
	}

	public static float distance(float[] a, float[] b) {
		return distance(a[0], a[1], b[0], b[1]);
	}

	public static float distance(float x1, float y1, float x2, float y2) {
		double a = x2 - x1;
		double b = y2 - y1;
		return (float) Math.sqrt(a * a + b * b);
	}

	public static float angle(float x1, float y1, float x2, float y2) {

		final double delta = (x1 * x2 + y1 * y2) / Math.sqrt((x1 * x1 + y1 * y1) * (x2 * x2 + y2 * y2));

		if (delta > 1.0) {
			return 0.0f;
		}
		if (delta < -1.0) {
			return 180.0f;
		}

		return (float) Math.toDegrees(Math.acos(delta));

	}

	public static float angle(float[] a, float[] b) {
		return angle(a[0], a[1], b[0], b[1]);
	}

	public static float[] vector(float[] p1, float[] p2) {
		var vector = new float[] { p2[0] - p1[0], p2[1] - p1[1] };

		var d = PhaserEditorUI.distance(0, 0, vector[0], vector[1]);
		vector[0] /= d;
		vector[1] /= d;

		return vector;
	}

	/**
	 * Rotates the Vector2 by 90 degrees in the specified direction, where >= 0 is
	 * counter-clockwise and < 0 is clockwise.
	 */
	public static float[] rotate90(float[] vector, int dir) {
		var result = new float[] { vector[0], vector[1] };

		var x = vector[0];
		var y = vector[1];

		if (dir >= 0) {
			result[0] = -y;
			result[1] = x;
		} else {
			result[0] = y;
			result[1] = -x;
		}

		return result;
	}

	public static float[] unitarianVector(float[] vector) {
		var d = distance(0, 0, vector[0], vector[1]);
		return new float[] { vector[0] / d, vector[1] / d };
	}

	/**
	 * 
	 * This always create a white image, with no transparency. Instead, use a
	 * BufferedImage with the final content and then convert it to an SWT Image.
	 * 
	 * @param device
	 * @param width
	 * @param height
	 * @return
	 */
	@Deprecated
	public static Image createTransparentSWTImage(Device device, int width, int height) {
		return new Image(device, width, height);
	}

	public static boolean isZoomEvent(ZoomCanvas canvas, Event e) {
		return isZoomEvent(canvas.isHandModeActivated(), e);
	}

	public static boolean isZoomEvent(HandModeUtils utils, Event e) {
		return isZoomEvent(utils.isActivated(), e);
	}

	private static boolean isZoomEvent(boolean handModeActivated, Event e) {
		return e.stateMask != 0 || handModeActivated;
	}

	public final static ScriptEngine scriptEngine;

	static {
		scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
	}

	public static Double scriptEngineEval(String value) {
		try {
			return Double.valueOf(value);
		} catch (NumberFormatException e) {
			try {
				Object result = scriptEngine.eval(value);
				return Double.valueOf(((Number) result).doubleValue());
			} catch (ScriptException e1) {
				throw new RuntimeException(e1);
			}
		}
	}

	public static String scriptEngineValidate(Object value) {
		if (value instanceof String) {
			String script = (String) value;
			try {
				Double.parseDouble(script);
			} catch (NumberFormatException e) {
				// try a javascript expression
				try {
					Object result = scriptEngine.eval(script);
					if (!(result instanceof Number)) {
						return "Invalid expression result.";
					}
				} catch (ScriptException e1) {
					return "Invalid number or script format.";
				}
			}
		}
		return null;
	}

	public static void makeScreenshot(Control control, Path writeTo) {
		control.setRedraw(false);
		var size = control.getBounds();
		var img = new Image(control.getDisplay(), size.width, size.height);
		try {

			var gc = new GC(img);

			control.print(gc);

			gc.dispose();

			control.setRedraw(true);

			saveImage(writeTo, img);

		} catch (IOException e) {
			logError(e);
		} finally {
			img.dispose();
		}
	}

	public static void saveImage(Path writeTo, Image img) throws IOException {
		var loader = new ImageLoader();
		loader.data = new ImageData[] { img.getImageData() };

		Files.createDirectories(writeTo.getParent());
		try (var os = Files.newOutputStream(writeTo)) {
			loader.save(os, SWT.IMAGE_PNG);
		}
	}
}
