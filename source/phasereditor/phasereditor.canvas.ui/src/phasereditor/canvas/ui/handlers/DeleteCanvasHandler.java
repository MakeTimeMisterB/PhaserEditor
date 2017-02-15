package phasereditor.canvas.ui.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.ide.actions.LTKLauncher;

import phasereditor.canvas.core.CanvasFile;

@SuppressWarnings("restriction")
public class DeleteCanvasHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = HandlerUtil.getCurrentStructuredSelection(event);

		List<IFile> toMove = new ArrayList<>();

		Object[] array = sel.toArray();

		for (Object obj : array) {
			CanvasFile cfile = (CanvasFile) obj;
			toMove.add(cfile.getFile());
		}

		LTKLauncher.openDeleteWizard(new StructuredSelection(toMove));

		return null;
	}

}
