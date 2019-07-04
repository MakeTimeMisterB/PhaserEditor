// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
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
package phasereditor.scene.core;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;
import org.json.JSONObject;
import org.json.JSONTokener;

import phasereditor.lic.LicCore;
import phasereditor.project.core.codegen.SourceLang;
import phasereditor.ui.Colors;

/**
 * @author arian
 *
 */
public class SceneModel {

	private static final int VERSION = 1;

	public static final String[] GAME_OBJECT_TYPES = {

			SpriteModel.TYPE,

			ImageModel.TYPE,

			TileSpriteModel.TYPE,

			BitmapTextModel.TYPE,

			DynamicBitmapTextModel.TYPE,

			ContainerModel.TYPE

	};

	private DisplayListModel _displayList;
	private GroupsModel _groupsModel;

	private boolean _snapEnabled;
	private int _snapWidth;
	private int _snapHeight;

	private boolean _generateMethodEvents;
	private boolean _onlyGenerateMethods;
	private String _superClassName;
	private String _preloadMethodName;
	private String _createMethodName;
	private String _sceneKey;
	private SourceLang _compilerLang;
	private boolean _debugPaintCalls;

	public enum MethodContextType {
		SCENE, OBJECT
	}

	private MethodContextType _methodContextType;

	private RGB _backgroundColor;
	private RGB _foregroundColor;
	private static final RGB DEF_FG_RGB = Colors.WHITESMOKE.rgb;
	private static final RGB DEF_BG_RGB = Colors.LIGHTGRAY.rgb;

	private int _borderX;
	private int _borderY;
	private int _borderWidth;
	private int _borderHeight;

	public SceneModel() {
		_displayList = new DisplayListModel();
		_groupsModel = new GroupsModel(this);

		_snapEnabled = false;
		_snapWidth = 16;
		_snapHeight = 16;

		_backgroundColor = DEF_BG_RGB;
		_foregroundColor = DEF_FG_RGB;

		_borderX = 0;
		_borderY = 0;
		_borderWidth = 800;
		_borderHeight = 600;

		_generateMethodEvents = false;
		_onlyGenerateMethods = false;

		_preloadMethodName = "";
		_createMethodName = "create";
		_superClassName = "Phaser.Scene";
		_methodContextType = MethodContextType.SCENE;

		_sceneKey = "";

		_compilerLang = SourceLang.JAVA_SCRIPT_6;

		_debugPaintCalls = false;
	}

	public boolean isDebugPaintCalls() {
		return _debugPaintCalls;
	}
	
	public void setDebugPaintCalls(boolean debugPaintCalls) {
		_debugPaintCalls = debugPaintCalls;
	}
	
	public SourceLang getCompilerLang() {
		return _compilerLang;
	}

	public void setCompilerLang(SourceLang compilerLang) {
		_compilerLang = compilerLang;
	}

	public boolean isOnlyGenerateMethods() {
		return _onlyGenerateMethods;
	}

	public void setOnlyGenerateMethods(boolean onlyGenerateMethods) {
		_onlyGenerateMethods = onlyGenerateMethods;
	}

	public String getSuperClassName() {
		return _superClassName;
	}

	public void setSuperClassName(String superClassName) {
		_superClassName = superClassName;
	}

	public String getCreateMethodName() {
		return _createMethodName;
	}

	public void setCreateMethodName(String createMethodName) {
		_createMethodName = createMethodName;
	}

	public String getPreloadMethodName() {
		return _preloadMethodName;
	}

	public void setPreloadMethodName(String preloadMethodName) {
		_preloadMethodName = preloadMethodName;
	}

	public boolean isGenerateMethodEvents() {
		return _generateMethodEvents;
	}

	public void setGenerateMethodEvents(boolean generateMethodEvents) {
		_generateMethodEvents = generateMethodEvents;
	}

	public MethodContextType getMethodContextType() {
		return _methodContextType;
	}

	public void setMethodContextType(MethodContextType methodContextType) {
		_methodContextType = methodContextType;
	}

	public String getSceneKey() {
		return _sceneKey;
	}

	public void setSceneKey(String sceneKey) {
		_sceneKey = sceneKey;
	}

	public int getBorderY() {
		return _borderY;
	}

	public void setBorderY(int borderY) {
		_borderY = borderY;
	}

	public int getBorderX() {
		return _borderX;
	}

	public void setBorderX(int borderX) {
		_borderX = borderX;
	}

	public int getBorderWidth() {
		return _borderWidth;
	}

	public void setBorderWidth(int borderWidth) {
		_borderWidth = borderWidth;
	}

	public int getBorderHeight() {
		return _borderHeight;
	}

	public void setBorderHeight(int borderHeight) {
		_borderHeight = borderHeight;
	}

	public RGB getBackgroundColor() {
		return _backgroundColor;
	}

	public void setBackgroundColor(RGB backgroundColor) {
		_backgroundColor = backgroundColor;
	}

	public RGB getForegroundColor() {
		return _foregroundColor;
	}

	public void setForegroundColor(RGB foregroundColor) {
		_foregroundColor = foregroundColor;
	}

	public boolean isSnapEnabled() {
		return _snapEnabled;
	}

	public void setSnapEnabled(boolean snapEnabled) {
		_snapEnabled = snapEnabled;
	}

	public int getSnapWidth() {
		return _snapWidth;
	}

	public void setSnapWidth(int snapWidth) {
		_snapWidth = snapWidth;
	}

	public int getSnapHeight() {
		return _snapHeight;
	}

	public void setSnapHeight(int snapHeight) {
		_snapHeight = snapHeight;
	}

	public float snapValueX(float x) {

		if (_snapEnabled) {
			return Math.round(x / _snapWidth) * _snapWidth;
		}

		return x;
	}

	public float snapValueY(float y) {

		if (_snapEnabled) {

			return Math.round(y / _snapHeight) * _snapHeight;
		}

		return y;
	}

	public DisplayListModel getDisplayList() {
		return _displayList;
	}

	public GroupsModel getGroupsModel() {
		return _groupsModel;
	}

	public void write(JSONObject data) {
		data.put("-app", "Scene Editor - " + LicCore.PRODUCT_NAME);
		data.put("-version", VERSION);

		{
			data.put("compilerLang", _compilerLang.name());
		}
		{
			// Display List

			JSONObject displayListData;
			if (_displayList == null) {
				displayListData = null;
			} else {
				displayListData = new JSONObject();
				_displayList.write(displayListData);
			}

			data.put("displayList", displayListData);
		}

		{
			// Groups

			JSONObject groupsData;
			groupsData = new JSONObject();
			_groupsModel.write(groupsData);

			data.put("groups", groupsData);
		}

		writeProperties(data);
	}

	public void read(IFile file) throws Exception {
		try (InputStream contents = file.getContents();) {
			String charset = file.getCharset();
			if (charset == null) {
				charset = "UTF-8";
			}
			InputStreamReader reader = new InputStreamReader(contents, charset);
			JSONObject data = new JSONObject(new JSONTokener(reader));

			read(data, file.getProject());
		}
	}

	public void save(IFile file, IProgressMonitor monitor) throws Exception {
		JSONObject data = new JSONObject();

		write(data);

		Charset charset;

		if (file.exists()) {
			charset = Charset.forName(file.getCharset());
		} else {
			charset = Charset.forName("UTF-8");
		}

		String content = data.toString(2);

		var stream = new ByteArrayInputStream(content.getBytes(charset));

		if (file.exists()) {
			file.setContents(stream, IResource.NONE, monitor);
		} else {
			file.create(stream, false, monitor);
			file.setCharset(charset.name(), monitor);
		}
	}

	public void read(JSONObject data, IProject project) {
		{
			_compilerLang = SourceLang.valueOf(data.optString("compilerLang", SourceLang.JAVA_SCRIPT_6.name()));
		}
		{
			var displayListData = data.optJSONObject("displayList");

			_displayList = new DisplayListModel();
			_displayList.read(displayListData, project);

			readProperties(data);
		}

		{
			var groupsData = data.optJSONObject("groups");

			_groupsModel = new GroupsModel(this);
			if (groupsData != null) {
				_groupsModel.read(groupsData, project);
			}
		}
		{
			_debugPaintCalls = data.optBoolean("debugPaintCalls", false);
		}
	}

	public void writeProperties(JSONObject data) {
		{
			data.put("snapEnabled", _snapEnabled, false);
			data.put("snapWidth", _snapWidth, 16);
			data.put("snapHeight", _snapHeight, 16);
		}

		{
			data.put("backgroundColor", asString(_backgroundColor), asString(DEF_BG_RGB));
			data.put("foregroundColor", asString(_foregroundColor), asString(DEF_FG_RGB));
		}

		{
			data.put("borderX", _borderX, 0);
			data.put("borderY", _borderY, 0);
			data.put("borderWidth", _borderWidth, 800);
			data.put("borderHeight", _borderHeight, 600);
		}

		{
			data.put("generateMethodEvents", _generateMethodEvents, false);
			data.put("onlyGenerateMethods", _onlyGenerateMethods, false);
			data.put("superClassName", _superClassName, "Phaser.Scene");
			data.put("preloadMethodName", _preloadMethodName, "");
			data.put("createMethodName", _createMethodName, "create");
			data.put("methodContextType", _methodContextType.name());
			data.put("sceneKey", _sceneKey, "");
		}

		{
			data.put("debugPaintCalls", _debugPaintCalls, false);
		}
	}

	public void readProperties(JSONObject data) {
		{
			_snapEnabled = data.optBoolean("snapEnabled", false);
			_snapWidth = data.optInt("snapWidth", 16);
			_snapHeight = data.optInt("snapHeight", 16);
		}

		{
			_backgroundColor = asRGB(data.optString("backgroundColor", asString(DEF_BG_RGB)));
			_foregroundColor = asRGB(data.optString("foregroundColor", asString(DEF_FG_RGB)));
		}

		{
			_borderX = data.optInt("borderX", 0);
			_borderY = data.optInt("borderY", 0);
			_borderWidth = data.optInt("borderWidth", 800);
			_borderHeight = data.optInt("borderHeight", 600);
		}

		{
			_generateMethodEvents = data.optBoolean("generateMethodEvents", false);
			_onlyGenerateMethods = data.optBoolean("onlyGenerateMethods", false);
			_superClassName = data.optString("superClassName", "Phaser.Scene");
			{
				_preloadMethodName = data.optString("preloadMethodName", "");
				var autoLoadAssets = data.optBoolean("autoLoadAssets", true);
				if (!autoLoadAssets) {
					if (_preloadMethodName.trim().length() > 0) {
						_preloadMethodName = "";
					}
				}
			}
			_createMethodName = data.optString("createMethodName", "create");
			_methodContextType = MethodContextType
					.valueOf(data.optString("methodContextType", MethodContextType.SCENE.name()));
			_sceneKey = data.optString("sceneKey", "");
		}
	}

	private static String asString(RGB color) {
		return StringConverter.asString(color);
	}

	private static RGB asRGB(String color) {
		return StringConverter.asRGB(color);
	}

	@SuppressWarnings("incomplete-switch")
	public static ObjectModel createModel(String type) {

		switch (type) {
		case ContainerModel.TYPE:
			return new ContainerModel();

		case SpriteModel.TYPE:
			return new SpriteModel();

		case ImageModel.TYPE:
			return new ImageModel();

		case TileSpriteModel.TYPE:
			return new TileSpriteModel();

		case BitmapTextModel.TYPE:
			return new BitmapTextModel();

		case DynamicBitmapTextModel.TYPE:
			return new DynamicBitmapTextModel();

		case TextModel.TYPE:
			return new TextModel();
		}

		return null;
	}

	public JSONObject toJSON() {

		JSONObject data = new JSONObject();

		write(data);

		return data;
	}

}
