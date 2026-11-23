;(function () { /*! Asciidoctor Carousels | Copyright (c) 2026-present Asciidoctor Carousels contributors | MIT License */
  'use strict'

  // Keep commonly used array methods local for both readability and minor performance gains.
  var forEach = Array.prototype.forEach
  var slice = Array.prototype.slice

  // Initialize every carousel already present in the document.
  init(document.querySelectorAll('.carousel'))

  // Initializes a single carousel: reads its configuration, sets up slide
  // navigation, autoplay progress, controls, indicators, and input handlers.
  function init (carousels) {
    forEach.call(carousels, function (carousel) {
      var stage = carousel.querySelector('.carousel-stage')
      var track = carousel.querySelector('.carousel-track')
      var slides = slice.call(carousel.querySelectorAll('.carousel-slide'))
      // A carousel must have a stage, a track, and at least one slide.
      if (!stage || !track || !slides.length) return

      // Read carousel behavior from the data attributes written by the processor.
      var config = stage.dataset || {}
      var activeIndex = clamp(parseInt(config.activeSlide || '1', 10) - 1, 0, slides.length - 1)
      var interval = parsePositiveInt(config.interval || '3000', 3000)
      var autoplay = config.autoplay === 'true'
      var loop = config.loop !== 'false'
      var keyboard = config.keyboard !== 'false'
      var touch = config.touch !== 'false'
      var pauseOnHover = config.pauseOnHover === 'true'
      var transition = config.transition || 'slide'
      // The slide transition moves the track; every other effect stacks the
      // slides and lets CSS decide how each one enters.
      var isSlide = transition === 'slide'
      var isCube = transition === 'cube'
      // Direction of the most recent navigation, used to pick the side a slide
      // enters from when both sides resolve to the same slide.
      var lastDirection = 1
      // Distance from the centre of the stage to a face of the cube, in pixels.
      var cubeApothem = 0
      var timer
      // resumeOnPointerLeave marks autoplay that was paused by an indicator
      // click and should restart when the pointer leaves the carousel.
      var resumeOnPointerLeave = false
      // progress is a value between 0 and 1 used to fill the active indicator.
      var progress = 0
      // lastTick stores the timestamp of the previous autoplay animation frame.
      var lastTick

      // Preview shown when the pointer hovers over an indicator. It is a
      // sibling of the indicators so the indicator's overflow clipping does
      // not hide the preview.
      var indicators = carousel.querySelector('.carousel-indicators')
      var preview = null
      var previewImg = null
      if (indicators) {
        preview = document.createElement('div')
        preview.className = 'carousel-indicator-preview'
        previewImg = document.createElement('img')
        preview.appendChild(previewImg)
        indicators.appendChild(preview)
      }

      // Remove the initial no-JS fallback so all slides can be managed by this script.
      slides.forEach(function (slide) { slide.classList.remove('is-hidden') })
      // Allow keyboard navigation to reach and interact with the carousel.
      if (keyboard) stage.setAttribute('tabindex', '0')

      // Marks the requested slide as active, updates slide visibility, and
      // synchronizes the indicator list. skipFocus is retained for API
      // compatibility and callers that may want to suppress focus changes.
      function setActive (index, skipFocus) {
        activeIndex = clamp(index, 0, slides.length - 1)
        slides.forEach(function (slide, idx) {
          var active = idx === activeIndex
          slide.classList[active ? 'add' : 'remove']('is-active')
          slide.setAttribute('aria-hidden', active ? 'false' : 'true')
        })
        if (isSlide) track.style.transform = 'translateX(-' + activeIndex * 100 + '%)'
        else if (isCube) rollTo(activeIndex)
        else updateNeighbors()
        forEach.call(carousel.querySelectorAll('.carousel-indicator'), function (indicator, idx) {
          indicator.classList[idx === activeIndex ? 'add' : 'remove']('is-active')
        })
      }

      // Marks the slides next to the active one so CSS can place them on the
      // correct side of the stack. The ends wrap when looping; the side that
      // does not exist is left unmarked, and a two-slide carousel uses the
      // direction the reader navigated in for its single neighbour.
      function updateNeighbors () {
        var count = slides.length
        if (count < 2) return
        var prevIndex = loop ? (activeIndex - 1 + count) % count : activeIndex - 1
        var nextIndex = loop ? (activeIndex + 1) % count : activeIndex + 1
        if (prevIndex === nextIndex) {
          if (lastDirection < 0) nextIndex = activeIndex
          else prevIndex = activeIndex
        }
        slides.forEach(function (slide, idx) {
          slide.classList[idx === prevIndex ? 'add' : 'remove']('is-prev')
          slide.classList[idx === nextIndex ? 'add' : 'remove']('is-next')
        })
      }

      // Rolls the cube so the requested slide faces the reader. The track holds
      // the whole prism, so a single rotation moves every face.
      function rollTo (index) {
        var angle = (360 / slides.length) * index
        track.style.transform = 'translateZ(-' + cubeApothem + 'px) rotateY(' +
          (angle ? -angle : 0) + 'deg)'
      }

      // Lays the slides out on the faces of a regular prism whose front face is
      // the stage. The distance to each face depends on the stage width, so it
      // is measured again whenever the window is resized.
      function measureCube () {
        if (!isCube || slides.length < 2) return
        var width = stage.clientWidth
        if (!width) return
        var step = 360 / slides.length
        cubeApothem = Math.round(width / 2 / Math.tan(Math.PI / slides.length) * 100) / 100
        slides.forEach(function (slide, idx) {
          slide.style.transform = 'rotateY(' + (step * idx) + 'deg) translateZ(' + cubeApothem + 'px)'
        })
        rollTo(activeIndex)
      }

      // Updates the CSS custom property that controls the autoplay progress
      // fill on the indicators. Non-active indicators are always reset to zero.
      function updateIndicatorProgress (value) {
        forEach.call(carousel.querySelectorAll('.carousel-indicator'), function (indicator) {
          var active = indicator.classList.contains('is-active')
          indicator.style.setProperty('--carousel-progress', active ? value : 0)
        })
      }

      // Toggles the visual state used after an indicator is clicked while
      // autoplay is enabled.
      function setPaused (paused) {
        var indicator = carousel.querySelector('.carousel-indicator.is-active')
        if (indicator) indicator.classList[paused ? 'add' : 'remove']('is-paused')
      }

      // Shows a half-size preview of the slide associated with the hovered
      // indicator, positioned just above that indicator.
      function showPreview (indicator) {
        if (!preview) return
        var index = parseInt(indicator.dataset.carouselIndex, 10)
        var image = slides[index] && slides[index].querySelector('img')
        if (!image) return
        previewImg.src = image.src
        previewImg.alt = image.alt || ''
        var stageRect = stage.getBoundingClientRect()
        var indicatorsRect = indicators.getBoundingClientRect()
        var indicatorRect = indicator.getBoundingClientRect()
        preview.style.width = Math.round(stageRect.width / 4) + 'px'
        preview.style.height = Math.round(stageRect.height / 4) + 'px'
        preview.style.left = (indicatorRect.left - indicatorsRect.left + indicatorRect.width / 2) + 'px'
        preview.style.bottom = (indicatorsRect.bottom - indicatorRect.top + 8) + 'px'
        preview.classList.add('is-visible')
      }

      function hidePreview () {
        if (preview) preview.classList.remove('is-visible')
      }

      // Moves to the adjacent slide. When called from autoplay, it preserves
      // the running animation; manual calls restart autoplay when enabled.
      function advance (direction, fromAutoplay) {
        var target = activeIndex + direction
        if (target >= slides.length) target = loop ? 0 : slides.length - 1
        if (target < 0) target = loop ? slides.length - 1 : 0
        lastDirection = direction
        setActive(target)
        progress = autoplay ? 0 : 1
        updateIndicatorProgress(progress)
        if (!fromAutoplay && autoplay) start()
        else if (fromAutoplay && !loop && target === slides.length - 1) stop()
      }

      // Public navigation helpers for the previous and next controls.
      function next () { advance(1, false) }

      function prev () { advance(-1, false) }

      // Jumps to a specific slide from an indicator click. Autoplay is paused
      // and will resume when the pointer leaves the carousel.
      function go (index) {
        stop()
        lastDirection = index < activeIndex ? -1 : 1
        setActive(index)
        progress = 1
        updateIndicatorProgress(progress)
        setPaused(autoplay)
        resumeOnPointerLeave = autoplay
      }

      // Starts or restarts autoplay and resets the indicator progress to zero.
      function start () {
        if (timer) stop()
        resumeOnPointerLeave = false
        setPaused(false)
        progress = 0
        lastTick = undefined
        updateIndicatorProgress(0)
        timer = window.requestAnimationFrame(tick)
      }

      // Runs once per animation frame while autoplay is active, updating the
      // indicator progress and advancing slides when the interval is complete.
      function tick (now) {
        if (!autoplay) return
        if (lastTick === undefined) lastTick = now
        var elapsed = now - lastTick
        lastTick = now
        progress += elapsed / interval
        if (progress >= 1) advance(1, true)
        else updateIndicatorProgress(progress)
        if (timer) timer = window.requestAnimationFrame(tick)
      }

      // Stops the autoplay animation loop.
      function stop () {
        if (!timer) return
        window.cancelAnimationFrame(timer)
        timer = undefined
      }

      // Wire up the previous/next controls.
      forEach.call(carousel.querySelectorAll('.carousel-control'), function (control) {
        control.addEventListener('click', function () {
          control.dataset.carouselAction === 'prev' ? prev() : next()
        })
      })

      // Wire up the slide indicators.
      forEach.call(carousel.querySelectorAll('.carousel-indicator'), function (indicator) {
        indicator.addEventListener('click', function () {
          go(parseInt(indicator.dataset.carouselIndex, 10))
        })
        indicator.addEventListener('mouseenter', function () { showPreview(indicator) })
        indicator.addEventListener('mouseleave', hidePreview)
      })

      // Resume autoplay after an indicator click once the pointer leaves the
      // carousel, regardless of whether pause-on-hover is enabled.
      carousel.addEventListener('mouseleave', function () {
        if (autoplay && resumeOnPointerLeave) start()
      })

      // Support left and right arrow keys when the carousel has keyboard focus.
      if (keyboard) {
        stage.addEventListener('keydown', function (e) {
          if (e.key === 'ArrowLeft') prev()
          else if (e.key === 'ArrowRight') next()
        })
      }

      // Support basic horizontal swipe gestures on touch and pointer devices.
      if (touch) {
        var startX
        stage.addEventListener('pointerdown', function (e) { startX = e.clientX })
        stage.addEventListener('pointerup', function (e) {
          if (startX === undefined) return
          var delta = e.clientX - startX
          if (Math.abs(delta) > 30) delta < 0 ? next() : prev()
          startX = undefined
        })
      }

      // Start the autoplay loop when requested.
      if (autoplay) start()
      // Pause and resume autoplay when the pointer enters or leaves the carousel.
      if (pauseOnHover) {
        carousel.addEventListener('mouseover', function (e) {
          var related = e.relatedTarget
          if (!related || !carousel.contains(related)) stop()
        })
        carousel.addEventListener('mouseout', function (e) {
          var related = e.relatedTarget
          if ((!related || !carousel.contains(related)) && autoplay) start()
        })
      }

      // Lay out the cube before the first slide is shown, and keep its geometry
      // in step with the stage width.
      if (isCube) {
        measureCube()
        window.addEventListener('resize', measureCube)
      }

      // Apply the initial slide and clear the loading state after setup.
      setActive(activeIndex, true)
      updateIndicatorProgress(autoplay ? 0 : 1)
      carousel.classList.remove('is-loading')
      carousel.classList.add('is-loaded')
    })
  }

  // Clamps value to the inclusive range between min and max.
  function clamp (value, min, max) {
    return Math.min(max, Math.max(min, value))
  }

  // Parses a positive integer, falling back when the value is missing or invalid.
  function parsePositiveInt (value, fallback) {
    var parsed = parseInt(value, 10)
    return parsed > 0 ? parsed : fallback
  }
})();
