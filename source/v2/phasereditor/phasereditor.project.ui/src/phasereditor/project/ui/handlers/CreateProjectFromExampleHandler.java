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
package phasereditor.project.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.inspect.core.examples.PhaserExampleModel;
import phasereditor.project.core.ProjectCore;
import phasereditor.project.core.codegen.SourceLang;

/**
 * @author arian
 *
 */
public class CreateProjectFromExampleHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Object elem = HandlerUtil.getCurrentStructuredSelection(event).getFirstElement();

		PhaserExampleModel example = Adapters.adapt(elem, PhaserExampleModel.class);

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		String name = example.getName();

		{
			int i = 1;
			while (root.getProject(name).exists()) {
				name = example.getName() + " " + i;
				i++;
			}
		}

		InputDialog dlg = new InputDialog(HandlerUtil.getActiveShell(event), "Create Project",
				"Create a project with the selected example.", name, new IInputValidator() {

					@Override
					public String isValid(String newText) {
						IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(newText);
						if (project.exists()) {
							return "That name already exists.";
						}
						return null;
					}
				});

		if (dlg.open() == Window.OK) {

			name = dlg.getValue();

			IProject project = root.getProject(name);

			WorkspaceJob job = new WorkspaceJob("Create example project '" + project.getName() + "'") {

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
					project.create(monitor);
					project.open(monitor);

					ProjectCore.configureNewPhaserProject(project, example, null, SourceLang.JAVA_SCRIPT_6, monitor);

					return Status.OK_STATUS;
				}
			};

			job.schedule();
		}

		return null;
	}

}
