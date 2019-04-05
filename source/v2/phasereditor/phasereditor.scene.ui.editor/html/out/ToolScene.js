var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var PhaserEditor2D;
(function (PhaserEditor2D) {
    var ToolScene = (function (_super) {
        __extends(ToolScene, _super);
        function ToolScene() {
            var _this = _super.call(this, "ToolScene") || this;
            _this._axisToken = null;
            _this._axisLabels = [];
            _this._selectionBoxPoints = [
                new Phaser.Math.Vector2(0, 0),
                new Phaser.Math.Vector2(0, 0),
                new Phaser.Math.Vector2(0, 0),
                new Phaser.Math.Vector2(0, 0)
            ];
            _this._selectedObjects = [];
            _this._selectionGraphics = null;
            return _this;
        }
        ToolScene.prototype.create = function () {
            this.initCamera();
            this._axisToken = "";
            var fg = eval("0x" + PhaserEditor2D.ScenePropertiesComponent.get_foregroundColor(PhaserEditor2D.Models.sceneProperties));
            this._gridGraphics = this.add.graphics();
            this._selectionGraphics = this.add.graphics({
                fillStyle: {
                    color: 0x00ff00
                },
                lineStyle: {
                    color: 0x00ff00,
                    width: 2
                }
            });
        };
        ToolScene.prototype.initCamera = function () {
            this.cameras.main.setRoundPixels(true);
            this.cameras.main.setOrigin(0, 0);
            this.scale.resize(window.innerWidth, window.innerHeight);
        };
        ToolScene.prototype.updateFromSceneProperties = function () {
            this._axisToken = "";
            this.renderAxis();
        };
        ToolScene.prototype.renderAxis = function () {
            var cam = PhaserEditor2D.Editor.getInstance().getObjectScene().cameras.main;
            var w = this.scale.baseSize.width;
            var h = this.scale.baseSize.height;
            var dx = 8;
            var dy = 8;
            var i = 1;
            while (dx * i * cam.zoom < 32) {
                i++;
            }
            dx = dx * i;
            i = 1;
            while (dy * i * cam.zoom < 32) {
                i++;
            }
            dy = dy * i;
            var sx = ((cam.scrollX / dx) | 0) * dx;
            var sy = ((cam.scrollY / dy) | 0) * dy;
            var bx = PhaserEditor2D.ScenePropertiesComponent.get_borderX(PhaserEditor2D.Models.sceneProperties);
            var by = PhaserEditor2D.ScenePropertiesComponent.get_borderY(PhaserEditor2D.Models.sceneProperties);
            var bw = PhaserEditor2D.ScenePropertiesComponent.get_borderWidth(PhaserEditor2D.Models.sceneProperties);
            var bh = PhaserEditor2D.ScenePropertiesComponent.get_borderHeight(PhaserEditor2D.Models.sceneProperties);
            var token = w + "-" + h + "-" + dx + "-" + dy + "-" + cam.zoom + "-" + cam.scrollX + "-" + cam.scrollY
                + "-" + bx + "-" + by + "-" + bw + "-" + bh;
            if (this._axisToken !== null && this._axisToken === token) {
                return;
            }
            this._axisToken = token;
            this._gridGraphics.clear();
            var fg = Phaser.Display.Color.RGBStringToColor("rgb(" + PhaserEditor2D.ScenePropertiesComponent.get_foregroundColor(PhaserEditor2D.Models.sceneProperties) + ")");
            this._gridGraphics.lineStyle(1, fg.color, 0.5);
            for (var _i = 0, _a = this._axisLabels; _i < _a.length; _i++) {
                var label_1 = _a[_i];
                label_1.destroy();
            }
            var label = null;
            var labelHeight = 0;
            for (var x = sx;; x += dx) {
                var x2 = (x - cam.scrollX) * cam.zoom;
                if (x2 > w) {
                    break;
                }
                if (label != null) {
                    if (label.x + label.width * 2 > x2) {
                        continue;
                    }
                }
                label = this.add.text(x2, 0, x.toString());
                label.style.setShadow(1, 1);
                this._axisLabels.push(label);
                labelHeight = label.height;
                label.setOrigin(0.5, 0);
            }
            var labelWidth = 0;
            for (var y = sy;; y += dy) {
                var y2 = (y - cam.scrollY) * cam.zoom;
                if (y2 > h) {
                    break;
                }
                if (y2 < labelHeight) {
                    continue;
                }
                var label_2 = this.add.text(0, y2, (y).toString());
                label_2.style.setShadow(1, 1);
                label_2.setOrigin(0, 0.5);
                this._axisLabels.push(label_2);
                labelWidth = Math.max(label_2.width, labelWidth);
            }
            for (var x = sx;; x += dx) {
                var x2 = (x - cam.scrollX) * cam.zoom;
                if (x2 > w) {
                    break;
                }
                if (x2 < labelWidth) {
                    continue;
                }
                this._gridGraphics.lineBetween(x2, labelHeight, x2, h);
            }
            for (var y = sy;; y += dy) {
                var y2 = (y - cam.scrollY) * cam.zoom;
                if (y2 > h) {
                    break;
                }
                if (y2 < labelHeight) {
                    continue;
                }
                this._gridGraphics.lineBetween(labelWidth, y2, w, y2);
            }
            this._gridGraphics.lineStyle(4, 0x000000, 1);
            this._gridGraphics.strokeRect((bx - cam.scrollX) * cam.zoom, (by - cam.scrollY) * cam.zoom, bw * cam.zoom, bh * cam.zoom);
            this._gridGraphics.lineStyle(2, 0xffffff, 1);
            this._gridGraphics.strokeRect((bx - cam.scrollX) * cam.zoom, (by - cam.scrollY) * cam.zoom, bw * cam.zoom, bh * cam.zoom);
        };
        ToolScene.prototype.updateSelectionObjects = function () {
            this._selectedObjects = [];
            var objectScene = PhaserEditor2D.Editor.getInstance().getObjectScene();
            for (var _i = 0, _a = PhaserEditor2D.Models.selection; _i < _a.length; _i++) {
                var id = _a[_i];
                var obj = objectScene.sys.displayList.getByName(id);
                if (obj) {
                    this._selectedObjects.push(obj);
                }
            }
        };
        ToolScene.prototype.update = function () {
            this.renderAxis();
            this.renderSelection();
        };
        ToolScene.prototype.renderSelection = function () {
            this._selectionGraphics.clear();
            var g2 = this._selectionGraphics;
            var cam = PhaserEditor2D.Editor.getInstance().getObjectScene().cameras.main;
            var point = new Phaser.Math.Vector2(0, 0);
            for (var _i = 0, _a = this._selectedObjects; _i < _a.length; _i++) {
                var obj = _a[_i];
                var worldTx = obj.getWorldTransformMatrix();
                worldTx.transformPoint(0, 0, point);
                point.x = (point.x - cam.scrollX) * cam.zoom;
                point.y = (point.y - cam.scrollY) * cam.zoom;
                this.paintSelectionBox(g2, obj);
            }
        };
        ToolScene.prototype.paintSelectionBox = function (graphics, gameObj) {
            var w = gameObj.width;
            var h = gameObj.height;
            var ox = gameObj.originX;
            var oy = gameObj.originY;
            var x = -w * ox;
            var y = -h * oy;
            var worldTx = gameObj.getWorldTransformMatrix();
            worldTx.transformPoint(x, y, this._selectionBoxPoints[0]);
            worldTx.transformPoint(x + w, y, this._selectionBoxPoints[1]);
            worldTx.transformPoint(x + w, y + h, this._selectionBoxPoints[2]);
            worldTx.transformPoint(x, y + h, this._selectionBoxPoints[3]);
            var cam = PhaserEditor2D.Editor.getInstance().getObjectScene().cameras.main;
            for (var _i = 0, _a = this._selectionBoxPoints; _i < _a.length; _i++) {
                var p = _a[_i];
                p.set((p.x - cam.scrollX) * cam.zoom, (p.y - cam.scrollY) * cam.zoom);
            }
            graphics.lineStyle(4, 0x000000);
            graphics.strokePoints(this._selectionBoxPoints, true);
            graphics.lineStyle(2, 0x00ff00);
            graphics.strokePoints(this._selectionBoxPoints, true);
        };
        return ToolScene;
    }(Phaser.Scene));
    PhaserEditor2D.ToolScene = ToolScene;
})(PhaserEditor2D || (PhaserEditor2D = {}));