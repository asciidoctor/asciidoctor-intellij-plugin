if (window.__IntelliJTools === undefined) {
  window.__IntelliJTools = {}
}

/**
 * Inject a <style> block that hides run buttons by default and shows them
 * only when the user hovers over the enclosing listing block.
 */
window.__IntelliJTools.injectRunButtonStyle = function () {
  if (document.getElementById('intellij-run-button-style')) {
    return
  }
  let style = document.createElement('style')
  style.id = 'intellij-run-button-style'
  // language=CSS
  style.textContent = [
    `.intellij-run-button {
      position: absolute;
      top: 4px;
      left: 4px;
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

    .listingblock:hover > .intellij-run-button {
      opacity: 0.9;
      pointer-events: auto;
    }

    .intellij-run-button:hover {
      opacity: 1 !important;
      background: rgba(74, 146, 89, 0.15) !important;
    }`
  ]
  document.head.appendChild(style)
}

/**
 * Create and append a run button to the given listing block.
 * Assumes the block already has position != static.
 */
window.__IntelliJTools.addRunButton = function (block, codeEl, lang) {
  // Guard against duplicate buttons (e.g. concurrent async callbacks)
  if (block.querySelector('.intellij-run-button')) {
    return
  }
  let btn = document.createElement('button')
  btn.className = 'intellij-run-button'
  btn.title = 'Run ' + lang
  btn.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 14 14" width="30" height="30">
                      <polygon points="0,1 7,7 0,13" fill="#59A869"/>
                      <polygon points="6,1 13,7 6,13" fill="#59A869"/>
                   </svg>`
  btn.addEventListener('click', function (event) {
    event.stopPropagation()
    event.preventDefault()
    let codeText = codeEl.textContent || codeEl.innerText || ''
    window.JavaPanelBridge.runCode(lang + '\n' + codeText)
  })
  block.appendChild(btn)
}

/**
 * Add a green play button overlay to each source code listing block that
 * has a recognized language (data-lang attribute on the <code> element).
 * Uses JavaPanelBridge.isApplicable to only show the button for languages
 * that are executable. The button is invisible by default
 * and fades in on mouse-over of the block.
 */
window.__IntelliJTools.addRunButtons = function () {
  if (!window.JavaPanelBridge || !window.JavaPanelBridge.runCode) {
    return
  }

  window.__IntelliJTools.injectRunButtonStyle()

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
      if (window.JavaPanelBridge.isApplicable) {
        // Ask Java whether any AsciiDocRunner is applicable for this language
        window.JavaPanelBridge.isApplicable(capturedLang, function (result) {
          if (result === 'true') {
            window.__IntelliJTools.addRunButton(capturedBlock, capturedCode, capturedLang)
          }
        }, function () { /* not applicable or error – do not add button */
        })
      } else {
        // Fallback when bridge does not yet expose isApplicable
        window.__IntelliJTools.addRunButton(capturedBlock, capturedCode, capturedLang)
      }
    })(block, codeEl, lang)
  }
}

/**
 * Remove all run buttons that were previously added.
 * Called before the content node is replaced to avoid stale listeners.
 */
window.__IntelliJTools.clearRunButtons = function () {
  let buttons = document.querySelectorAll('.intellij-run-button')
  for (let i = 0; i < buttons.length; i++) {
    let btn = buttons[i]
    if (btn.parentNode) {
      btn.parentNode.removeChild(btn)
    }
  }
}
