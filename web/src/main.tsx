import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { App } from './App';
import { ProvedorDaSessao } from './lib/sessao';
import './estilos/global.css';

const raiz = document.getElementById('raiz');

if (!raiz) {
  throw new Error('O elemento raiz não existe no HTML');
}

createRoot(raiz).render(
  <StrictMode>
    <BrowserRouter>
      <ProvedorDaSessao>
        <App />
      </ProvedorDaSessao>
    </BrowserRouter>
  </StrictMode>,
);
