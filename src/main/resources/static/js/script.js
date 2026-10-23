/* =====================================================================
   SYNERGY · script.js
   Mejoras de interfaz progresivas: si este archivo no carga, todas las
   páginas siguen funcionando igual (solo se pierden las animaciones).
   ===================================================================== */
(function () {
  'use strict';

  // Marca que JS está activo (activa las animaciones .reveal del CSS)
  document.documentElement.classList.add('js');

  var reduceMotion = window.matchMedia &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  function ready(fn) {
    if (document.readyState !== 'loading') fn();
    else document.addEventListener('DOMContentLoaded', fn);
  }

  ready(function () {

    /* ---------- FAQ (acordeón) ---------- */
    var faqItems = document.querySelectorAll('.faq-item');
    faqItems.forEach(function (item) {
      var question = item.querySelector('.faq-question');
      if (!question) return;

      question.setAttribute('role', 'button');
      question.setAttribute('tabindex', '0');
      question.setAttribute('aria-expanded', 'false');

      function toggle() {
        faqItems.forEach(function (other) {
          if (other !== item) {
            other.classList.remove('active');
            var q = other.querySelector('.faq-question');
            if (q) q.setAttribute('aria-expanded', 'false');
          }
        });
        var open = item.classList.toggle('active');
        question.setAttribute('aria-expanded', open ? 'true' : 'false');
      }

      question.addEventListener('click', toggle);
      question.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          toggle();
        }
      });
    });

    /* ---------- Header con fondo al hacer scroll ---------- */
    var header = document.querySelector('.site-header');
    if (header) {
      var onScroll = function () {
        header.classList.toggle('is-scrolled', window.scrollY > 20);
      };
      onScroll();
      window.addEventListener('scroll', onScroll, { passive: true });

      /* Menú móvil */
      var toggleBtn = header.querySelector('.nav-toggle');
      if (toggleBtn) {
        toggleBtn.addEventListener('click', function () {
          var open = header.classList.toggle('nav-open');
          toggleBtn.setAttribute('aria-expanded', open ? 'true' : 'false');
          var icon = toggleBtn.querySelector('i');
          if (icon) {
            icon.classList.toggle('fa-bars', !open);
            icon.classList.toggle('fa-xmark', open);
          }
        });
        header.querySelectorAll('.nav-links a').forEach(function (a) {
          a.addEventListener('click', function () {
            header.classList.remove('nav-open');
          });
        });
      }
    }

    /* ---------- Revelado al hacer scroll ---------- */
    var reveals = document.querySelectorAll('.reveal');
    if ('IntersectionObserver' in window && !reduceMotion) {
      var io = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-visible');
            io.unobserve(entry.target);
          }
        });
      }, { threshold: 0.12, rootMargin: '0px 0px -40px 0px' });
      reveals.forEach(function (el) { io.observe(el); });
    } else {
      reveals.forEach(function (el) { el.classList.add('is-visible'); });
    }

    /* ---------- Brillo que sigue al cursor en tarjetas ---------- */
    if (!reduceMotion) {
      document.querySelectorAll('.feature-card').forEach(function (card) {
        card.addEventListener('pointermove', function (e) {
          var r = card.getBoundingClientRect();
          card.style.setProperty('--mx', (e.clientX - r.left) + 'px');
          card.style.setProperty('--my', (e.clientY - r.top) + 'px');
        });
      });
    }

    /* ---------- Mostrar / ocultar contraseña ---------- */
    document.querySelectorAll('.toggle-pass').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var input = btn.parentElement.querySelector('input');
        if (!input) return;
        var show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        btn.setAttribute('aria-label', show ? 'Ocultar contraseña' : 'Mostrar contraseña');
        var icon = btn.querySelector('i');
        if (icon) {
          icon.classList.toggle('fa-eye', !show);
          icon.classList.toggle('fa-eye-slash', show);
        }
      });
    });

    /* ---------- Nombre del archivo seleccionado ---------- */
    document.querySelectorAll('.file-drop').forEach(function (drop) {
      var input = drop.querySelector('input[type="file"]');
      var label = drop.querySelector('.file-drop-name');
      if (!input || !label) return;
      var original = label.textContent;

      input.addEventListener('change', function () {
        var has = input.files && input.files.length > 0;
        label.textContent = has ? input.files[0].name : original;
        drop.classList.toggle('has-file', !!has);
      });
      ['dragenter', 'dragover'].forEach(function (ev) {
        drop.addEventListener(ev, function () { drop.classList.add('is-dragover'); });
      });
      ['dragleave', 'drop'].forEach(function (ev) {
        drop.addEventListener(ev, function () { drop.classList.remove('is-dragover'); });
      });
    });

    /* ---------- Barra de progreso de lectura + volver arriba ---------- */
    var progress = document.querySelector('.scroll-progress');
    var toTop = document.querySelector('.back-to-top');
    if (progress || toTop) {
      var ticking = false;
      var update = function () {
        var h = document.documentElement.scrollHeight - window.innerHeight;
        var p = h > 0 ? window.scrollY / h : 0;
        if (progress) progress.style.setProperty('--progress', p.toFixed(4));
        if (toTop) toTop.classList.toggle('is-visible', window.scrollY > 600);
        ticking = false;
      };
      update();
      window.addEventListener('scroll', function () {
        if (!ticking) { ticking = true; requestAnimationFrame(update); }
      }, { passive: true });
    }

    /* ---------- Palabra rotativa del hero ---------- */
    var rotator = document.querySelector('.rotator');
    if (rotator && !reduceMotion) {
      var words = rotator.querySelectorAll('.rotator-word');
      var idx = 0;
      if (words.length > 1) {
        setInterval(function () {
          var current = words[idx];
          idx = (idx + 1) % words.length;
          var next = words[idx];
          current.classList.remove('is-active');
          current.classList.add('is-leaving');
          next.classList.remove('is-leaving');
          next.classList.add('is-active');
          setTimeout(function () { current.classList.remove('is-leaving'); }, 650);
        }, 2400);
      }
    }

    /* ---------- Inclinación 3D + luz del hero siguiendo al cursor ---------- */
    var hero = document.querySelector('.hero');
    var tilt = document.querySelector('[data-tilt]');
    var finePointer = window.matchMedia && window.matchMedia('(pointer: fine)').matches;
    if (hero && !reduceMotion && finePointer) {
      var win = tilt ? tilt.querySelector('.mock-window') : null;
      hero.addEventListener('pointermove', function (e) {
        var r = hero.getBoundingClientRect();
        hero.style.setProperty('--sx', ((e.clientX - r.left) / r.width * 100) + '%');
        hero.style.setProperty('--sy', ((e.clientY - r.top) / r.height * 100) + '%');
        if (win && window.innerWidth > 1024) {
          var t = tilt.getBoundingClientRect();
          var x = (e.clientX - (t.left + t.width / 2)) / t.width;
          var y = (e.clientY - (t.top + t.height / 2)) / t.height;
          win.style.transform = 'rotateY(' + (-8 + x * 10).toFixed(2) + 'deg) rotateX(' + (4 - y * 8).toFixed(2) + 'deg)';
        }
      });
      hero.addEventListener('pointerleave', function () {
        if (win) win.style.transform = '';
      });
    }

    /* ---------- Contadores animados ---------- */
    var counters = document.querySelectorAll('[data-count]');
    if (counters.length && 'IntersectionObserver' in window && !reduceMotion) {
      var co = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
          if (!entry.isIntersecting) return;
          var el = entry.target;
          var target = parseFloat(el.getAttribute('data-count')) || 0;
          var suffix = el.getAttribute('data-suffix') || '';
          var start = null;
          var dur = 1400;
          var step = function (ts) {
            if (!start) start = ts;
            var k = Math.min((ts - start) / dur, 1);
            var eased = 1 - Math.pow(1 - k, 3);
            el.textContent = Math.round(target * eased) + suffix;
            if (k < 1) requestAnimationFrame(step);
          };
          requestAnimationFrame(step);
          co.unobserve(el);
        });
      }, { threshold: 0.6 });
      counters.forEach(function (el) { co.observe(el); });
    }

    /* ---------- Onda (ripple) al hacer clic en botones ---------- */
    if (!reduceMotion) {
      document.addEventListener('pointerdown', function (e) {
        var btn = e.target.closest && e.target.closest('.btn');
        if (!btn || btn.disabled) return;
        var r = btn.getBoundingClientRect();
        var dot = document.createElement('span');
        dot.className = 'ripple';
        dot.style.left = (e.clientX - r.left) + 'px';
        dot.style.top = (e.clientY - r.top) + 'px';
        btn.appendChild(dot);
        setTimeout(function () { dot.remove(); }, 700);
      });
    }

    /* ---------- Tema claro / oscuro ---------- */
    document.querySelectorAll('[data-theme-toggle]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var root = document.documentElement;
        var nuevo = root.getAttribute('data-theme') === 'light' ? 'dark' : 'light';
        root.classList.add('theme-transition');
        root.setAttribute('data-theme', nuevo);
        try { localStorage.setItem('synergy-theme', nuevo); } catch (e) { /* sin almacenamiento */ }
        setTimeout(function () { root.classList.remove('theme-transition'); }, 400);
      });
    });

    /* ---------- Menú lateral en móvil ---------- */
    var sidebar = document.getElementById('sidebar');
    if (sidebar) {
      var overlay = document.createElement('div');
      overlay.className = 'sidebar-overlay';
      document.body.appendChild(overlay);
      var cerrar = function () { document.body.classList.remove('sidebar-open'); };
      document.querySelectorAll('[data-sidebar-toggle]').forEach(function (btn) {
        btn.addEventListener('click', function () {
          document.body.classList.toggle('sidebar-open');
        });
      });
      overlay.addEventListener('click', cerrar);
      document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') cerrar();
      });
    }

    /* ---------- Ventana de confirmación (data-confirm) ---------- */
    function confirmar(mensaje, textoOk, alAceptar) {
      var fondo = document.createElement('div');
      fondo.className = 'modal-backdrop';
      fondo.innerHTML =
        '<div class="modal" role="alertdialog" aria-modal="true" aria-labelledby="modal-titulo">' +
        '<div class="modal-icon"><i class="fa-solid fa-triangle-exclamation"></i></div>' +
        '<h3 id="modal-titulo">¿Estás seguro?</h3><p></p>' +
        '<div class="modal-actions">' +
        '<button type="button" class="btn btn-secondary" data-cancelar>Cancelar</button>' +
        '<button type="button" class="btn btn-danger" data-aceptar></button>' +
        '</div></div>';
      fondo.querySelector('p').textContent = mensaje;
      fondo.querySelector('[data-aceptar]').textContent = textoOk || 'Sí, continuar';
      document.body.appendChild(fondo);
      requestAnimationFrame(function () { fondo.classList.add('is-open'); });

      var anterior = document.activeElement;
      var cerrarModal = function () {
        fondo.classList.remove('is-open');
        setTimeout(function () { fondo.remove(); }, 250);
        document.removeEventListener('keydown', teclas);
        if (anterior && anterior.focus) anterior.focus();
      };
      var teclas = function (e) { if (e.key === 'Escape') cerrarModal(); };
      document.addEventListener('keydown', teclas);
      fondo.addEventListener('click', function (e) { if (e.target === fondo) cerrarModal(); });
      fondo.querySelector('[data-cancelar]').addEventListener('click', cerrarModal);
      var ok = fondo.querySelector('[data-aceptar]');
      ok.addEventListener('click', function () { cerrarModal(); alAceptar(); });
      ok.focus();
    }

    document.addEventListener('click', function (e) {
      var el = e.target.closest && e.target.closest('[data-confirm]');
      if (!el || el.tagName === 'FORM' || el.dataset.confirmado === '1') return;
      e.preventDefault();
      confirmar(el.getAttribute('data-confirm'), el.getAttribute('data-confirm-ok'), function () {
        el.dataset.confirmado = '1';
        if (el.tagName === 'A') {
          window.location.href = el.href;
        } else if (el.form) {
          if (el.form.requestSubmit) el.form.requestSubmit(el); else el.form.submit();
        }
        setTimeout(function () { delete el.dataset.confirmado; }, 1000);
      });
    });

    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
      form.addEventListener('submit', function (e) {
        if (form.dataset.confirmado === '1') return;
        e.preventDefault();
        confirmar(form.getAttribute('data-confirm'), form.getAttribute('data-confirm-ok'), function () {
          form.dataset.confirmado = '1';
          form.submit();
        });
      });
    });

    /* ---------- Alertas de éxito se ocultan solas ---------- */
    document.querySelectorAll('.alert-success[data-autohide]').forEach(function (alert) {
      setTimeout(function () {
        alert.classList.add('is-leaving');
        setTimeout(function () { alert.remove(); }, 400);
      }, 6000);
    });

    /* ---------- Estado "enviando" en botones de formularios ---------- */
    document.querySelectorAll('form[data-loading]').forEach(function (form) {
      form.addEventListener('submit', function () {
        var btn = form.querySelector('button[type="submit"]');
        if (!btn) return;
        // Espera un tick por si un confirm() canceló el envío
        setTimeout(function () {
          btn.classList.add('is-loading');
          var icon = btn.querySelector('i');
          if (icon) icon.className = 'fa-solid fa-circle-notch';
        }, 0);
      });
    });
  });

  /* ---------- Toast global (compatible con showToast existente) ---------- */
  window.showToast = function (message, type) {
    var toast = document.getElementById('toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'toast';
      toast.className = 'toast hidden';
      toast.setAttribute('role', 'status');
      document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.className = 'toast ' + (type || 'success');
    clearTimeout(window.__toastTimer);
    window.__toastTimer = setTimeout(function () {
      toast.classList.add('hidden');
    }, 2800);
  };

  /* ---------- Filtros: se aplican al cambiar un selector ---------- */
  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('form[data-autosubmit]').forEach(function (form) {
      form.querySelectorAll('select').forEach(function (sel) {
        sel.addEventListener('change', function () {
          if (form.requestSubmit) form.requestSubmit(); else form.submit();
        });
      });
      // No enviar parámetros vacíos (URL más limpia: ?q=playa en vez de ?q=playa&categoria=&...)
      form.addEventListener('submit', function () {
        form.querySelectorAll('input[name], select[name]').forEach(function (el) {
          if (!el.value) el.disabled = true;
        });
        setTimeout(function () {
          form.querySelectorAll('[disabled]').forEach(function (el) { el.disabled = false; });
        }, 0);
      });
    });
  });
})();

/* =====================================================================
   Synergy v6: simulador de riesgo, tablero kanban, fechas relativas,
   impresión de reportes y mapa del administrador
   ===================================================================== */
(function () {
  'use strict';

  function ready(fn) {
    if (document.readyState !== 'loading') fn();
    else document.addEventListener('DOMContentLoaded', fn);
  }

  /* Token CSRF de la página (Thymeleaf lo pone en cada formulario POST) */
  function csrf() {
    var input = document.querySelector('input[name="_csrf"]');
    return input ? input.value : null;
  }

  /* ---------- Simulador "¿qué pasa si...?" ---------- */
  function pct(v) { return Math.round((v || 0) * 100) + ' %'; }

  ready(function () {
    document.querySelectorAll('[data-simulador]').forEach(function (box) {
      var url = box.getAttribute('data-simulador');
      var out = box.querySelector('.simulador-resultado');
      var btn = box.querySelector('[data-simular]');
      if (!btn || !out) return;
      btn.addEventListener('click', function () {
        var params = new URLSearchParams();
        ['completar', 'activar', 'nuevos'].forEach(function (n) {
          var el = box.querySelector('input[name="' + n + '"]');
          params.set(n, Math.max(0, parseInt(el && el.value, 10) || 0));
        });
        btn.disabled = true;
        out.textContent = 'Calculando…';
        fetch(url + '?' + params.toString(), { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
          .then(function (r) { return r.json().then(function (j) { return { ok: r.ok, j: j }; }); })
          .then(function (res) {
            if (!res.ok) { out.textContent = res.j.message || 'No se pudo simular.'; return; }
            var a = res.j.antes, d = res.j.despues;
            out.innerHTML = '';
            var p = document.createElement('p');
            var mejora = d.puntaje < a.puntaje - 0.01, empeora = d.puntaje > a.puntaje + 0.01;
            p.className = 'simulador-veredicto ' + (mejora ? 'is-good' : (empeora ? 'is-bad' : ''));
            p.textContent = (a.nivel || 'Sin datos') + ' (' + pct(a.probabilidades[a.nivel] || 0) + ') → '
              + (d.nivel || 'Sin datos') + ' (' + pct(d.probabilidades[d.nivel] || 0) + ')'
              + (mejora ? ': el riesgo baja.' : (empeora ? ': el riesgo sube.' : ': casi no cambia.'));
            out.appendChild(p);
            var info = document.createElement('small');
            info.className = 'cell-muted';
            info.textContent = 'Hay ' + res.j.maxCompletar + ' tareas pendientes y ' + res.j.maxActivar
              + ' voluntarios que aún no participan.' + (d.factor ? ' Después seguiría influyendo: ' + d.factor + '.' : '');
            out.appendChild(info);
          })
          .catch(function () { out.textContent = 'No se pudo conectar con el servidor.'; })
          .finally(function () { btn.disabled = false; });
      });
    });
  });

  /* ---------- Tablero kanban: arrastrar una tarea a otra columna ---------- */
  ready(function () {
    var board = document.querySelector('[data-kanban]');
    if (!board) return;
    // En el celular (sin arrastrar): cambiar el selector guarda el estado
    board.querySelectorAll('.kanban-form select').forEach(function (sel) {
      sel.addEventListener('change', function () {
        if (sel.form.requestSubmit) sel.form.requestSubmit(); else sel.form.submit();
      });
    });
    var arrastrada = null;
    board.querySelectorAll('.kanban-card[draggable="true"]').forEach(function (card) {
      card.addEventListener('dragstart', function (e) {
        arrastrada = card;
        card.classList.add('is-dragging');
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/plain', card.getAttribute('data-id'));
      });
      card.addEventListener('dragend', function () { card.classList.remove('is-dragging'); });
    });
    board.querySelectorAll('.kanban-col').forEach(function (col) {
      col.addEventListener('dragover', function (e) { e.preventDefault(); col.classList.add('is-over'); });
      col.addEventListener('dragleave', function () { col.classList.remove('is-over'); });
      col.addEventListener('drop', function (e) {
        e.preventDefault();
        col.classList.remove('is-over');
        if (!arrastrada) return;
        var estado = col.getAttribute('data-estado');
        var anterior = arrastrada.parentElement;
        if (anterior === col.querySelector('.kanban-items')) return;
        var card = arrastrada;
        col.querySelector('.kanban-items').appendChild(card);
        var body = new URLSearchParams({ estado: estado });
        var token = csrf();
        if (token) body.set('_csrf', token);
        fetch(card.getAttribute('data-url'), {
          method: 'POST', credentials: 'same-origin',
          headers: { 'X-Requested-With': 'fetch', 'Content-Type': 'application/x-www-form-urlencoded' },
          body: body.toString()
        }).then(function (r) {
          if (!r.ok) throw new Error();
          var sel = card.querySelector('select[name="estado"]');
          if (sel) sel.value = estado;
          contar();
        }).catch(function () {
          anterior.appendChild(card); // se devuelve si el servidor no aceptó el cambio
          alertaKanban('No se pudo mover la tarea. Recarga la página e inténtalo de nuevo.');
        });
      });
    });
    function contar() {
      board.querySelectorAll('.kanban-col').forEach(function (col) {
        var c = col.querySelector('.count');
        if (c) c.textContent = col.querySelectorAll('.kanban-card').length;
      });
    }
    function alertaKanban(msg) {
      var a = board.querySelector('.kanban-alerta');
      if (a) { a.textContent = msg; a.hidden = false; }
    }
  });

  /* ---------- Fechas relativas: <time data-relative datetime="2026-10-20T10:00"> ---------- */
  ready(function () {
    var ahora = Date.now();
    document.querySelectorAll('time[data-relative]').forEach(function (el) {
      var t = Date.parse(el.getAttribute('datetime'));
      if (isNaN(t)) return;
      var s = Math.round((ahora - t) / 1000), txt;
      if (s < 60) txt = 'hace un momento';
      else if (s < 3600) txt = 'hace ' + Math.round(s / 60) + ' min';
      else if (s < 86400) txt = 'hace ' + Math.round(s / 3600) + ' h';
      else if (s < 172800) txt = 'ayer';
      else if (s < 2592000) txt = 'hace ' + Math.round(s / 86400) + ' días';
      else return; // fechas viejas: se deja la fecha exacta
      if (!el.title) el.title = el.textContent.trim();
      el.textContent = txt;
    });
  });

  /* ---------- Botones de imprimir / guardar como PDF ---------- */
  ready(function () {
    document.querySelectorAll('[data-print]').forEach(function (b) {
      b.addEventListener('click', function () { window.print(); });
    });
  });

  /* ---------- Mapa de proyectos por barrio (Leaflet + OpenStreetMap) ---------- */
  ready(function () {
    var el = document.getElementById('mapa-proyectos');
    if (!el || !window.L) return;
    var puntos = [];
    try { puntos = JSON.parse(el.getAttribute('data-puntos') || '[]'); } catch (e) { return; }
    var mapa = L.map(el, { scrollWheelZoom: false }).setView([10.41, -75.51], 12);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18, attribution: '&copy; OpenStreetMap'
    }).addTo(mapa);
    var max = Math.max.apply(null, puntos.map(function (p) { return p.n; }).concat([1]));
    puntos.forEach(function (p) {
      L.circleMarker([p.lat, p.lng], {
        radius: 6 + 18 * Math.sqrt(p.n / max), color: '#059669', weight: 1.5, fillColor: '#10B981', fillOpacity: 0.55
      }).addTo(mapa).bindPopup('<strong>' + p.barrio.replace(/</g, '&lt;') + '</strong><br>' + p.n + ' proyectos activos');
    });
  });
})();

/* ---------- Indicador de fortaleza de contraseña (mismas reglas que PoliticaContrasena.java) ---------- */
(function () {
  'use strict';
  function ready(fn) { if (document.readyState !== 'loading') fn(); else document.addEventListener('DOMContentLoaded', fn); }
  ready(function () {
    document.querySelectorAll('input[data-fortaleza]').forEach(function (input) {
      var caja = document.createElement('div');
      caja.className = 'fortaleza';
      caja.setAttribute('aria-live', 'polite');
      caja.innerHTML = '<div class="fortaleza-barra"><span></span></div><ul>' +
        '<li data-r="largo">8 caracteres o más</li><li data-r="mayus">Una mayúscula</li>' +
        '<li data-r="minus">Una minúscula</li><li data-r="num">Un número</li></ul>';
      var ancla = input.closest('.float-field, .field, .input-group, .form-group, label') || input;
      ancla.insertAdjacentElement('afterend', caja);
      function revisar() {
        var v = input.value || '';
        var r = { largo: v.length >= 8 && v.length <= 72, mayus: /[A-ZÁÉÍÓÚÑ]/.test(v), minus: /[a-záéíóúñ]/.test(v), num: /\d/.test(v) };
        var n = 0;
        Object.keys(r).forEach(function (k) {
          var li = caja.querySelector('[data-r="' + k + '"]');
          li.classList.toggle('ok', r[k]);
          if (r[k]) n++;
        });
        caja.querySelector('.fortaleza-barra span').style.width = (n * 25) + '%';
        caja.setAttribute('data-nivel', n);
        input.setCustomValidity(n === 4 || v.length === 0 ? '' : 'La contraseña debe tener 8 caracteres, una mayúscula, una minúscula y un número.');
      }
      input.addEventListener('input', revisar);
      revisar();
    });
  });
})();
