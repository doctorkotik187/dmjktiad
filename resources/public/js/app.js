document.addEventListener('DOMContentLoaded', function () {

  var form = document.querySelector('form[data-loading]');
  if (form) {
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      var btn = form.querySelector('button[type="submit"]');
      if (btn) {
        btn.disabled = true;
        btn.textContent = 'Loading...';
      }
      showOverlay();
      requestAnimationFrame(function () {
        requestAnimationFrame(function () {
          form.submit();
        });
      });
    });
  }

  document.querySelectorAll('a[data-loading]').forEach(function (link) {
    link.addEventListener('click', function (e) {
      e.preventDefault();
      var href = link.href;
      link.textContent = 'Loading...';
      showOverlay();
      requestAnimationFrame(function () {
        requestAnimationFrame(function () {
          window.location.href = href;
        });
      });
    });
  });

  window.addEventListener('pageshow', function (e) {
    if (e.persisted) {
      document.querySelectorAll('button, a').forEach(function (el) {
        el.disabled = false;
        if (el.textContent === 'Loading...') el.textContent = el.getAttribute('data-original-text') || 'Refresh';
      });
      hideOverlay();
    }
  });

  function showOverlay() {
    var overlay = document.getElementById('loading-overlay');
    if (overlay) overlay.style.display = 'flex';
  }

  function hideOverlay() {
    var overlay = document.getElementById('loading-overlay');
    if (overlay) overlay.style.display = 'none';
  }

});
