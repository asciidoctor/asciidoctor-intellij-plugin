if (window.__IntelliJTools === undefined) {
  window.__IntelliJTools = {}
}

/**
 * Inject a <style> block that hides run buttons by default and shows them
 * only when the user hovers over the enclosing listing block.
 */
window.__IntelliJTools.injectCopyButtonStyle = function () {
  if (document.getElementById('intellij-copy-button-style')) {
    return
  }
  let style = document.createElement('style')
  style.id = 'intellij-copy-button-style'
  // language=CSS
  style.textContent = [
    `.intellij-copy-button {
      position: absolute;
      top: 4px;
      left: 30px;
      z-index: 100;
      background: transparent;
      border: none;
      border-radius: 4px;
      width: 22px;
      height: 22px;
      padding: 0;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: opacity 0.15s ease, background 0.1s ease;
      pointer-events: none;
    }

    .listingblock:hover > .intellij-copy-button {
      opacity: 0.9;
      pointer-events: auto;
    }

    .intellij-copy-button:hover {
      opacity: 1 !important;
      background: rgba(74, 146, 89, 0.15) !important;
    }`
  ]
  document.head.appendChild(style)
}

/**
 * Create and append a copy button to the given listing block.
 * Assumes the block already has position != static.
 */
window.__IntelliJTools.addCopyButton = function (block, codeEl, lang) {
  // Guard against duplicate buttons (e.g. concurrent async callbacks)
  if (block.querySelector('.intellij-copy-button')) {
    return
  }
  let btn = document.createElement('button')
  btn.className = 'intellij-copy-button'
  btn.title = 'Copy ' + lang
  btn.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg"
                       viewBox="0 0 24 24" width="30" height="30"
                       fill="none" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"
                            fill="#FFE082" stroke="#0D47B1" stroke-width="2.2"/>
                      <rect x="8" y="2" width="8" height="4" rx="1" ry="1"
                            fill="#D0D0D0" stroke="#0D47A1" stroke-width="2.2"/>
                      <path d="M9 14l2 2 4-4" stroke="#FFFFFF" stroke-width="4"/>
                      <path d="M9 14l2 2 4-4" stroke="#00C853" stroke-width="2.4"/>
                  </svg>`
  btn.addEventListener('click', function (event) {
    event.stopPropagation()
    event.preventDefault()
    let codeText = codeEl.textContent || codeEl.innerText || ''
    window.JavaPanelBridge.copyCode(lang + '\n' + codeText)
  })
  block.appendChild(btn)
}

/**
 * Add a copy code button overlay to each source code listing block.
 * The button is invisible by default and fades in on mouse-over of the block.
 */
window.__IntelliJTools.addCopyButtons = function () {
  if (!window.JavaPanelBridge || !window.JavaPanelBridge.runCode) {
    return
  }

  window.__IntelliJTools.injectCopyButtonStyle()

  let blocks = document.querySelectorAll('.listingblock')
  for (let i = 0; i < blocks.length; i++) {
    let block = blocks[i]

    // Skip if a button was already added (e.g. after inplace content refresh)
    if (block.querySelector('.intellij-run-button')) {
      continue
    }

    let codeEl = block.querySelector('code[data-lang]')
    if (!codeEl) {
      continue
    }

    let lang = codeEl.getAttribute('data-lang')
    if (!lang) {
      continue
    }

    // Make the block a positioning context so the button can be placed absolute
    let pos = window.getComputedStyle(block).position
    if (pos === 'static') {
      block.style.position = 'relative'
    }

    // Use a closure to capture the correct block, codeEl and lang for each iteration
    (function (capturedBlock, capturedCode, capturedLang) {
      window.__IntelliJTools.addCopyButton(capturedBlock, capturedCode, capturedLang)
    })(block, codeEl, lang)
  }
}

/**
 * Remove all copy buttons that were previously added.
 * Called before the content node is replaced to avoid stale listeners.
 */
window.__IntelliJTools.clearCopyButtons = function () {
  let buttons = document.querySelectorAll('.intellij-copy-button')
  for (let i = 0; i < buttons.length; i++) {
    let btn = buttons[i]
    if (btn.parentNode) {
      btn.parentNode.removeChild(btn)
    }
  }
}
