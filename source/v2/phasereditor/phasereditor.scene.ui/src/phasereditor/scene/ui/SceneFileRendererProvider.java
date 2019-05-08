// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.scene.ui;

import org.eclipse.core.resources.IFile;

import phasereditor.project.ui.IFileRendererProvider;
import phasereditor.scene.core.SceneCore;
import phasereditor.ui.BaseTreeCanvasItemRenderer;
import phasereditor.ui.ImageProxy;
import phasereditor.ui.ImageProxyTreeCanvasItemRenderer;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;

/**
 * @author arian
 *
 */
public class SceneFileRendererProvider implements IFileRendererProvider {

	@Override
	public BaseTreeCanvasItemRenderer createRenderer(TreeCanvasItem item) {
		var file = (IFile) item.getData();
		if (SceneCore.isSceneFile(file)) {
			var screenshotFile = SceneUI.getSceneScreenshotFile((IFile) item.getData(), false);

			if (screenshotFile != null) {
				var proxy = ImageProxy.get(screenshotFile.toFile(), null);
				if (proxy != null) {
					return new ImageProxyTreeCanvasItemRenderer(item, proxy);
				}
			}
		}

		return null;
	}

}
