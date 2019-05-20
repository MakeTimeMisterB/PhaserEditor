namespace PhaserEditor2D {
    export class TileScaleTool extends InteractiveTool implements SpotTool {

        private _handlerShape: Phaser.GameObjects.Shape;
        private _changeX: boolean;
        private _changeY: boolean;
        private _dragging = false;
        private _color: number;

        constructor(changeX: boolean, changeY: boolean) {
            super();

            this._changeX = changeX;
            this._changeY = changeY;

            if (changeX && changeY) {
                this._color = 0xffff00;
            } else if (changeX) {
                this._color = 0xff0000;
            } else {
                this._color = 0x00ff00;
            }

            this._handlerShape = this.createRectangleShape();
            this._handlerShape.setFillStyle(this._color);
        }

        canEdit(obj: any): boolean {
            return obj instanceof Phaser.GameObjects.TileSprite;
        }

        getX() {
            return this._handlerShape.x;
        }

        getY() {
            return this._handlerShape.y;
        }

        clear() {
            this._handlerShape.visible = false;
        }

        render(list: Phaser.GameObjects.TileSprite[]) {
            const pos = new Phaser.Math.Vector2(0, 0);
            let angle = 0;

            for (let obj of list) {
                const sprite = <Phaser.GameObjects.TileSprite>obj;

                const worldXY = new Phaser.Math.Vector2();
                const worldTx = sprite.getWorldTransformMatrix();

                const localLeft = -sprite.width * sprite.originX;
                const localTop = -sprite.height * sprite.originY;

                let localX = localLeft + sprite.tilePositionX;
                let localY = localTop + sprite.tilePositionY;

                if (!this._changeX || !this._changeY) {
                    if (this._changeX) {
                        localX += sprite.width * sprite.tileScaleX;
                    } else {
                        localY += sprite.height * sprite.tileScaleY;
                    }
                }

                angle += this.objectGlobalAngle(sprite);

                worldTx.transformPoint(localX, localY, worldXY);

                pos.add(worldXY);
            }

            const len = this.getObjects().length;
            const cam = Editor.getInstance().getObjectScene().cameras.main;

            const cameraX = (pos.x / len - cam.scrollX) * cam.zoom;
            const cameraY = (pos.y / len - cam.scrollY) * cam.zoom;

            this._handlerShape.setPosition(cameraX, cameraY);
            this._handlerShape.angle = angle / len;
            if (this._changeX) {
                this._handlerShape.angle -= 90;
            }
            this._handlerShape.visible = true;
        }

        containsPointer() {
            const pointer = this.getToolPointer();

            const d = Phaser.Math.Distance.Between(pointer.x, pointer.y, this._handlerShape.x, this._handlerShape.y);

            return d <= this._handlerShape.width;
        }

        isEditing() {
            return this._dragging;
        }

        onMouseDown() {
            if (this.containsPointer()) {

                this._dragging = true;

                this.requestRepaint = true;

                this._handlerShape.setFillStyle(0xffffff);

                const worldTx = new Phaser.GameObjects.Components.TransformMatrix();

                const shapePos = this.getScenePoint(this._handlerShape.x, this._handlerShape.y);

                for (let obj of this.getObjects()) {
                    const sprite = <Phaser.GameObjects.TileSprite>obj;

                    const initLocalPos = new Phaser.Math.Vector2();
                    sprite.getWorldTransformMatrix(worldTx);
                    worldTx.applyInverse(shapePos.x, shapePos.y, initLocalPos);

                    sprite.setData("TileScaleTool", {
                        initTileScaleX: sprite.tileScaleX,
                        initTileScaleY: sprite.tileScaleY,
                        initLocalPos: initLocalPos
                    });

                }
            }
        }

        onMouseMove() {
            if (!this._dragging) {
                return;
            }

            this.requestRepaint = true;

            const pointer = this.getToolPointer();

            const pointerPos = this.getScenePoint(pointer.x, pointer.y);

            const worldTx = new Phaser.GameObjects.Components.TransformMatrix();

            for (let obj of this.getObjects()) {
                const sprite = <Phaser.GameObjects.TileSprite>obj;
                const data = sprite.data.get("TileScaleTool");
                const initLocalPos: Phaser.Math.Vector2 = data.initLocalPos;

                const localPos = new Phaser.Math.Vector2();
                sprite.getWorldTransformMatrix(worldTx);
                worldTx.applyInverse(pointerPos.x, pointerPos.y, localPos);

                const dx = localPos.x - initLocalPos.x;
                const dy = localPos.y - initLocalPos.y;

                const tileScaleX = data.initTileScaleX + dx / sprite.width;
                const tileScaleY = data.initTileScaleY + dy / sprite.height;

                if (this._changeX) {
                    sprite.tileScaleX = tileScaleX;
                }

                if (this._changeY) {
                    sprite.tileScaleY = tileScaleY;
                }
            }
        }

        onMouseUp() {

            if (this._dragging) {
                const msg = BuildMessage.SetTileSpriteProperties(this.getObjects());
                Editor.getInstance().sendMessage(msg);
            }

            this._dragging = false;

            this._handlerShape.setFillStyle(this._color);

            this.requestRepaint = true;
        }

    }
}