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
    PhaserEditor2D.ARROW_LENGTH = 80;
    var InteractiveTool = (function () {
        function InteractiveTool() {
            this.toolScene = PhaserEditor2D.Editor.getInstance().getToolScene();
            this.objScene = PhaserEditor2D.Editor.getInstance().getObjectScene();
            this.requestRepaint = false;
        }
        InteractiveTool.prototype.getObjects = function () {
            var _this = this;
            var sel = this.toolScene.getSelectedObjects();
            return sel.filter(function (obj) { return _this.canEdit(obj); });
        };
        InteractiveTool.prototype.containsPointer = function () {
            return false;
        };
        InteractiveTool.prototype.isEditing = function () {
            return false;
        };
        InteractiveTool.prototype.clear = function () {
        };
        InteractiveTool.prototype.activated = function () {
        };
        InteractiveTool.prototype.update = function () {
            var list = this.getObjects();
            if (list.length === 0) {
                this.clear();
            }
            else {
                this.render(list);
            }
        };
        InteractiveTool.prototype.render = function (objects) {
        };
        InteractiveTool.prototype.onMouseDown = function () {
        };
        InteractiveTool.prototype.onMouseUp = function () {
        };
        InteractiveTool.prototype.onMouseMove = function () {
        };
        InteractiveTool.prototype.getToolPointer = function () {
            return this.toolScene.input.activePointer;
        };
        InteractiveTool.prototype.getScenePoint = function (toolX, toolY) {
            return this.objScene.getScenePoint(toolX, toolY);
        };
        InteractiveTool.prototype.objectGlobalAngle = function (obj) {
            var a = obj.angle;
            var parent = obj.parentContainer;
            if (parent) {
                a += this.objectGlobalAngle(parent);
            }
            return a;
        };
        InteractiveTool.prototype.objectGlobalScale = function (obj) {
            var scaleX = obj.scaleX;
            var scaleY = obj.scaleY;
            var parent = obj.parentContainer;
            if (parent) {
                var parentScale = this.objectGlobalScale(parent);
                scaleX *= parentScale.x;
                scaleY *= parentScale.y;
            }
            return new Phaser.Math.Vector2(scaleX, scaleY);
        };
        InteractiveTool.prototype.angle2 = function (a, b) {
            return this.angle(a.x, a.y, b.x, b.y);
        };
        InteractiveTool.prototype.angle = function (x1, y1, x2, y2) {
            var delta = (x1 * x2 + y1 * y2) / Math.sqrt((x1 * x1 + y1 * y1) * (x2 * x2 + y2 * y2));
            if (delta > 1.0) {
                return 0;
            }
            if (delta < -1.0) {
                return 180;
            }
            return Phaser.Math.RadToDeg(Math.acos(delta));
        };
        InteractiveTool.prototype.snapValueX = function (x) {
            return PhaserEditor2D.Editor.getInstance().snapValueX(x);
        };
        InteractiveTool.prototype.snapValueY = function (y) {
            return PhaserEditor2D.Editor.getInstance().snapValueY(y);
        };
        InteractiveTool.prototype.createArrowShape = function () {
            var s = this.toolScene.add.triangle(0, 0, 0, 0, 12, 0, 6, 12);
            s.setStrokeStyle(1, 0, 0.8);
            return s;
        };
        InteractiveTool.prototype.createRectangleShape = function () {
            var s = this.toolScene.add.rectangle(0, 0, 12, 12);
            s.setStrokeStyle(1, 0, 0.8);
            return s;
        };
        InteractiveTool.prototype.createCircleShape = function () {
            var s = this.toolScene.add.circle(0, 0, 6);
            s.setStrokeStyle(1, 0, 0.8);
            return s;
        };
        InteractiveTool.prototype.createLineShape = function () {
            var s = this.toolScene.add.line();
            return s;
        };
        InteractiveTool.prototype.localToParent = function (sprite, point) {
            var result = new Phaser.Math.Vector2();
            var tx = new Phaser.GameObjects.Components.TransformMatrix();
            sprite.getWorldTransformMatrix(tx);
            tx.transformPoint(point.x, point.y, result);
            if (sprite.parentContainer) {
                sprite.parentContainer.getWorldTransformMatrix(tx);
                tx.applyInverse(result.x, result.y, result);
            }
            return result;
        };
        return InteractiveTool;
    }());
    PhaserEditor2D.InteractiveTool = InteractiveTool;
    var SimpleLineTool = (function (_super) {
        __extends(SimpleLineTool, _super);
        function SimpleLineTool(tool1, tool2, color) {
            var _this = _super.call(this) || this;
            _this._color = color;
            _this._tool1 = tool1;
            _this._tool2 = tool2;
            _this._line = _this.createLineShape();
            _this._line.setStrokeStyle(4, 0);
            _this._line.setOrigin(0, 0);
            _this._line.depth = -1;
            _this._line2 = _this.createLineShape();
            _this._line2.setStrokeStyle(2, color);
            _this._line2.setOrigin(0, 0);
            _this._line2.depth = -1;
            return _this;
        }
        SimpleLineTool.prototype.canEdit = function (obj) {
            return this._tool1.canEdit(obj) && this._tool2.canEdit(obj);
        };
        SimpleLineTool.prototype.render = function (objects) {
            this._line.setTo(this._tool1.getX(), this._tool1.getY(), this._tool2.getX(), this._tool2.getY());
            this._line2.setTo(this._tool1.getX(), this._tool1.getY(), this._tool2.getX(), this._tool2.getY());
            this._line.visible = true;
            this._line2.visible = true;
        };
        SimpleLineTool.prototype.clear = function () {
            this._line.visible = false;
            this._line2.visible = false;
        };
        SimpleLineTool.prototype.onMouseDown = function () {
            if (this._tool2.isEditing()) {
                this._line2.strokeColor = 0xffffff;
            }
        };
        SimpleLineTool.prototype.onMouseUp = function () {
            this._line2.strokeColor = this._color;
        };
        return SimpleLineTool;
    }(InteractiveTool));
    PhaserEditor2D.SimpleLineTool = SimpleLineTool;
    var ToolFactory = (function () {
        function ToolFactory() {
        }
        ToolFactory.createByName = function (name) {
            switch (name) {
                case "TileSize": {
                    return [
                        new PhaserEditor2D.TileSizeTool(true, false),
                        new PhaserEditor2D.TileSizeTool(false, true),
                        new PhaserEditor2D.TileSizeTool(true, true)
                    ];
                }
                case "TilePosition": {
                    var toolX = new PhaserEditor2D.TilePositionTool(true, false);
                    var toolY = new PhaserEditor2D.TilePositionTool(false, true);
                    var toolXY = new PhaserEditor2D.TilePositionTool(true, true);
                    return [
                        toolX,
                        toolY,
                        toolXY,
                        new SimpleLineTool(toolXY, toolX, 0xff0000),
                        new SimpleLineTool(toolXY, toolY, 0x00ff00),
                    ];
                }
                case "TileScale": {
                    var toolX = new PhaserEditor2D.TileScaleTool(true, false);
                    var toolY = new PhaserEditor2D.TileScaleTool(false, true);
                    var toolXY = new PhaserEditor2D.TileScaleTool(true, true);
                    return [
                        toolX,
                        toolY,
                        toolXY,
                        new SimpleLineTool(toolXY, toolX, 0xff0000),
                        new SimpleLineTool(toolXY, toolY, 0x00ff00),
                    ];
                }
                case "Origin": {
                    var toolX = new PhaserEditor2D.OriginTool(true, false);
                    var toolY = new PhaserEditor2D.OriginTool(false, true);
                    var toolXY = new PhaserEditor2D.OriginTool(true, true);
                    return [
                        toolX,
                        toolY,
                        toolXY,
                        new SimpleLineTool(toolXY, toolX, 0xff0000),
                        new SimpleLineTool(toolXY, toolY, 0x00ff00),
                    ];
                }
                case "Angle": {
                    var tool = new PhaserEditor2D.AngleTool();
                    return [
                        tool,
                        new PhaserEditor2D.AngleLineTool(tool, true),
                        new PhaserEditor2D.AngleLineTool(tool, false)
                    ];
                }
                case "Scale": {
                    return [
                        new PhaserEditor2D.ScaleTool(true, false),
                        new PhaserEditor2D.ScaleTool(false, true),
                        new PhaserEditor2D.ScaleTool(true, true)
                    ];
                }
                case "Position": {
                    var toolX = new PhaserEditor2D.PositionTool(true, false);
                    var toolY = new PhaserEditor2D.PositionTool(false, true);
                    var toolXY = new PhaserEditor2D.PositionTool(true, true);
                    return [
                        toolX,
                        toolY,
                        toolXY,
                        new SimpleLineTool(toolXY, toolX, 0xff0000),
                        new SimpleLineTool(toolXY, toolY, 0x00ff00),
                    ];
                }
                case "Hand": {
                    return [new PhaserEditor2D.HandTool()];
                }
            }
            return [];
        };
        return ToolFactory;
    }());
    PhaserEditor2D.ToolFactory = ToolFactory;
})(PhaserEditor2D || (PhaserEditor2D = {}));
