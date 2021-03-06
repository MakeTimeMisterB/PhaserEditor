var PhaserEditor2D;
(function (PhaserEditor2D) {
    var Editor = (function () {
        function Editor() {
            this._closed = false;
            this._isReloading = false;
            this.selection = [];
            Editor._instance = this;
            this.openSocket();
        }
        Editor.prototype.hitTestPointer = function (scene, pointer) {
            var input = scene.game.input;
            var real = input.real_hitTest;
            var fake = input.hitTest;
            input.hitTest = real;
            var result = scene.input.hitTestPointer(pointer);
            input.hitTest = fake;
            return result;
        };
        Editor.getInstance = function () {
            return Editor._instance;
        };
        Editor.prototype.repaint = function () {
            consoleLog("repaint");
            this._game.loop.tick();
        };
        Editor.prototype.stop = function () {
            consoleLog("loop.stop");
            this._game.loop.stop();
        };
        Editor.prototype.getCreate = function () {
            return this._create;
        };
        Editor.prototype.getGame = function () {
            return this._game;
        };
        Editor.prototype.getObjectScene = function () {
            return this._objectScene;
        };
        Editor.prototype.getToolScene = function () {
            return this.getObjectScene().getToolScene();
        };
        Editor.prototype.sceneCreated = function () {
            var self = this;
            this._game.canvas.addEventListener("mousedown", function (e) {
                if (self._closed) {
                    return;
                }
                if (self.getToolScene().containsPointer()) {
                    self.getToolScene().onToolsMouseDown();
                }
                else {
                    self.getObjectScene().getDragCameraManager().onMouseDown(e);
                    self.getObjectScene().getPickManager().onMouseDown(e);
                    var dragging = self.getObjectScene().getDragObjectsManager().onMouseDown(e);
                    if (!dragging) {
                        self.getToolScene().onSelectionDragMouseDown(e);
                    }
                }
            });
            this._game.canvas.addEventListener("mousemove", function (e) {
                if (self._closed) {
                    return;
                }
                if (self.getToolScene().isEditing()) {
                    self.getToolScene().onToolsMouseMove();
                }
                else {
                    self.getObjectScene().getDragObjectsManager().onMouseMove(e);
                    self.getObjectScene().getDragCameraManager().onMouseMove(e);
                    var repaint = self.getToolScene().onSelectionDragMouseMove(e);
                    if (repaint) {
                        self.repaint();
                    }
                }
            });
            this._game.canvas.addEventListener("mouseup", function (e) {
                if (self._closed) {
                    return;
                }
                if (self.getToolScene().isEditing()) {
                    self.getToolScene().onToolsMouseUp();
                }
                else {
                    self.getObjectScene().getDragCameraManager().onMouseUp();
                    var found = self.getObjectScene().getPickManager().onMouseUp(e);
                    self.getObjectScene().getDragObjectsManager().onMouseUp();
                    if (found) {
                        self.getToolScene().selectionDragClear();
                    }
                    else {
                        self.getToolScene().onSelectionDragMouseUp(e);
                    }
                }
            });
            this._game.canvas.addEventListener("mouseleave", function () {
                if (self._closed) {
                    return;
                }
                self.getObjectScene().getDragObjectsManager().onMouseUp();
                self.getObjectScene().getDragCameraManager().onMouseUp();
            });
            this.sendMessage({
                method: "GetInitialState"
            });
        };
        Editor.prototype.sendKeyDown = function (e) {
            var data = {
                keyCode: e.keyCode,
                ctrlKey: e.ctrlKey || e.metaKey,
                shiftKey: e.shiftKey
            };
            this.sendMessage({
                method: "KeyDown",
                data: data
            });
        };
        Editor.prototype.onResize = function () {
            for (var _i = 0, _a = this._game.scene.scenes; _i < _a.length; _i++) {
                var scene = _a[_i];
                var scene2 = scene;
                scene2.cameras.main.setSize(window.innerWidth, window.innerHeight);
                scene2.scale.resize(window.innerWidth, window.innerHeight);
            }
            this.repaint();
        };
        Editor.prototype.openSocket = function () {
            consoleLog("Open socket");
            this._socket = new WebSocket(this.getWebSocketUrl());
            var self = this;
            this._socket.onopen = function () {
                self.sendMessage({
                    method: "GetCreateGame"
                });
            };
            this._socket.onmessage = function (event) {
                var msg = JSON.parse(event.data);
                self.onServerMessage(msg);
            };
            this._socket.onclose = function (event) {
                self.onClosedSocket();
            };
            window.addEventListener("beforeunload", function (event) {
                if (self._socket) {
                    consoleLog("Closing socket...");
                    self.closeSocket();
                }
            });
        };
        Editor.prototype.closeSocket = function () {
            this._socket.onclose = function () { };
            this._socket.close();
        };
        Editor.prototype.onClosedSocket = function () {
            consoleLog("Socket closed");
            if (this._isReloading) {
                consoleLog("Closed because a reload.");
                return;
            }
            this._closed = true;
            var body = document.getElementById("body");
            var elem = document.createElement("div");
            elem.innerHTML = "<p><br><br><br>Lost the connection with Phaser Editor</p><button onclick='document.location.reload()'>Reload</button>";
            elem.setAttribute("class", "lostConnection");
            body.appendChild(elem);
        };
        Editor.prototype.onSelectObjects = function (msg) {
            this.selection = msg.objectIds;
            this.getToolScene().updateSelectionObjects();
            var list = [];
            var point = new Phaser.Math.Vector2(0, 0);
            var tx = new Phaser.GameObjects.Components.TransformMatrix();
            for (var _i = 0, _a = this.getToolScene().getSelectedObjects(); _i < _a.length; _i++) {
                var obj = _a[_i];
                var objTx = obj;
                objTx.getWorldTransformMatrix(tx);
                tx.transformPoint(0, 0, point);
                var info = {
                    id: obj.name
                };
                if (obj instanceof Phaser.GameObjects.BitmapText) {
                    info.displayWidth = obj.width;
                    info.displayHeight = obj.height;
                }
                else {
                    info.displayWidth = obj.displayWidth;
                    info.displayHeight = obj.displayHeight;
                }
                list.push(info);
            }
            this.sendMessage({
                method: "SetObjectDisplayProperties",
                list: list
            });
        };
        ;
        Editor.prototype.onUpdateObjects = function (msg) {
            var list = msg.objects;
            for (var i = 0; i < list.length; i++) {
                var objData = list[i];
                var id = objData["-id"];
                var obj = this._objectScene.sys.displayList.getByName(id);
                this._create.updateObject(obj, objData);
            }
        };
        Editor.prototype.onReloadPage = function () {
            this._isReloading = true;
            this._socket.close();
            window.location.reload();
        };
        Editor.prototype.onUpdateSceneProperties = function (msg) {
            this.sceneProperties = msg.sceneProperties;
            this.getObjectScene().updateBackground();
            this.getToolScene().updateFromSceneProperties();
            this.updateBodyColor();
        };
        Editor.prototype.updateBodyColor = function () {
            var body = document.getElementsByTagName("body")[0];
            body.style.backgroundColor = "rgb(" + PhaserEditor2D.ScenePropertiesComponent.get_backgroundColor(this.sceneProperties) + ")";
        };
        Editor.prototype.onCreateGame = function (msg) {
            var self = this;
            this._webgl = msg.webgl;
            this._chromiumWebview = msg.chromiumWebview;
            this.sceneProperties = msg.sceneProperties;
            this._create = new PhaserEditor2D.Create();
            this._game = new Phaser.Game({
                title: "Phaser Editor 2D - Web Scene Editor",
                width: window.innerWidth,
                height: window.innerWidth,
                type: this._webgl ? Phaser.WEBGL : Phaser.CANVAS,
                render: {
                    pixelArt: true
                },
                audio: {
                    noAudio: true
                },
                url: "https://phasereditor2d.com",
                scale: {
                    mode: Phaser.Scale.RESIZE
                }
            });
            this._game.config.postBoot = function (game) {
                consoleLog("Game booted");
                setTimeout(function () { return self.stop(); }, 500);
            };
            var input = this._game.input;
            input.real_hitTest = input.hitTest;
            input.hitTest = function () {
                return [];
            };
            this._objectScene = new PhaserEditor2D.ObjectScene();
            this._game.scene.add("ObjectScene", this._objectScene);
            this._game.scene.add("ToolScene", PhaserEditor2D.ToolScene);
            this._game.scene.start("ObjectScene", {
                displayList: msg.displayList,
                projectUrl: msg.projectUrl,
                pack: msg.pack
            });
            this._resizeToken = 0;
            window.addEventListener('resize', function (event) {
                if (self._closed) {
                    return;
                }
                self._resizeToken += 1;
                setTimeout((function (token) {
                    return function () {
                        if (token === self._resizeToken) {
                            self.onResize();
                        }
                    };
                })(self._resizeToken), 200);
            }, false);
            window.addEventListener("wheel", function (e) {
                if (self._closed) {
                    return;
                }
                self.getObjectScene().onMouseWheel(e);
                self.repaint();
            });
            this.updateBodyColor();
        };
        Editor.prototype.snapValueX = function (x) {
            var props = this.sceneProperties;
            if (PhaserEditor2D.ScenePropertiesComponent.get_snapEnabled(props)) {
                var snap = PhaserEditor2D.ScenePropertiesComponent.get_snapWidth(props);
                return Math.round(x / snap) * snap;
            }
            return x;
        };
        Editor.prototype.snapValueY = function (y) {
            var props = this.sceneProperties;
            if (PhaserEditor2D.ScenePropertiesComponent.get_snapEnabled(props)) {
                var snap = PhaserEditor2D.ScenePropertiesComponent.get_snapHeight(props);
                return Math.round(y / snap) * snap;
            }
            return y;
        };
        Editor.prototype.onDropObjects = function (msg) {
            consoleLog("onDropObjects()");
            var list = msg.list;
            for (var _i = 0, list_1 = list; _i < list_1.length; _i++) {
                var model = list_1[_i];
                this._create.createObject(this.getObjectScene(), model);
            }
            this.repaint();
        };
        Editor.prototype.onDeleteObjects = function (msg) {
            var scene = this.getObjectScene();
            var list = msg.list;
            for (var _i = 0, list_2 = list; _i < list_2.length; _i++) {
                var id = list_2[_i];
                var obj = scene.sys.displayList.getByName(id);
                if (obj) {
                    obj.destroy();
                }
            }
        };
        Editor.prototype.onResetScene = function (msg) {
            var scene = this.getObjectScene();
            scene.removeAllObjects();
            this._create.createWorld(scene, msg.displayList);
        };
        Editor.prototype.onRunPositionAction = function (msg) {
            var actionName = msg.action;
            var action;
            switch (actionName) {
                case "Align":
                    action = new PhaserEditor2D.AlignAction(msg);
                    break;
            }
            if (action) {
                action.run();
            }
        };
        Editor.prototype.onServerMessage = function (batch) {
            consoleLog("onServerMessage:");
            consoleLog(batch);
            consoleLog("----");
            var list = batch.list;
            this.processMessageList(0, list);
        };
        ;
        Editor.prototype.onLoadAssets = function (index, list) {
            var _this = this;
            var loadMsg = list[index];
            var self = this;
            if (loadMsg.pack) {
                var scene = this.getObjectScene();
                Editor.getInstance().stop();
                scene.load.once(Phaser.Loader.Events.COMPLETE, (function (index2, list2) {
                    return function () {
                        consoleLog("Loader complete.");
                        self.processMessageList(index2, list2);
                    };
                })(index + 1, list), this);
                consoleLog("Load: ");
                consoleLog(loadMsg.pack);
                scene.load.crossOrigin = "anonymous";
                scene.load.addPack(loadMsg.pack);
                scene.load.start();
                setTimeout(function () { return _this.repaint(); }, 100);
            }
            else {
                this.processMessageList(index + 1, list);
            }
        };
        Editor.prototype.onSetObjectOriginKeepPosition = function (msg) {
            var list = msg.list;
            var value = msg.value;
            var is_x_axis = msg.axis === "x";
            var displayList = this.getObjectScene().sys.displayList;
            var point = new Phaser.Math.Vector2();
            var tx = new Phaser.GameObjects.Components.TransformMatrix();
            var data = [];
            for (var _i = 0, list_3 = list; _i < list_3.length; _i++) {
                var id = list_3[_i];
                var obj = displayList.getByName(id);
                var x = -obj.width * obj.originX;
                var y = -obj.height * obj.originY;
                obj.getWorldTransformMatrix(tx);
                tx.transformPoint(x, y, point);
                data.push({
                    obj: obj,
                    x: point.x,
                    y: point.y
                });
            }
            for (var _a = 0, data_1 = data; _a < data_1.length; _a++) {
                var item = data_1[_a];
                var obj = item.obj;
                if (is_x_axis) {
                    obj.setOrigin(value, obj.originY);
                }
                else {
                    obj.setOrigin(obj.originX, value);
                }
            }
            this.repaint();
            var list2 = [];
            for (var _b = 0, data_2 = data; _b < data_2.length; _b++) {
                var item = data_2[_b];
                var obj = item.obj;
                var x = -obj.width * obj.originX;
                var y = -obj.height * obj.originY;
                obj.getWorldTransformMatrix(tx);
                tx.transformPoint(x, y, point);
                obj.x += item.x - point.x;
                obj.y += item.y - point.y;
                list2.push({
                    id: obj.name,
                    originX: obj.originX,
                    originY: obj.originY,
                    x: obj.x,
                    y: obj.y
                });
            }
            Editor.getInstance().sendMessage({
                method: "SetObjectOrigin",
                list: list2
            });
        };
        Editor.prototype.onSetCameraState = function (msg) {
            var cam = this.getObjectScene().cameras.main;
            if (msg.cameraState.scrollX !== undefined) {
                cam.scrollX = msg.cameraState.scrollX;
                cam.scrollY = msg.cameraState.scrollY;
                cam.zoom = msg.cameraState.zoom;
            }
        };
        Editor.prototype.onSetInteractiveTool = function (msg) {
            var tools = [];
            for (var _i = 0, _a = msg.list; _i < _a.length; _i++) {
                var name_1 = _a[_i];
                var tools2 = PhaserEditor2D.ToolFactory.createByName(name_1);
                for (var _b = 0, tools2_1 = tools2; _b < tools2_1.length; _b++) {
                    var tool = tools2_1[_b];
                    tools.push(tool);
                }
            }
            this._transformLocalCoords = msg.transformLocalCoords;
            this.getToolScene().setTools(tools);
        };
        Editor.prototype.isTransformLocalCoords = function () {
            return this._transformLocalCoords;
        };
        Editor.prototype.isWebGL = function () {
            return this._webgl;
        };
        Editor.prototype.isChromiumWebview = function () {
            return this._chromiumWebview;
        };
        Editor.prototype.onSetTransformCoords = function (msg) {
            this._transformLocalCoords = msg.transformLocalCoords;
        };
        Editor.prototype.onGetPastePosition = function (msg) {
            var x = 0;
            var y = 0;
            if (msg.placeAtCursorPosition) {
                var pointer = this.getObjectScene().input.activePointer;
                var point = this.getObjectScene().getScenePoint(pointer.x, pointer.y);
                x = point.x;
                y = point.y;
            }
            else {
                var cam = this.getObjectScene().cameras.main;
                x = cam.midPoint.x;
                y = cam.midPoint.y;
            }
            this.sendMessage({
                method: "PasteEvent",
                parent: msg.parent,
                x: x,
                y: y
            });
        };
        Editor.prototype.onRevealObject = function (msg) {
            var sprite = this.getObjectScene().sys.displayList.getByName(msg.id);
            if (sprite) {
                var tx = sprite.getWorldTransformMatrix();
                var p = new Phaser.Math.Vector2();
                tx.transformPoint(0, 0, p);
                var cam = this.getObjectScene().cameras.main;
                cam.setScroll(p.x - cam.width / 2, p.y - cam.height / 2);
            }
        };
        Editor.prototype.processMessageList = function (startIndex, list) {
            for (var i = startIndex; i < list.length; i++) {
                var msg = list[i];
                var method = msg.method;
                switch (method) {
                    case "ReloadPage":
                        this.onReloadPage();
                        break;
                    case "CreateGame":
                        this.onCreateGame(msg);
                        break;
                    case "UpdateObjects":
                        this.onUpdateObjects(msg);
                        break;
                    case "SelectObjects":
                        this.onSelectObjects(msg);
                        break;
                    case "UpdateSceneProperties":
                        this.onUpdateSceneProperties(msg);
                        break;
                    case "DropObjects":
                        this.onDropObjects(msg);
                        break;
                    case "DeleteObjects":
                        this.onDeleteObjects(msg);
                        break;
                    case "ResetScene":
                        this.onResetScene(msg);
                        break;
                    case "RunPositionAction":
                        this.onRunPositionAction(msg);
                        break;
                    case "LoadAssets":
                        this.onLoadAssets(i, list);
                        return;
                    case "SetObjectOriginKeepPosition":
                        this.onSetObjectOriginKeepPosition(msg);
                        break;
                    case "SetCameraState":
                        this.onSetCameraState(msg);
                        break;
                    case "SetInteractiveTool":
                        this.onSetInteractiveTool(msg);
                        break;
                    case "SetTransformCoords":
                        this.onSetTransformCoords(msg);
                        break;
                    case "GetPastePosition":
                        this.onGetPastePosition(msg);
                        break;
                    case "RevealObject":
                        this.onRevealObject(msg);
                        break;
                }
            }
            this.repaint();
        };
        Editor.prototype.sendMessage = function (msg) {
            consoleLog("Sending message:");
            consoleLog(msg);
            consoleLog("----");
            this._socket.send(JSON.stringify(msg));
        };
        Editor.prototype.getWebSocketUrl = function () {
            var loc = document.location;
            var channel = this.getChannelId();
            return "ws://" + loc.host + "/ws/api?channel=" + channel;
        };
        Editor.prototype.getChannelId = function () {
            var s = document.location.search;
            var i = s.indexOf("=");
            var c = s.substring(i + 1);
            return c;
        };
        Editor.prototype.getWorldBounds = function (sprite, points) {
            var w = sprite.width;
            var h = sprite.height;
            if (sprite instanceof Phaser.GameObjects.BitmapText) {
                w = w / sprite.scaleX;
                h = h / sprite.scaleY;
            }
            var flipX = sprite.flipX ? -1 : 1;
            var flipY = sprite.flipY ? -1 : 1;
            if (sprite instanceof Phaser.GameObjects.TileSprite) {
                flipX = 1;
                flipY = 1;
            }
            var ox = sprite.originX;
            var oy = sprite.originY;
            var x = -w * ox * flipX;
            var y = -h * oy * flipY;
            var worldTx = sprite.getWorldTransformMatrix();
            worldTx.transformPoint(x, y, points[0]);
            worldTx.transformPoint(x + w * flipX, y, points[1]);
            worldTx.transformPoint(x + w * flipX, y + h * flipY, points[2]);
            worldTx.transformPoint(x, y + h * flipY, points[3]);
            var cam = this.getObjectScene().cameras.main;
            for (var _i = 0, points_1 = points; _i < points_1.length; _i++) {
                var p = points_1[_i];
                p.set((p.x - cam.scrollX) * cam.zoom, (p.y - cam.scrollY) * cam.zoom);
            }
        };
        return Editor;
    }());
    PhaserEditor2D.Editor = Editor;
    var PaintDelayUtil = (function () {
        function PaintDelayUtil() {
            this._delayPaint = Editor.getInstance().isChromiumWebview();
        }
        PaintDelayUtil.prototype.startPaintLoop = function () {
            if (this._delayPaint) {
                this._now = Date.now();
            }
        };
        PaintDelayUtil.prototype.shouldPaintThisTime = function () {
            if (this._delayPaint) {
                var now = Date.now();
                if (now - this._now > 40) {
                    this._now = now;
                    return true;
                }
                return false;
            }
            return true;
        };
        return PaintDelayUtil;
    }());
    PhaserEditor2D.PaintDelayUtil = PaintDelayUtil;
})(PhaserEditor2D || (PhaserEditor2D = {}));
