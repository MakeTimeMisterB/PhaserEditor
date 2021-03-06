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
package phasereditor.assetpack.ui.editor;

import static java.util.stream.Collectors.toList;
import static phasereditor.ui.IEditorSharedImages.IMG_ADD;
import static phasereditor.ui.IEditorSharedImages.IMG_DELETE;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.operations.UndoRedoActionGroup;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;

import phasereditor.assetpack.core.AssetFactory;
import phasereditor.assetpack.core.AssetGroupModel;
import phasereditor.assetpack.core.AssetModel;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.AssetPackModel;
import phasereditor.assetpack.core.AssetSectionModel;
import phasereditor.assetpack.core.AssetType;
import phasereditor.assetpack.core.AudioAssetModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.core.MultiScriptAssetModel;
import phasereditor.assetpack.core.SceneFileAssetModel;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.assetpack.ui.AssetPackUI;
import phasereditor.assetpack.ui.AssetsContentProvider;
import phasereditor.assetpack.ui.AssetsTreeCanvasViewer;
import phasereditor.assetpack.ui.IAssetPackEditor;
import phasereditor.assetpack.ui.ImageResourceDialog;
import phasereditor.assetpack.ui.SvgResourceDialog;
import phasereditor.assetpack.ui.editor.blocks.AssetPackEditorBlocksProvider;
import phasereditor.assetpack.ui.editor.undo.GlobalOperation;
import phasereditor.audio.ui.AudioResourceDialog;
import phasereditor.lic.LicCore;
import phasereditor.project.core.ProjectCore;
import phasereditor.scene.core.SceneCore;
import phasereditor.ui.EditorBlockProvider;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.FilteredTreeCanvasContentOutlinePage;
import phasereditor.ui.IEditorHugeToolbar;
import phasereditor.ui.ImageProxy;
import phasereditor.ui.ImageProxyTreeCanvasItemRenderer;
import phasereditor.ui.TreeCanvas.TreeCanvasItem;
import phasereditor.ui.TreeCanvasViewer;
import phasereditor.ui.editors.EditorFileMoveHelper;
import phasereditor.ui.editors.EditorFileStampHelper;

/**
 * @author arian
 *
 */
public class AssetPackEditor extends EditorPart implements IGotoMarker, IShowInSource, IAssetPackEditor {

	public static final String ID = "phasereditor.assetpack.ui.editor.AssetPackEditor";

	public static final IUndoContext UNDO_CONTEXT = new IUndoContext() {

		@Override
		public boolean matches(IUndoContext context) {
			return context == this;
		}

		@Override
		public String getLabel() {
			return "ASSET_PACK_EDITOR_CONTEXT";
		}
	};

	private static final QualifiedName EDITING_NODE = new QualifiedName("phasereditor.assetpack", "editingNode_v2");

	private AssetPackModel _model;

	private AssetPackEditorOutlinePage _outliner;

	private PackEditorCanvas _assetsCanvas;

	private EditorFileStampHelper _fileStampHelper;

	private boolean _dirty;

	private UndoRedoActionGroup _undoRedoGroup;

	private IPartListener _partListener;

	public AssetPackEditor() {
		_fileStampHelper = new EditorFileStampHelper(this, this::reloadMethod, this::saveMethod);
	}

	public AssetPackEditorOutlinePage getOutliner() {
		return _outliner;
	}

	public void setOutliner(AssetPackEditorOutlinePage outliner) {
		_outliner = outliner;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		_fileStampHelper.helpDoSave(monitor);
	}

	private void saveMethod(IProgressMonitor monitor) {
		_model.save(monitor);
		setDirty(false);
		saveEditingPoint();
		refresh();
	}

	public void refresh() {
		if (_outliner != null) {
			_outliner.refresh();
		}

		_assetsCanvas.redraw();
		_assetsCanvas.updateScroll();

		if (_blocksProvider != null) {
			_blocksProvider.refresh();
			_blocksProvider.updateProperties();
		}
	}

	public void saveEditingPoint() {
		AssetPackModel pack = getModel();
		if (pack == null) {
			return;
		}

		var sel = getSelection();

		if (sel.length == 0) {
			return;
		}

		Object elem = sel[0];
		if (elem != null) {
			IFile file = pack.getFile();
			try {
				file.setPersistentProperty(EDITING_NODE, pack.getStringReference(elem));
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	private void recoverEditingPoint() {
		AssetPackModel pack = getModel();
		IFile file = pack.getFile();
		try {
			String str = file.getPersistentProperty(EDITING_NODE);
			if (str != null) {
				Object elem = pack.getElementFromStringReference(str);
				revealElement(elem);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void doSaveAs() {
		//
	}

	@Override
	public void dispose() {
		if (_model != null && _model.getFile().exists()) {
			saveEditingPoint();
		}

		getEditorSite().getPage().removePartListener(_partListener);

		super.dispose();
	}

	@Override
	public void gotoMarker(IMarker marker) {
		try {
			String ref = (String) marker.getAttribute(AssetPackCore.ASSET_EDITOR_GOTO_MARKER_ATTR);
			if (ref != null) {
				Object asset = getModel().getElementFromStringReference(ref);
				revealElement(asset);
			}
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);

		registerUndoRedoActions();
	}

	private void registerUndoRedoActions() {

		var site = getEditorSite();

		_undoRedoGroup = new UndoRedoActionGroup(site, UNDO_CONTEXT, true);

		var actionBars = site.getActionBars();

		_undoRedoGroup.fillActionBars(actionBars);

		actionBars.updateActionBars();

		_partListener = createUndoRedoPartListener();

		getEditorSite().getPage().addPartListener(_partListener);

	}

	public UndoRedoActionGroup getUndoRedoGroup() {
		return _undoRedoGroup;
	}

	private IPartListener createUndoRedoPartListener() {
		return new IPartListener() {

			@Override
			public void partOpened(IWorkbenchPart part) {
				//
			}

			@Override
			public void partDeactivated(IWorkbenchPart part) {
				//
			}

			@Override
			public void partClosed(IWorkbenchPart part) {
				//
			}

			@Override
			public void partBroughtToTop(IWorkbenchPart part) {
				//
			}

			@Override
			public void partActivated(IWorkbenchPart part) {
				if (part == AssetPackEditor.this) {
					var actionBars = getEditorSite().getActionBars();
					_undoRedoGroup.fillActionBars(actionBars);
					actionBars.updateActionBars();
					// if (_pendingBuild) {
					// realBuild();
					// }
				}
			}
		};
	}

	public void setDirty(boolean dirty) {
		if (dirty != _dirty) {
			_dirty = dirty;
			firePropertyChange(PROP_DIRTY);
		}
	}

	@Override
	public boolean isDirty() {
		return _dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@SuppressWarnings("unused")
	public void showAddAssetMenu() {
		var dlg = new AssetTypeDialog(getSite());
		if (dlg.open() == Window.OK) {
			var result = dlg.getResult();
			swtRun(() -> openAddAssetDialog(result));
		}

	}

	@SuppressWarnings({ "boxing" })
	private List<AssetModel> openNewAssetListDialog(AssetFactory factory) throws Exception {
		var section = _model.getSections().get(0);
		AssetType type = factory.getType();

		var pack = getModel();

		switch (type) {

		case image:
			return openNewImageListDialog(section);
		case audio:
			return openNewAudioListDialog(section);
		case atlas:
		case atlasXML:
		case unityAtlas:
		case multiatlas:
			return openNewAtlasListDialog(section);
		case audioSprite:
			return openNewAudioSpriteListDialog(section);
		case binary:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return getModel().discoverFiles(f -> true);
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}
			});
		case json:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return getModel().discoverJsonFiles();
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case physics:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return pack.discoverJsonFiles();
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case glsl:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return pack.discoverTextFiles(AssetPackCore.SHADER_EXTS);
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case text:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return pack.discoverFiles(f -> Boolean.TRUE);
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case xml:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return pack.discoverTextFiles("xml");
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case htmlTexture:
		case html:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return pack.discoverTextFiles("html");
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case css:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return pack.discoverTextFiles("css");
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case script:
		case plugin:
		case scenePlugin:
		case sceneFile:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return pack.discoverTextFiles("js");
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case scripts:
			return openNewMultiScriptsListDialog(section);
		case bitmapFont:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return pack.discoverBitmapFontFiles();
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case spritesheet:
			return openNewSpritesheetListDialog(section);
		case tilemapCSV:
		case tilemapTiledJSON:
		case tilemapImpact:
			return openNewTilemapListDialog(section, type);
		case animation:
			return openNewSimpleFileListDialog(section, type, () -> {
				try {
					return pack.discoverAnimationsFiles();
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		case svg:
			return openNewSvgListDialog(section);
		case video:
			break;
		default:
			break;
		}

		return Collections.emptyList();
	}

	private List<AssetModel> openNewMultiScriptsListDialog(AssetSectionModel section) throws CoreException {
		AssetPackModel pack = getModel();
		var jsFiles = pack.discoverTextFiles("js");

		var shell = getEditorSite().getShell();

		List<AssetModel> list = new ArrayList<>();

		List<IFile> selectedFiles = AssetPackUI.browseManyAssetFile(pack, "scripts", jsFiles, shell);

		var asset = new MultiScriptAssetModel(pack.createKey("scripts"), section);
		var urls = asset.getUrlsFromFiles(selectedFiles);
		asset.setUrls(urls);
		list.add(asset);

		return list;
	}

	public void importFiles(AssetFactory factory, List<IFile> files) {

		if (isEvaluationProductOutOfLimits()) {
			return;
		}

		var section = ensureThereIsASection();

		var assets = new ArrayList<AssetModel>();

		for (var file : files) {
			create_Asset_from_File_and_add_to_List(assets, section, factory.getType(), file);
		}

		addNewAssets(section, assets);
	}

	protected void openAddAssetDialog(AssetType initialType) {
		if (isEvaluationProductOutOfLimits()) {
			return;
		}

		var section = ensureThereIsASection();

		try {
			AssetFactory factory = AssetFactory.getFactory(initialType);

			if (factory != null) {
				var assets = openNewAssetListDialog(factory);

				addNewAssets(section, assets);
			}

		} catch (Exception e) {
			AssetPackUIEditor.logError(e);
			throw new RuntimeException(e);
		}
	}

	private void addNewAssets(AssetSectionModel section, List<AssetModel> assets) {
		if (!assets.isEmpty()) {

			var before = GlobalOperation.readState(this);

			for (var asset : assets) {
				section.addAsset(asset);
			}

			var after = GlobalOperation.readState(this);

			executeOperation(new GlobalOperation("Add new assets", before, after));

			swtRun(() -> {
				_assetsCanvas.getUtils().setSelectionList(assets);

				if (_outliner != null) {
					_outliner.revealAndSelect(new StructuredSelection(assets));
				}

				if (!assets.isEmpty()) {
					_assetsCanvas.reveal(assets.get(0));
				}

				_assetsCanvas.updateScroll();
			});
		}
	}

	private boolean isEvaluationProductOutOfLimits() {
		if (LicCore.isEvaluationProduct()) {

			IProject project = getEditorInput().getFile().getProject();

			String rule = AssetPackCore.isFreeVersionAllowed(project);
			if (rule != null) {
				LicCore.launchGoPremiumDialogs(rule);
				return true;
			}
		}

		return false;
	}

	private AssetSectionModel ensureThereIsASection() {
		AssetSectionModel section;
		if (_model.getSections().isEmpty()) {
			_model.addSection(section = new AssetSectionModel("section", _model));
		} else {
			section = _model.getSections().get(0);
		}
		return section;
	}

	private List<AssetModel> openNewTilemapListDialog(AssetSectionModel section, AssetType type) throws Exception {
		AssetPackModel pack = getModel();
		var tilemapFiles = pack.discoverTilemapFiles(type);

		var shell = getEditorSite().getShell();

		List<AssetModel> list = new ArrayList<>();

		List<IFile> selectedFiles = AssetPackUI.browseManyAssetFile(pack, "tilemap", tilemapFiles, shell);

		create_Assets_from_Files_and_add_to_List(list, section, type, selectedFiles);

		return list;
	}

	private List<AssetModel> openNewSpritesheetListDialog(AssetSectionModel section) throws CoreException {
		AssetPackModel pack = getModel();
		List<IFile> imageFiles = pack.discoverImageFiles();

		Set<IFile> usedFiles = pack.sortFilesByNotUsed(imageFiles);

		var shell = getEditorSite().getShell();
		var dlg = new ImageResourceDialog(shell);
		dlg.setLabelProvider(AssetPackUI.createFilesLabelProvider(usedFiles, shell));
		dlg.setInput(imageFiles);
		dlg.setObjectName(AssetType.spritesheet.getCapitalName());

		List<AssetModel> list = new ArrayList<>();

		if (dlg.open() == Window.OK) {
			for (Object obj : dlg.getMultipleSelection()) {
				IFile file = (IFile) obj;
				create_Asset_from_File_and_add_to_List(list, section, AssetType.spritesheet, file);
			}
		}

		return list;
	}

	private List<AssetModel> openNewSimpleFileListDialog(AssetSectionModel section, AssetType type,
			Supplier<List<IFile>> discoverFiles) {
		var pack = getModel();

		var dialogFiles = discoverFiles.get();

		var shell = getEditorSite().getShell();

		var list = new ArrayList<AssetModel>();

		var selectedFiles = AssetPackUI.browseManyAssetFile(pack, type.getCapitalName(), dialogFiles, shell);

		create_Assets_from_Files_and_add_to_List(list, section, type, selectedFiles);

		return list;
	}

	private List<AssetModel> openNewAudioSpriteListDialog(AssetSectionModel section) throws CoreException {
		AssetPackModel pack = getModel();

		List<IFile> audiospriteFiles = pack.discoverAudioSpriteFiles();

		var shell = getEditorSite().getShell();

		List<AssetModel> list = new ArrayList<>();

		List<IFile> selectedFiles = AssetPackUI.browseManyAssetFile(pack, "audiosprite", audiospriteFiles, shell);

		create_Assets_from_Files_and_add_to_List(list, section, AssetType.audioSprite, selectedFiles);

		return list;
	}

	private List<AssetModel> openNewAtlasListDialog(AssetSectionModel section) throws Exception {
		AssetPackModel pack = getModel();

		var fileTypeMap = new HashMap<IFile, AssetType>();

		for (var type : new AssetType[] { AssetType.atlas, AssetType.multiatlas, AssetType.atlasXML,
				AssetType.unityAtlas }) {
			for (var file : pack.discoverAtlasFiles(type)) {
				fileTypeMap.put(file, type);
			}
		}

		var atlasFiles = new ArrayList<>(fileTypeMap.keySet());

		var shell = getEditorSite().getShell();

		var list = new ArrayList<AssetModel>();

		var selectedFiles = AssetPackUI.browseManyAssetFile(pack, "Atlas (Multi, JSON, XML, Unity)", atlasFiles, shell);

		for (IFile file : selectedFiles) {
			var type = fileTypeMap.get(file);
			create_Asset_from_File_and_add_to_List(list, section, type, file);
		}

		return list;
	}

	private List<AssetModel> openNewAudioListDialog(AssetSectionModel section) throws CoreException {
		AssetPackModel pack = getModel();

		List<IFile> audioFiles = pack.discoverAudioFiles();

		Set<IFile> usedFiles = pack.sortFilesByNotUsed(audioFiles);

		var shell = getEditorSite().getShell();
		var dlg = new AudioResourceDialog(shell, false);
		dlg.setLabelProvider(AssetPackUI.createFilesLabelProvider(usedFiles, shell));
		dlg.setInput(audioFiles);

		List<AssetModel> list = new ArrayList<>();

		if (dlg.open() == Window.OK) {

			Map<String, List<String>> filesMap = new HashMap<>();

			for (IFile file : dlg.getSelection()) {
				String prefix = file.getFullPath().removeFileExtension().toPortableString();

				if (!filesMap.containsKey(prefix)) {
					filesMap.put(prefix, new ArrayList<>());
				}

				filesMap.get(prefix).add(ProjectCore.getAssetUrl(file));
			}

			for (var entry : filesMap.entrySet()) {
				var path = new Path(entry.getKey());
				var asset = new AudioAssetModel(pack.createKey(path.lastSegment()), section);
				asset.setUrls(entry.getValue());
				list.add(asset);
			}
		}

		return list;
	}

	private static void create_Asset_from_File_and_add_to_List(List<AssetModel> list, AssetSectionModel section,
			AssetType type, IFile file) {
		try {
			var asset = AssetFactory.getFactory(type).createAsset(section, file);
			list.add(asset);
		} catch (Exception e) {
			AssetPackUI.logError(e);
			throw new RuntimeException(e);
		}
	}

	private static void create_Assets_from_Files_and_add_to_List(List<AssetModel> list, AssetSectionModel section,
			AssetType type, List<IFile> selectedFiles) {
		for (var file : selectedFiles) {
			create_Asset_from_File_and_add_to_List(list, section, type, file);
		}
	}

	private List<AssetModel> openNewImageListDialog(AssetSectionModel section) throws CoreException {
		AssetPackModel pack = getModel();
		List<IFile> imageFiles = pack.discoverImageFiles();

		Set<IFile> usedFiles = pack.sortFilesByNotUsed(imageFiles);

		var shell = getEditorSite().getShell();
		ImageResourceDialog dlg = new ImageResourceDialog(shell);
		dlg.setObjectName(AssetType.image.getCapitalName());
		dlg.setLabelProvider(AssetPackUI.createFilesLabelProvider(usedFiles, shell));
		dlg.setInput(imageFiles);

		List<AssetModel> list = new ArrayList<>();

		if (dlg.open() == Window.OK) {
			for (Object obj : dlg.getMultipleSelection()) {
				IFile file = (IFile) obj;
				create_Asset_from_File_and_add_to_List(list, section, AssetType.image, file);
			}
		}

		return list;
	}

	@SuppressWarnings("boxing")
	private List<AssetModel> openNewSvgListDialog(AssetSectionModel section) throws CoreException {
		AssetPackModel pack = getModel();
		List<IFile> imageFiles = pack.discoverFiles(f -> "svg".equals(f.getFileExtension()));

		Set<IFile> usedFiles = pack.sortFilesByNotUsed(imageFiles);

		var shell = getEditorSite().getShell();
		var dlg = new SvgResourceDialog(shell);
		dlg.setLabelProvider(AssetPackUI.createFilesLabelProvider(usedFiles, shell));
		dlg.setInput(imageFiles);
		dlg.setObjectName(AssetType.svg.getCapitalName());

		List<AssetModel> list = new ArrayList<>();

		if (dlg.open() == Window.OK) {
			for (Object obj : dlg.getMultipleSelection()) {
				IFile file = (IFile) obj;
				create_Asset_from_File_and_add_to_List(list, section, AssetType.svg, file);
			}
		}

		return list;
	}

	Map<AssetSectionModel, AssetGroupModel> _lastSelectedTypeMap = new HashMap<>();

	private Action _deleteSelectionAction;

	private MenuManager _menuManager;

	private Action _addAssetsAction;

	@Override
	public void createPartControl(Composite parent) {

		createActions();

		var comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, false));

		_assetsCanvas = new PackEditorCanvas(this, comp, 0);
		_assetsCanvas.setLayoutData(new GridData(GridData.FILL_BOTH));

		_assetsCanvas.getUtils().addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (_assetsCanvas.isFocusControl()) {
					if (getOutliner() != null) {
						getOutliner().revealAndSelect(event.getStructuredSelection());
					}
				}
			}
		});

		getEditorSite().setSelectionProvider(_assetsCanvas.getUtils());

		recoverEditingPoint();

		_assetsCanvas.setModel(_model);

		swtRun(this::refresh);

		_assetsCanvas.getUtils().addSelectionChangedListener(e -> updateActions());

		{
			var helper = new EditorFileMoveHelper<>(this) {

				@Override
				protected IFile getEditorFile(AssetPackEditor editor) {
					return getEditorInput().getFile();
				}

				@SuppressWarnings("synthetic-access")
				@Override
				protected void setEditorFile(AssetPackEditor editor, IFile file) {
					_model.setFile(file);

					AssetPackEditor.super.setInput(new FileEditorInput(file));

					setPartName(_model.getName());
				}

			};

			parent.addDisposeListener(e -> helper.dispose());
		}

		_assetsCanvas.setMenu(getMenuManager().createContextMenu(_assetsCanvas));
	}

	public MenuManager getMenuManager() {
		return _menuManager;
	}

	private void updateActions() {
		var sel = _assetsCanvas.getUtils().getSelectedObjects();
		_deleteSelectionAction.setEnabled(!sel.isEmpty());
	}

	public void deleteSelection() {

		var selectedAssets = Arrays.stream(getSelection())

				.filter(obj -> obj instanceof AssetModel)

				.map(obj -> (AssetModel) obj)

				.collect(toList());

		if (!selectedAssets.isEmpty()) {

			var before = GlobalOperation.readState(this);

			for (var asset : selectedAssets) {
				asset.getSection().removeAsset(asset);
			}

			var after = GlobalOperation.readState(this);

			executeOperation(new GlobalOperation("Delete assets", before, after));

			getSite().getSelectionProvider().setSelection(StructuredSelection.EMPTY);
		}

	}

	private void createActions() {
		_addAssetsAction = new Action("Add File Key") {
			{
				setToolTipText("Add new files");
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IMG_ADD));
			}

			@Override
			public void run() {
				showAddAssetMenu();
			}

		};
		_deleteSelectionAction = new Action("Delete", EditorSharedImages.getImageDescriptor(IMG_DELETE)) {

			{
				setDescription("Delete selection (Refactoring)");
			}

			@Override
			public void run() {
				deleteSelection();
			}
		};

		_menuManager = new MenuManager();
		_menuManager.add(_addAssetsAction);
		_menuManager.add(_deleteSelectionAction);
		_menuManager.addMenuListener(m -> {
			_deleteSelectionAction.setEnabled(Arrays.stream(getSelection())

					.filter(obj -> obj instanceof AssetModel)

					.map(obj -> (AssetModel) obj)

					.count() > 0);
		});
	}

	public Action getDeleteSelectionAction() {
		return _deleteSelectionAction;
	}

	@Override
	public ShowInContext getShowInContext() {
		return new ShowInContext(getEditorInput(), new StructuredSelection(getSelection()));
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);

		FileEditorInput fileInput = getEditorInput();
		IFile file = fileInput.getFile();

		try {
			// we create a model copy detached from the AssetCore registry.
			_model = new AssetPackModel(file);
			_model.build();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		_model.addPropertyChangeListener(this::propertyChange);

		setPartName(_model.getName());

	}

	public void build() {
		getModel().build();
		refresh();
	}

	public void reloadFile() {
		_fileStampHelper.helpReloadFile();
	}

	private void reloadMethod() {
		try {
			_model = new AssetPackModel(getEditorInput().getFile());
			firePropertyChange(PROP_DIRTY);

			_model.addPropertyChangeListener(this::propertyChange);

			if (_outliner != null) {
				_outliner.getTreeViewer().setInput(_model);
			}

			_assetsCanvas.setModel(_model);

			build();

			getEditorSite().getSelectionProvider().setSelection(StructuredSelection.EMPTY);
			_assetsCanvas.getUtils().setSelection(StructuredSelection.EMPTY);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void propertyChange(@SuppressWarnings("unused") PropertyChangeEvent evt) {
		getEditorSite().getShell().getDisplay().asyncExec(() -> {
			firePropertyChange(PROP_DIRTY);
			refresh();
		});
	}

	@Override
	public AssetPackModel getModel() {
		return _model;
	}

	public IResource getAssetsFolder() {
		return getEditorInput().getFile().getParent();
	}

	@Override
	public FileEditorInput getEditorInput() {
		return (FileEditorInput) super.getEditorInput();
	}

	@Override
	public void setFocus() {
		_assetsCanvas.setFocus();
	}

	public void executeOperation(IUndoableOperation operation) {
		operation.addContext(UNDO_CONTEXT);
		IWorkbench workbench = getSite().getWorkbenchWindow().getWorkbench();
		try {
			IOperationHistory history = workbench.getOperationSupport().getOperationHistory();
			history.execute(operation, null, this);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public void revealElement(Object elem) {
		if (elem == null) {
			return;
		}

		if (elem instanceof IAssetKey) {
			var assetKey = (IAssetKey) elem;
			getAssetsCanvas().reveal(assetKey.getAsset());
			_assetsCanvas.getUtils().setSelectionList(List.of(assetKey.getAsset()));
		}

	}

	public class AssetPackEditorOutlinePage extends FilteredTreeCanvasContentOutlinePage {
		private ISelectionChangedListener _listener;

		public AssetPackEditorOutlinePage() {
		}

		public AssetPackEditor getEditor() {
			return AssetPackEditor.this;
		}

		@Override
		protected TreeCanvasViewer createViewer() {
			var viewer = new AssetsTreeCanvasViewer(getFilteredTreeCanvas().getTree(), new AssetsContentProvider(true),
					AssetLabelProvider.GLOBAL_16) {
				@Override
				protected void setItemProperties(TreeCanvasItem item) {
					super.setItemProperties(item);

					var elem = item.getData();

					if (elem instanceof SceneFileAssetModel) {
						var asset = (SceneFileAssetModel) elem;
						var imgFile = SceneCore.getSceneScreenshotFile(asset);
						if (imgFile != null) {
							item.setRenderer(new ImageProxyTreeCanvasItemRenderer(item, ImageProxy.get(imgFile, null)));
						}
					}
				}
			};
			viewer.getTree().getUtils().setFilterInputWhenSetSelection(false);
			return viewer;
		}

		public void revealAndSelect(IStructuredSelection selection) {
			getTreeViewer().setSelection(selection, true);
		}

		@Override
		public void createControl(Composite parent) {
			super.createControl(parent);

			var viewer = getTreeViewer();

			viewer.addSelectionChangedListener(_listener = new ISelectionChangedListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					if (!viewer.getTree().isFocusControl()) {
						return;
					}

					var list = event.getStructuredSelection().toList().stream()

							.filter(o -> o instanceof IAssetKey)

							.map(o -> ((IAssetKey) o).getAsset())

							.toArray();

					getAssetsCanvas().getUtils().setSelectionList(Arrays.asList(list));

					if (list.length > 0) {
						getAssetsCanvas().reveal((AssetModel) list[0]);
					} else {
						getAssetsCanvas().redraw();
					}
				}
			});

			AssetPackUI.installAssetTooltips(viewer.getTree(), viewer.getTree().getUtils());

			viewer.setInput(getModel());

			viewer.getControl().setMenu(getMenuManager().createContextMenu(viewer.getControl()));

			registerUndoRedoActions();
		}

		private void registerUndoRedoActions() {
			getUndoRedoGroup().fillActionBars(getSite().getActionBars());
		}

		@Override
		public void dispose() {

			getTreeViewer().removeSelectionChangedListener(_listener);

			setOutliner(null);

			super.dispose();
		}
	}

	private List<AssetPackEditorPropertyPage> _propertyPageList = new ArrayList<>();

	private AssetPackEditorHugeToolbar _toolbar;

	private AssetPackEditorBlocksProvider _blocksProvider;

	List<AssetPackEditorPropertyPage> getPropertyPageList() {
		return _propertyPageList;
	}

	public void updatePropertyPages() {
		for (var page : _propertyPageList) {
			page.selectionChanged(this, getSite().getSelectionProvider().getSelection());
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == IPropertySheetPage.class) {
			var page = new AssetPackEditorPropertyPage(this);
			_propertyPageList.add(page);
			return page;
		}

		if (adapter == IContentOutlinePage.class) {
			if (_outliner == null) {
				_outliner = new AssetPackEditorOutlinePage();
			}
			return _outliner;
		}

		if (adapter == IContextProvider.class) {
			return new IContextProvider() {

				@Override
				public String getSearchExpression(Object target) {
					return null;
				}

				@Override
				public int getContextChangeMask() {
					return NONE;
				}

				@Override
				public IContext getContext(Object target) {
					IContext context = HelpSystem.getContext("phasereditor.help.assetpackeditor");
					return context;
				}
			};
		}

		if (adapter == IEditorHugeToolbar.class) {
			if (_toolbar == null) {
				_toolbar = new AssetPackEditorHugeToolbar();
			}
			return _toolbar;
		}

		if (adapter == EditorBlockProvider.class) {
			if (_blocksProvider == null) {
				_blocksProvider = new AssetPackEditorBlocksProvider(this);
			}
			return _blocksProvider;
		}

		return super.getAdapter(adapter);
	}

	class AssetPackEditorHugeToolbar implements IEditorHugeToolbar {

		@SuppressWarnings("unused")
		@Override
		public void createContent(Composite parent) {
			new ActionButton(parent, _addAssetsAction, true);
		}

	}

	public PackEditorCanvas getAssetsCanvas() {
		return _assetsCanvas;
	}

	private Object[] getSelection() {
		var sel = (IStructuredSelection) getSite().getSelectionProvider().getSelection();

		if (sel.isEmpty()) {
			if (_outliner != null) {
				return ((IStructuredSelection) _outliner.getSelection()).toArray();
			}
		}

		return sel.toArray();
	}

	public void clearSelection() {
		if (_outliner != null) {
			_outliner.setSelection(StructuredSelection.EMPTY);
		}
		_assetsCanvas.getUtils().setSelectionList(List.of());
	}
}
