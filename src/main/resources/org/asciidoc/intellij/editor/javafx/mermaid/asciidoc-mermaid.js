// Renders all Mermaid diagrams, and shows the parser's message for diagrams with errors.
// Mermaid's default error output only says "Syntax error in text" without any details.
(function () {
  mermaid.initialize({startOnLoad: false});
  window.mermaid = mermaid;

  function showError(element, error) {
    const pre = document.createElement('pre');
    pre.className = 'asciidoc-mermaid-error';
    pre.style.color = '#c00';
    pre.style.whiteSpace = 'pre-wrap';
    pre.textContent = 'Mermaid syntax error (mermaid ' + (mermaid.version ? mermaid.version() : '') + '):\n'
      + (error && error.message ? error.message : String(error))
      + '\n\n' + element.textContent;
    element.replaceWith(pre);
  }

  let counter = 0;

  // Rendering each diagram on its own, as only render() reports errors that happen after parsing
  mermaid.run = async function () {
    const elements = document.querySelectorAll('.mermaid:not([data-processed])');
    for (const element of Array.from(elements)) {
      element.setAttribute('data-processed', 'true');
      const id = 'asciidoc-mermaid-' + (counter++);
      const source = element.textContent;
      try {
        const result = await mermaid.render(id, source);
        element.innerHTML = result.svg;
        if (result.bindFunctions) {
          result.bindFunctions(element);
        }
      } catch (e) {
        // render() leaves its error diagram in the page
        const leftover = document.getElementById('d' + id);
        if (leftover) {
          leftover.remove();
        }
        showError(element, e);
      }
    }
  };

  const start = function () {
    mermaid.run();
  };
  if (document.readyState === 'loading') {
    window.addEventListener('DOMContentLoaded', start);
  } else {
    start();
  }
})();
