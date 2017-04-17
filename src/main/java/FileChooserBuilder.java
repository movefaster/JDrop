/*
 * Copyright [2017] [Morton Mo]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;

/**
 * Created by Morton on 4/16/17.
 */
public class FileChooserBuilder {
    private FileChooser fc;
    public FileChooserBuilder() {
        fc = new FileChooser();
    }
    public FileChooserBuilder setPath(File path) {
        fc.setInitialDirectory(path);
        return this;
    }
    public FileChooserBuilder setTitle(String title) {
        fc.setTitle(title);
        return this;
    }
    public FileChooserBuilder setExtensionFilter(FileChooser.ExtensionFilter filter) {
        fc.setSelectedExtensionFilter(filter);
        return this;
    }
    public FileChooserBuilder setFilename(String filename) {
        fc.setInitialFileName(filename);
        return this;
    }
    public File showSaveDialog(Window ownerWindow) {
        return fc.showSaveDialog(ownerWindow);
    }
    public File showOpenDialog(Window ownerWindow) {
        return fc.showOpenDialog(ownerWindow);
    }
    public List<File> showOpenMultipleDialog(Window ownerWindow) {
        return fc.showOpenMultipleDialog(ownerWindow);
    }
}
