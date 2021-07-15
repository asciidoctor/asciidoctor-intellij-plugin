if (window.__IntelliJTools === undefined) {
  window.__IntelliJTools = {}
}

window.__IntelliJTools.getLine = function (node) {
  if (!node || !('className' in node)) {
    return null
  }
  var classes = node.className.split(' ');

  // JavaFX WebView doesn't support named groups, therefore don't use them here
  var re = /^data-line-(.*)-([0-9]*)$/;

  for (var i = 0; i < classes.length; i++) {
    var className = classes[i];
    var found = className.match(re);
    if (found) {
      return found;
    }
  }

  return null;
}

window.__IntelliJTools.lineCount = 0;

  window.__IntelliJTools.calculateOffset = function (element) {
  var offset = 0
  while (element != null) {
    offset += element.offsetTop
    element = element.offsetParent
  }
  return offset
}

window.__IntelliJTools.scrollEditorToLine = function (event) {
  try {
    var blocks = document.getElementsByClassName('has-source-line');
    var startY;
    var startFile = 'stdin';
    var startLine = 1;
    var endY;
    var endLine = window.__IntelliJTools.lineCount;
    var endFile = 'stdin';
    var parent;
    var tempEndY;
    for (var i = 0; i < blocks.length; i++) {
      var block = blocks[i];
      var result = window.__IntelliJTools.getLine(block);
      if (result === null) {
        continue;
      }
      var fileOfBlock = result[1];
      var lineOfBlock = Number(result[2]);
      if (block.contains(event.currentTarget)) {
        startY = window.__IntelliJTools.calculateOffset(block)
        startLine = lineOfBlock;
        startFile = fileOfBlock;
        // there might be no further block, therefore assume that the end is at the end of this block
        endY = startY + block.offsetHeight;
        parent = block;
      } else if (parent && !parent.contains(block)) {
        // we just left the block we clicked in
        tempEndY = window.__IntelliJTools.calculateOffset(block)
        if (tempEndY > endY) {
          endY = tempEndY;
        }
        endLine = lineOfBlock - 1;
        endFile = fileOfBlock;
        break;
      } else if (event.clientY + window.scrollY > window.__IntelliJTools.calculateOffset(block)) {
        startY = window.__IntelliJTools.calculateOffset(block)
        startLine = lineOfBlock;
        startFile = fileOfBlock;
        // there might be no further block, therefore assume that the end is at the end of this block
        endY = startY + block.offsetHeight;
      } else if (event.clientY + window.scrollY < window.__IntelliJTools.calculateOffset(block)) {
        tempEndY = window.__IntelliJTools.calculateOffset(block)
        if (tempEndY > endY) {
          endY = tempEndY;
        }
        endLine = lineOfBlock - 1;
        endFile = fileOfBlock;
        break;
      }
    }
    var editorLine;
    if (startFile === endFile) {
      editorLine = startLine + (event.clientY + window.scrollY - startY) * (endLine - startLine) / (endY - startY);
    } else {
      editorLine = startLine;
    }
    window.JavaPanelBridge.scrollEditorToLine(editorLine + ":" + startFile);
    event.stopPropagation();
  } catch (e) {
    window.JavaPanelBridge.log("can't pick source line: " + JSON.stringify(e));
  }
}

window.__IntelliJTools.pickSourceLine = function (lc) {

  // the sourcelines will be as CSS class elements that also have class has-source-line
  var blocks = document.getElementsByClassName('has-source-line');

  for (var i = 0; i < blocks.length; i++) {
    blocks[i].addEventListener('click', window.__IntelliJTools.scrollEditorToLine);
  }

  window.__IntelliJTools.lineCount = lc;

}

window.__IntelliJTools.clearSourceLine = function () {

  // the sourcelines will be as CSS class elements that also have class has-source-line
  var blocks = document.getElementsByClassName('has-source-line');

  for (var i = 0; i < blocks.length; i++) {
    blocks[i].removeEventListener('click', window.__IntelliJTools.scrollEditorToLine);
  }

}
