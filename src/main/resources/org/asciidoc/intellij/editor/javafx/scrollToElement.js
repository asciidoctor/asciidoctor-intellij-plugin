if (window.__IntelliJTools === undefined) {
  window.__IntelliJTools = {}
}

window.__IntelliJTools.scrollToLine = (function () {

  var oldLineToScroll = 0;

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

  function calculateOffset(element) {
    var offset = 0
    while(element != null) {
      offset += element.offsetTop
      element = element.offsetParent
    }
    return offset
  }

  var scrollToLine = function (newLineToScroll, lineCount) {

    // the sourcelines will be as CSS class elements that also have class has-source-line
    var blocks = document.getElementsByClassName('has-source-line');
    var startY;
    var startLine;
    var endY;
    var endLine = lineCount;

    for (var i = 0; i < blocks.length; i++) {
      var block = blocks[i]
      var lineOfBlock = getLine(block);
      if (lineOfBlock <= newLineToScroll) {
        startY = calculateOffset(block)
        startLine = lineOfBlock
        // there might be no further block, therefore assume that the end is at the end of this block
        endY = startY + block.offsetHeight
      }
      else if (lineOfBlock > newLineToScroll) {
        endY = calculateOffset(block)
        endLine = lineOfBlock -1;
        break
      }
    }

    var resultY = startY

    // interpolate the relative position inside the current block
    if (endY !== undefined && newLineToScroll != startLine) {
      resultY += (newLineToScroll - startLine) / (endLine - startLine) * (endY - startY)
    }

    var height = window.innerHeight
    var relativeWindowPosition = 0.5;
    var oldValue = document.documentElement.scrollTop || document.body.scrollTop;

    // ensure that the assumed position is between x% of the window height depending on scroll direction
    if (oldLineToScroll < newLineToScroll) {
      relativeWindowPosition = 0.8
    }
    else if (oldLineToScroll > newLineToScroll) {
      relativeWindowPosition = 0.1
    }
    // if we catch the cursor (i.e. due to edit after scrolling), we'll place it at x% of the window hight
    else if (resultY < oldValue || resultY > oldValue + window.height) {
      relativeWindowPosition = 0.3
    }

    if (oldLineToScroll != newLineToScroll) {
      var newValue = resultY - height * relativeWindowPosition;

      // ensure consistent scrolling when scrolling up or down
      if (
          (oldLineToScroll < newLineToScroll && oldValue < newValue) || // consistent scrolling up
          (oldLineToScroll > newLineToScroll && oldValue > newValue) || // consistent scrolling down
          (resultY < document.documentElement.scrollTop) || // position above window
          (resultY > document.documentElement.scrollTop + window.height) // position below window
         ) {
        document.documentElement.scrollTop = document.body.scrollTop = newValue;
      }

      oldLineToScroll = newLineToScroll;
    }

  }

  return scrollToLine
})()

