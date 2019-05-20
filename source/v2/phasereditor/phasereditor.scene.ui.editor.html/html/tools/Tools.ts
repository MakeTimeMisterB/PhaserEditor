namespace PhaserEditor2D {

    export const ARROW_LENGTH = 80;

    export abstract class InteractiveTool {

        protected toolScene: ToolScene = Editor.getInstance().getToolScene();
        protected objScene: ObjectScene = Editor.getInstance().getObjectScene();
        requestRepaint = false;

        constructor() {
        }

        abstract canEdit(obj: any): boolean;

        getObjects(): Phaser.GameObjects.GameObject[] {
            const sel = this.toolScene.getSelectedObjects();
            return sel.filter(obj => this.canEdit(obj));
        }

        containsPointer(): boolean {
            return false;
        }

        isEditing() {
            return false;
        }

        clear() {

        }

        activated() {

        }

        update() {
            const list = this.getObjects();

            if (list.length === 0) {
                this.clear();
            } else {
                this.render(list);
            }
        }

        render(objects: Phaser.GameObjects.GameObject[]) {

        }

        onMouseDown() {

        }

        onMouseUp() {

        }

        onMouseMove() {

        }

        protected getToolPointer() {
            return this.toolScene.input.activePointer;
        }

        protected getScenePoint(toolX: number, toolY: number) {
            return this.objScene.getScenePoint(toolX, toolY);
        }

        protected objectGlobalAngle(obj: Phaser.GameObjects.GameObject) {
            let a: number = (<any>obj).angle;

            const parent = obj.parentContainer;

            if (parent) {
                a += this.objectGlobalAngle(parent);
            }

            return a;
        }

        protected objectGlobalScale(obj: Phaser.GameObjects.GameObject) {
            let scaleX: number = (<any>obj).scaleX;
            let scaleY: number = (<any>obj).scaleY;

            const parent = obj.parentContainer;

            if (parent) {
                const parentScale = this.objectGlobalScale(parent);
                scaleX *= parentScale.x;
                scaleY *= parentScale.y;
            }

            return new Phaser.Math.Vector2(scaleX, scaleY);
        }

        protected angle2(a: Phaser.Math.Vector2, b: Phaser.Math.Vector2) {
            return this.angle(a.x, a.y, b.x, b.y);
        }

        protected angle(x1: number, y1: number, x2: number, y2: number) {

            const delta = (x1 * x2 + y1 * y2) / Math.sqrt((x1 * x1 + y1 * y1) * (x2 * x2 + y2 * y2));

            if (delta > 1.0) {
                return 0;
            }

            if (delta < -1.0) {
                return 180;
            }

            return Phaser.Math.RadToDeg(Math.acos(delta));
        }

        protected snapValueX(x: number) {
            return Editor.getInstance().snapValueX(x);
        }

        protected snapValueY(y: number) {
            return Editor.getInstance().snapValueY(y);
        }

        protected createArrowShape() {
            const s = this.toolScene.add.triangle(0, 0, 0, 0, 12, 0, 6, 12);
            s.setStrokeStyle(1, 0, 0.8);
            return s;
        }

        protected createRectangleShape() {
            const s = this.toolScene.add.rectangle(0, 0, 12, 12);
            s.setStrokeStyle(1, 0, 0.8);
            return s;
        }

        protected createCircleShape() {
            const s = this.toolScene.add.circle(0, 0, 6);
            s.setStrokeStyle(1, 0, 0.8);
            return s;
        }

        protected createLineShape() {
            const s = this.toolScene.add.line();
            return s;
        }

        protected localToParent(sprite: Phaser.GameObjects.Sprite, point: Phaser.Math.Vector2) {
            const result = new Phaser.Math.Vector2();
            const tx = new Phaser.GameObjects.Components.TransformMatrix();

            sprite.getWorldTransformMatrix(tx);
            tx.transformPoint(point.x, point.y, result);

            if (sprite.parentContainer) {
                sprite.parentContainer.getWorldTransformMatrix(tx);
                tx.applyInverse(result.x, result.y, result);
            }

            return result;
        }
    }

    export interface SpotTool {
        getX(): number;
        getY(): number;
        canEdit(obj: any): boolean;
        isEditing(): boolean;
    }

    export class SimpleLineTool extends InteractiveTool {

        private _tool1: SpotTool;
        private _tool2: SpotTool;
        private _line: Phaser.GameObjects.Line;
        private _line2: Phaser.GameObjects.Line;
        private _color: number;

        constructor(tool1: SpotTool, tool2: SpotTool, color: number) {
            super();

            this._color = color;

            this._tool1 = tool1;
            this._tool2 = tool2;

            this._line = this.createLineShape();
            this._line.setStrokeStyle(4, 0);
            this._line.setOrigin(0, 0);
            this._line.depth = -1;

            this._line2 = this.createLineShape();
            this._line2.setStrokeStyle(2, color);
            this._line2.setOrigin(0, 0);
            this._line2.depth = -1;
        }

        canEdit(obj: any): boolean {
            return this._tool1.canEdit(obj) && this._tool2.canEdit(obj);
        }

        render(objects: Phaser.GameObjects.GameObject[]) {
            this._line.setTo(this._tool1.getX(), this._tool1.getY(), this._tool2.getX(), this._tool2.getY());
            this._line2.setTo(this._tool1.getX(), this._tool1.getY(), this._tool2.getX(), this._tool2.getY());
            this._line.visible = true;
            this._line2.visible = true;
        }

        clear() {
            this._line.visible = false;
            this._line2.visible = false;
        }

        onMouseDown() {
            if (this._tool2.isEditing()) {
                this._line2.strokeColor = 0xffffff;
            }
        }

        onMouseUp() {
            this._line2.strokeColor = this._color;
        }

    }

    export class ToolFactory {

        static createByName(name: string): InteractiveTool[] {
            switch (name) {
                case "TileSize": {
                    return [
                        new TileSizeTool(true, false),
                        new TileSizeTool(false, true),
                        new TileSizeTool(true, true)
                    ];
                }
                case "TilePosition": {
                    const toolX = new TilePositionTool(true, false);
                    const toolY = new TilePositionTool(false, true);
                    const toolXY = new TilePositionTool(true, true);
                    return [
                        toolX,
                        toolY,
                        toolXY,
                        new SimpleLineTool(toolXY, toolX, 0xff0000),
                        new SimpleLineTool(toolXY, toolY, 0x00ff00),
                    ];
                }
                case "TileScale": {
                    const toolX = new TileScaleTool(true, false);
                    const toolY = new TileScaleTool(false, true);
                    const toolXY = new TileScaleTool(true, true);
                    return [
                        toolX,
                        toolY,
                        toolXY,
                        new SimpleLineTool(toolXY, toolX, 0xff0000),
                        new SimpleLineTool(toolXY, toolY, 0x00ff00),
                    ];
                }
                case "Origin": {
                    const toolX = new OriginTool(true, false);
                    const toolY = new OriginTool(false, true);
                    const toolXY = new OriginTool(true, true);
                    return [
                        toolX,
                        toolY,
                        toolXY,
                        new SimpleLineTool(toolXY, toolX, 0xff0000),
                        new SimpleLineTool(toolXY, toolY, 0x00ff00),
                    ];
                }
                case "Angle": {
                    const tool = new AngleTool();
                    return [
                        tool,
                        new AngleLineTool(tool, true),
                        new AngleLineTool(tool, false)
                    ];
                }
                case "Scale": {
                    return [
                        new ScaleTool(true, false),
                        new ScaleTool(false, true),
                        new ScaleTool(true, true)
                    ]
                }
                case "Position": {
                    const toolX = new PositionTool(true, false);
                    const toolY = new PositionTool(false, true);
                    const toolXY = new PositionTool(true, true);
                    return [
                        toolX,
                        toolY,
                        toolXY,
                        new SimpleLineTool(toolXY, toolX, 0xff0000),
                        new SimpleLineTool(toolXY, toolY, 0x00ff00),
                    ];
                }
                case "Hand": {
                    return [new HandTool()];
                }

            }

            return [];
        }
    }
}