
namespace phasereditor2d.scene.ui.editor {

    import io = colibri.core.io;

    class SceneEditorFactory extends colibri.ui.ide.EditorFactory {

        constructor() {
            super("phasereditor2d.SceneEditorFactory");
        }

        acceptInput(input: any): boolean {

            if (input instanceof io.FilePath) {
                const contentType = colibri.ui.ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(input);
                return contentType === core.CONTENT_TYPE_SCENE;
            }

            return false;
        }

        createEditor(): colibri.ui.ide.EditorPart {
            
            // TODO: remove this
            pack.core.PackFinder.reset_temporal_method_TODO();

            return new SceneEditor();
        }
    }

    export class SceneEditor extends colibri.ui.ide.FileEditor {

        private _blocksProvider: blocks.SceneEditorBlocksProvider;
        private _outlineProvider: outline.SceneEditorOutlineProvider;
        private _propertyProvider: properties.SceneEditorSectionProvider;
        private _game: Phaser.Game;
        private _overlayLayer: OverlayLayer;
        private _gameCanvas: HTMLCanvasElement;
        private _gameScene: GameScene;
        private _sceneMaker: SceneMaker;
        private _dropManager: DropManager;
        private _cameraManager: CameraManager;
        private _selectionManager: SelectionManager;
        private _actionManager: ActionManager;
        private _gameBooted: boolean;
        private _sceneRead: boolean;

        static getFactory(): colibri.ui.ide.EditorFactory {
            return new SceneEditorFactory();
        }

        constructor() {
            super("phasereditor2d.SceneEditor");

            this.addClass("SceneEditor");

            this._blocksProvider = new blocks.SceneEditorBlocksProvider(this);
            this._outlineProvider = new outline.SceneEditorOutlineProvider(this);
            this._propertyProvider = new properties.SceneEditorSectionProvider();
        }

        async doSave() {

            const writer = new json.SceneWriter(this.getGameScene());

            const data = writer.toJSON();

            const content = JSON.stringify(data, null, 4);

            try {

                await colibri.ui.ide.FileUtils.setFileString_async(this.getInput(), content);

                this.setDirty(false);

            } catch (e) {
                console.error(e);
            }

            const win = colibri.ui.ide.Workbench.getWorkbench().getActiveWindow() as ide.ui.DesignWindow;
            
            win.saveWindowState();
        }

        saveState(state: any) {

            if (!this._gameScene) {
                return;
            }

            const camera = this._gameScene.cameras.main;

            state.cameraZoom = camera.zoom;
            state.cameraScrollX = camera.scrollX;
            state.cameraScrollY = camera.scrollY;
        }

        restoreState(state: any) {

            this._gameScene.setInitialState(state);
        }

        protected onEditorInputContentChanged() {
            //TODO: missing to implement
        }

        protected createPart() {

            this.setLayoutChildren(false);


            const container = document.createElement("div");
            container.classList.add("SceneEditorContainer");

            this.getElement().appendChild(container);

            this._gameCanvas = document.createElement("canvas");
            this._gameCanvas.style.position = "absolute";
            this.getElement().appendChild(container);

            container.appendChild(this._gameCanvas);

            this._overlayLayer = new OverlayLayer(this);
            container.appendChild(this._overlayLayer.getCanvas());


            // create game scene

            this._gameScene = new GameScene();

            this._game = new Phaser.Game({
                type: Phaser.WEBGL,
                canvas: this._gameCanvas,
                scale: {
                    mode: Phaser.Scale.NONE
                },
                render: {
                    pixelArt: true,
                    transparent: true
                },
                audio: {
                    noAudio: true
                },
                scene: this._gameScene,
            });

            this._sceneRead = false;

            this._gameBooted = false;

            (<any>this._game.config).postBoot = () => {
                this.onGameBoot();
            };

            // init managers and factories

            this._sceneMaker = new SceneMaker(this.getGameScene());
            this._dropManager = new DropManager(this);
            this._cameraManager = new CameraManager(this);
            this._selectionManager = new SelectionManager(this);
            this._actionManager = new ActionManager(this);

        }

        async setInput(file: colibri.core.io.FilePath) {
            super.setInput(file);

            if (this._gameBooted) {
                await this.readScene();
            }
        }

        private async readScene() {

            this._sceneRead = true;

            try {

                const file = this.getInput();

                await colibri.ui.ide.FileUtils.preloadFileString(file);

                const content = colibri.ui.ide.FileUtils.getFileString(file);

                const data = JSON.parse(content);

                if (json.SceneParser.isValidSceneDataFormat(data)) {

                    const parser = new json.SceneParser(this.getGameScene());

                    await parser.createSceneCache_async(data);

                    await parser.createScene(data);

                } else {
                    alert("Invalid file format.");
                }

            } catch (e) {
                alert(e.message);
                throw e;
            }
        }

        getSelectedGameObjects() {
            return this.getSelection()
                .filter(obj => obj instanceof Phaser.GameObjects.GameObject)
                .map(obj => <Phaser.GameObjects.GameObject>obj);
        }

        getActionManager() {
            return this._actionManager;
        }

        getSelectionManager() {
            return this._selectionManager;
        }

        getOverlayLayer() {
            return this._overlayLayer;
        }

        getGameCanvas() {
            return this._gameCanvas;
        }

        getGameScene() {
            return this._gameScene;
        }

        getGame() {
            return this._game;
        }

        getSceneMaker() {
            return this._sceneMaker;
        }

        layout() {

            super.layout();

            if (!this._gameBooted) {
                return;
            }

            this._overlayLayer.resizeTo();

            const parent = this._gameCanvas.parentElement;
            const w = parent.clientWidth;
            const h = parent.clientHeight;

            this._game.scale.resize(w, h);
            this._gameScene.scale.resize(w, h);
            this._gameScene.getCamera().setSize(w, h);

            this.repaint();
        }

        getPropertyProvider() {
            return this._propertyProvider;
        }

        async onPartActivated() {

            super.onPartActivated();

            if (this._blocksProvider) {

                await this._blocksProvider.preload()

                this._blocksProvider.repaint();
            }
        }

        getEditorViewerProvider(key: string) {

            switch (key) {
                case phasereditor2d.blocks.ui.views.BlocksView.EDITOR_VIEWER_PROVIDER_KEY:
                    return this._blocksProvider;

                case phasereditor2d.outline.ui.views.OutlineView.EDITOR_VIEWER_PROVIDER_KEY:
                    return this._outlineProvider;

                default:
                    break;
            }

            return null;
        }

        getOutlineProvider() {
            return this._outlineProvider;
        }

        refreshOutline() {
            this._outlineProvider.repaint();
        }

        private async onGameBoot() {
            this._gameBooted = true;

            if (!this._sceneRead) {
                await this.readScene();
            }

            this.layout();

            this.refreshOutline();

            // for some reason, we should do this after a time, or the game is not stopped well.
            setTimeout(() => this._game.loop.stop(), 500);
        }

        repaint(): void {

            if (!this._gameBooted) {
                return;
            }

            this._game.loop.tick();

            this._overlayLayer.render();
        }
    }

}