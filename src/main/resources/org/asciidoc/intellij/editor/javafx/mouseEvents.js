if (window.__IntelliJTools === undefined) {
  window.__IntelliJTools = {}
}

window.__IntelliJTools.addMouseHandler = function () {
  function listenMouseButtons(event) {
    // left+right or middle button
    if (event.buttons === 3 || event.buttons === 4) {
      event.preventDefault();
      window.JavaPanelBridge.zoomReset();
    }
  }
  const el = document.querySelector('body');
  el.onmousedown = listenMouseButtons
}
