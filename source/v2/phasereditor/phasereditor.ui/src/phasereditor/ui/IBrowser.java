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
package phasereditor.ui;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @author arian
 *
 */
public interface IBrowser {

	public Control getControl();

	public void setUrl(String url);

	public void setText(String text);

	public boolean execute(String script);

	public Object createFunction(String name, IBrowserFunction function);
	
	public void addLocationListener(LocationListener listener);

	public interface IBrowserFunction {
		public Object function(Object[] arguments);
	}

	public static IBrowser create(Composite parent, int style) {
		if (PhaserEditorUI.isUsingChromium()) {
			return createChromiumBrowser(parent, style);
		}

		return createDefaultBrowser(parent, style);
	}

	private static IBrowser createDefaultBrowser(Composite parent, int style) {
		var browser = new Browser(parent, style);
		var browser2 = new IBrowser() {

			@Override
			public boolean execute(String script) {
				return browser.execute(script);
			}

			@Override
			public Control getControl() {
				return browser;
			}

			@Override
			public void setUrl(String url) {
				browser.setUrl(url);
			}

			@Override
			public void setText(String text) {
				browser.setText(text);
			}

			@Override
			public Object createFunction(String name, IBrowserFunction function) {
				return new BrowserFunction(browser, name) {
					@Override
					public Object function(Object[] arguments) {
						super.function(arguments);
						return function.function(arguments);
					}
				};
			}

			@Override
			public void addLocationListener(LocationListener listener) {
				browser.addLocationListener(listener);
			}

		};

		browser.setData("phasereditor.IBrowser", browser2);

		return browser2;
	}

	private static IBrowser createChromiumBrowser(Composite parent, int style) {
		var browser = new org.eclipse.swt.chromium.Browser(parent, style);
		var browser2 = new IBrowser() {
			@Override
			public Control getControl() {
				return browser;
			}

			@Override
			public void setUrl(String url) {
				browser.setUrl(url);
			}

			@Override
			public void setText(String text) {
				browser.setText(text);
			}

			@Override
			public boolean execute(String script) {
				return browser.execute(script);
			}

			@Override
			public Object createFunction(String name, IBrowserFunction function) {
				return new org.eclipse.swt.chromium.BrowserFunction(browser, name) {
					@Override
					public Object function(Object[] arguments) {
						super.function(arguments);
						return function.function(arguments);
					}
				};
			}

			@Override
			public void addLocationListener(LocationListener listener) {
				browser.addLocationListener(listener);
			}
		};

		browser.setData("phasereditor.IBrowser", browser2);

		return browser2;
	}

	public static IBrowser get(Control control) {
		return (IBrowser) control.getData("phasereditor.IBrowser");
	}

}
