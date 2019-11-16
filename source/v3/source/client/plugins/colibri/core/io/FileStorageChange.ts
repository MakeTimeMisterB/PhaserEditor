namespace colibri.core.io {

    export declare type RenameData = {

        oldName: string,

        newFile: FilePath
    }

    export class FileStorageChange {

        private _renameRecords_fromPath: Set<string>;
        private _renameRecords_toPath: Set<string>;
        private _renameFromToMap: Map<string, string>;
        private _deletedRecords: Set<string>;
        private _addedRecords: Set<string>;
        private _modifiedRecords: Set<string>;

        constructor() {

            this._renameRecords_fromPath = new Set();
            this._renameRecords_toPath = new Set();
            this._deletedRecords = new Set();
            this._addedRecords = new Set();
            this._modifiedRecords = new Set();
            this._renameFromToMap = new Map();
        }

        recordRename(fromPath: string, toPath: string) {
            
            this._renameRecords_fromPath.add(fromPath);
            this._renameRecords_toPath.add(toPath);

            this._renameFromToMap[fromPath] = toPath;
        }

        getRenameTo(fromPath: string) {
            return this._renameFromToMap[fromPath];
        }

        isRenamed(fromPath: string) {
            return this._renameFromToMap.has(fromPath);
        }

        wasRenamed(toPath: string) {
            return this._renameRecords_toPath.has(toPath);
        }

        getRenameRecords() {
            return this._renameRecords_toPath;
        }

        recordDelete(path : string) {
            this._deletedRecords.add(path);
        }

        isDeleted(path : string) {
            return this._deletedRecords.has(path);
        }

        recordAdd(path : string) {
            this._addedRecords.add(path);
        }

        isAdded(path : string) {
            return this._addedRecords.has(path);
        }

        getAddRecords() {
            return this._addedRecords;
        }

        recordModify(path : string) {
            this._modifiedRecords.add(path);
        }

        isModified(path : string) {
            return this._modifiedRecords.has(path);
        }
    }
}