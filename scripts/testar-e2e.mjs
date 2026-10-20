import { spawn, spawnSync } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import { createServer } from 'node:net';
import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const raiz = fileURLToPath(new URL('../', import.meta.url));
const jar = path.join(raiz, 'servidor-unico/target/servidor-unico-1.0.0.jar');
const banco = process.env.BANCO_URL ?? '';
// A suíte cadastra, modera e remove dados. Exige um banco local com nome
// reservado para E2E, para não alterar por engano o banco de desenvolvimento.
if (!/^jdbc:postgresql:\/\/(localhost|127\.0\.0\.1):\d+\/vitrine_e2e(?:_[\w-]+)?$/.test(banco)) {
  throw new Error('Defina BANCO_URL para um banco local vitrine_e2e. Ex.: jdbc:postgresql://127.0.0.1:5432/vitrine_e2e');
}
if (!existsSync(jar)) throw new Error('Empacote antes: mvn -pl servidor-unico -am package -DskipTests');

async function portaLivre() {
  const servidor = createServer();
  await new Promise(resolve => servidor.listen(0, '127.0.0.1', resolve));
  const porta = servidor.address().port;
  await new Promise(resolve => servidor.close(resolve));
  return porta;
}
const apiPorta = await portaLivre();
const webPorta = await portaLivre();
const ambiente = {
  ...process.env,
  PORTA: String(apiPorta),
  DEMO_ATIVO: 'true',
  // A suíte faz muitos logins da mesma origem; segurança é testada no Maven.
  LIMITE_ATIVO: 'false',
  VITRINE_SEGREDO_JWT: randomBytes(48).toString('hex'),
  ORIGENS_PERMITIDAS: `http://127.0.0.1:${webPorta}`,
  VITE_API_URL: '',
  VITE_API_ALVO: `http://127.0.0.1:${apiPorta}`,
  E2E_URL: `http://127.0.0.1:${webPorta}`,
  E2E_REUTILIZAR: 'false',
};
const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin/java') : 'java';
const api = spawn(java, ['-XX:ActiveProcessorCount=2', '-Xmx384m', '-jar', jar], {
  cwd: raiz, env: ambiente, stdio: ['ignore', 'inherit', 'inherit'], windowsHide: true,
});
let teste;
function encerrar(processo) {
  if (!processo?.pid || processo.exitCode !== null) return;
  if (process.platform === 'win32') {
    spawnSync('taskkill', ['/PID', String(processo.pid), '/T', '/F'], { stdio: 'ignore', windowsHide: true });
  } else processo.kill('SIGTERM');
}
process.once('SIGINT', () => { encerrar(teste); encerrar(api); process.exit(130); });
process.once('SIGTERM', () => { encerrar(teste); encerrar(api); process.exit(143); });
try {
  const prazo = Date.now() + 180_000;
  while (true) {
    if (api.exitCode !== null) throw new Error('A API encerrou durante a partida. Confira o log acima.');
    try {
      const resposta = await fetch(`http://127.0.0.1:${apiPorta}/actuator/health`, { signal: AbortSignal.timeout(1500) });
      if (resposta.ok) break;
    } catch { /* a partida ainda está em andamento */ }
    if (Date.now() >= prazo) throw new Error('A API não ficou pronta em três minutos.');
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  const cli = path.join(raiz, 'web/node_modules/@playwright/test/cli.js');
  teste = spawn(process.execPath, [cli, 'test', ...process.argv.slice(2)], {
    cwd: path.join(raiz, 'web'), env: ambiente, stdio: 'inherit', windowsHide: true,
  });
  process.exitCode = await new Promise((resolve, reject) => {
    teste.once('error', reject);
    teste.once('exit', code => resolve(code ?? 1));
  });
} finally { encerrar(teste); encerrar(api); }
