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
package phasereditor.scene.ui.build;

import static java.lang.System.out;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import org.eclipse.swt.browser.Browser;
import org.eclipse.ui.PlatformUI;

import phasereditor.webrun.core.WebRunCore;

public class HeadlessSceneScreenshotBrowser {

	public static void start() {
		out.println("HeadlessSceneScreenshotBrowser.start() -> Create Browser");

		var shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		shell.addDisposeListener(e -> {
			out.println("HeadlessSceneScreenshotBrowser -> Browser disposed.");
			// restart the browser again, if the IDE is not closing
			if (!PlatformUI.getWorkbench().isClosing()) {
				swtRun(() -> start());
			}
		});

		var browser = new Browser(shell, 0);
		browser.setBounds(-100, -100, 100, 100);

		shell.open();

		var port = WebRunCore.getServerPort();
		var url = "http://localhost:" + port
				+ "/extension/phasereditor.scene.ui.editor.html/sceneEditor/screenshot.html?channel="
				+ SceneScreenshotBuildParticipant2.SOCKET_CHANNEL;

		browser.setUrl(url);
	}
}