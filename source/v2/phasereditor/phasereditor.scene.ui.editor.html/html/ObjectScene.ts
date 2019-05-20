namespace PhaserEditor2D {

    export class ObjectScene extends Phaser.Scene {

        private _toolScene: ToolScene;
        private _dragCameraManager: DragCameraManager;
        private _dragObjectsManager: DragObjectsManager;
        private _pickManager: PickObjectManager;
        private _backgroundScene: Phaser.Scene;
        private _initData: any;

        constructor() {
            super("ObjectScene");
        }

        init(initData) {
            this._initData = initData;
        }

        preload() {
            console.log("preload()");
            this.load.setBaseURL(this._initData.projectUrl);
            this.load.pack("pack", this._initData.pack);
        }

        create() {
            Editor.getInstance().stop();

            this._dragCameraManager = new DragCameraManager(this);

            this._dragObjectsManager = new DragObjectsManager();

            this._pickManager = new PickObjectManager();

            new DropManager();

            this.initCamera();

            this.initSelectionScene();

            const editor = Editor.getInstance();

            this.initBackground();

            editor.getCreate().createWorld(this, this._initData.displayList);

            this.initBackground();

            editor.sceneCreated();

            editor.repaint();

        }

        getPickManager() {
            return this._pickManager;
        }

        getDragCameraManager() {
            return this._dragCameraManager;
        }

        getDragObjectsManager() {
            return this._dragObjectsManager;
        }

        removeAllObjects() {
            let list = this.sys.displayList.list;
            for (let obj of list) {
                obj.destroy();
            }
            this.sys.displayList.removeAll(false);
        }

        getScenePoint(pointerX: number, pointerY: number): any {
            const cam = this.cameras.main;

            const sceneX = pointerX / cam.zoom + cam.scrollX;
            const sceneY = pointerY / cam.zoom + cam.scrollY;

            return new Phaser.Math.Vector2(sceneX, sceneY);
        }

        private initBackground() {
            this.scene.launch("BackgroundScene");
            this._backgroundScene = this.scene.get("BackgroundScene");
            this.scene.moveDown("BackgroundScene");
        }

        private initSelectionScene() {
            this.scene.launch("ToolScene");
            this._toolScene = <ToolScene>this.scene.get("ToolScene");
        }

        private initCamera() {
            var cam = this.cameras.main;
            cam.setOrigin(0, 0);
            cam.setRoundPixels(true);

            this.scale.resize(window.innerWidth, window.innerHeight);
        };

        getToolScene() {
            return this._toolScene;
        }

        getBackgroundScene() {
            return this._backgroundScene;
        }

        onMouseWheel(e: WheelEvent) {
            var cam = this.cameras.main;

            var delta: number = e.deltaY;

            var zoom = (delta > 0 ? 0.9 : 1.1);

            cam.zoom *= zoom;

            this.sendRecordCameraStateMessage();

        }

        sendRecordCameraStateMessage() {
            let cam = this.cameras.main;
            Editor.getInstance().sendMessage({
                method: "RecordCameraState",
                cameraState: {
                    scrollX: cam.scrollX,
                    scrollY: cam.scrollY,
                    zoom: cam.zoom
                }
            });
        }

        performResize() {
            this.cameras.main.setSize(window.innerWidth, window.innerHeight);
            this._backgroundScene.cameras.main.setSize(window.innerWidth, window.innerHeight);
        }
    }

    export class BackgroundScene extends Phaser.Scene {
        private _bg: Phaser.GameObjects.Graphics;

        constructor() {
            super("BackgroundScene");
        }

        create() {
            this._bg = this.add.graphics();
            this.repaint();
        }

        private repaint() {
            this._bg.clear();
            const bgColor = Phaser.Display.Color.RGBStringToColor("rgb(" + ScenePropertiesComponent.get_backgroundColor(Editor.getInstance().sceneProperties) + ")");
            this._bg.fillStyle(bgColor.color, 1);
            this._bg.fillRect(0, 0, window.innerWidth, window.innerHeight);
        }

        update() {
            this.repaint();
        }
    }

    class PickObjectManager {

        onMouseDown(e: MouseEvent) {

            const editor = Editor.getInstance();

            const scene = editor.getObjectScene();
            const pointer = scene.input.activePointer;

            if (e.buttons !== 1) {
                return;
            }

            const result = scene.input.hitTestPointer(pointer);

            console.log(result);

            let gameObj = result.pop();

            editor.sendMessage({
                method: "ClickObject",
                ctrl: e.ctrlKey,
                shift: e.shiftKey,
                id: gameObj ? gameObj.name : undefined
            });

            return gameObj;
        }
    }

    class DragObjectsManager {

        private _startPoint: Phaser.Math.Vector2;
        private _dragging: boolean;

        constructor() {
            this._startPoint = null;
            this._dragging = false;
        }

        private getScene() {
            return Editor.getInstance().getObjectScene();
        }

        private getScelectedObjects() {
            return Editor.getInstance().getToolScene().getSelectedObjects();
        }

        private getPointer() {
            return this.getScene().input.activePointer;
        }

        onMouseDown(e: MouseEvent) {

            if (e.buttons !== 1 || this.getScelectedObjects().length === 0) {
                return;
            }

            this._startPoint = this.getScene().getScenePoint(this.getPointer().x, this.getPointer().y);

            const tx = new Phaser.GameObjects.Components.TransformMatrix();
            const p = new Phaser.Math.Vector2();

            for (let obj of this.getScelectedObjects()) {
                const sprite: Phaser.GameObjects.Sprite = <any>obj;
                sprite.getWorldTransformMatrix(tx);
                tx.transformPoint(0, 0, p);
                sprite.setData("DragObjectsManager", {
                    initX: p.x,
                    initY: p.y
                });
            }
        }

        onMouseMove(e: MouseEvent) {
            if (e.buttons !== 1 || this._startPoint === null) {
                return;
            }

            this._dragging = true;

            const pos = this.getScene().getScenePoint(this.getPointer().x, this.getPointer().y);
            const dx = pos.x - this._startPoint.x;
            const dy = pos.y - this._startPoint.y;

            for (let obj of this.getScelectedObjects()) {
                const sprite: Phaser.GameObjects.Sprite = <any>obj;
                const data = sprite.getData("DragObjectsManager");

                const x = Editor.getInstance().snapValueX(data.initX + dx);
                const y = Editor.getInstance().snapValueX(data.initY + dy);

                if (sprite.parentContainer) {
                    const tx = sprite.parentContainer.getWorldTransformMatrix();
                    const p = new Phaser.Math.Vector2();
                    tx.applyInverse(x, y, p);
                    sprite.setPosition(p.x, p.y);
                } else {
                    sprite.setPosition(x, y);
                }
            }

            Editor.getInstance().repaint();
        }

        onMouseUp() {
            if (this._startPoint !== null && this._dragging) {
                this._dragging = false;
                this._startPoint = null;
                Editor.getInstance().sendMessage(BuildMessage.SetTransformProperties(this.getScelectedObjects()));
            }
        }
    }

    class DragCameraManager {
        private _scene: ObjectScene;
        private _dragStartPoint: Phaser.Math.Vector2;
        private _dragStartCameraScroll: Phaser.Math.Vector2;

        constructor(scene: ObjectScene) {
            this._scene = scene;
            this._dragStartPoint = null;
        }

        onMouseDown(e: MouseEvent) {
            // if middle button peressed
            if (e.buttons === 4) {
                this._dragStartPoint = new Phaser.Math.Vector2(e.clientX, e.clientY);
                const cam = this._scene.cameras.main;
                this._dragStartCameraScroll = new Phaser.Math.Vector2(cam.scrollX, cam.scrollY);

                e.preventDefault();
            }

        }

        onMouseMove(e: MouseEvent) {
            if (this._dragStartPoint === null) {
                return;
            }

            const dx = this._dragStartPoint.x - e.clientX;
            const dy = this._dragStartPoint.y - e.clientY;

            const cam = this._scene.cameras.main;

            cam.scrollX = this._dragStartCameraScroll.x + dx / cam.zoom;
            cam.scrollY = this._dragStartCameraScroll.y + dy / cam.zoom;

            Editor.getInstance().repaint();

            e.preventDefault();
        }

        onMouseUp() {

            if (this._dragStartPoint !== null) {
                this._scene.sendRecordCameraStateMessage();
            }

            this._dragStartPoint = null;
            this._dragStartCameraScroll = null;
        }
    }

    class DropManager {

        constructor() {
            window.addEventListener("drop", function (e) {
                let editor = Editor.getInstance();

                let point = editor.getObjectScene().cameras.main.getWorldPoint(e.clientX, e.clientY);

                editor.sendMessage({
                    method: "DropEvent",
                    x: point.x,
                    y: point.y
                });
            })

            window.addEventListener("dragover", function (e: DragEvent) {
                e.preventDefault();
            });
        }
    }



}