import React, { useEffect, useState } from 'react';
import './App.css';

const STORAGE_KEY = 'abrigo-animais-dados';

const ANIMAIS_INICIAIS = [
  { id: 'A001', nome: 'Pipoca', especie: 'Cachorro', idade: 3, estadoSaude: 'Saudável', statusAdocao: 'DISPONIVEL', imagem: '/fotos/Cachorro.jpeg' },
  { id: 'A002', nome: 'Mia', especie: 'Gato', idade: 2, estadoSaude: 'Saudável', statusAdocao: 'DISPONIVEL', imagem: '/fotos/Gato.jpeg' },
  { id: 'A003', nome: 'Penosa', especie: 'Galinha', idade: 1, estadoSaude: 'Saudável', statusAdocao: 'DISPONIVEL', imagem: '/fotos/Galinha.jpeg' },
  { id: 'A004', nome: 'Vicente', especie: 'Cabrito', idade: 2, estadoSaude: 'Saudável', statusAdocao: 'DISPONIVEL', imagem: '/fotos/Cabrito.jpeg' },
];

const dataFutura = (dias) => {
  const data = new Date();
  data.setDate(data.getDate() + dias);
  return `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}-${String(data.getDate()).padStart(2, '0')}`;
};

const DADOS_INICIAIS = {
  animais: ANIMAIS_INICIAIS,
  usuarios: [],
  solicitacoes: [],
  registrosMedicos: [],
  atividades: [
    { id: 'AT001', titulo: 'Mutirão de limpeza', descricao: 'Ajude a organizar e higienizar os recintos.', data: dataFutura(5), aberta: true },
    { id: 'AT002', titulo: 'Campanha de adoção', descricao: 'Apoie a equipe em um evento de adoção responsável.', data: dataFutura(12), aberta: true },
  ],
  inscricoes: [],
};

const lerDadosSalvos = () => {
  try {
    const salvo = localStorage.getItem(STORAGE_KEY);
    if (!salvo) return DADOS_INICIAIS;

    const dados = JSON.parse(salvo);
    return Object.fromEntries(
      Object.entries(DADOS_INICIAIS).map(([chave, inicial]) => [
        chave,
        Array.isArray(dados[chave]) ? dados[chave] : inicial,
      ]),
    );
  } catch (error) {
    console.error('Não foi possível carregar os dados salvos do abrigo.', error);
    return DADOS_INICIAIS;
  }
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
  const [filtroEspecie, setFiltroEspecie] = useState('Todos');
  const [tipoUsuario, setTipoUsuario] = useState('Voluntário');
  const [dados, setDados] = useState(lerDadosSalvos);
  const [formAnimal, setFormAnimal] = useState({
    nome: '', especie: 'Cachorro', raca: '', idade: '', porte: 'Pequeno',
    estadoSaude: 'Saudável', dataResgate: '', imagem: '',
  });

  const { animais, usuarios, solicitacoes, registrosMedicos, atividades, inscricoes } = dados;

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(dados));
    } catch (error) {
      console.error('Não foi possível salvar os dados do abrigo no navegador.', error);
      setAviso({ tipo: 'erro', texto: 'Não foi possível salvar os dados no navegador. Verifique o espaço disponível e as permissões de armazenamento.' });
    }
  }, [dados]);

  const notificar = (tipo, texto) => setAviso({ tipo, texto });
  const atualizarDados = (alteracoes) => setDados((atuais) => ({ ...atuais, ...alteracoes }));
  const animaisDisponiveis = animais.filter((animal) =>
    normalizar(animal.statusAdocao) === 'DISPONIVEL'
    && normalizar(animal.estadoSaude) !== 'CRITICO');
  const voluntarios = usuarios.filter((usuario) => usuario.tipo === 'Voluntário');

  const handleCadastrarAnimal = (evento) => {
    evento.preventDefault();
    if (Number(formAnimal.idade) < 0) {
      notificar('erro', 'A idade do animal não pode ser negativa.');
      return;
    }

    const novoId = `A${String(animais.length + 1).padStart(3, '0')}`;
    const novoAnimal = {
      ...formAnimal,
      id: novoId,
      idade: Number(formAnimal.idade),
      statusAdocao: 'DISPONIVEL',
      imagem: formAnimal.imagem || '/fotos/Cachorro.jpeg',
    };
    atualizarDados({ animais: [...animais, novoAnimal] });
    notificar('sucesso', `Animal ${novoAnimal.nome} cadastrado com sucesso! ID: ${novoId}`);
    setFormAnimal({
      nome: '', especie: 'Cachorro', raca: '', idade: '', porte: 'Pequeno',
      estadoSaude: 'Saudável', dataResgate: '', imagem: '',
    });
  };

  const handleCadastrarUsuario = (evento) => {
    evento.preventDefault();
    const formulario = new FormData(evento.currentTarget);
    const tipo = formulario.get('tipo');
    const nome = String(formulario.get('nome')).trim();
    const prefixos = { Voluntário: 'V', Veterinário: 'VT', Administrador: 'ADM' };
    const id = `${prefixos[tipo]}${String(usuarios.filter((usuario) => usuario.tipo === tipo).length + 1).padStart(3, '0')}`;
    const novoUsuario = {
      id,
      tipo,
      nome,
      email: String(formulario.get('email')).trim(),
      telefone: String(formulario.get('telefone')).trim(),
      ...(tipo === 'Veterinário' ? { crmv: String(formulario.get('crmv')).trim() } : {}),
      ...(tipo === 'Administrador' ? { nivelAcesso: formulario.get('nivelAcesso') } : {}),
    };

    atualizarDados({ usuarios: [...usuarios, novoUsuario] });
    evento.currentTarget.reset();
    notificar('sucesso', `${tipo} ${nome} cadastrado(a) com sucesso! ID: ${id}`);
  };

  const handleSolicitarAdocao = (evento) => {
    evento.preventDefault();
    const formulario = new FormData(evento.currentTarget);
    const animalId = String(formulario.get('animalId'));
    const animal = animais.find((item) => item.id === animalId);

    if (!animal || normalizar(animal.statusAdocao) !== 'DISPONIVEL' || normalizar(animal.estadoSaude) === 'CRITICO') {
      notificar('erro', 'Este animal não está disponível para adoção.');
      return;
    }

    const id = `S${String(solicitacoes.length + 1).padStart(3, '0')}`;
    const pedido = {
      id,
      animalId,
      adotanteNome: String(formulario.get('nome')).trim(),
      adotanteContato: String(formulario.get('contato')).trim(),
      dataSolicitacao: dataFutura(0),
      status: 'PENDENTE',
    };
    atualizarDados({ solicitacoes: [...solicitacoes, pedido] });
    evento.currentTarget.reset();
    notificar('sucesso', `Solicitação ${id} registrada para ${animal.nome}. A equipe do abrigo fará a análise.`);
  };

  const handleAtualizarSolicitacao = (pedido, aprovar) => {
    const animal = animais.find((item) => item.id === pedido.animalId);
    if (!animal) {
      notificar('erro', 'Não foi possível localizar o animal vinculado à solicitação.');
      return;
    }

    if (aprovar && (normalizar(animal.estadoSaude) === 'CRITICO' || normalizar(animal.statusAdocao) !== 'DISPONIVEL')) {
      notificar('erro', 'A adoção não pode ser aprovada: o animal está indisponível ou em estado de saúde crítico.');
      return;
    }

    const status = aprovar ? 'APROVADO' : 'REJEITADO';
    const solicitacoesAtualizadas = solicitacoes.map((item) => {
      if (item.id === pedido.id) return { ...item, status };
      if (aprovar && item.animalId === pedido.animalId && item.status === 'PENDENTE') {
        return { ...item, status: 'REJEITADO' };
      }
      return item;
    });
    const animaisAtualizados = aprovar
      ? animais.map((item) => item.id === pedido.animalId ? { ...item, statusAdocao: 'ADOTADO' } : item)
      : animais;

    atualizarDados({ solicitacoes: solicitacoesAtualizadas, animais: animaisAtualizados });
    notificar('sucesso', aprovar
      ? `Adoção aprovada. ${animal.nome} agora está com status Adotado.`
      : `Solicitação ${pedido.id} rejeitada. O animal continua disponível para adoção.`);
  };

  const handleRegistrarAtendimento = (evento) => {
    evento.preventDefault();
    const formulario = new FormData(evento.currentTarget);
    const animalId = String(formulario.get('animalId'));
    const tipo = String(formulario.get('tipo'));
    const descricao = String(formulario.get('descricao')).trim();
    const data = String(formulario.get('data'));
    const duplicada = tipo === 'VACINA' && registrosMedicos.some((registro) =>
      registro.animalId === animalId
      && registro.tipo === 'VACINA'
      && normalizar(registro.descricao) === normalizar(descricao)
      && registro.data === data);

    if (duplicada) {
      notificar('erro', `Vacina duplicada: já existe um registro de "${descricao}" para este animal nesta data.`);
      return;
    }

    const registro = {
      id: `R${String(registrosMedicos.length + 1).padStart(3, '0')}`,
      animalId,
      veterinarioCrmv: String(formulario.get('crmv')).trim(),
      tipo,
      descricao,
      data,
    };
    atualizarDados({ registrosMedicos: [...registrosMedicos, registro] });
    evento.currentTarget.reset();
    notificar('sucesso', 'Atendimento médico registrado com sucesso.');
  };

  const handleInscricao = (evento, atividade) => {
    evento.preventDefault();
    const formulario = new FormData(evento.currentTarget);
    const voluntarioId = String(formulario.get('voluntarioId'));
    if (!voluntarioId) {
      notificar('erro', 'Cadastre ou selecione um voluntário para realizar a inscrição.');
      return;
    }
    if (inscricoes.some((inscricao) => inscricao.atividadeId === atividade.id && inscricao.voluntarioId === voluntarioId)) {
      notificar('erro', 'Este voluntário já está inscrito nesta atividade.');
      return;
    }

    const voluntario = voluntarios.find((item) => item.id === voluntarioId);
    atualizarDados({ inscricoes: [...inscricoes, { atividadeId: atividade.id, voluntarioId }] });
    notificar('sucesso', `${voluntario.nome} foi inscrito(a) em ${atividade.titulo}.`);
  };

  return (
    <div className="app">
      <header className="header-azul">
        <h1>Abrigo de Animais</h1>
        <p>Sistema de Gerenciamento de Abrigos e Resgates de Animais</p>
      </header>

      <div className="layout-body">
        <aside className="sidebar" aria-label="Navegação principal">
          <button onClick={() => setTelaAtual('home')}>Início</button>
          <button onClick={() => setTelaAtual('cadastrar')}>Cadastrar Animal</button>
          <button onClick={() => setTelaAtual('buscar')}>Buscar Animais</button>
          <button onClick={() => setTelaAtual('usuarios')}>Cadastrar Usuários</button>
          <button onClick={() => setTelaAtual('adocao')}>Solicitar Adoção</button>
          <button onClick={() => setTelaAtual('admin')}>Painel do Administrador</button>
          <button onClick={() => setTelaAtual('medico')}>Atendimento Médico</button>
          <button onClick={() => setTelaAtual('voluntario')}>Atividades e Voluntariado</button>
        </aside>

        <main className="conteudo-principal">
          {aviso && (
            <div className={aviso.tipo === 'erro' ? 'aviso-erro' : 'aviso-sucesso'} role="status">
              {aviso.texto}
              <button className="fechar-aviso" onClick={() => setAviso(null)} aria-label="Fechar aviso">×</button>
            </div>
          )}

          {telaAtual === 'home' && (
            <section>
              <h2>Bem-vindo à nossa ONG!</h2>
              <p>Trabalhamos no resgate e reabilitação de animais abandonados.</p>
              <div className="estatisticas">
                <div className="card-stat"><h3>{animais.length}</h3><p>Animais cadastrados</p></div>
                <div className="card-stat"><h3>{solicitacoes.filter((item) => item.status === 'APROVADO').length}</h3><p>Adoções realizadas</p></div>
                <div className="card-stat"><h3>{solicitacoes.filter((item) => item.status === 'PENDENTE').length}</h3><p>Pedidos pendentes</p></div>
              </div>
              <h3>Animais do abrigo</h3>
              <div className="grid-animais">
                {animais.map((pet) => (
                  <article key={pet.id} className="card-pet">
                    <img src={pet.imagem} alt={pet.nome} />
                    <h4>{pet.nome}</h4>
                    <p>{pet.especie} • {pet.idade} anos</p>
                    <p>{pet.estadoSaude} · {pet.statusAdocao}</p>
                  </article>
                ))}
              </div>
            </section>
          )}

          {telaAtual === 'cadastrar' && (
            <section>
              <h2>Cadastrar Novo Animal</h2>
              <form onSubmit={handleCadastrarAnimal} className="form-simples">
                <label>Nome do animal:<input type="text" required value={formAnimal.nome} onChange={(e) => setFormAnimal({ ...formAnimal, nome: e.target.value })} /></label>
                <label>Espécie:
                  <select value={formAnimal.especie} onChange={(e) => setFormAnimal({ ...formAnimal, especie: e.target.value })}>
                    <option>Cachorro</option><option>Gato</option><option>Galinha</option><option>Cabrito</option><option>Outro</option>
                  </select>
                </label>
                <label>Raça:<input type="text" value={formAnimal.raca} onChange={(e) => setFormAnimal({ ...formAnimal, raca: e.target.value })} /></label>
                <label>Idade (anos):<input type="number" min="0" required value={formAnimal.idade} onChange={(e) => setFormAnimal({ ...formAnimal, idade: e.target.value })} /></label>
                <label>Porte:
                  <select value={formAnimal.porte} onChange={(e) => setFormAnimal({ ...formAnimal, porte: e.target.value })}>
                    <option>Pequeno</option><option>Médio</option><option>Grande</option>
                  </select>
                </label>
                <label>Estado de saúde:
                  <select value={formAnimal.estadoSaude} onChange={(e) => setFormAnimal({ ...formAnimal, estadoSaude: e.target.value })}>
                    <option>Saudável</option><option>Em tratamento</option><option>Crítico</option>
                  </select>
                </label>
                <label>Data do resgate:<input type="date" value={formAnimal.dataResgate} onChange={(e) => setFormAnimal({ ...formAnimal, dataResgate: e.target.value })} /></label>
                <label>URL da imagem:<input type="url" placeholder="Opcional" value={formAnimal.imagem} onChange={(e) => setFormAnimal({ ...formAnimal, imagem: e.target.value })} /></label>
                <button type="submit">Cadastrar animal</button>
              </form>
            </section>
          )}

          {telaAtual === 'buscar' && (
            <section>
              <h2>Buscar Animais</h2>
              <label>Filtrar por espécie:{' '}
                <select onChange={(e) => setFiltroEspecie(e.target.value)} value={filtroEspecie}>
                  <option>Todos</option><option>Cachorro</option><option>Gato</option><option>Galinha</option><option>Cabrito</option><option>Outro</option>
                </select>
              </label>
              <div className="grid-animais">
                {animais.filter((pet) => filtroEspecie === 'Todos' || pet.especie === filtroEspecie).map((pet) => (
                  <article key={pet.id} className="card-pet">
                    <img src={pet.imagem} alt={pet.nome} />
                    <h4>{pet.nome}</h4>
                    <p>{pet.especie} • {pet.idade} anos</p>
                    <p>{pet.estadoSaude} · {pet.statusAdocao}</p>
                  </article>
                ))}
              </div>
            </section>
          )}

          {telaAtual === 'usuarios' && (
            <section>
              <h2>Cadastro de Usuários</h2>
              <p>Cadastre cada perfil de usuário. Veterinários precisam informar o CRMV e administradores, o nível de acesso.</p>
              <form onSubmit={handleCadastrarUsuario} className="form-simples">
                <label>Perfil:
                  <select name="tipo" required value={tipoUsuario} onChange={(evento) => setTipoUsuario(evento.target.value)}>
                    <option value="Voluntário">Voluntário</option>
                    <option value="Veterinário">Veterinário</option>
                    <option value="Administrador">Administrador</option>
                  </select>
                </label>
                <label>Nome completo:<input name="nome" type="text" required /></label>
                <label>E-mail:<input name="email" type="email" required /></label>
                <label>Telefone:<input name="telefone" type="tel" required /></label>
                <label>CRMV (obrigatório para veterinários):<input name="crmv" type="text" required={tipoUsuario === 'Veterinário'} placeholder="CRMV-SP 12345" /></label>
                <label>Nível de acesso (administrador):
                  <select name="nivelAcesso"><option value="Básico">Básico</option><option value="Gerencial">Gerencial</option><option value="Total">Total</option></select>
                </label>
                <button type="submit">Cadastrar usuário</button>
              </form>
              <h3 className="titulo-lista">Usuários cadastrados ({usuarios.length})</h3>
              {usuarios.length === 0 ? <p>Nenhum usuário cadastrado ainda.</p> : (
                <div className="tabela-container">
                  <table>
                    <thead><tr><th>Nome</th><th>Perfil</th><th>Contato</th><th>Dados do perfil</th></tr></thead>
                    <tbody>{usuarios.map((usuario) => (
                      <tr key={usuario.id}>
                        <td>{usuario.nome}<small>{usuario.id}</small></td>
                        <td>{usuario.tipo}</td>
                        <td>{usuario.email}<small>{usuario.telefone}</small></td>
                        <td>{usuario.crmv || usuario.nivelAcesso || '—'}</td>
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

          {telaAtual === 'admin' && (
            <section>
              <h2>Painel do Administrador</h2>
              <p>Analise os pedidos de adoção pendentes. Ao aprovar, o animal passa para o status Adotado.</p>
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
              <p>Registre vacinas, consultas, cirurgias e medicamentos. A mesma vacina não pode ser lançada para o mesmo animal na mesma data.</p>
              {animais.length === 0 ? <p>Cadastre um animal antes de registrar atendimento.</p> : (
                <form onSubmit={handleRegistrarAtendimento} className="form-simples">
                  <label>Animal:
                    <select name="animalId" required>{animais.map((animal) => <option key={animal.id} value={animal.id}>{animal.nome} ({animal.id})</option>)}</select>
                  </label>
                  <label>CRMV do veterinário:<input name="crmv" type="text" required placeholder="CRMV-SP 12345" /></label>
                  <label>Tipo de atendimento:
                    <select name="tipo" required><option value="VACINA">VACINA</option><option value="CONSULTA">CONSULTA</option><option value="CIRURGIA">CIRURGIA</option><option value="MEDICAMENTO">MEDICAMENTO</option></select>
                  </label>
                  <label>Descrição:<input name="descricao" type="text" required placeholder="Ex.: Vacina V10" /></label>
                  <label>Data:<input name="data" type="date" required defaultValue={dataFutura(0)} /></label>
                  <button type="submit">Salvar atendimento</button>
                </form>
              )}
              <h3 className="titulo-lista">Registros médicos ({registrosMedicos.length})</h3>
              {registrosMedicos.length > 0 && (
                <div className="tabela-container">
                  <table>
                    <thead><tr><th>Data</th><th>Animal</th><th>Tipo</th><th>Descrição</th><th>CRMV</th></tr></thead>
                    <tbody>{registrosMedicos.map((registro) => {
                      const animal = animais.find((item) => item.id === registro.animalId);
                      return <tr key={registro.id}><td>{formatarData(registro.data)}</td><td>{animal ? animal.nome : registro.animalId}</td><td>{registro.tipo}</td><td>{registro.descricao}</td><td>{registro.veterinarioCrmv}</td></tr>;
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
              {voluntarios.length === 0 && (
                <p className="dica-voluntario">Ainda não há voluntários cadastrados. <button type="button" className="link-botao" onClick={() => setTelaAtual('usuarios')}>Cadastrar voluntário</button></p>
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
              {atividades.filter((atividade) => atividade.aberta).length === 0 && <p>Não há atividades abertas no momento.</p>}
            </section>
          )}
        </main>
      </div>
    </div>
  );
}
