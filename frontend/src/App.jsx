import React, { useState } from 'react';
import './App.css';

const CATEGORIAS_EXEMPLO = [
  { id: 'ex1', tipo: 'Cachorro', imagem: '/fotos/Cachorro.jpeg'},
  { id: 'ex2', tipo: 'Gato', imagem: '/fotos/Gato.jpeg'},
  { id: 'ex3', tipo: 'Galinha', imagem: '/fotos/Galinha.jpeg'},
  { id: 'ex4', tipo: 'Cabrito', imagem: '/fotos/Cabrito.jpeg'},
];  

const dataFutura = (dias) => {
  const data = new Date();
  data.setDate(data.getDate() + dias);
  return `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}-${String(data.getDate()).padStart(2, '0')}`;
};

const DADOS_INICIAIS = {
  animais: [],
  usuarios: [],
  solicitacoes: [],
  registrosMedicos: [],
  atividades: [],
  inscricoes: [],
};

const requisicaoApi = async (caminho, opcoes = {}, token) => {
  let resposta;
  try {
    resposta = await fetch(`/api${caminho}`, {
      ...opcoes,
      headers: {
        ...(opcoes.body ? { 'Content-Type': 'application/json' } : {}),
        ...opcoes.headers,
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });
  } catch (error) {
    throw new Error('Não foi possível conectar ao backend. Verifique se a aplicação Java está em execução.');
  }

  let conteudo = null;

  if (resposta.status !== 204) {
    const texto = await resposta.text();
    if (texto.trim()) {
      try {
        conteudo = JSON.parse(texto);
      } catch {
        if (resposta.ok) {
          throw new Error('O backend retornou uma resposta em formato inválido.');
        }
      }
    } else if (resposta.ok) {
      conteudo = {};
    }
  }

  if (!resposta.ok) {
    throw new Error(conteudo?.mensagem || conteudo?.erro || `Erro no servidor (Status HTTP ${resposta.status})`);
  }

  return conteudo;
};

const normalizar = (valor) => String(valor ?? '')
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .toUpperCase();

const formatarData = (data) => {
  if (!data) return '—';
  return new Date(`${data}T00:00:00`).toLocaleDateString('pt-BR');
};

export default function App() {
  const [telaAtual, setTelaAtual] = useState('home');
  const [aviso, setAviso] = useState(null);
  const [dados, setDados] = useState(DADOS_INICIAIS);
  const [carregando, setCarregando] = useState(false);
  const [sessao, setSessao] = useState(null);
  const [erroLogin, setErroLogin] = useState('');

  const [filtroEspecie, setFiltroEspecie] = useState('Todos');
  const [filtroPorte, setFiltroPorte] = useState('Todos');
  const [filtroSaude, setFiltroSaude] = useState('Todos');
  const [filtroStatus, setFiltroStatus] = useState('Todos');

  const [tipoUsuario, setTipoUsuario] = useState('Voluntário');
  const [animalEditando, setAnimalEditando] = useState(null);
  const [usuarioEditando, setUsuarioEditando] = useState(null);
  const [animalProntuarioFiltro, setAnimalProntuarioFiltro] = useState('');
  const [formAtividade, setFormAtividade] = useState({ titulo: '', descricao: '', data: '' });

  const [formAnimal, setFormAnimal] = useState({
    nome: '', especie: 'Cachorro', raca: '', idade: '', porte: 'Pequeno',
    estadoSaude: 'Saudável', dataResgate: '', imagem: '',
  });

  const { animais, usuarios, solicitacoes, registrosMedicos, atividades, inscricoes } = dados;

  const notificar = (tipo, texto) => setAviso({ tipo, texto });
  const atualizarEstado = async (caminho, metodo, corpo) => {
    const resultado = await requisicaoApi(caminho, {
      method: metodo,
      ...(corpo ? { body: JSON.stringify(corpo) } : {}),
    }, sessao?.token);
    setDados(await requisicaoApi('/state', {}, sessao?.token));
    return resultado;
  };

  const handleLogin = async (evento) => {
    evento.preventDefault();
    const formulario = new FormData(evento.currentTarget);
    setCarregando(true);
    setErroLogin('');
    try {
      const novaSessao = await requisicaoApi('/auth/login', {
        method: 'POST',
        body: JSON.stringify({
          email: String(formulario.get('email')).trim(),
          senha: String(formulario.get('senha')),
        }),
      });
      const estado = await requisicaoApi('/state', {}, novaSessao.token);
      setSessao(novaSessao);
      setDados(estado);
    } catch (error) {
      setErroLogin(error.message);
    } finally {
      setCarregando(false);
    }
  };

  const handleLogout = async () => {
    try {
      await requisicaoApi('/auth/logout', { method: 'POST' }, sessao.token);
      setSessao(null);
      setDados(DADOS_INICIAIS);
      setTelaAtual('home');
    } catch (error) {
      notificar('erro', error.message);
    }
  };

  const animaisDisponiveis = animais.filter((animal) =>
    normalizar(animal.statusAdocao) === 'DISPONIVEL'
    && normalizar(animal.estadoSaude) !== 'CRITICO');
  
  const voluntarios = usuarios.filter((usuario) => usuario.tipo === 'Voluntário');

  const handleCadastrarAnimal = async (evento) => {
    evento.preventDefault();
    const corpo = { ...formAnimal, idade: Number(formAnimal.idade), imagem: formAnimal.imagem || '/fotos/Cachorro.jpeg' };
    try {
      if (animalEditando) {
        await atualizarEstado(`/animals/${animalEditando.id}`, 'PUT', corpo);
        notificar('sucesso', `Animal ${formAnimal.nome} atualizado com sucesso!`);
        setAnimalEditando(null);
      } else {
        const animal = await atualizarEstado('/animals', 'POST', corpo);
        notificar('sucesso', `Animal ${animal.nome} cadastrado com sucesso! ID: ${animal.id}`);
      }
      setFormAnimal({
        nome: '', especie: 'Cachorro', raca: '', idade: '', porte: 'Pequeno',
        estadoSaude: 'Saudável', dataResgate: '', imagem: '',
      });
    } catch (error) {
      notificar('erro', error.message);
    }
  };

  const handleIniciarEdicaoAnimal = (animal) => {
    setAnimalEditando(animal);
    setFormAnimal({
      nome: animal.nome,
      especie: animal.especie,
      raca: animal.raca,
      idade: String(animal.idade),
      porte: animal.porte,
      estadoSaude: animal.estadoSaude,
      dataResgate: animal.dataResgate || '',
      imagem: animal.imagem || '',
    });
    setTelaAtual('cadastrar');
  };

  const handleExcluirAnimal = async (id) => {
    if (window.confirm('Tem certeza que deseja excluir este animal?')) {
      try {
        await atualizarEstado(`/animals/${id}`, 'DELETE');
        notificar('sucesso', 'Animal excluído com sucesso.');
      } catch (error) {
        notificar('erro', error.message);
      }
    }
  };

  const handleDisponibilizarAnimal = async (id) => {
    try {
      await atualizarEstado(`/animals/${id}/availability`, 'POST');
      notificar('sucesso', 'Animal disponibilizado para adoção.');
    } catch (error) {
      notificar('erro', error.message);
    }
  };

  const handleCadastrarUsuario = async (evento) => {
    evento.preventDefault();
    const formularioElemento = evento.currentTarget;
    const formulario = new FormData(evento.currentTarget);
    const tipo = formulario.get('tipo');
    const nome = String(formulario.get('nome')).trim();
    const email = String(formulario.get('email')).trim();
    const telefone = String(formulario.get('telefone')).trim();

    const corpo = {
      tipo,
      nome,
      email,
      telefone,
      crmv: String(formulario.get('crmv') || '').trim(),
      nivelAcesso: String(formulario.get('nivelAcesso') || ''),
      senha: String(formulario.get('senha') || ''),
    };
    try {
      if (usuarioEditando) {
        await atualizarEstado(`/users/${usuarioEditando.id}`, 'PUT', corpo);
        notificar('sucesso', `Usuário ${nome} atualizado com sucesso!`);
        setUsuarioEditando(null);
      } else {
        const usuario = await atualizarEstado('/users', 'POST', corpo);
        notificar('sucesso', `${tipo} ${nome} cadastrado(a) com sucesso! ID: ${usuario.id}`);
      }
      formularioElemento.reset();
    } catch (error) {
      notificar('erro', error.message);
    }
  };

  const handleExcluirUsuario = async (id) => {
    if (window.confirm('Tem certeza que deseja remover este usuário?')) {
      try {
        await atualizarEstado(`/users/${id}`, 'DELETE');
        notificar('sucesso', 'Usuário removido com sucesso.');
      } catch (error) {
        notificar('erro', error.message);
      }
    }
  };

  const handleSolicitarAdocao = async (evento) => {
    evento.preventDefault();
    const formularioElemento = evento.currentTarget;
    const formulario = new FormData(evento.currentTarget);
    const animalId = String(formulario.get('animalId'));
    const animal = animais.find((item) => item.id === animalId);
    try {
      const pedido = await atualizarEstado('/requests', 'POST', {
        animalId,
        adotanteNome: String(formulario.get('nome')).trim(),
        adotanteContato: String(formulario.get('contato')).trim(),
      });
      formularioElemento.reset();
      notificar('sucesso', `Solicitação ${pedido.id} registrada para ${animal?.nome || 'o animal'}. A equipe fará a análise.`);
    } catch (error) {
      notificar('erro', error.message);
    }
  };

  const handleAtualizarSolicitacao = async (pedido, aprovar) => {
    try {
      await atualizarEstado(`/requests/${pedido.id}`, 'PATCH', {
        decisao: aprovar ? 'APROVADO' : 'REJEITADO',
      });
      notificar('sucesso', aprovar ? 'Adoção aprovada.' : `Solicitação ${pedido.id} rejeitada.`);
    } catch (error) {
      notificar('erro', error.message);
    }
  };

  const handleRegistrarAtendimento = async (evento) => {
    evento.preventDefault();
    const formularioElemento = evento.currentTarget;
    const formulario = new FormData(evento.currentTarget);
    const animalId = String(formulario.get('animalId'));
    const tipo = String(formulario.get('tipo'));
    const descricao = String(formulario.get('descricao')).trim();
    const data = String(formulario.get('data'));

    try {
      await atualizarEstado('/medical-records', 'POST', {
        animalId,
        tipo,
        descricao,
        data,
      });
      formularioElemento.reset();
      notificar('sucesso', 'Atendimento médico registrado com sucesso.');
    } catch (error) {
      notificar('erro', error.message);
    }
  };

  const handleInscricao = async (evento, atividade) => {
    evento.preventDefault();
    const formularioElemento = evento.currentTarget;
    const formulario = new FormData(evento.currentTarget);
    const voluntarioId = String(formulario.get('voluntarioId'));
    if (!voluntarioId) {
      notificar('erro', 'Cadastre ou selecione um voluntário para realizar a inscrição.');
      return;
    }
    const voluntario = voluntarios.find((item) => item.id === voluntarioId);
    try {
      await atualizarEstado(`/activities/${atividade.id}/registrations`, 'POST', { voluntarioId });
      formularioElemento.reset();
      notificar('sucesso', `${voluntario?.nome || 'Voluntário'} foi inscrito(a) em ${atividade.titulo}.`);
    } catch (error) {
      notificar('erro', error.message);
    }
  };

  const handleCadastrarAtividade = async (evento) => {
    evento.preventDefault();
    if (sessao?.usuario?.tipo !== 'Administrador') {
      notificar('erro', 'Apenas administradores autenticados podem criar atividades.');
      return;
    }
    try {
      const atividade = await atualizarEstado('/activities', 'POST', formAtividade);
      setFormAtividade({ titulo: '', descricao: '', data: '' });
      notificar('sucesso', `Atividade "${atividade.titulo}" cadastrada.`);
    } catch (error) {
      notificar('erro', error.message);
    }
  };

  const animaisFiltrados = animais.filter((pet) => {
    const batEspecie = filtroEspecie === 'Todos' || pet.especie === filtroEspecie;
    const batPorte = filtroPorte === 'Todos' || pet.porte === filtroPorte;
    const batSaude = filtroSaude === 'Todos' || pet.estadoSaude === filtroSaude;
    const batStatus = filtroStatus === 'Todos' || pet.statusAdocao === filtroStatus;
    return batEspecie && batPorte && batSaude && batStatus;
  });

  return (
    <div className="app">
      <header className="header-azul">
        <h1>Abrigo de Animais</h1>
        <p>Sistema de Gerenciamento de Abrigos e Resgates de Animais</p>
        {sessao && (
          <div>
            <span>{sessao.usuario.nome} ({sessao.usuario.tipo})</span>
            <button type="button" onClick={handleLogout}>Sair</button>
          </div>
        )}
      </header>

      <div className="layout-body">
        {sessao && (
          <aside className="sidebar" aria-label="Navegação principal">
            <button onClick={() => setTelaAtual('home')}>Início</button>
            {sessao.usuario.tipo === 'Veterinário' && (
              <button onClick={() => { setAnimalEditando(null); setTelaAtual('cadastrar'); }}>
                {animalEditando ? 'Editar Animal' : 'Cadastrar Animal'}
              </button>
            )}
            <button onClick={() => setTelaAtual('buscar')}>Buscar Animais</button>
            {sessao.usuario.tipo === 'Administrador' && (
              <>
                <button onClick={() => { setUsuarioEditando(null); setTelaAtual('usuarios'); }}>Cadastrar Usuários</button>
                <button onClick={() => setTelaAtual('admin')}>Painel do Administrador</button>
              </>
            )}
            <button onClick={() => setTelaAtual('adocao')}>Solicitar Adoção</button>
            <button onClick={() => setTelaAtual('medico')}>Prontuário Médico</button>
            <button onClick={() => setTelaAtual('voluntario')}>Atividades e Voluntariado</button>
          </aside>
        )}

        <main className="conteudo-principal">
          {carregando && <p role="status">Conectando ao backend...</p>}
          {aviso && (
            <div className={aviso.tipo === 'erro' ? 'aviso-erro' : 'aviso-sucesso'} role="status">
              {aviso.texto}
              <button className="fechar-aviso" onClick={() => setAviso(null)} aria-label="Fechar aviso">×</button>
            </div>
          )}

          {!sessao ? (
            <section>
              <h2>Entrar</h2>
              <p>Entre com o e-mail e a senha da sua conta cadastrada pelo administrador.</p>
              <form onSubmit={handleLogin} className="form-simples">
                <label>E-mail:<input name="email" type="email" required autoComplete="username" /></label>
                <label>Senha:<input name="senha" type="password" required autoComplete="current-password" /></label>
                {erroLogin && <p className="aviso-erro" role="alert">{erroLogin}</p>}
                <button type="submit" disabled={carregando}>{carregando ? 'Entrando...' : 'Entrar'}</button>
              </form>
            </section>
          ) : (
            <>
          {telaAtual === 'home' && (
            <section>
              <h2>Bem-vindo à nossa ONG!</h2>
              <p>Trabalhamos no resgate e reabilitação de animais abandonados.</p>
              
              <div className="estatisticas">
                <div className="card-stat"><h3>{animais.length}</h3><p>Animais cadastrados</p></div>
                <div className="card-stat"><h3>{solicitacoes.filter((item) => item.status === 'APROVADO').length}</h3><p>Adoções realizadas</p></div>
                <div className="card-stat"><h3>{solicitacoes.filter((item) => item.status === 'PENDENTE').length}</h3><p>Pedidos pendentes</p></div>
              </div>

              <h3>Exemplos de Tipos de Animais Atendidos</h3>
              <div className="grid-exemplos">
                {CATEGORIAS_EXEMPLO.map((cat) => (
                  <article key={cat.id} className="card-exemplo">
                    <img src={cat.imagem} alt={cat.tipo} />
                    <h4>{cat.tipo}</h4>
                    <p>{cat.descricao}</p>
                  </article>
                ))}
              </div>
            </section>
          )}

          {telaAtual === 'cadastrar' && (
            <section>
              <h2>{animalEditando ? `Editar Animal (${animalEditando.id})` : 'Cadastrar Novo Animal'}</h2>
              <form onSubmit={handleCadastrarAnimal} className="form-simples">
                <label>Nome do animal:
                  <input type="text" required value={formAnimal.nome} onChange={(e) => setFormAnimal({ ...formAnimal, nome: e.target.value })} />
                </label>
                <label>Espécie:
                  <select value={formAnimal.especie} onChange={(e) => setFormAnimal({ ...formAnimal, especie: e.target.value })}>
                    <option>Cachorro</option><option>Gato</option><option>Galinha</option><option>Cabrito</option><option>Outro</option>
                  </select>
                </label>
                <label>Raça:
                  <input type="text" value={formAnimal.raca} onChange={(e) => setFormAnimal({ ...formAnimal, raca: e.target.value })} />
                </label>
                <label>Idade (anos):
                  <input type="number" min="0" required value={formAnimal.idade} onChange={(e) => setFormAnimal({ ...formAnimal, idade: e.target.value })} />
                </label>
                <label>Porte:
                  <select value={formAnimal.porte} onChange={(e) => setFormAnimal({ ...formAnimal, porte: e.target.value })}>
                    <option>Pequeno</option><option>Médio</option><option>Grande</option>
                  </select>
                </label>
                <label>Estado de saúde:
                  <select disabled={sessao.usuario.tipo !== 'Veterinário'} value={formAnimal.estadoSaude} onChange={(e) => setFormAnimal({ ...formAnimal, estadoSaude: e.target.value })}>
                    <option>Saudável</option><option>Em tratamento</option><option>Crítico</option>
                  </select>
                </label>
                {sessao.usuario.tipo !== 'Veterinário' && (
                  <p>Somente veterinários podem alterar o estado de saúde do animal.</p>
                )}
                <label>Data do resgate:
                  <input type="date" value={formAnimal.dataResgate} onChange={(e) => setFormAnimal({ ...formAnimal, dataResgate: e.target.value })} />
                </label>
                <label>URL da imagem (ex: /fotos/cachorro2.jpeg):
                  <input type="url" placeholder="Opcional" value={formAnimal.imagem} onChange={(e) => setFormAnimal({ ...formAnimal, imagem: e.target.value })} />
                </label>
                <div className="botoes-form">
                  <button type="submit">{animalEditando ? 'Salvar Alterações' : 'Cadastrar animal'}</button>
                  {animalEditando && (
                    <button type="button" className="botao-cancelar" onClick={() => { setAnimalEditando(null); setTelaAtual('buscar'); }}>Cancelar</button>
                  )}
                </div>
              </form>
            </section>
          )}

          {telaAtual === 'buscar' && (
            <section>
              <h2>Buscar Animais</h2>
              <div className="filtros-container">
                <label>Espécie:{' '}
                  <select onChange={(e) => setFiltroEspecie(e.target.value)} value={filtroEspecie}>
                    <option>Todos</option><option>Cachorro</option><option>Gato</option><option>Galinha</option><option>Cabrito</option><option>Outro</option>
                  </select>
                </label>
                <label>Porte:{' '}
                  <select onChange={(e) => setFiltroPorte(e.target.value)} value={filtroPorte}>
                    <option>Todos</option><option>Pequeno</option><option>Médio</option><option>Grande</option>
                  </select>
                </label>
                <label>Saúde:{' '}
                  <select onChange={(e) => setFiltroSaude(e.target.value)} value={filtroSaude}>
                    <option>Todos</option><option>Saudável</option><option>Em tratamento</option><option>Crítico</option>
                  </select>
                </label>
                <label>Status:{' '}
                  <select onChange={(e) => setFiltroStatus(e.target.value)} value={filtroStatus}>
                    <option>Todos</option><option>DISPONIVEL</option><option>INDISPONIVEL</option><option>ADOTADO</option>
                  </select>
                </label>
              </div>

              {animaisFiltrados.length === 0 ? (
                <p>Nenhum animal encontrado com os filtros selecionados.</p>
              ) : (
                <div className="grid-animais">
                  {animaisFiltrados.map((pet) => (
                    <article key={pet.id} className="card-pet">
                      <img src={pet.imagem || '/fotos/Cachorro.jpeg'} alt={pet.nome} />
                      <h4>{pet.nome} <small>({pet.id})</small></h4>
                      <p>{pet.especie} • {pet.raca || 'SRD'} • {pet.idade} anos</p>
                      <p>Porte: {pet.porte}</p>
                      <p>Saúde: <strong>{pet.estadoSaude}</strong> · Status: <strong>{pet.statusAdocao}</strong></p>
                      <div className="acoes-card">
                        <button type="button" onClick={() => handleIniciarEdicaoAnimal(pet)}>Editar</button>
                        {normalizar(pet.statusAdocao) === 'INDISPONIVEL'
                          && normalizar(pet.estadoSaude) !== 'CRITICO'
                          && <button type="button" onClick={() => handleDisponibilizarAnimal(pet.id)}>Disponibilizar para adoção</button>}
                        <button type="button" className="botao-rejeitar" onClick={() => handleExcluirAnimal(pet.id)}>Excluir</button>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          )}

          {telaAtual === 'usuarios' && sessao.usuario.tipo === 'Administrador' && (
            <section>
              <h2>{usuarioEditando ? `Editar Usuário (${usuarioEditando.id})` : 'Cadastro de Usuários'}</h2>
              <p>Cadastre perfis e defina uma senha de acesso. Veterinários precisam informar o CRMV.</p>
              <form onSubmit={handleCadastrarUsuario} className="form-simples">
                <label>Perfil:
                  <select name="tipo" required value={tipoUsuario} onChange={(evento) => setTipoUsuario(evento.target.value)}>
                    <option value="Voluntário">Voluntário</option>
                    <option value="Veterinário">Veterinário</option>
                    <option value="Administrador">Administrador</option>
                  </select>
                </label>
                <label>Nome completo:<input name="nome" type="text" required defaultValue={usuarioEditando?.nome || ''} /></label>
                <label>E-mail:<input name="email" type="email" required defaultValue={usuarioEditando?.email || ''} /></label>
                <label>Telefone:<input name="telefone" type="tel" required defaultValue={usuarioEditando?.telefone || ''} /></label>
                <label>Senha {usuarioEditando ? '(deixe em branco para manter a atual)' : '(mínimo 12 caracteres)'}:
                  <input name="senha" type="password" minLength="12" required={!usuarioEditando} autoComplete="new-password" />
                </label>
                {tipoUsuario === 'Veterinário' && (
                  <label>CRMV (obrigatório para veterinários):
                    <input name="crmv" type="text" required placeholder="CRMV-SP 12345" defaultValue={usuarioEditando?.crmv || ''} />
                  </label>
                )}
                {tipoUsuario === 'Administrador' && (
                  <label>Nível de acesso (administrador):
                    <select name="nivelAcesso" defaultValue={usuarioEditando?.nivelAcesso || 'Básico'}>
                      <option value="Básico">Básico</option>
                      <option value="Gerencial">Gerencial</option>
                      <option value="Total">Total</option>
                    </select>
                  </label>
                )}
                <div className="botoes-form">
                  <button type="submit">{usuarioEditando ? 'Salvar Alterações' : 'Cadastrar usuário'}</button>
                  {usuarioEditando && (
                    <button type="button" className="botao-cancelar" onClick={() => setUsuarioEditando(null)}>Cancelar</button>
                  )}
                </div>
              </form>
              
              <h3 className="titulo-lista">Usuários cadastrados ({usuarios.length})</h3>
              {usuarios.length === 0 ? <p>Nenhum usuário cadastrado ainda.</p> : (
                <div className="tabela-container">
                  <table>
                    <thead><tr><th>Nome</th><th>Perfil</th><th>Contato</th><th>Dados do perfil</th><th>Ações</th></tr></thead>
                    <tbody>{usuarios.map((usuario) => (
                      <tr key={usuario.id}>
                        <td>{usuario.nome}<small>{usuario.id}</small></td>
                        <td>{usuario.tipo}</td>
                        <td>{usuario.email}<small>{usuario.telefone}</small></td>
                        <td>{usuario.crmv || usuario.nivelAcesso || '—'}</td>
                        <td className="acoes-tabela">
                          <button type="button" onClick={() => { setUsuarioEditando(usuario); setTipoUsuario(usuario.tipo); }}>Editar</button>
                          <button type="button" className="botao-rejeitar" onClick={() => handleExcluirUsuario(usuario.id)}>Remover</button>
                        </td>
                      </tr>
                    ))}</tbody>
                  </table>
                </div>
              )}
            </section>
          )}

          {telaAtual === 'adocao' && (
            <section>
              <h2>Solicitar Adoção</h2>
              <p>Animais em estado de saúde crítico não podem receber solicitações de adoção.</p>
              {animaisDisponiveis.length === 0 ? <p className="aviso-erro">Não há animais disponíveis para adoção no momento.</p> : (
                <form onSubmit={handleSolicitarAdocao} className="form-simples">
                  <label>Animal:
                    <select name="animalId" required defaultValue="">
                      <option value="" disabled>Selecione um animal</option>
                      {animaisDisponiveis.map((animal) => <option key={animal.id} value={animal.id}>{animal.nome} ({animal.especie})</option>)}
                    </select>
                  </label>
                  <label>Nome completo:<input name="nome" type="text" required /></label>
                  <label>Telefone/WhatsApp:<input name="contato" type="tel" required /></label>
                  <button type="submit">Enviar solicitação</button>
                </form>
              )}
            </section>
          )}

          {telaAtual === 'admin' && sessao.usuario.tipo === 'Administrador' && (
            <section>
              <h2>Painel do Administrador</h2>
              <p>Analise os pedidos de adoção pendentes. Ao aprovar, o animal passa para o status Adotado.</p>
              <p>Administrador autenticado: {sessao.usuario.nome}</p>
              {solicitacoes.length === 0 ? <p>Nenhuma solicitação de adoção cadastrada.</p> : (
                <div className="tabela-container">
                  <table>
                    <thead><tr><th>Solicitação</th><th>Animal</th><th>Adotante</th><th>Data</th><th>Status</th><th>Ações</th></tr></thead>
                    <tbody>{solicitacoes.map((pedido) => {
                      const animal = animais.find((item) => item.id === pedido.animalId);
                      const animalCritico = animal && normalizar(animal.estadoSaude) === 'CRITICO';
                      return (
                        <tr key={pedido.id}>
                          <td>{pedido.id}</td>
                          <td>{animal ? `${animal.nome} (${animal.id})` : 'Animal não encontrado'}{animalCritico && <small className="texto-erro">Saúde crítica: adoção bloqueada</small>}</td>
                          <td>{pedido.adotanteNome}<small>{pedido.adotanteContato}</small></td>
                          <td>{formatarData(pedido.dataSolicitacao)}</td>
                          <td><span className={`status status-${pedido.status.toLowerCase()}`}>{pedido.status}</span></td>
                          <td className="acoes-tabela">
                            {pedido.status === 'PENDENTE' && (
                              <>
                                <button type="button" disabled={!animal || animalCritico || normalizar(animal.statusAdocao) !== 'DISPONIVEL'} onClick={() => handleAtualizarSolicitacao(pedido, true)}>Aprovar</button>
                                <button type="button" className="botao-rejeitar" onClick={() => handleAtualizarSolicitacao(pedido, false)}>Rejeitar</button>
                              </>
                            )}
                          </td>
                        </tr>
                      );
                    })}</tbody>
                  </table>
                </div>
              )}
            </section>
          )}

          {telaAtual === 'medico' && (
            <section>
              <h2>Prontuário Médico</h2>
              <p>Registre vacinas, consultas, cirurgias e medicamentos.</p>
              {sessao.usuario.tipo !== 'Veterinário' ? (
                <p>Somente veterinários autenticados podem registrar ou alterar informações médicas.</p>
              ) : animais.length === 0 ? <p>Cadastre um animal antes de registrar atendimento.</p> : (
                <form onSubmit={handleRegistrarAtendimento} className="form-simples">
                  <label>Animal:
                    <select name="animalId" required>{animais.map((animal) => <option key={animal.id} value={animal.id}>{animal.nome} ({animal.id})</option>)}</select>
                  </label>
                  <p>Veterinário autenticado: {sessao.usuario.nome}</p>
                  <label>Tipo de atendimento:
                    <select name="tipo" required><option value="VACINA">VACINA</option><option value="CONSULTA">CONSULTA</option><option value="CIRURGIA">CIRURGIA</option><option value="MEDICAMENTO">MEDICAMENTO</option></select>
                  </label>
                  <label>Descrição:<input name="descricao" type="text" required placeholder="Ex.: Vacina V10" /></label>
                  <label>Data:<input name="data" type="date" required defaultValue={dataFutura(0)} /></label>
                  <button type="submit">Salvar atendimento</button>
                </form>
              )}

              <h3 className="titulo-lista">Histórico Médico Individual</h3>
              <div className="filtros-container">
                <label>Filtrar Histórico por Animal:{' '}
                  <select value={animalProntuarioFiltro} onChange={(e) => setAnimalProntuarioFiltro(e.target.value)}>
                    <option value="">Todos os Animais</option>
                    {animais.map((a) => <option key={a.id} value={a.id}>{a.nome} ({a.id})</option>)}
                  </select>
                </label>
              </div>

              {registrosMedicos.length === 0 ? (
                <p>Nenhum atendimento médico registrado até o momento.</p>
              ) : (
                <div className="tabela-container">
                  <table>
                    <thead><tr><th>Data</th><th>Animal</th><th>Tipo</th><th>Descrição</th><th>CRMV Veterinário</th></tr></thead>
                    <tbody>{registrosMedicos
                      .filter((reg) => !animalProntuarioFiltro || reg.animalId === animalProntuarioFiltro)
                      .map((registro) => {
                        const animal = animais.find((item) => item.id === registro.animalId);
                        return (
                          <tr key={registro.id}>
                            <td>{formatarData(registro.data)}</td>
                            <td>{animal ? `${animal.nome} (${animal.id})` : registro.animalId}</td>
                            <td>{registro.tipo}</td>
                            <td>{registro.descricao}</td>
                            <td>{registro.veterinarioCrmv}</td>
                          </tr>
                        );
                      })}</tbody>
                  </table>
                </div>
              )}
            </section>
          )}

          {telaAtual === 'voluntario' && (
            <section>
              <h2>Atividades e Voluntariado</h2>
              <p>Confira os mutirões e campanhas abertos e inscreva os voluntários cadastrados.</p>
              {sessao.usuario.tipo === 'Administrador' && (
                <>
                <h3>Cadastrar atividade</h3>
                <form onSubmit={handleCadastrarAtividade} className="form-simples">
                  <label>Título:<input type="text" required value={formAtividade.titulo} onChange={(evento) => setFormAtividade({ ...formAtividade, titulo: evento.target.value })} /></label>
                  <label>Descrição:<input type="text" required value={formAtividade.descricao} onChange={(evento) => setFormAtividade({ ...formAtividade, descricao: evento.target.value })} /></label>
                  <label>Data:<input type="date" required value={formAtividade.data} onChange={(evento) => setFormAtividade({ ...formAtividade, data: evento.target.value })} /></label>
                  <button type="submit">Cadastrar atividade</button>
                </form>
                </>
              )}
              {sessao.usuario.tipo === 'Administrador' && voluntarios.length === 0 && (
                <p className="dica-voluntario">Ainda não há voluntários cadastrados. <button type="button" className="link-botao" onClick={() => { setTipoUsuario('Voluntário'); setTelaAtual('usuarios'); }}>Cadastrar voluntário</button></p>
              )}
              <div className="lista-atividades">
                {atividades.filter((atividade) => atividade.aberta).map((atividade) => {
                  const inscritos = inscricoes.filter((item) => item.atividadeId === atividade.id);
                  return (
                    <article key={atividade.id} className="card-atividade">
                      <div>
                        <span className="status status-aberta">Inscrições abertas</span>
                        <h3>{atividade.titulo}</h3>
                        <p>{atividade.descricao}</p>
                        <p><strong>Data:</strong> {formatarData(atividade.data)}</p>
                        <p><strong>Inscritos:</strong> {inscritos.length}</p>
                      </div>
                      {voluntarios.length > 0 && (
                        <form className="form-inscricao" onSubmit={(evento) => handleInscricao(evento, atividade)}>
                          <label htmlFor={`voluntario-${atividade.id}`}>Voluntário:</label>
                          <select id={`voluntario-${atividade.id}`} name="voluntarioId" required defaultValue="">
                            <option value="" disabled>Selecione</option>
                            {voluntarios.map((voluntario) => <option key={voluntario.id} value={voluntario.id}>{voluntario.nome}</option>)}
                          </select>
                          <button type="submit">Inscrever-se</button>
                        </form>
                      )}
                    </article>
                  );
                })}
              </div>
            </section>
          )}
            </>
          )}
        </main>
      </div>
    </div>
  );
}