// -- user code here --
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
/* --- start generated code --- */
// Generated by  1.4.3 (Phaser v2.6.2)
/**
 * Level.
 */
var Level = (function (_super) {
    __extends(Level, _super);
    function Level() {
        return _super.call(this) || this;
    }
    Level.prototype.init = function () {
        this.stage.backgroundColor = '#ff8000';
    };
    Level.prototype.preload = function () {
    };
    Level.prototype.create = function () {
        this.add.text(300, 290, 'You are playing now!', { "font": "bold 20px Arial" });
    };
    return Level;
}(Phaser.State));
/* --- end generated code --- */
// -- user code here --