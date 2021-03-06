/*
 * Copyright 2018 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.inspector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.impl.XSourcePositionImpl;
import io.flutter.utils.JsonUtils;

import java.util.ArrayList;

public class InspectorSourceLocation {
  private final JsonObject json;
  private final InspectorSourceLocation parent;

  public InspectorSourceLocation(JsonObject json, InspectorSourceLocation parent) {
    this.json = json;
    this.parent = parent;
  }

  public VirtualFile getFile() {
    String fileName = JsonUtils.getStringMember(json, "file");
    if (fileName == null) {
      return parent != null ? parent.getFile() : null;
    }

    // We have to strip the file:// prefix from the Dart file path for compatibility.
    final String filePrefix = "file://";
    if (fileName.startsWith(filePrefix)) {
      fileName = fileName.substring(filePrefix.length());
    }

    final VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(fileName);
    if (virtualFile != null && !virtualFile.exists()) {
      return null;
    }
    return virtualFile;
  }

  public int getLine() {
    return JsonUtils.getIntMember(json, "line");
  }

  public String getName() {
    return JsonUtils.getStringMember(json, "name");
  }

  public int getColumn() {
    return JsonUtils.getIntMember(json, "column");
  }

  public XSourcePosition getXSourcePosition() {
    final VirtualFile file = getFile();
    if (file == null) {
      return null;
    }
    final int line = getLine();
    final int column = getColumn();
    if (line < 0 || column < 0) {
      return null;
    }
    return XSourcePositionImpl.create(file, line - 1, column - 1);
  }

  ArrayList<InspectorSourceLocation> getParameterLocations() {
    if (json.has("parameterLocations")) {
      final JsonArray parametersJson = json.getAsJsonArray("parameterLocations");
      final ArrayList<InspectorSourceLocation> ret = new ArrayList<>();
      for (int i = 0; i < parametersJson.size(); ++i) {
        ret.add(new InspectorSourceLocation(parametersJson.get(i).getAsJsonObject(), this));
      }
      return ret;
    }
    return null;
  }
}
