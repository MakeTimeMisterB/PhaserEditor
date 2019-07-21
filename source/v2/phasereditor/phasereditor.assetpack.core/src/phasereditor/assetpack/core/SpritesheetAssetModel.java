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
package phasereditor.assetpack.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.json.JSONException;
import org.json.JSONObject;

import phasereditor.ui.FrameData;
import phasereditor.ui.PhaserEditorUI;

public class SpritesheetAssetModel extends AssetModel {

	private String _url;
	private int _frameWidth;
	private int _frameHeight;
	private int _margin;
	private int _startFrame;
	private int _endFrame;
	private int _spacing;
	private String _normalMap;

	private List<FrameModel> _frames;
	private Rectangle _imageSize;

	{
		_startFrame = 0;
		_endFrame = -1;
		_margin = 0;
		_spacing = 0;
		_frames = null;
	}

	public SpritesheetAssetModel(JSONObject jsonData, AssetSectionModel section) throws JSONException {
		super(jsonData, section);
		readInfo(jsonData);
	}

	@Override
	public void readInfo(JSONObject jsonData) {
		_url = jsonData.optString("url", null);

		JSONObject jsonFrameConfig = jsonData.optJSONObject("frameConfig");
		if (jsonFrameConfig != null) {
			_frameWidth = jsonFrameConfig.optInt("frameWidth", 0);
			_frameHeight = jsonFrameConfig.optInt("frameHeight", 0);
			_startFrame = jsonFrameConfig.optInt("startFrame", 0);
			_endFrame = jsonFrameConfig.optInt("endFrame", -1);
			_margin = jsonFrameConfig.optInt("margin", 0);
			_spacing = jsonFrameConfig.optInt("spacing", 0);
		}
	}

	public SpritesheetAssetModel(String key, AssetSectionModel section) throws JSONException {
		super(key, AssetType.spritesheet, section);
	}

	@Override
	protected void writeParameters(JSONObject obj) {
		super.writeParameters(obj);
		obj.put("url", _url);
		JSONObject jsonFrameConfig = new JSONObject();
		obj.put("frameConfig", jsonFrameConfig);
		jsonFrameConfig.put("frameWidth", _frameWidth);
		jsonFrameConfig.put("frameHeight", _frameHeight);
		jsonFrameConfig.put("startFrame", _startFrame, 0);
		jsonFrameConfig.put("endFrame", _endFrame, -1);
		jsonFrameConfig.put("margin", _margin);
		jsonFrameConfig.put("spacing", _spacing);
	}

	public IFile getUrlFile() {
		return getFileFromUrl(_url);
	}

	public String getUrl() {
		return _url;
	}

	public void setUrl(String url) {
		_url = url;
	}

	public String getNormalMap() {
		return _normalMap;
	}

	public void setNormalMap(String normalMap) {
		_normalMap = normalMap;
	}

	public IFile getNormalMapFile() {
		return getFileFromUrl(_normalMap);
	}

	@Override
	public IFile[] computeUsedFiles() {
		if (_normalMap == null || _normalMap.length() == 0) {
			return new IFile[] { getUrlFile() };
		}
		return new IFile[] { getUrlFile(), getNormalMapFile() };
	}

	public int getFrameWidth() {
		return _frameWidth;
	}

	public void setFrameWidth(int frameWidth) {
		_frameWidth = frameWidth;
	}

	public int getFrameHeight() {
		return _frameHeight;
	}

	public void setFrameHeight(int frameHeight) {
		_frameHeight = frameHeight;
	}

	public int getStartFrame() {
		return _startFrame;
	}

	public void setStartFrame(int startFrame) {
		_startFrame = startFrame;
	}

	public int getEndFrame() {
		return _endFrame;
	}

	public void setEndFrame(int endFrame) {
		_endFrame = endFrame;
	}

	public void setFrames(List<FrameModel> frames) {
		_frames = frames;
	}

	public int getMargin() {
		return _margin;
	}

	public void setMargin(int margin) {
		_margin = margin;
	}

	public int getSpacing() {
		return _spacing;
	}

	public void setSpacing(int spacing) {
		_spacing = spacing;
	}

	public List<FrameModel> getFrames() {
		if (_frames == null) {
			buildFrames();
		}
		return _frames;
	}

	@Override
	public List<? extends IAssetElementModel> getSubElements() {
		return getFrames();
	}

	@Override
	public void internalBuild(List<IStatus> problems) {
		validateUrl(problems, "url", _url);

		_imageSize = null;

		buildFrames();
	}

	public static class FrameModel implements IAssetElementModel, IAssetFrameModel {
		public Rectangle _bounds;
		private SpritesheetAssetModel _asset;
		private int _index;
		private int _row;
		private int _column;

		public FrameModel(SpritesheetAssetModel asset, int index, int row, int column, Rectangle bounds) {
			super();
			_asset = asset;
			_index = index;
			_row = row;
			_column = column;
			_bounds = bounds;
		}

		public int getColumn() {
			return _column;
		}

		public int getRow() {
			return _row;
		}

		@Override
		public FrameData getFrameData() {
			FrameData data = new FrameData(_index);
			data.src = _bounds;
			data.dst = new Rectangle(0, 0, _bounds.width, _bounds.height);
			data.srcSize = new Point(_bounds.width, _bounds.height);
			return data;
		}

		@Override
		public IFile getImageFile() {
			return _asset.getUrlFile();
		}

		public int getIndex() {
			return _index;
		}

		public Rectangle getBounds() {
			return _bounds;
		}

		@Override
		public String getName() {
			return Integer.toString(_index);
		}

		@Override
		public SpritesheetAssetModel getAsset() {
			return _asset;
		}
	}

	public Rectangle getImageSize() {
		var file = getUrlFile();
		if (file != null && file.exists()) {
			Rectangle size = PhaserEditorUI.getImageBounds(file);
			_imageSize = size;
		}
		return _imageSize;
	}

	private void buildFrames() {
		// taken from AssetPackUI.generateSpriteSheetRects(...) method.

		List<FrameModel> list = new ArrayList<>();
		try {
			int w = getFrameWidth();
			int h = getFrameHeight();
			int margin = getMargin();
			int spacing = getSpacing();

			if (w <= 0 || h <= 0 || spacing < 0 || margin < 0) {
				// invalid parameters
				return;
			}

			IFile file = getUrlFile();
			if (file == null) {
				return;
			}

			Rectangle b;
			try (InputStream contents = file.getContents()) {
				ImageData data = new ImageData(contents);
				b = new Rectangle(0, 0, data.width, data.height);
			} catch (IOException | CoreException e) {
				e.printStackTrace();
				return;
			}

			int start = getStartFrame() < 0 ? 0 : getStartFrame();
			int end = getEndFrame() < 0 ? Integer.MAX_VALUE : getEndFrame();

			int i = 0;
			int row = 0;
			int column = 0;
			int x = margin;
			int y = margin;
			while (true) {
				if (i > end || y >= b.height) {
					break;
				}

				if (i >= start) {
					if (x + w <= b.width && y + h <= b.height) {
						FrameModel frame = new FrameModel(this, i, row, column, new Rectangle(x, y, w, h));
						list.add(frame);
					}
				}

				column++;

				x += w + spacing;

				if (x >= b.width) {
					x = margin;
					y += h + spacing;
					column = 0;
					row++;
				}

				i++;
			}
		} finally {
			_frames = list;
		}
	}

	@Override
	public void fileChanged(IFile file, IFile newFile) {
		String url = getUrlFromFile(file);
		if (url.equals(_url)) {
			_url = getUrlFromFile(newFile);
		}
	}
}
