// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.project.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @author arian
 *
 */
public class ProjectUI {
	public static final String PLUGIN_ID = Activator.PLUGIN_ID;

	public static final String PREF_PROP_PROJECT_WIZARD_GAME_WIDTH = "phasereditor.project.ui.projectWizardGameWidth";
	public static final String PREF_PROP_PROJECT_WIZARD_GAME_HEIGHT = "phasereditor.project.ui.projectWizardGameHeight";
	public static final String PREF_PROP_PROJECT_WIZARD_LANGUAJE = "phasereditor.project.ui.projectWizardLang";

	public static void logError(Exception e) {
		e.printStackTrace();
		StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

	public static IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}
}
