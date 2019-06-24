if (window.__IntelliJTools === undefined) {
  window.__IntelliJTools = {}
}

window.__IntelliJTools.processImages = (function () {
  var saveImage = function (imagePath) {
    window.JavaPanelBridge.saveImage(imagePath);
  }

  window.__IntelliJTools.processRightClick = function (event) {
    saveImage(this.src);
    return false;
  }

  var processImages = function () {
    var images = document.getElementsByTagName("img");
    for (var i = 0; i < images.length; ++i) {
      var image = images[i];
      image.oncontextmenu = __IntelliJTools.processRightClick
    }
    // disable context menu in all other places as they don't offer helpful options
    document.oncontextmenu = function() {
      return false;
    }
  }

  return processImages;

})()

