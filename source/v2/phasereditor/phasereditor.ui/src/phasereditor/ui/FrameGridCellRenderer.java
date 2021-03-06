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
package phasereditor.ui;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

/**
 * @author arian
 *
 */
public class FrameGridCellRenderer implements ICanvasCellRenderer {

	private IFrameProvider _provider;
	private int _maxCount;

	public FrameGridCellRenderer(IFrameProvider provider) {
		this(provider, Integer.MAX_VALUE);
	}

	public FrameGridCellRenderer(IFrameProvider provider, int maxCount) {
		super();
		_provider = provider;
		_maxCount = maxCount;
	}

	@Override
	public void render(Canvas canvas, GC gc, int x, int y, int width, int height) {
		var realCount = _provider.getFrameCount();
		var frameCount = realCount;

		if (frameCount == 0) {
			return;
		}

		float step = 1;

		if (frameCount > _maxCount) {
			step = (float) frameCount / _maxCount;
			frameCount = _maxCount;
		}

		var size = (int) (Math.sqrt(width * height / frameCount) * 0.9);

		var cols = width / size;
		var rows = frameCount / cols + (frameCount % cols == 0 ? 0 : 1);
		var marginX = Math.max(0, (width - cols * size) / 2);
		var marginY = Math.max(0, (height - rows * size) / 2);

		var itemX = 0;
		var itemY = 0;

		int startX = x + marginX;
		int startY = y + marginY;

		for (var i = 0; i < frameCount; i++) {
			var proxy = _provider.getFrameImageProxy(Math.min(realCount - 1, Math.round(i * step)));

			if (itemY + size > height) {
				break;
			}
			
			if (proxy != null) {
				// PhaserEditorUI.paintScaledImageInArea(gc, image, fd,
				// new Rectangle(startX + itemX, startY + itemY, size, size));
				proxy.paintScaledInArea(gc, new Rectangle(startX + itemX, startY + itemY, size, size));
			}

			itemX += size;

			if (itemX + size > width) {
				itemY += size;
				itemX = 0;
			}
		}
	}

}
