if (window.__IntelliJTools === undefined) {
  window.__IntelliJTools = {}
}

window.__IntelliJTools.processClick = function (event) {
  // prevent opening the link in the preview
  event.preventDefault()

  if (!this.href) {
    return false;
  }

  var href;
  if (this.href instanceof SVGAnimatedString) {
    href = this.href.baseVal;
  } else {
    href = this.getAttribute("href");
  }

  if (href[0] === '#') {
    var elementId = href.substring(1)
    var elementById = document.getElementById(elementId);
    if (elementById) {
      elementById.scrollIntoView();
    }
  } else {
    window.JavaPanelBridge.openLink(href);
  }

  return false;
}

window.__IntelliJTools.processLinks = function () {
  // This will work for inlined SVG diagrams.
  // This will NOT work for interactive SVG diagrams, as these will be inaccessible for JavaScript
  //    (possibly due to file:// URLs and cross-domain concerns in browsers)

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
