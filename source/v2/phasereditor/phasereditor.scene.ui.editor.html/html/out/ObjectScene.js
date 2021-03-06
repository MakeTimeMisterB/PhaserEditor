var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var PhaserEditor2D;
(function (PhaserEditor2D) {
    var ObjectScene = (function (_super) {
        __extends(ObjectScene, _super);
        function ObjectScene() {
            return _super.call(this, "ObjectScene") || this;
        }
        ObjectScene.prototype.init = function (initData) {
            this._initData = initData;
        };
        ObjectScene.prototype.preload = function () {
            consoleLog("preload()");
            this.load.setBaseURL(this._initData.projectUrl);
            this.load.pack("pack", this._initData.pack);
        };
        ObjectScene.prototype.create = function () {
            var editor = PhaserEditor2D.Editor.getInstance();
            this._dragCameraManager = new DragCameraManager(this);
            this._dragObjectsManager = new DragObjectsManager();
            this._pickManager = new PickObjectManager();
            new DropManager();
            this.initCamera();
            this.initSelectionScene();
            editor.getCreate().createWorld(this, this._initData.displayList);
            editor.sceneCreated();
            this.sendRecordCameraStateMessage();
            editor.stop();
        };
        ObjectScene.prototype.updateBackground = function () {
            var rgb = "rgb(" + PhaserEditor2D.ScenePropertiesComponent.get_backgroundColor(PhaserEditor2D.Editor.getInstance().sceneProperties) + ")";
            this.cameras.main.setBackgroundColor(rgb);
        };
        ObjectScene.prototype.getPickManager = function () {
            return this._pickManager;
        };
        ObjectScene.prototype.getDragCameraManager = function () {
            return this._dragCameraManager;
        };
        ObjectScene.prototype.getDragObjectsManager = function () {
            return this._dragObjectsManager;
        };
        ObjectScene.prototype.removeAllObjects = function () {
            var list = this.sys.displayList.list;
            for (var _i = 0, list_1 = list; _i < list_1.length; _i++) {
                var obj = list_1[_i];
                obj.destroy();
            }
            this.sys.displayList.removeAll(false);
        };
        ObjectScene.prototype.getScenePoint = function (pointerX, pointerY) {
            var cam = this.cameras.main;
            var sceneX = pointerX / cam.zoom + cam.scrollX;
            var sceneY = pointerY / cam.zoom + cam.scrollY;
            return new Phaser.Math.Vector2(sceneX, sceneY);
        };
        ObjectScene.prototype.initSelectionScene = function () {
            this.scene.launch("ToolScene");
            this._toolScene = this.scene.get("ToolScene");
        };
        ObjectScene.prototype.initCamera = function () {
            var cam = this.cameras.main;
            cam.setOrigin(0, 0);
            cam.setRoundPixels(true);
            this.updateBackground();
            this.scale.resize(window.innerWidth, window.innerHeight);
        };
        ;
        ObjectScene.prototype.getToolScene = function () {
            return this._toolScene;
        };
        ObjectScene.prototype.onMouseWheel = function (e) {
            var cam = this.cameras.main;
            var delta = e.deltaY;
            var zoom = (delta > 0 ? 0.9 : 1.1);
            var pointer = this.input.activePointer;
            var point1 = cam.getWorldPoint(pointer.x, pointer.y);
            cam.zoom *= zoom;
            cam.preRender(this.scale.resolution);
            var point2 = cam.getWorldPoint(pointer.x, pointer.y);
            var dx = point2.x - point1.x;
            var dy = point2.y - point1.y;
            cam.scrollX += -dx;
            cam.scrollY += -dy;
            this.sendRecordCameraStateMessage();
        };
        ObjectScene.prototype.sendRecordCameraStateMessage = function () {
            var cam = this.cameras.main;
            PhaserEditor2D.Editor.getInstance().sendMessage({
                method: "RecordCameraState",
                cameraState: {
                    scrollX: cam.scrollX,
                    scrollY: cam.scrollY,
                    width: cam.width,
                    height: cam.height,
                    zoom: cam.zoom
                }
            });
        };
        ObjectScene.prototype.performResize = function () {
            this.cameras.main.setSize(window.innerWidth, window.innerHeight);
        };
        return ObjectScene;
    }(Phaser.Scene));
    PhaserEditor2D.ObjectScene = ObjectScene;
    var PickObjectManager = (function () {
        function PickObjectManager() {
            this._temp = [
                new Phaser.Math.Vector2(0, 0),
                new Phaser.Math.Vector2(0, 0),
                new Phaser.Math.Vector2(0, 0),
                new Phaser.Math.Vector2(0, 0)
            ];
        }
        PickObjectManager.prototype.onMouseDown = function (e) {
            this._down = e;
        };
        PickObjectManager.prototype.onMouseUp = function (e) {
            if (!this._down || this._down.x !== e.x || this._down.y !== e.y || !PhaserEditor2D.isLeftButton(this._down)) {
                return null;
            }
            var editor = PhaserEditor2D.Editor.getInstance();
            var scene = editor.getObjectScene();
            var pointer = scene.input.activePointer;
            var result = editor.hitTestPointer(scene, pointer);
            consoleLog(result);
            var gameObj = result.pop();
            editor.sendMessage({
                method: "ClickObject",
                ctrl: e.ctrlKey,
                shift: e.shiftKey,
                id: gameObj ? gameObj.name : undefined
            });
            return gameObj;
        };
        PickObjectManager.prototype.selectArea = function (start, end) {
            console.log("---");
            var editor = PhaserEditor2D.Editor.getInstance();
            var scene = editor.getObjectScene();
            var list = scene.children.getAll();
            var x = start.x;
            var y = start.y;
            var width = end.x - start.x;
            var height = end.y - start.y;
            if (width < 0) {
                x = end.x;
                width = -width;
            }
            if (height < 0) {
                y = end.y;
                height = -height;
            }
            var area = new Phaser.Geom.Rectangle(x, y, width, height);
            var selection = [];
            for (var _i = 0, list_2 = list; _i < list_2.length; _i++) {
                var obj = list_2[_i];
                if (obj.name) {
                    var sprite = obj;
                    var points = this._temp;
                    editor.getWorldBounds(sprite, points);
                    if (area.contains(points[0].x, points[0].y)
                        && area.contains(points[1].x, points[1].y)
                        && area.contains(points[2].x, points[2].y)
                        && area.contains(points[3].x, points[3].y)) {
                        selection.push(sprite.name);
                    }
                }
            }
            editor.sendMessage({
                method: "SetSelection",
                list: selection
            });
        };
        return PickObjectManager;
    }());
    var DragObjectsManager = (function () {
        function DragObjectsManager() {
            this._startPoint = null;
            this._dragging = false;
            this._now = 0;
            this._paintDelayUtil = new PhaserEditor2D.PaintDelayUtil();
        }
        DragObjectsManager.prototype.getScene = function () {
            return PhaserEditor2D.Editor.getInstance().getObjectScene();
        };
        DragObjectsManager.prototype.getSelectedObjects = function () {
            return PhaserEditor2D.Editor.getInstance().getToolScene().getSelectedObjects();
        };
        DragObjectsManager.prototype.getPointer = function () {
            return this.getScene().input.activePointer;
        };
        DragObjectsManager.prototype.onMouseDown = function (e) {
            if (!PhaserEditor2D.isLeftButton(e)) {
                return false;
            }
            var set1 = new Phaser.Structs.Set(PhaserEditor2D.Editor.getInstance().hitTestPointer(this.getScene(), this.getPointer()));
            var set2 = new Phaser.Structs.Set(this.getSelectedObjects());
            var hit = set1.intersect(set2).size > 0;
            if (!hit) {
                return false;
            }
            this._paintDelayUtil.startPaintLoop();
            this._startPoint = this.getScene().getScenePoint(this.getPointer().x, this.getPointer().y);
            var tx = new Phaser.GameObjects.Components.TransformMatrix();
            var p = new Phaser.Math.Vector2();
            for (var _i = 0, _a = this.getSelectedObjects(); _i < _a.length; _i++) {
                var obj = _a[_i];
                var sprite = obj;
                sprite.getWorldTransformMatrix(tx);
                tx.transformPoint(0, 0, p);
                sprite.setData("DragObjectsManager", {
                    initX: p.x,
                    initY: p.y
                });
            }
            return true;
        };
        DragObjectsManager.prototype.onMouseMove = function (e) {
            if (!PhaserEditor2D.isLeftButton(e) || this._startPoint === null) {
                return;
            }
            this._dragging = true;
            var pos = this.getScene().getScenePoint(this.getPointer().x, this.getPointer().y);
            var dx = pos.x - this._startPoint.x;
            var dy = pos.y - this._startPoint.y;
            for (var _i = 0, _a = this.getSelectedObjects(); _i < _a.length; _i++) {
                var obj = _a[_i];
                var sprite = obj;
                var data = sprite.getData("DragObjectsManager");
                if (!data) {
                    continue;
                }
                var x = PhaserEditor2D.Editor.getInstance().snapValueX(data.initX + dx);
                var y = PhaserEditor2D.Editor.getInstance().snapValueX(data.initY + dy);
                if (sprite.parentContainer) {
                    var tx = sprite.parentContainer.getWorldTransformMatrix();
                    var p = new Phaser.Math.Vector2();
                    tx.applyInverse(x, y, p);
                    sprite.setPosition(p.x, p.y);
                }
                else {
                    sprite.setPosition(x, y);
                }
            }
            if (this._paintDelayUtil.shouldPaintThisTime()) {
                PhaserEditor2D.Editor.getInstance().repaint();
            }
        };
        DragObjectsManager.prototype.onMouseUp = function () {
            if (this._startPoint !== null && this._dragging) {
                this._dragging = false;
                this._startPoint = null;
                PhaserEditor2D.Editor.getInstance().sendMessage(PhaserEditor2D.BuildMessage.SetTransformProperties(this.getSelectedObjects()));
            }
            for (var _i = 0, _a = this.getSelectedObjects(); _i < _a.length; _i++) {
                var obj = _a[_i];
                var sprite = obj;
                if (sprite.data) {
                    sprite.data.remove("DragObjectsManager");
                }
            }
            PhaserEditor2D.Editor.getInstance().repaint();
        };
        return DragObjectsManager;
    }());
    var DragCameraManager = (function () {
        function DragCameraManager(scene) {
            this._scene = scene;
            this._dragStartPoint = null;
        }
        DragCameraManager.prototype.onMouseDown = function (e) {
            if (PhaserEditor2D.isMiddleButton(e)) {
                this._dragStartPoint = new Phaser.Math.Vector2(e.clientX, e.clientY);
                var cam = this._scene.cameras.main;
                this._dragStartCameraScroll = new Phaser.Math.Vector2(cam.scrollX, cam.scrollY);
                e.preventDefault();
            }
        };
        DragCameraManager.prototype.onMouseMove = function (e) {
            if (this._dragStartPoint === null) {
                return;
            }
            var dx = this._dragStartPoint.x - e.clientX;
            var dy = this._dragStartPoint.y - e.clientY;
            var cam = this._scene.cameras.main;
            cam.scrollX = this._dragStartCameraScroll.x + dx / cam.zoom;
            cam.scrollY = this._dragStartCameraScroll.y + dy / cam.zoom;
            PhaserEditor2D.Editor.getInstance().repaint();
            e.preventDefault();
        };
        DragCameraManager.prototype.onMouseUp = function () {
            if (this._dragStartPoint !== null) {
                this._scene.sendRecordCameraStateMessage();
            }
            this._dragStartPoint = null;
            this._dragStartCameraScroll = null;
        };
        return DragCameraManager;
    }());
    var DropManager = (function () {
        function DropManager() {
            window.addEventListener("drop", function (e) {
                var editor = PhaserEditor2D.Editor.getInstance();
                var point = editor.getObjectScene().cameras.main.getWorldPoint(e.clientX, e.clientY);
                editor.sendMessage({
                    method: "DropEvent",
                    x: point.x,
                    y: point.y
                });
            });
            window.addEventListener("dragover", function (e) {
                e.preventDefault();
            });
        }
        return DropManager;
    }());
})(PhaserEditor2D || (PhaserEditor2D = {}));
