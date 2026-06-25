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

  var cooldown = document.getElementById('refresh-cooldown');
  if (cooldown) {
    var remaining = parseInt(cooldown.dataset.seconds, 10);
    function tick() {
      if (remaining <= 0) {
        cooldown.textContent = 'Refresh now';
        return;
      }
      var mins = Math.floor(remaining / 60);
      var secs = remaining % 60;
      cooldown.textContent = 'Refresh in ' + mins + ':' + (secs < 10 ? '0' : '') + secs;
      remaining--;
      setTimeout(tick, 1000);
    }
    tick();
  }

  var input = document.getElementById('riot-id-input');
  var datalist = document.getElementById('recent-searches');
  if (input && datalist) {
    var STORAGE_KEY = 'drake-checker-recent';
    function loadRecent() {
      try { return JSON.parse(localStorage.getItem(STORAGE_KEY)) || []; }
      catch (e) { return []; }
    }
    function saveRecent(list) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
    }
    function renderDatalist(filter) {
      var list = loadRecent();
      if (filter) {
        list = list.filter(function (x) { return x.toLowerCase().indexOf(filter.toLowerCase()) !== -1; });
      }
      datalist.innerHTML = '';
      list.forEach(function (item) {
        var opt = document.createElement('option');
        opt.value = item;
        datalist.appendChild(opt);
      });
    }
    input.addEventListener('input', function () {
      renderDatalist(input.value.trim());
    });
    input.addEventListener('focus', function () {
      renderDatalist(input.value.trim());
    });
    input.addEventListener('blur', function () {
      var val = input.value.trim();
      if (!val) return;
      var list = loadRecent();
      list = list.filter(function (x) { return x !== val; });
      list.unshift(val);
      if (list.length > 10) list = list.slice(0, 10);
      saveRecent(list);
    });
    renderDatalist();
  }

});
