if (window.__IntelliJTools === undefined) {
  window.__IntelliJTools = {}
}

window.__IntelliJTools.processImageClick = function (event) {
  event.preventDefault();
  window.JavaPanelBridge.saveImage(this.src);
}

window.__IntelliJTools.processImages = function () {
  var links = document.getElementsByTagName("img");
  for (var i = 0; i < links.length; ++i) {
    var link = links[i];
    link.addEventListener('contextmenu', window.__IntelliJTools.processImageClick);
  }
}

window.__IntelliJTools.clearImages = function () {
  var links = document.getElementsByTagName("img");
  for (var i = 0; i < links.length; ++i) {
    var link = links[i];
    link.removeEventListener('contextmenu', __IntelliJTools.processImageClick);
  }
}
