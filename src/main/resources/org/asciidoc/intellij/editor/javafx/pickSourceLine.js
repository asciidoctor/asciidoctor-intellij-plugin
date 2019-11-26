if (window.__IntelliJTools === undefined) {
  window.__IntelliJTools = {}
}

window.__IntelliJTools.getLine = function (node) {
  if (!node || !('className' in node)) {
    return null
  }
  var classes = node.className.split(' ');

  for (var i = 0; i < classes.length; i++) {
    var className = classes[i]
    if (className.match(/^data-line-stdin-/)) {
      return Number(className.substr("data-line-stdin-".length));
    }
  }

  return null
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
    var sourceLine = window.__IntelliJTools.getLine(this)

    var blocks = document.getElementsByClassName('has-source-line');
    var startY;
    var startLine;
    var endY;
    var endLine = window.__IntelliJTools.lineCount;

    for (var i = 0; i < blocks.length; i++) {
      var block = blocks[i]
      var lineOfBlock = window.__IntelliJTools.getLine(block);
      if (lineOfBlock <= sourceLine) {
        startY = window.__IntelliJTools.calculateOffset(block)
        startLine = lineOfBlock
        // there might be no further block, therefore assume that the end is at the end of this block
        endY = startY + block.offsetHeight
      } else if (lineOfBlock > sourceLine) {
        endY = window.__IntelliJTools.calculateOffset(block)
        endLine = lineOfBlock - 1;
        break
      }
    }
    var editorLine = startLine + (event.clientY + window.scrollY - startY) * (endLine - startLine) / (endY - startY)
    window.JavaPanelBridge.scrollEditorToLine(editorLine)
    event.stopPropagation()
  } catch (e) {
    window.JavaPanelBridge.log("can't pick source line", JSON.stringify(e));
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
