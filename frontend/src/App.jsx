import React, { useState } from 'react';
import './App.css';

// Exemplo de dados dos animais (substitua pelas informações e fotos dos seus pets!)
const MEUS_PETS = [
  {
    id: 1,
    nome: 'Rex',
    especie: 'Cachorro',
    idade: '2 anos',
    descricao: 'Muito brincalhão e adorável. Adora correr no quintal.',
    // Substitua a URL abaixo pelo caminho da foto do seu pet (ex: '/fotos/rex.jpg')
    imagem: 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&q=80&w=400'
  },
  {
    id: 2,
    nome: 'Mimi',
    especie: 'Gato',
    idade: '1 ano',
    descricao: 'Carinhosa, tranquila e ama um dengo no colo.',
    imagem: 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&q=80&w=400'
  },
  {
    id: 3,
    nome: 'Thor',
    especie: 'Cachorro',
    idade: '3 anos',
    descricao: 'Protetor, companheiro e muito esperto.',
    imagem: 'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?auto=format&fit=crop&q=80&w=400'
  }
];

export default function App() {
  const [animais] = useState(MEUS_PETS);
  const [busca, setBusca] = useState('');

  const animaisFiltrados = animais.filter(animal =>
    animal.nome.toLowerCase().includes(busca.toLowerCase()) ||
    animal.especie.toLowerCase().includes(busca.toLowerCase())
  );

  return (
    <div className="app-container">
      {/* Cabeçalho */}
      <header className="header">
        <h1>🐾 Abrigo de Animais</h1>
        <p>Sistema de Adoção & Gestão de Pets</p>
      </header>

      {/* Conteúdo Principal */}
      <main className="content">
        <section className="busca-section">
          <h2>Exemplos de Animais para Adoção</h2>
          <input
            type="text"
            placeholder="Buscar por nome ou espécie..."
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            className="input-busca"
          />
        </section>

        {/* Galeria de Animais */}
        <div className="cards-grid">
          {animaisFiltrados.map((animal) => (
            <div key={animal.id} className="card">
              <img src={animal.imagem} alt={animal.nome} className="card-img" />
              <div className="card-body">
                <h3>{animal.nome}</h3>
                <span className="badge">{animal.especie}</span> • <span>{animal.idade}</span>
                <p>{animal.descricao}</p>
                <button className="btn-adotar">Tenho Interesse</button>
              </div>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}