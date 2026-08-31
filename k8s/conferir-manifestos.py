# -*- coding: utf-8 -*-
"""Confere os manifestos do Kubernetes antes de alguém aplicar.

Existe por causa de um defeito real: a pasta tinha a base e a borda, o HPA da
borda apontava para um Deployment `servico-busca`, e esse Deployment não estava
escrito em lugar nenhum. Um `kubectl apply` subia um agrupamento sem serviço
nenhum e com um autoscaler órfão, sem erro nenhum na saída.

Nenhuma ferramenta de esquema pega isso: o YAML é válido e o campo existe. O
que falta é conferir que uma coisa referida existe de verdade, que é o mesmo
tipo de conferência que o teste de arquitetura faz no código Java.

Roda sem cluster e sem kubectl, que é o ponto: vale no CI de graça.
"""
import glob
import os
import sys

try:
    import yaml
except ImportError:
    print('pyyaml não está instalado: pip install pyyaml')
    sys.exit(2)

PASTA = os.path.dirname(os.path.abspath(__file__))

documentos = []
for caminho in sorted(glob.glob(os.path.join(PASTA, '*.yaml'))):
    with open(caminho, encoding='utf-8') as arquivo:
        for doc in yaml.safe_load_all(arquivo):
            if doc:
                documentos.append((os.path.basename(caminho), doc))

erros = []


def de(tipo):
    return [(a, d) for a, d in documentos if d.get('kind') == tipo]


nomes_de_deployment = {d['metadata']['name'] for _, d in de('Deployment')}
rotulos_de_deployment = {
    d['spec']['selector']['matchLabels'].get('app.kubernetes.io/name'): d['metadata']['name']
    for _, d in de('Deployment')
}

# 1. Todo alvo de escala precisa existir.
for arq, d in de('HorizontalPodAutoscaler'):
    alvo = d['spec']['scaleTargetRef']['name']
    if alvo not in nomes_de_deployment:
        erros.append('%s: o HPA %s escala o Deployment %s, que não existe'
                     % (arq, d['metadata']['name'], alvo))

for arq, d in de('PodDisruptionBudget'):
    rotulo = d['spec']['selector']['matchLabels'].get('app.kubernetes.io/name')
    if rotulo not in rotulos_de_deployment:
        erros.append('%s: o PDB %s seleciona %s, que nenhum Deployment tem'
                     % (arq, d['metadata']['name'], rotulo))

# 2. Todo Service precisa achar pod.
for arq, d in de('Service'):
    rotulo = d['spec']['selector'].get('app.kubernetes.io/name')
    if rotulo not in rotulos_de_deployment:
        erros.append('%s: o Service %s aponta para %s, que nenhum Deployment tem'
                     % (arq, d['metadata']['name'], rotulo))

# 3. Todo Ingress precisa achar Service, e na porta certa.
portas_de_servico = {}
for _, d in de('Service'):
    portas_de_servico[d['metadata']['name']] = {p['port'] for p in d['spec']['ports']}

for arq, d in de('Ingress'):
    for regra in d['spec'].get('rules', []):
        for caminho in regra.get('http', {}).get('paths', []):
            alvo = caminho['backend']['service']
            if alvo['name'] not in portas_de_servico:
                erros.append('%s: o Ingress manda para o Service %s, que não existe'
                             % (arq, alvo['name']))
            elif alvo['port']['number'] not in portas_de_servico[alvo['name']]:
                erros.append('%s: o Ingress usa a porta %s do Service %s, que expõe %s'
                             % (arq, alvo['port']['number'], alvo['name'],
                                sorted(portas_de_servico[alvo['name']])))

# 4. Toda chave de Secret e de ConfigMap referida precisa estar declarada.
chaves = {}
for _, d in de('Secret'):
    chaves[d['metadata']['name']] = set(d.get('stringData', {}) or {})
configmaps = {d['metadata']['name'] for _, d in de('ConfigMap')}

for arq, d in de('Deployment'):
    for c in d['spec']['template']['spec']['containers']:
        for var in c.get('env', []):
            ref = (var.get('valueFrom') or {}).get('secretKeyRef')
            if not ref:
                continue
            if ref['name'] not in chaves:
                erros.append('%s: %s pede o Secret %s, que não está declarado'
                             % (arq, var['name'], ref['name']))
            elif ref['key'] not in chaves[ref['name']]:
                erros.append('%s: %s pede a chave %s do Secret %s, que tem %s'
                             % (arq, var['name'], ref['key'], ref['name'],
                                sorted(chaves[ref['name']])))
        for origem in c.get('envFrom', []):
            nome = (origem.get('configMapRef') or {}).get('name')
            if nome and nome not in configmaps:
                erros.append('%s: envFrom pede o ConfigMap %s, que não existe' % (arq, nome))

# 5. O que a plataforma exige do próprio contêiner.
for arq, d in de('Deployment'):
    for c in d['spec']['template']['spec']['containers']:
        nome = d['metadata']['name']
        if 'livenessProbe' not in c or 'readinessProbe' not in c:
            erros.append('%s: %s sem liveness ou readiness' % (arq, nome))
        if not (c.get('resources', {}).get('limits', {}) or {}).get('memory'):
            erros.append('%s: %s sem teto de memória, e um vazamento derruba o nó' % (arq, nome))
        if c.get('securityContext', {}).get('readOnlyRootFilesystem') is not True:
            erros.append('%s: %s com raiz gravável' % (arq, nome))
        if ':latest' in c.get('image', ''):
            erros.append('%s: %s usa a imagem latest, que não dá para reverter' % (arq, nome))

# 6. Todos os serviços do sistema precisam estar aqui, senão a topologia é
#    parcial e ninguém percebe olhando.
ESPERADOS = {'servico-cadastro', 'servico-catalogo', 'servico-busca',
             'servico-notificacoes', 'borda'}
faltando = ESPERADOS - nomes_de_deployment
if faltando:
    erros.append('faltam Deployments para: %s' % ', '.join(sorted(faltando)))

if erros:
    print('manifestos com problema:\n')
    for e in erros:
        print(' -', e)
    sys.exit(1)

print('manifestos conferidos: %d documentos, %d Deployments, nada solto'
      % (len(documentos), len(nomes_de_deployment)))
