if (window.__IntelliJTools === undefined) {
  window.__IntelliJTools = {}
}

window.__IntelliJTools.processLinks = (function () {
  var openInExternalBrowser = function (href) {
    window.JavaPanelBridge.openInExternalBrowser(href);
  }

  window.__IntelliJTools.processClick = function (event) {
    // stop propagation to prevent triggering scrolling to source line
    event.stopPropagation()

    if (!this.href) {
      return false;
    }

    if (this.href[0] == '#') {
      var elementId = this.href.substring(1)
      var elementById = document.getElementById(elementId);
      if (elementById) {
        elementById.scrollIntoView();
      }
    }
    else {
      openInExternalBrowser(this.href);
    }

    return false;
  }

  var processLinks = function () {
    var links = document.getElementsByTagName("a");
    // window.JavaPanelBridge.log(links.length)
    for (var i = 0; i < links.length; ++i) {
      var link = links[i];

      link.onclick = __IntelliJTools.processClick
      // window.JavaPanelBridge.log(link + ' ' + link.onclick)
    }
  }

  return processLinks;

})()

