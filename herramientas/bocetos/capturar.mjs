// =============================================================================
//  Bocetos anotados de DonaTrack: capturas de las pantallas REALES con marcadores
//  numerados que explican qué requerimiento cubre cada parte.
//
//  Requisitos: DonaTrack corriendo (scripts/iniciar.*) y Microsoft Edge o Chrome.
//    cd herramientas/bocetos && npm install && npm run capturar
//  Variables: DONATRACK_URL (def. http://localhost:8080), NAVEGADOR (ruta al ejecutable)
//  Salida: docs/bocetos/NN-nombre.png + docs/bocetos/README.md (índice)
// =============================================================================
import puppeteer from 'puppeteer-core';
import { mkdirSync, writeFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const BASE = process.env.DONATRACK_URL ?? 'http://localhost:8080';
const CLAVE = 'Demo1234!';
const SALIDA = join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'docs', 'bocetos');
const NAVEGADORES = [
  process.env.NAVEGADOR,
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  '/usr/bin/google-chrome', '/usr/bin/chromium', '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
].filter(Boolean);

// cuenta: null = público. url: ruta o función async (page) => ruta. marcas: [selector, nota].
const PANTALLAS = [
  { archivo: '01-landing', titulo: 'Landing pública', reqs: [1], cuenta: null, url: '/', marcas: [
    ['#titulo-heroe', 'Propósito de DonaTrack y llamada a la acción para donar o registrarse'],
    ['#titulo-impacto', 'Métricas de impacto: lo que ya se logró'],
    ['#titulo-destacadas', 'Donaciones destacadas del último mes, con fotos de la recepción'],
    ['#titulo-podio', 'Podio del ranking mensual (top 3 donantes)'],
    ['header nav, nav', 'Accesos: mapa, galería, ingreso y registro'] ] },
  { archivo: '02-mapa', titulo: 'Mapa público de donaciones entregadas', reqs: [2], cuenta: null, url: '/mapa', marcas: [
    ['#mapa', 'Un marcador por entrega; al tocarlo muestra entidad, bienes y fecha (sin datos del donante)'],
    ['.tabla-envoltura', 'Alternativa accesible al mapa: la misma información en tabla'] ] },
  { archivo: '03-registro', titulo: 'Registro', reqs: [3], cuenta: null, url: '/registro', marcas: [
    ['main .grilla', 'Elección del tipo de cuenta: persona humana, jurídica o entidad beneficiaria'] ] },
  { archivo: '04-login', titulo: 'Inicio de sesión', reqs: [4], cuenta: null, url: '/login', marcas: [
    ['#email', 'Un único login para todos los roles: el sistema redirige según el rol'],
    ['#clave', 'Contraseña; tras 10 intentos por minuto se aplica rate limiting (429)'] ] },

  { archivo: '05-donante-donaciones', titulo: 'Donante · Mis donaciones', reqs: [5], cuenta: 'ana@donatrack.org', url: '/donante/donaciones', marcas: [
    ['main form', 'Filtros por estado y por categoría/subcategoría'],
    ['.tabla-envoltura', 'Estado de cada donación y acceso a su detalle/trazabilidad'] ] },
  { archivo: '06-donante-entidades', titulo: 'Donante · Entidades beneficiarias', reqs: [6], cuenta: 'ana@donatrack.org', url: '/donante/entidades', marcas: [
    ['#texto', 'Búsqueda de entidades por nombre'],
    ['main .grilla', 'Fichas de entidades con sus necesidades activas'] ] },
  { archivo: '07-donante-incentivos', titulo: 'Donante · Misiones e insignias', reqs: [7], cuenta: 'ana@donatrack.org', url: '/donante/incentivos', marcas: [
    ['#t-mision', 'Misión en curso y su progreso'],
    ['#t-insignias', 'Insignias obtenidas'],
    ['#t-evolucion', 'Evolución de categoría del donante'] ] },
  { archivo: '08-notificaciones', titulo: 'Notificaciones', reqs: [8, 13], cuenta: 'ana@donatrack.org', url: '/notificaciones', marcas: [
    ['main h1', 'Bandeja de avisos (asignación, misión, categoría, recepción); también llegan por el medio preferido'] ] },
  { archivo: '09-seguimiento', titulo: 'Seguimiento de camiones', reqs: [9, 14], cuenta: 'ana@donatrack.org', url: '/donante/seguimiento', marcas: [
    ['#mapa', 'Posición del camión en tiempo real (polling) y sólo las paradas propias'] ] },

  { archivo: '10-entidad-necesidades', titulo: 'Entidad · Necesidades', reqs: [10], cuenta: 'escuela10@donatrack.org', url: '/entidad/necesidades', marcas: [
    ['#descripcion', 'Alta de necesidad: qué se necesita'],
    ['#periodicidad', 'Extraordinaria o recurrente (periodicidad)'],
    ['#t-activas', 'Necesidades activas, con baja'] ] },
  { archivo: '11-entidad-donaciones', titulo: 'Entidad · Donaciones asignadas', reqs: [11, 12], cuenta: 'escuela10@donatrack.org', url: '/entidad/donaciones', marcas: [
    ['article.tarjeta', 'Estado de cada donación + estado de la entrega física (paginado de a 20; el paginador aparece con más de una página)'],
    ['input[type=file]', 'Fotos de lo recibido. Con el camión en camino, acá se confirma la recepción con fotos o se informa que no llegó'] ] },

  { archivo: '12-admin-asignacion', titulo: 'Administración · Asignación', reqs: [17], cuenta: 'admin@donatrack.org',
    // Busca una donación pendiente que tenga coincidencias entre algoritmos (la más ilustrativa).
    url: async (page) => {
      await page.goto(`${BASE}/admin/asignaciones`, { waitUntil: 'networkidle2' });
      const hrefs = await page.$$eval('a[href^="/admin/asignaciones/"]', as => [...new Set(as.map(a => a.getAttribute('href')))]);
      for (const href of hrefs.slice(0, 15)) {
        await page.goto(`${BASE}${href}`, { waitUntil: 'networkidle2' });
        if (await page.$('#t-coinc')) return href;
      }
      return hrefs[0] ?? '/admin/asignaciones';
    }, marcas: [
    ['#t-coinc, main .banner', 'Coincidencias entre ambos algoritmos de matchmaking (o aviso si no las hay)'],
    ['main .grilla', 'Ranking top 10 de cada algoritmo: compatibilidad semántica y prioridad a sub-atendidos'],
    ['main form', 'La persona administradora elige la entidad final (o recalcula)'] ] },
  { archivo: '13-admin-camiones', titulo: 'Administración · Camiones', reqs: [18], cuenta: 'admin@donatrack.org', url: '/admin/camiones', marcas: [
    ['#t-nuevo', 'Alta de camión con capacidad (volumen, altura, carga)'],
    ['#t-flota', 'Flota: edición y baja'] ] },
  { archivo: '14-admin-rankings', titulo: 'Administración · Ranking mensual', reqs: [19], cuenta: 'admin@donatrack.org', url: '/admin/rankings', marcas: [
    ['#t-actual', 'Ranking del mes (por misiones cumplidas)'],
    ['#t-hist', 'Historial de rankings anteriores'] ] },
  { archivo: '15-admin-importaciones', titulo: 'Administración · Importación CSV', reqs: [20], cuenta: 'admin@donatrack.org', url: '/admin/importaciones', marcas: [
    ['#archivo', 'Carga de CSV (+10.000 filas), procesado asíncrono por lotes'],
    ['#t-hist', 'Historial y progreso de importaciones'] ] },
  { archivo: '16-admin-monitoreo', titulo: 'Administración · Monitoreo de flota', reqs: [], cuenta: 'admin@donatrack.org', url: '/admin/monitoreo', marcas: [
    ['#mapa', 'Todos los camiones en ruta, en tiempo real'],
    ['#tabla-rutas', 'Rutas activas y planificadas'] ] },
];

async function iniciarSesion(page, email) {
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle2' });
  await page.type('#email', email);
  await page.type('#clave', CLAVE);
  await Promise.all([page.waitForNavigation({ waitUntil: 'networkidle2' }), page.click('main form button[type=submit]')]);
  if (page.url().includes('/login')) throw new Error(`No se pudo iniciar sesión como ${email}`);
}

/** Inserta cabecera con leyenda y marcadores numerados sobre los elementos. Devuelve los selectores no encontrados. */
function anotar({ titulo, reqs, marcas, numero }) {
  const cab = document.createElement('section');
  cab.setAttribute('data-boceto', '');
  cab.style.cssText = 'background:#fffbeb;border-bottom:4px solid #f59e0b;padding:16px 24px;font:15px system-ui;color:#1f2937';
  const req = reqs.length ? ` · Requerimiento${reqs.length > 1 ? 's' : ''} ${reqs.join(', ')}` : '';
  cab.innerHTML = `<strong style="font-size:18px">Boceto ${numero} — ${titulo}</strong><span>${req}</span><ol style="margin:8px 0 0 20px;padding:0"></ol>`;
  const lista = cab.querySelector('ol');
  const faltantes = [];
  marcas.forEach(([selector, nota], i) => {
    const li = document.createElement('li');
    li.textContent = nota;
    lista.appendChild(li);
    const el = document.querySelector(selector);
    if (!el) { faltantes.push(selector); return; }
    el.style.outline = '3px dashed #f59e0b';
    el.style.outlineOffset = '4px';
    const r = el.getBoundingClientRect();
    const b = document.createElement('div');
    b.setAttribute('data-boceto', '');
    b.textContent = String(i + 1);
    b.style.cssText = `position:absolute;z-index:99999;left:${Math.max(4, r.left + window.scrollX - 14)}px;top:${Math.max(4, r.top + window.scrollY - 14)}px;`
      + 'width:30px;height:30px;border-radius:50%;background:#f59e0b;color:#111;font:bold 16px/30px system-ui;text-align:center;box-shadow:0 2px 6px rgba(0,0,0,.35)';
    document.body.appendChild(b);
  });
  document.body.prepend(cab);
  // la cabecera desplaza el contenido: corregir la posición de los marcadores
  const alto = cab.getBoundingClientRect().height;
  document.querySelectorAll('div[data-boceto]').forEach(b => { b.style.top = `${parseFloat(b.style.top) + alto}px`; });
  return faltantes;
}

async function main() {
  const ejecutable = NAVEGADORES.find(p => existsSync(p));
  if (!ejecutable) throw new Error('No encontré Edge ni Chrome. Definí NAVEGADOR con la ruta al ejecutable.');
  mkdirSync(SALIDA, { recursive: true });
  const navegador = await puppeteer.launch({ executablePath: ejecutable, headless: true, defaultViewport: { width: 1366, height: 900 } });
  const indice = [];
  let problemas = 0;
  try {
    const porCuenta = new Map();
    for (const p of PANTALLAS) {
      if (!porCuenta.has(p.cuenta)) porCuenta.set(p.cuenta, []);
      porCuenta.get(p.cuenta).push(p);
    }
    for (const [cuenta, pantallas] of porCuenta) {
      const contexto = await navegador.createBrowserContext(); // sesión aislada por rol
      const page = await contexto.newPage();
      if (cuenta) await iniciarSesion(page, cuenta);
      for (const p of pantallas) {
        const ruta = typeof p.url === 'function' ? await p.url(page) : p.url;
        await page.goto(`${BASE}${ruta}`, { waitUntil: 'networkidle2' });
        await new Promise(r => setTimeout(r, 800)); // tiles del mapa / polling inicial
        const numero = PANTALLAS.indexOf(p) + 1;
        const faltantes = await page.evaluate(anotar, { titulo: p.titulo, reqs: p.reqs, marcas: p.marcas, numero });
        await page.screenshot({ path: join(SALIDA, `${p.archivo}.png`), fullPage: true });
        const aviso = faltantes.length ? `  ⚠ sin marcador para: ${faltantes.join(', ')}` : '';
        if (faltantes.length) problemas++;
        console.log(`✔ ${p.archivo}.png  (${ruta})${aviso}`);
        indice.push(`| ${numero} | [${p.titulo}](${p.archivo}.png) | ${p.reqs.join(', ') || '—'} | \`${ruta}\` |`);
      }
      await contexto.close();
    }
  } finally {
    await navegador.close();
  }
  writeFileSync(join(SALIDA, 'README.md'), [
    '# Bocetos anotados de la interfaz',
    '',
    'Capturas de las pantallas **reales** de DonaTrack con marcadores numerados: cada número de la imagen',
    'corresponde a la nota de la cabecera y al requerimiento de UI/UX que cubre (ver tabla de trazabilidad del README).',
    'Se regeneran con `cd herramientas/bocetos && npm install && npm run capturar` (DonaTrack corriendo).',
    '',
    '| # | Pantalla | Requerimientos | Ruta |',
    '|---|---|---|---|',
    ...indice, ''].join('\n'));
  console.log(`\n${indice.length} bocetos en ${SALIDA}${problemas ? ` — ${problemas} con marcadores faltantes (revisar selectores)` : ''}`);
}

main().catch(e => { console.error(e.message); process.exit(1); });
