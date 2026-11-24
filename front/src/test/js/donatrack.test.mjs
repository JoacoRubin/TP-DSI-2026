// Ejecutar con: node --test front/src/test/js
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import vm from 'node:vm';

const fuente = readFileSync(fileURLToPath(new URL('../../main/resources/static/js/donatrack.js', import.meta.url)), 'utf8');

function cargarDonaTrack() {
  const window = {};
  const contexto = {
    window,
    document: { addEventListener() {}, querySelector() { return null; }, querySelectorAll() { return []; } },
    L: { divIcon: opciones => opciones },
  };
  vm.runInNewContext(fuente, contexto);
  return window.DonaTrack;
}

test('icono escapa el título: una razón social maliciosa no inyecta HTML en /mapa', () => {
  const DonaTrack = cargarDonaTrack();
  const icono = DonaTrack.icono('💚', 'Entrega a "><img src=x onerror=alert(1)>');

  assert.ok(!icono.html.includes('<img'), 'no debe quedar una etiqueta <img> ejecutable');
  assert.ok(icono.html.includes('aria-label="Entrega a &quot;&gt;&lt;img src=x onerror=alert(1)&gt;"'));
});

// Hallazgo de la demo: Chrome resuelve 'es-AR' con ciclo de 12 h y SIN "p. m.": las 16:10 se veían "04:10".
test('hora usa 24 h en horario de Argentina', () => {
  const DonaTrack = cargarDonaTrack();
  const texto = DonaTrack.hora('2026-11-24T19:10:14Z'); // 16:10:14 en Buenos Aires
  assert.match(texto, /16:10:14/);
  assert.match(texto, /24\/11\/(20)?26/);
  assert.doesNotMatch(texto, /04:10/);
});

test('horaCorta da sólo la hora en 24 h', () => {
  assert.equal(cargarDonaTrack().horaCorta(new Date('2026-11-24T19:10:14Z')), '16:10:14');
});

test('icono conserva el emoji y un título normal', () => {
  const icono = cargarDonaTrack().icono('🚚', 'Camión AE123BC');
  assert.ok(icono.html.includes('aria-label="Camión AE123BC"'));
  assert.ok(icono.html.includes('🚚'));
});
