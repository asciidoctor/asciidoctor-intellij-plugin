if (window.__IntelliJTools === undefined) {
  window.__IntelliJTools = {}
}

window.__IntelliJTools.processImages = (function () {
  var saveImage = function (imagePath) {
    window.JavaPanelBridge.saveImage(imagePath);
  }

  window.__IntelliJTools.processDoubleClick = function (event) {
    saveImage(this.src);
    return false;
  }

  var processImages = function () {
    var images = document.getElementsByTagName("img");
    for (var i = 0; i < images.length; ++i) {
      var image = images[i];
      image.ondblclick = __IntelliJTools.processDoubleClick
    }
  }

  return processImages;

})()

