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
      right: 4px;
      z-index: 100;
      background: transparent;
      color: inherit;
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

    /* make room for the button next to the language label of the default and the Antora stylesheet */
    .listingblock:has(> .intellij-copy-button) code[data-lang]::before,
    .listingblock:has(> .intellij-copy-button) .source-toolbox {
      right: 32px;
    }

    .listingblock:has(> .intellij-copy-button):has(> .intellij-run-button) code[data-lang]::before,
    .listingblock:has(> .intellij-copy-button):has(> .intellij-run-button) .source-toolbox {
      right: 58px;
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
  // IntelliJ "Copy" icon (AllIcons.Actions.Copy), colored like the surrounding text to fit light and dark themes
  btn.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" width="16" height="16">
                      <g fill="currentColor" fill-rule="evenodd">
                        <path d="M11,3 L4,3 L4,11 L2,11 L2,1 L11,1 L11,3 Z"/>
                        <path d="M5,4 L14,4 L14,14 L5,14 L5,4 Z M7,6 L7,7 L12,7 L12,6 L7,6 Z M7,10 L7,11 L12,11 L12,10 L7,10 Z M7,8 L7,9 L12,9 L12,8 L7,8 Z"/>
                      </g>
                   </svg>`
  btn.addEventListener('click', function (event) {
    event.stopPropagation()
    event.preventDefault()
    let codeText = codeEl.textContent || codeEl.innerText || ''
    window.JavaPanelBridge.copyCode(codeText)
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
