import React, { useState } from 'react';
import './App.css';

const ANIMAIS_INICIAIS = [
  { id: 1, nome: 'Pipoca', especie: 'Cachorro', imagem:  '/fotos/Cachorro.jpeg'},
  { id: 2, nome: 'Mia', especie: 'Gato', imagem: '/fotos/Gato.jpeg'},
  { id: 3, nome: 'Penosa', especie: 'Galinha', imagem: '/fotos/Galinha.jpeg'},
  { id: 4, nome: 'Vicente', especie: 'Cabrito', imagem: '/fotos/Cabrito.jpeg'},
];

export default function App() {
  const [telaAtual, setTelaAtual] = useState('home');
  const [mensagemSucesso, setMensagemSucesso] = useState('');
  const [filtroEspecie, setFiltroEspecie] = useState('Todos');

  const handleSubmit = (e) => {
    e.preventDefault();
    setMensagemSucesso('Dados salvos com sucesso!');
    setTimeout(() => setMensagemSucesso(''), 3000);
  };

  return (
    <div className="app">
      {/* 1. Header Azul */}
      <header className="header-azul">
        <h1>🐾 Abrigo de Animais</h1>
        <p>Sistema de Gerenciamento e Resgates</p>
      </header>

      <div className="layout-body">
        {/* 2. Menu Lateral (Sidebar) */}
        <aside className="sidebar">
          <button onClick={() => setTelaAtual('home')}>Início</button>
          <button onClick={() => setTelaAtual('cadastrar')}>Cadastrar Animal</button>
          <button onClick={() => setTelaAtual('buscar')}>Buscar Animais</button>
          <button onClick={() => setTelaAtual('adocao')}>Solicitar Adoção</button>
          <button onClick={() => setTelaAtual('medico')}>Atendimento Médico</button>
          <button onClick={() => setTelaAtual('voluntario')}>Voluntariado</button>
        </aside>

        {/* 3. Área Conteúdo Principal */}
        <main className="conteudo-principal">
          {mensagemSucesso && <div className="aviso-sucesso">{mensagemSucesso}</div>}

          {/* TELA 1: INÍCIO */}
          {telaAtual === 'home' && (
            <div>
              <h2>Bem-vindo à nossa ONG!</h2>
              <p>Trabalhamos no resgate e reabilitação de animais abandonados.</p>
              
              <div className="estatisticas">
                <div className="card-stat">
                  <h3>120+</h3>
                  <p>Animais Salvos</p>
                </div>
                <div className="card-stat">
                  <h3>85</h3>
                  <p>Adoções Realizadas</p>
                </div>
              </div>

              <h3>Exemplo de Animais para Adoção</h3>
              <div className="grid-animais">
                {ANIMAIS_INICIAIS.map((pet) => (
                  <div key={pet.id} className="card-pet">
                    <img src={pet.imagem} alt={pet.nome} />
                    <h4>{pet.nome}</h4>
                    <p>{pet.especie} • {pet.idade}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TELA 2: CADASTRAR ANIMAL */}
          {telaAtual === 'cadastrar' && (
            <div>
              <h2>Cadastrar Novo Animal</h2>
              <form onSubmit={handleSubmit} className="form-simples">
                <label>Nome do Animal:</label>
                <input type="text" required placeholder="Ex: Mel" />

                <label>Espécie:</label>
                <select>
                  <option value="Selecione">Selecione uma opção: </option>
                  <option value="Cachorro">Cachorro</option>
                  <option value="Gato">Gato</option>
                  <option value="Gato">Galinha</option>
                  <option value="Gato">Cabrito</option>
                  <option value="Outro">Outro</option>
                </select>

                <label>Raça:</label>
                <input type="text" placeholder="Ex: Vira-lata" />

                <label>Idade:</label>
                <input type="text" placeholder="Ex: 2 anos" />

                <label>Porte:</label>
                <select>
                  <option value="Selecione">Selecione uma opção: </option>
                  <option value="Pequeno">Pequeno</option>
                  <option value="Médio">Médio</option>
                  <option value="Grande">Grande</option>
                </select>

                <label>Estado de Saúde:</label>
                <input type="text" placeholder="Ex: Saudável, Vacinado" />

                <button type="submit">Cadastrar</button>
              </form>
            </div>
          )}

          {/* TELA 3: BUSCAR ANIMAIS */}
          {telaAtual === 'buscar' && (
            <div>
              <h2>Buscar Animais</h2>
              <label>Filtrar por espécie: </label>
              <select onChange={(e) => setFiltroEspecie(e.target.value)} value={filtroEspecie}>
                <option value="Todos">Todos</option>
                <option value="Cachorro">Cachorro</option>
                <option value="Gato">Gato</option>
                <option value="Galinha">Galinha</option>
                <option value="Cabrito">Cabrito</option>
              </select>

              <div className="grid-animais" style={{ marginTop: '20px' }}>
                {ANIMAIS_INICIAIS
                  .filter((pet) => filtroEspecie === 'Todos' || pet.especie === filtroEspecie)
                  .map((pet) => (
                    <div key={pet.id} className="card-pet">
                      <img src={pet.imagem} alt={pet.nome} />
                      <h4>{pet.nome}</h4>
                      <p>{pet.especie} • {pet.idade}</p>
                    </div>
                  ))}
              </div>
            </div>
          )}

          {/* TELA 4: SOLICITAR ADOÇÃO */}
          {telaAtual === 'adocao' && (
            <div>
              <h2>Solicitar Adoção</h2>
              <form onSubmit={handleSubmit} className="form-simples">
                <label>Selecione o Animal:</label>
                <select>
                  {ANIMAIS_INICIAIS.map((pet) => (
                    <option key={pet.id} value={pet.nome}>{pet.nome} ({pet.especie})</option>
                  ))}
                </select>

                <label>Seu Nome Completo:</label>
                <input type="text" required placeholder="Digite seu nome" />

                <label>Seu Telefone/WhatsApp:</label>
                <input type="text" required placeholder="(00) 00000-0000" />

                <button type="submit">Enviar Solicitação</button>
              </form>
            </div>
          )}

          {/* TELA 5: REGISTRO MÉDICO */}
          {telaAtual === 'medico' && (
            <div>
              <h2>Registrar Atendimento Médico</h2>
              <form onSubmit={handleSubmit} className="form-simples">
                <label>ID / Nome do Animal:</label>
                <input type="text" required placeholder="Ex: 1 ou Thor" />

                <label>CRMV do Veterinário:</label>
                <input type="text" required placeholder="Ex: CRMV-SP 12345" />

                <label>Tipo de Atendimento:</label>
                <input type="text" required placeholder="Ex: Vacina V10, Consulta de rotina" />

                <label>Data:</label>
                <input type="date" required />

                <button type="submit">Salvar Registro</button>
              </form>
            </div>
          )}

          {/* TELA 6: VOLUNTARIADO */}
          {telaAtual === 'voluntario' && (
            <div>
              <h2>Inscrição para Voluntariado</h2>
              <form onSubmit={handleSubmit} className="form-simples">
                <label>Seu Nome:</label>
                <input type="text" required placeholder="Digite seu nome" />

                <label>Atividade Desejada:</label>
                <select>
                  <option value="Passear com cães">Passear com cães</option>
                  <option value="Limpeza dos abrigos">Limpeza dos abrigos</option>
                  <option value="Eventos de adoção">Eventos de adoção</option>
                </select>

                <label>Disponibilidade:</label>
                <input type="text" placeholder="Ex: Finais de semana" />

                <button type="submit">Inscrever-se</button>
              </form>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}