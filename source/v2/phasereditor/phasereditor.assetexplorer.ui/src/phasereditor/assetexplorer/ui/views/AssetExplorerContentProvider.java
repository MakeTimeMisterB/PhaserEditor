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
package phasereditor.assetexplorer.ui.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.animations.AnimationModel;
import phasereditor.assetpack.ui.AssetsContentProvider;
import phasereditor.atlas.core.AtlasCore;
import phasereditor.project.core.ProjectCore;
import phasereditor.scene.core.SceneCore;

class AssetExplorerContentProvider extends AssetsContentProvider {

	private IProject _projectInContent;

	public AssetExplorerContentProvider() {
		super(true);
	}

	@Override
	public Object[] getChildren(Object parent) {
		IProject activeProjet = ProjectCore.getActiveProject();

		if (activeProjet == null) {
			if (_projectInContent != null && _projectInContent.exists()) {
				activeProjet = _projectInContent;
			}
		} else {
			_projectInContent = activeProjet;
		}

		if (parent == AssetsView.ROOT) {

			if (activeProjet == null) {
				return new Object[0];
			}

			return new Object[] {

					AssetsView.SCENES_NODE,

					AssetsView.ANIMATIONS_NODE,

					AssetsView.PACK_NODE,

					AssetsView.ATLAS_NODE,

			};
		}

		if (parent == AssetsView.ANIMATIONS_NODE) {
			return AssetPackCore.getAnimationsFileCache().getProjectData(activeProjet).stream().map(d -> d.getFile())
					.toArray();
		}

		if (parent == AssetsView.ATLAS_NODE) {
			return AtlasCore.getAtlasFileCache().getProjectData(activeProjet).stream().map(d -> d.getFile()).toArray();
		}

		if (parent == AssetsView.PACK_NODE) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();

			List<Object> list = new ArrayList<>();

			for (IProject project : workspace.getRoot().getProjects()) {

				if (activeProjet != project) {
					continue;
				}

				{
					List<AssetPackModel> packs = AssetPackCore.getAssetPackModels(project);
					list.addAll(packs);
				}
			}

			return list.toArray();
		}

		if (parent == AssetsView.SCENES_NODE) {
			return SceneCore.getSceneFileDataCache().getProjectData(activeProjet).stream().map(d -> d.getFile())
					.toArray();
		}

		// if (parent instanceof CanvasType) {
		// List<CanvasFile> cfiles =
		// CanvasCore.getCanvasFileCache().getProjectData(activeProjet);
		//
		// List<CanvasFile> list = new ArrayList<>();
		//
		// for (CanvasFile cfile : cfiles) {
		// if (cfile.getType() == parent) {
		// list.add(cfile);
		// }
		// }
		//
		// list.sort((a, b) -> {
		// return a.getFile().getName().compareTo(b.getFile().getName());
		// });
		//
		// return list.toArray();
		// }

		if (parent instanceof AssetsContentProvider.Container) {
			return ((Container) parent).children;
		}

		if (parent instanceof AnimationModel) {
			return new Object[0];
		}

		return super.getChildren(parent);
	}

	public IProject getProjectInContent() {
		return _projectInContent;
	}
}