if (window.__IntelliJTools === undefined) {
  window.__IntelliJTools = {}
}

window.__IntelliJTools.pickSourceLine = (function () {

  var getLine = function (node) {
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

  var lineCount;

  function calculateOffset(element) {
    var offset = 0
    while(element != null) {
      offset += element.offsetTop
      element = element.offsetParent
    }
    return offset
  }

  window.__IntelliJTools.srcollEditorToLine = function (event) {
    var sourceLine = getLine(this)

    var blocks = document.getElementsByClassName('has-source-line');
    var startY;
    var startLine;
    var endY;
    var endLine = lineCount;

    for (var i = 0; i < blocks.length; i++) {
      var block = blocks[i]
      var lineOfBlock = getLine(block);
      if (lineOfBlock <= sourceLine) {
        startY = calculateOffset(block)
        startLine = lineOfBlock
        // there might be no further block, therefore assume that the end is at the end of this block
        endY = startY + block.offsetHeight
      }
      else if (lineOfBlock > sourceLine) {
        endY = calculateOffset(block)
        endLine = lineOfBlock -1;
        break
      }
    }

    var editorLine = startLine + (event.clientY + window.scrollY - startY) * (endLine - startLine) / (endY - startY)

    window.JavaPanelBridge.scollEditorToLine(editorLine)
    event.stopPropagation()
  }

  var initializeContent = function (lc) {

    // the sourcelines will be as CSS class elements that also have class has-source-line
    var blocks = document.getElementsByClassName('has-source-line');

    for (var i = 0; i < blocks.length; i++) {
      blocks[i].onclick = window.__IntelliJTools.srcollEditorToLine;
    }

    lineCount = lc;

  }

  return initializeContent

})()

