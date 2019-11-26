if (window.__IntelliJTools === undefined) {
  window.__IntelliJTools = {}
}

window.__IntelliJTools.processClick = function (event) {
  // prevent opening the link in the preview
  event.preventDefault()

  if (!this.href) {
    return false;
  }

  if (this.href[0] == '#') {
    var elementId = this.href.substring(1)
    var elementById = document.getElementById(elementId);
    if (elementById) {
      elementById.scrollIntoView();
    }
  } else {
    window.JavaPanelBridge.openLink(this.href);
  }

  return false;
}

window.__IntelliJTools.processLinks = function () {
  var links = document.getElementsByTagName("a");
  // window.JavaPanelBridge.log(links.length)
  for (var i = 0; i < links.length; ++i) {
    var link = links[i];

    link.addEventListener('click', window.__IntelliJTools.processClick);
    // window.JavaPanelBridge.log(link + ' ' + link.onclick)
  }
}

window.__IntelliJTools.clearLinks = function () {
  var links = document.getElementsByTagName("a");
  // window.JavaPanelBridge.log(links.length)
  for (var i = 0; i < links.length; ++i) {
    var link = links[i];

    link.removeEventListener('click', __IntelliJTools.processClick);
    // window.JavaPanelBridge.log(link + ' ' + link.onclick)
  }
}
