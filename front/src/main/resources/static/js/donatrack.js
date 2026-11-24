/*
 * DonaTrack — comportamiento progresivo. Todas las páginas funcionan sin JavaScript;
 * esto agrega: menú móvil, toasts, loaders, confirmaciones accesibles, mapas y polling.
 */
(function () {
  'use strict';

  const DonaTrack = {};
  window.DonaTrack = DonaTrack;

  // ------------------------------------------------------------------ CSRF para fetch
  DonaTrack.csrf = function () {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    return token && header ? { [header.content]: token.content } : {};
  };

  DonaTrack.json = async function (url, opciones) {
    const r = await fetch(url, Object.assign({ headers: { 'Accept': 'application/json' }, credentials: 'same-origin' }, opciones || {}));
    if (r.status === 401 || r.redirected) { window.location.href = '/login?expirada'; return null; }
    if (!r.ok) { throw new Error('Error ' + r.status); }
    return r.json();
  };

  // ------------------------------------------------------------------ toasts
  DonaTrack.toast = function (mensaje, tipo) {
    const zona = document.querySelector('.toasts');
    if (!zona) return;
    const t = document.createElement('div');
    t.className = 'toast ' + (tipo || '');
    t.innerHTML = '<span></span><button type="button" aria-label="Cerrar aviso">×</button>';
    t.querySelector('span').textContent = mensaje;
    zona.appendChild(t);
    programarCierre(t);
  };

  function programarCierre(t) {
    const cerrar = () => t.remove();
    t.querySelector('button')?.addEventListener('click', cerrar);
    // Los errores quedan visibles hasta que se cierran (el usuario debe poder leerlos).
    if (!t.classList.contains('error')) setTimeout(cerrar, 6000);
  }

  // ------------------------------------------------------------------ inicialización
  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.toast').forEach(t => {
      if (!t.querySelector('button')) {
        const b = document.createElement('button');
        b.type = 'button'; b.setAttribute('aria-label', 'Cerrar aviso'); b.textContent = '×';
        t.appendChild(b);
      }
      programarCierre(t);
    });

    // Menú móvil
    const boton = document.querySelector('.menu-boton');
    const nav = document.getElementById('navegacion');
    if (boton && nav) {
      boton.addEventListener('click', () => {
        const abierto = nav.classList.toggle('abierto');
        boton.setAttribute('aria-expanded', String(abierto));
      });
      document.addEventListener('keydown', e => {
        if (e.key === 'Escape' && nav.classList.contains('abierto')) {
          nav.classList.remove('abierto'); boton.setAttribute('aria-expanded', 'false'); boton.focus();
        }
      });
    }

    // Marca el enlace de la sección actual
    document.querySelectorAll('.nav a[href]').forEach(a => {
      const href = a.getAttribute('href');
      if (href !== '/' && window.location.pathname.startsWith(href)) a.setAttribute('aria-current', 'page');
    });

    // Loader en envíos: sólo aparece si la respuesta tarda más de 300 ms
    document.querySelectorAll('form').forEach(f => f.addEventListener('submit', () => {
      const b = f.querySelector('button[type=submit], button:not([type])');
      if (!b) return;
      setTimeout(() => {
        b.classList.add('cargando'); b.setAttribute('aria-busy', 'true'); b.disabled = true;
      }, 300);
    }));

    // Confirmaciones con <dialog> accesible (en lugar de window.confirm)
    const dialogo = document.getElementById('confirmacion');
    document.querySelectorAll('form[data-confirmar]').forEach(f => f.addEventListener('submit', e => {
      if (f.dataset.confirmado === 'si' || !dialogo || !dialogo.showModal) return;
      e.preventDefault();
      dialogo.querySelector('[data-mensaje]').textContent = f.dataset.confirmar;
      dialogo.showModal();
      dialogo.querySelector('[data-aceptar]').onclick = () => { f.dataset.confirmado = 'si'; dialogo.close(); f.requestSubmit(); };
      dialogo.querySelector('[data-cancelar]').onclick = () => dialogo.close();
    }));

    // Badge de notificaciones no leídas
    const badge = document.getElementById('no-leidas');
    if (badge) {
      DonaTrack.json('/front-api/notificaciones/no-leidas').then(r => {
        if (r && r.cantidad > 0) { badge.textContent = r.cantidad; badge.hidden = false; badge.setAttribute('aria-label', r.cantidad + ' sin leer'); }
      }).catch(() => {});
    }
  });

  // ------------------------------------------------------------------ mapas (Leaflet)
  const ORIGEN = [-34.5986, -58.4202]; // depósito UTN Medrano

  DonaTrack.mapa = function (id, centro, zoom) {
    const m = L.map(id, { scrollWheelZoom: false }).setView(centro || ORIGEN, zoom || 10);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19, attribution: '&copy; colaboradores de OpenStreetMap'
    }).addTo(m);
    return m;
  };

  DonaTrack.icono = function (emoji, titulo) {
    return L.divIcon({
      className: '', iconSize: [36, 36], iconAnchor: [18, 18],
      // titulo puede traer datos de usuario (razón social): SIEMPRE escapado.
      html: '<div role="img" aria-label="' + DonaTrack.escapar(titulo) + '" style="font-size:26px;line-height:36px;text-align:center;filter:drop-shadow(0 1px 2px rgba(0,0,0,.4))">' + DonaTrack.escapar(emoji) + '</div>'
    });
  };

  DonaTrack.escapar = function (s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  };

  // 'es-AR' resuelve a 12 h y Chrome omite "p. m.": las 16:10 salían "04:10". Siempre 24 h y hora argentina
  // (la misma zona que usa el servidor), sin depender de cómo esté configurado el dispositivo.
  const ZONA = 'America/Argentina/Buenos_Aires';

  DonaTrack.hora = function (iso) {
    if (!iso) return 'sin datos';
    return new Date(iso).toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'medium', hourCycle: 'h23', timeZone: ZONA });
  };

  DonaTrack.horaCorta = function (fecha) {
    return new Date(fecha).toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23', timeZone: ZONA });
  };

  /**
   * Dibuja rutas con camión, paradas y recorrido. Devuelve una función de actualización
   * para usar con polling (no recrea el mapa: mueve marcadores).
   */
  DonaTrack.rutasEnMapa = function (mapa) {
    const capa = L.layerGroup().addTo(mapa);
    let primeraVez = true;
    return function (rutas) {
      capa.clearLayers();
      const puntos = [];
      (rutas || []).forEach(r => {
        (r.paradas || []).forEach(p => {
          const propia = (p.entregas || []).some(e => e.propia);
          L.marker([p.latitud, p.longitud], { icon: DonaTrack.icono(propia ? '🏠' : '📍', 'Parada ' + p.orden) })
            .bindPopup('<strong>' + DonaTrack.escapar(p.entidadNombre) + '</strong><br>' + DonaTrack.escapar(p.direccion)
              + '<br>Parada ' + p.orden + ' · ' + (p.entregas || []).length + ' entrega(s)')
            .addTo(capa);
          puntos.push([p.latitud, p.longitud]);
        });
        if (r.recorrido && r.recorrido.length > 1) {
          L.polyline(r.recorrido, { color: '#0f766e', weight: 4, opacity: .7 }).addTo(capa);
        }
        if (r.ultimaPosicion) {
          const pos = [r.ultimaPosicion.latitud, r.ultimaPosicion.longitud];
          L.marker(pos, { icon: DonaTrack.icono('🚚', 'Camión ' + r.patente), zIndexOffset: 1000 })
            .bindPopup('<strong>Camión ' + DonaTrack.escapar(r.patente) + '</strong><br>Velocidad: '
              + Math.round(r.ultimaPosicion.velocidadKmh) + ' km/h<br>Última actualización: ' + DonaTrack.hora(r.ultimaPosicion.registradaEn))
            .addTo(capa);
          puntos.push(pos);
        }
      });
      if (primeraVez && puntos.length) { mapa.fitBounds(puntos, { padding: [40, 40], maxZoom: 13 }); primeraVez = false; }
    };
  };

  /** Polling con pausa automática cuando la pestaña no está visible (ahorra batería y red). */
  DonaTrack.polling = function (url, cada, alRecibir) {
    let temporizador;
    async function ciclo() {
      try { const datos = await DonaTrack.json(url); if (datos) alRecibir(datos); }
      catch (e) { console.warn('Polling', e); }
      if (!document.hidden) temporizador = setTimeout(ciclo, cada);
    }
    document.addEventListener('visibilitychange', () => { clearTimeout(temporizador); if (!document.hidden) ciclo(); });
    ciclo();
  };

  // ------------------------------------------------------------------ selector de ubicación (registro de entidades)
  DonaTrack.selectorUbicacion = function (idMapa, idLat, idLng) {
    const lat = document.getElementById(idLat), lng = document.getElementById(idLng);
    const inicial = lat.value && lng.value ? [parseFloat(lat.value), parseFloat(lng.value)] : ORIGEN;
    const m = DonaTrack.mapa(idMapa, inicial, lat.value ? 15 : 11);
    let marcador = lat.value ? L.marker(inicial, { draggable: true }).addTo(m) : null;
    function fijar(p) {
      lat.value = p.lat.toFixed(6); lng.value = p.lng.toFixed(6);
      if (!marcador) { marcador = L.marker(p, { draggable: true }).addTo(m); marcador.on('dragend', ev => fijar(ev.target.getLatLng())); }
      else marcador.setLatLng(p);
      document.getElementById(idMapa + '-estado').textContent = 'Ubicación seleccionada: ' + lat.value + ', ' + lng.value;
    }
    if (marcador) marcador.on('dragend', ev => fijar(ev.target.getLatLng()));
    m.on('click', ev => fijar(ev.latlng));
  };
})();
