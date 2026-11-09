[1mdiff --git a/backend/Main.java b/backend/Main.java[m
[1mindex dd7b22c..ffd0706 100644[m
[1m--- a/backend/Main.java[m
[1m+++ b/backend/Main.java[m
[36m@@ -1,5 +1,59 @@[m
 package backend;[m
 [m
[32m+[m[32mimport backend.exception.AnimalIndisponivelException;[m
[32m+[m[32mimport backend.exception.VacinaDuplicadaException;[m
[32m+[m[32mimport backend.model.*;[m
[32m+[m[32mimport backend.service.*;[m
[32m+[m[32mimport java.time.LocalDate;[m
[32m+[m
 public class Main {[m
[31m-    [m
[31m-}[m
[32m+[m[32m    public static void main(String[] args) {[m
[32m+[m[32m        System.out.println("=== SISTEMA DE GERENCIAMENTO DE ABRIGOS E RESGATES ===");[m
[32m+[m
[32m+[m[32m        AnimalService animalService = new AnimalService();[m
[32m+[m[32m        AdocaoService adocaoService = new AdocaoService();[m
[32m+[m[32m        SaudeService saudeService = new SaudeService();[m
[32m+[m[32m        AtividadeService atividadeService = new AtividadeService();[m
[32m+[m
[32m+[m[32m        Animal rex = new Animal("A001", "Rex", "Cachorro", "Labrador", 3, "Grande", "Saudável", LocalDate.now().minusDays(10), "DISPONIVEL");[m
[32m+[m[32m        animalService.cadastrarAnimal(rex);[m
[32m+[m[32m        System.out.println("Animal cadastrado: " + rex.getNome() + " | Status: " + rex.getStatusAdocao());[m
[32m+[m
[32m+[m[32m        System.out.println("\n--- Testando Controle de Saúde ---");[m
[32m+[m[32m        LocalDate dataVacina = LocalDate.now();[m
[32m+[m[41m        [m
[32m+[m[32m        try {[m
[32m+[m[32m            saudeService.registrarAtendimento("R001", "A001", "CRMV1234", "Vacina V10", "VACINA", dataVacina);[m
[32m+[m[32m            System.out.println("1ª Aplicação da vacina registrada com sucesso.");[m
[32m+[m
[32m+[m[32m            saudeService.registrarAtendimento("R002", "A001", "CRMV1234", "Vacina V10", "VACINA", dataVacina);[m
[32m+[m[32m        } catch (VacinaDuplicadaException e) {[m
[32m+[m[32m            System.out.println("[EXCEÇÃO CAPTURADA COM SUCESSO]: " + e.getMessage());[m
[32m+[m[32m        }[m
[32m+[m
[32m+[m[32m        System.out.println("\n--- Testando Processo de Adoção ---");[m
[32m+[m[32m        try {[m
[32m+[m[32m            adocaoService.solicitarAdocao("S001", rex, "Maria Silva", "(11) 98888-7777");[m
[32m+[m[32m            System.out.println("Solicitação de adoção para " + rex.getNome() + " realizada com sucesso.");[m
[32m+[m
[32m+[m[32m            adocaoService.aprovarAdocao("S001");[m
[32m+[m[32m            System.out.println("Adoção aprovada pelo Administrador! Novo status do animal: " + rex.getStatusAdocao());[m
[32m+[m
[32m+[m[32m            adocaoService.solicitarAdocao("S002", rex, "João Santos", "(11) 99999-1111");[m
[32m+[m
[32m+[m[32m        } catch (AnimalIndisponivelException e) {[m
[32m+[m[32m            System.out.println("[EXCEÇÃO CAPTURADA COM SUCESSO]: " + e.getMessage());[m
[32m+[m[32m        }[m
[32m+[m
[32m+[m[32m        System.out.println("\n--- Testando Inscrição de Voluntários ---");[m
[32m+[m[32m        Voluntario voluntario = new Voluntario("V001", "Carlos Lima", "carlos@email.com", "(11) 97777-6666");[m
[32m+[m[32m        Atividade mutirao = new Atividade("AT001", "Mutirão de Limpeza", "Limpeza dos recintos", LocalDate.now().plusDays(5));[m
[32m+[m[41m        [m
[32m+[m[32m        atividadeService.cadastrarAtividade(mutirao);[m
[32m+[m[32m        atividadeService.inscreverVoluntario("AT001", voluntario);[m
[32m+[m[41m        [m
[32m+[m[32m        System.out.println("Voluntário " + voluntario.getNome() + " inscrito na atividade: " + voluntario.getAtividadesInscritas().get(0));[m
[32m+[m[41m        [m
[32m+[m[32m        System.out.println("\n=== EXECUÇÃO FINALIZADA COM SUCESSO ===");[m
[32m+[m[32m    }[m
[32m+[m[32m}[m
\ No newline at end of file[m
[1mdiff --git a/backend/exception/AcessoNegadoException.java b/backend/exception/AcessoNegadoException.java[m
[1mindex e69de29..5fd2b26 100644[m
[1m--- a/backend/exception/AcessoNegadoException.java[m
[1m+++ b/backend/exception/AcessoNegadoException.java[m
[36m@@ -0,0 +1,7 @@[m
[32m+[m[32mpackage backend.exception;[m
[32m+[m
[32m+[m[32mpublic class AcessoNegadoException extends RuntimeException {[m
[32m+[m[32m    public AcessoNegadoException(String mensagem) {[m
[32m+[m[32m        super(mensagem);[m
[32m+[m[32m    }[m
[32m+[m[32m}[m
\ No newline at end of file[m
[1mdiff --git a/backend/exception/VacinaDuplicadaException.java b/backend/exception/VacinaDuplicadaException.java[m
[1mindex e69de29..0c9fe46 100644[m
[1m--- a/backend/exception/VacinaDuplicadaException.java[m
[1m+++ b/backend/exception/VacinaDuplicadaException.java[m
[36m@@ -0,0 +1,7 @@[m
[32m+[m[32mpackage backend.exception;[m
[32m+[m
[32m+[m[32mpublic class VacinaDuplicadaException extends RuntimeException {[m
[32m+[m[32m    public VacinaDuplicadaException(String mensagem) {[m
[32m+[m[32m        super(mensagem);[m
[32m+[m[32m    }[m
[32m+[m[32m}[m
\ No newline at end of file[m
[1mdiff --git a/backend/model/Administrador.java b/backend/model/Administrador.java[m
[1mindex ab2592d..a8b9776 100644[m
[1m--- a/backend/model/Administrador.java[m
[1m+++ b/backend/model/Administrador.java[m
[36m@@ -1,5 +1,13 @@[m
 package backend.model;[m
 [m
[31m-public class Administrador {[m
[31m-    [m
[31m-}[m
[32m+[m[32mpublic class Administrador extends Usuario {[m
[32m+[m[32m    private String nivelAcesso;[m
[32m+[m
[32m+[m[32m    public Administrador(String id, String nome, String email, String telefone, String nivelAcesso) {[m
[32m+[m[32m        super(id, nome, email, telefone);[m
[32m+[m[32m        this.nivelAcesso = nivelAcesso;[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public String getNivelAcesso() { return nivelAcesso; }[m
[32m+[m[32m    public void setNivelAcesso(String nivelAcesso) { this.nivelAcesso = nivelAcesso; }[m
[32m+[m[32m}[m
\ No newline at end of file[m
[1mdiff --git a/backend/model/Animal.java b/backend/model/Animal.java[m
[1mindex 2f9ebb5..fb80a57 100644[m
[1m--- a/backend/model/Animal.java[m
[1m+++ b/backend/model/Animal.java[m
[36m@@ -2,6 +2,7 @@[m [mpackage backend.model;[m
 import java.time.LocalDate;[m
 [m
 public class Animal {[m
[32m+[m[32m    private String id;[m
     private String nome;[m
     private String especie;[m
     private String raca;[m
[36m@@ -11,90 +12,86 @@[m [mpublic class Animal {[m
     private LocalDate dataResgate;[m
     private String statusAdocao;[m
 [m
[31m-    public Animal{String nome; String especie; String raca;int idade; String porte;String estadoSaude; LocalDate dataResgate; String statusAdocao}{[m
[31m-        this.nome=nome;[m
[31m-        this.especie=especie;[m
[31m-        this.raca=raca;[m
[31m-        this.idade=idade;[m
[31m-        this.porte=porte;[m
[31m-        this.estadoSaude=estadoSaude;[m
[31m-        this.dataResgate=dataResgate;[m
[31m-        this.statusAdocao=statusAdocao;[m
[31m-    }[m
[31m-[m
[31m-    public String getNome(){[m
[31m-        return nome;[m
[31m-    }[m
[31m-[m
[31m-    public String getEspecie() {[m
[31m-        return especie;[m
[32m+[m[32m    public Animal(String id, String nome, String especie, String raca, int idade, String porte, String estadoSaude, LocalDate dataResgate, String statusAdocao) {[m
[32m+[m[32m        this.id = id;[m
[32m+[m[32m        this.nome = nome;[m
[32m+[m[32m        this.especie = especie;[m
[32m+[m[32m        this.raca = raca;[m
[32m+[m[32m        setIdade(idade);[m
[32m+[m[32m        this.porte = porte;[m
[32m+[m[32m        this.estadoSaude = estadoSaude;[m
[32m+[m[32m        this.dataResgate = dataResgate;[m
[32m+[m[32m        this.statusAdocao = statusAdocao;[m
     }[m
 [m
[31m-    public String getRaca() {[m
[31m-        return raca;[m
[32m+[m[32m    public String getId() {[m[41m [m
[32m+[m[32m        return id;[m[41m [m
     }[m
[31m-[m
[31m-    public int getIdade() {[m
[31m-        return idade;[m
[32m+[m[32m    public void setId(String id) {[m[41m [m
[32m+[m[32m        this.id = id;[m[41m [m
     }[m
 [m
[31m-    public String getPorte() {[m
[31m-        return porte;[m
[32m+[m[32m    public String getNome() {[m[41m [m
[32m+[m[32m        return nome;[m[41m [m
     }[m
[31m-[m
[31m-    public String getEstadoSaude() {[m
[31m-        return estadoSaude;[m
[32m+[m[32m    public void setNome(String nome) {[m[41m [m
[32m+[m[32m        this.nome = nome;[m[41m [m
     }[m
 [m
[31m-    public LocalDate getDataResgate() {[m
[31m-        return dataResgate;[m
[32m+[m[32m    public String getEspecie() {[m[41m [m
[32m+[m[32m        return especie;[m[41m [m
     }[m
[31m-[m
[31m-    public String getStatusAdocao() {[m
[31m-        return statusAdocao;[m
[32m+[m[32m    public void setEspecie(String especie) {[m[41m [m
[32m+[m[32m        this.especie = especie;[m[41m [m
     }[m
 [m
[31m-     public void setNome(String nome) {[m
[31m-        this.nome = nome;[m
[32m+[m[32m    public String getRaca() {[m[41m [m
[32m+[m[32m        return raca;[m[41m [m
     }[m
[31m-[m
[31m-    public void setEspecie(String especie) {[m
[31m-        this.especie = especie;[m
[32m+[m[32m    public void setRaca(String raca) {[m[41m [m
[32m+[m[32m        this.raca = raca;[m[41m [m
     }[m
 [m
[31m-    public void setRaca(String raca) {[m
[31m-        this.raca = raca;[m
[32m+[m[32m    public int getIdade() {[m[41m [m
[32m+[m[32m        return idade;[m[41m [m
     }[m
[31m-[m
     public void setIdade(int idade) {[m
         if (idade < 0) {[m
[31m-            throw new IllegalArgumentException([m
[31m-                "A idade não pode ser negativa."[m
[31m-            );[m
[32m+[m[32m            throw new IllegalArgumentException("A idade não pode ser negativa.");[m
         }[m
         this.idade = idade;[m
     }[m
 [m
[31m-    public void setPorte(String porte) {[m
[31m-        this.porte = porte;[m
[32m+[m[32m    public String getPorte() {[m[41m [m
[32m+[m[32m        return porte;[m[41m [m
     }[m
[31m-[m
[31m-    public void setEstadoSaude(String estadoSaude) {[m
[31m-        this.estadoSaude = estadoSaude;[m
[32m+[m[32m    public void setPorte(String porte) {[m[41m [m
[32m+[m[32m        this.porte = porte;[m[41m [m
     }[m
 [m
[31m-    public void setDataResgate(LocalDate dataResgate) {[m
[31m-        this.dataResgate = dataResgate;[m
[32m+[m[32m    public String getEstadoSaude() {[m[41m [m
[32m+[m[32m        return estadoSaude;[m[41m [m
     }[m
[31m-[m
[31m-    public void setStatusAdocao(String statusAdocao) {[m
[31m-        this.statusAdocao = statusAdocao;[m
[32m+[m[32m    public void setEstadoSaude(String estadoSaude) {[m[41m [m
[32m+[m[32m        this.estadoSaude = estadoSaude;[m[41m [m
     }[m
 [m
[31m-     public boolean estaDisponivelParaAdocao() {[m
[31m-        return "Disponível".equalsIgnoreCase(statusAdocao)[m
[31m-                && !"Crítico".equalsIgnoreCase(estadoSaude);[m
[31m-     }[m
[32m+[m[32m    public LocalDate getDataResgate() {[m[41m [m
[32m+[m[32m        return dataResgate;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public void setDataResgate(LocalDate dataResgate) {[m[41m [m
[32m+[m[32m        this.dataResgate = dataResgate;[m[41m [m
[32m+[m[32m    }[m
 [m
[32m+[m[32m    public String getStatusAdocao() {[m[41m [m
[32m+[m[32m        return statusAdocao;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public void setStatusAdocao(String statusAdocao) {[m[41m [m
[32m+[m[32m        this.statusAdocao = statusAdocao;[m[41m [m
[32m+[m[32m    }[m
 [m
[31m-}[m
[32m+[m[32m    public boolean estaDisponivelParaAdocao() {[m
[32m+[m[32m        return "DISPONIVEL".equalsIgnoreCase(statusAdocao)[m
[32m+[m[32m                && !"CRITICO".equalsIgnoreCase(estadoSaude);[m
[32m+[m[32m    }[m
[32m+[m[32m}[m
\ No newline at end of file[m
[1mdiff --git a/backend/model/Atividade.java b/backend/model/Atividade.java[m
[1mindex e69de29..f0a3a02 100644[m
[1m--- a/backend/model/Atividade.java[m
[1m+++ b/backend/model/Atividade.java[m
[36m@@ -0,0 +1,36 @@[m
[32m+[m[32mpackage backend.model;[m
[32m+[m[32mimport java.time.LocalDate;[m
[32m+[m[32mimport java.util.ArrayList;[m
[32m+[m[32mimport java.util.List;[m
[32m+[m
[32m+[m[32mpublic class Atividade {[m
[32m+[m[32m    private String id;[m
[32m+[m[32m    private String titulo;[m
[32m+[m[32m    private String descricao;[m
[32m+[m[32m    private LocalDate data;[m
[32m+[m[32m    private List<String> voluntariosInscritosId;[m
[32m+[m
[32m+[m[32m    public Atividade(String id, String titulo, String descricao, LocalDate data) {[m
[32m+[m[32m        this.id = id;[m
[32m+[m[32m        this.titulo = titulo;[m
[32m+[m[32m        this.descricao = descricao;[m
[32m+[m[32m        this.data = data;[m
[32m+[m[32m        this.voluntariosInscritosId = new ArrayList<>();[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public String getId() {[m[41m [m
[32m+[m[32m        return id;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public String getTitulo() {[m[41m [m
[32m+[m[32m        return titulo;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public String getDescricao() {[m[41m [m
[32m+[m[32m        return descricao;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public LocalDate getData() {[m[41m [m
[32m+[m[32m        return data;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public List<String> getVoluntariosInscritosId() {[m[41m [m
[32m+[m[32m        return voluntariosInscritosId;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m}[m
\ No newline at end of file[m
[1mdiff --git a/backend/model/RegistroMedico.java b/backend/model/RegistroMedico.java[m
[1mindex e69de29..15682c3 100644[m
[1m--- a/backend/model/RegistroMedico.java[m
[1m+++ b/backend/model/RegistroMedico.java[m
[36m@@ -0,0 +1,39 @@[m
[32m+[m[32mpackage backend.model;[m
[32m+[m[32mimport java.time.LocalDate;[m
[32m+[m
[32m+[m[32mpublic class RegistroMedico {[m
[32m+[m[32m    private String id;[m
[32m+[m[32m    private String animalId;[m
[32m+[m[32m    private String veterinarioCrmv;[m
[32m+[m[32m    private String descricao;[m
[32m+[m[32m    private String tipo;[m[41m [m
[32m+[m[32m    private LocalDate data;[m
[32m+[m
[32m+[m[32m    public RegistroMedico(String id, String animalId, String veterinarioCrmv, String descricao, String tipo, LocalDate data) {[m
[32m+[m[32m        this.id = id;[m
[32m+[m[32m        this.animalId = animalId;[m
[32m+[m[32m        this.veterinarioCrmv = veterinarioCrmv;[m
[32m+[m[32m        this.descricao = descricao;[m
[32m+[m[32m        this.tipo = tipo;[m
[32m+[m[32m        this.data = data;[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public String getId() {[m[41m [m
[32m+[m[32m        return id;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public String getAnimalId() {[m[41m [m
[32m+[m[32m        return animalId;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public String getVeterinarioCrmv() {[m[41m [m
[32m+[m[32m        return veterinarioCrmv;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public String getDescricao() {[m[41m [m
[32m+[m[32m        return descricao;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public String getTipo() {[m[41m [m
[32m+[m[32m        return tipo;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public LocalDate getData() {[m[41m [m
[32m+[m[32m        return data;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m}[m
\ No newline at end of file[m
[1mdiff --git a/backend/model/SolicitacaoAdocao.java b/backend/model/SolicitacaoAdocao.java[m
[1mindex e69de29..064a744 100644[m
[1m--- a/backend/model/SolicitacaoAdocao.java[m
[1m+++ b/backend/model/SolicitacaoAdocao.java[m
[36m@@ -0,0 +1,41 @@[m
[32m+[m[32mpackage backend.model;[m
[32m+[m[32mimport java.time.LocalDate;[m
[32m+[m
[32m+[m[32mpublic class SolicitacaoAdocao {[m
[32m+[m[32m    private String id;[m
[32m+[m[32m    private Animal animal;[m
[32m+[m[32m    private String adotanteNome;[m
[32m+[m[32m    private String adotanteContato;[m
[32m+[m[32m    private LocalDate dataSolicitacao;[m
[32m+[m[32m    private String status;[m[41m [m
[32m+[m
[32m+[m[32m    public SolicitacaoAdocao(String id, Animal animal, String adotanteNome, String adotanteContato) {[m
[32m+[m[32m        this.id = id;[m
[32m+[m[32m        this.animal = animal;[m
[32m+[m[32m        this.adotanteNome = adotanteNome;[m
[32m+[m[32m        this.adotanteContato = adotanteContato;[m
[32m+[m[32m        this.status = "PENDENTE";[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public String getId() {[m[41m [m
[32m+[m[32m        return id;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public Animal getAnimal() {[m[41m [m
[32m+[m[32m        return animal;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public String getAdotanteNome() {[m[41m [m
[32m+[m[32m        return adotanteNome;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public String getAdotanteContato() {[m[41m [m
[32m+[m[32m        return adotanteContato;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public LocalDate getDataSolicitacao() {[m[41m [m
[32m+[m[32m        return dataSolicitacao;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public String getStatus() {[m[41m [m
[32m+[m[32m        return status;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public void setStatus(String status) {[m[41m [m
[32m+[m[32m        this.status = status;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m}[m
\ No newline at end of file[m
[1mdiff --git a/backend/model/Usuario.java b/backend/model/Usuario.java[m
[1mindex 6624732..9a58014 100644[m
[1m--- a/backend/model/Usuario.java[m
[1m+++ b/backend/model/Usuario.java[m
[36m@@ -1,5 +1,43 @@[m
 package backend.model;[m
 [m
[31m-public class Usuario {[m
[31m-    [m
[31m-}[m
[32m+[m[32mpublic abstract class Usuario {[m
[32m+[m[32m    private String id;[m
[32m+[m[32m    private String nome;[m
[32m+[m[32m    private String email;[m
[32m+[m[32m    private String telefone;[m
[32m+[m
[32m+[m[32m    public Usuario(String id, String nome, String email, String telefone) {[m
[32m+[m[32m        this.id = id;[m
[32m+[m[32m        this.nome = nome;[m
[32m+[m[32m        this.email = email;[m
[32m+[m[32m        this.telefone = telefone;[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public String getId() {[m[41m [m
[32m+[m[32m        return id;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public void setId(String id) {[m
[32m+[m[32m        this.id = id;[m[41m [m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public String getNome() {[m[41m [m
[32m+[m[32m        return nome;[m
[32m+[m[32m    }[m
[32m+[m[32m    public void setNome(String nome) {[m[41m [m
[32m+[m[32m        this.nome = nome;[m[41m [m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public String getEmail() {[m[41m [m
[32m+[m[32m        return email;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public void setEmail(String email) {[m[41m [m
[32m+[m[32m        this.email = email;[m[41m [m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public String getTelefone() {[m[41m [m
[32m+[m[32m        return telefone;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m    public void setTelefone(String telefone) {[m[41m [m
[32m+[m[32m        this.telefone = telefone;[m[41m [m
[32m+[m[32m    }[m
[32m+[m[32m}[m
\ No newline at end of file[m
[1mdiff --git a/backend/model/Veterinario.java b/backend/model/Veterinario.java[m
[1mindex 6c454b1..9bcd8a7 100644[m
[1m--- a/backend/model/Veterinario.java[m
[1m+++ b/backend/model/Veterinario.java[m
[36m@@ -1,3 +1,13 @@[m
 package backend.model;[m
 [m
[31m-public class Veterinario{}[m
\ No newline at end of file[m
[32m+[m[32mpublic class Veterinario extends Usuario {[m
[32m+[m[32m    private String crmv;[m
[32m+[m
[32m+[m[32m    public Veterinario(String id, String nome, String email, String telefone, String crmv) {[m
[32m+[m[32m        super(id, nome, email, telefone);[m
[32m+[m[32m        this.crmv = crmv;[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public String getCrmv() { return crmv; }[m
[32m+[m[32m    public void setCrmv(String crmv) { this.crmv = crmv; }[m
[32m+[m[32m}[m
\ No newline at end of file[m
[1mdiff --git a/backend/model/Voluntario.java b/backend/model/Voluntario.java[m
[1mindex 205b6ce..e38b42b 100644[m
[1m--- a/backend/model/Voluntario.java[m
[1m+++ b/backend/model/Voluntario.java[m
[36m@@ -1,5 +1,18 @@[m
 package backend.model;[m
[32m+[m[32mimport java.util.ArrayList;[m
[32m+[m[32mimport java.util.List;[m
 [m
[31m-public class Voluntario {[m
[32m+[m[32mpublic class Voluntario extends Usuario {[m
[32m+[m[41m    [m
[32m+[m[32m    private List<String> atividadesInscritas;[m
[32m+[m[32m    private List<String> historicoParticipacoes;[m
 [m
[32m+[m[32m    public Voluntario(String id, String nome, String email, String telefone) {[m
[32m+[m[32m        super(id, nome, email, telefone);[m
[32m+[m[32m        this.atividadesInscritas = new ArrayList<>();[m
[32m+[m[32m        this.historicoParticipacoes = new ArrayList<>();[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public List<String> getAtividadesInscritas() { return atividadesInscritas; }[m
[32m+[m[32m    public List<String> getHistoricoParticipacoes() { return historicoParticipacoes; }[m
 }[m
\ No newline at end of file[m
[1mdiff --git a/backend/service/AdocaoService.java b/backend/service/AdocaoService.java[m
[1mindex e69de29..d3ba77c 100644[m
[1m--- a/backend/service/AdocaoService.java[m
[1m+++ b/backend/service/AdocaoService.java[m
[36m@@ -0,0 +1,44 @@[m
[32m+[m[32mpackage backend.service;[m
[32m+[m
[32m+[m[32mimport backend.exception.AnimalIndisponivelException;[m
[32m+[m[32mimport backend.model.Animal;[m
[32m+[m[32mimport backend.model.SolicitacaoAdocao;[m
[32m+[m[32mimport java.util.ArrayList;[m
[32m+[m[32mimport java.util.List;[m
[32m+[m
[32m+[m[32mpublic class AdocaoService {[m
[32m+[m[32m    private List<SolicitacaoAdocao> solicitacoes = new ArrayList<>();[m
[32m+[m[32m    private List<SolicitacaoAdocao> historicoAdocoes = new ArrayList<>();[m
[32m+[m
[32m+[m[32m    public void solicitarAdocao(String idSolicitacao, Animal animal, String adotanteNome, String adotanteContato) {[m
[32m+[m[32m        if (!animal.estaDisponivelParaAdocao()) {[m
[32m+[m[32m            throw new AnimalIndisponivelException("O animal " + animal.getNome() + " não está disponível para adoção.");[m
[32m+[m[32m        }[m
[32m+[m
[32m+[m[32m        boolean jaAdotado = solicitacoes.stream()[m
[32m+[m[32m                .anyMatch(s -> s.getAnimal().getId().equals(animal.getId()) && "APROVADO".equalsIgnoreCase(s.getStatus()));[m
[32m+[m[41m        [m
[32m+[m[32m        if (jaAdotado) {[m
[32m+[m[32m            throw new AnimalIndisponivelException("Este animal já foi adotado e não aceita novas solicitações.");[m
[32m+[m[32m        }[m
[32m+[m
[32m+[m[32m        SolicitacaoAdocao novaSolicitacao = new SolicitacaoAdocao(idSolicitacao, animal, adotanteNome, adotanteContato);[m
[32m+[m[32m        solicitacoes.add(novaSolicitacao);[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public void aprovarAdocao(String idSolicitacao) {[m
[32m+[m[32m        SolicitacaoAdocao solicitacao = solicitacoes.stream()[m
[32m+[m[32m                .filter(s -> s.getId().equals(idSolicitacao))[m
[32m+[m[32m                .findFirst()[m
[32m+[m[32m                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada."));[m
[32m+[m
[32m+[m[32m        solicitacao.setStatus("APROVADO");[m
[32m+[m[32m        solicitacao.getAnimal().setStatusAdocao("ADOTADO");[m
[32m+[m[41m        [m
[32m+[m[32m        historicoAdocoes.add(solicitacao);[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public List<SolicitacaoAdocao> getHistoricoAdocoes() {[m
[32m+[m[32m        return historicoAdocoes;[m
[32m+[m[32m    }[m
[32m+[m[32m}[m
[1mdiff --git a/backend/service/AnimalService.java b/backend/service/AnimalService.java[m
[1mnew file mode 100644[m
[1mindex 0000000..9f7abd1[m
[1m--- /dev/null[m
[1m+++ b/backend/service/AnimalService.java[m
[36m@@ -0,0 +1,43 @@[m
[32m+[m[32mpackage backend.service;[m
[32m+[m
[32m+[m[32mimport backend.exception.AnimalIndisponivelException;[m
[32m+[m[32mimport backend.model.Animal;[m
[32m+[m
[32m+[m[32mimport java.util.ArrayList;[m
[32m+[m[32mimport java.util.List;[m
[32m+[m[32mimport java.util.stream.Collectors;[m
[32m+[m
[32m+[m[32mpublic class AnimalService {[m
[32m+[m[32m    private List<Animal> animais = new ArrayList<>();[m
[32m+[m
[32m+[m[32m    public void cadastrarAnimal(Animal animal) {[m
[32m+[m[32m        animais.add(animal);[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public List<Animal> listarTodos() {[m
[32m+[m[32m        return animais;[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public Animal buscarPorId(String id) {[m
[32m+[m[32m        return animais.stream()[m
[32m+[m[32m                .filter(a -> a.getId().equals(id))[m
[32m+[m[32m                .findFirst()[m
[32m+[m[32m                .orElseThrow(() -> new RuntimeException("Animal não encontrado com o ID: " + id));[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public List<Animal> filtrarPorEspecie(String especie) {[m
[32m+[m[32m        return animais.stream()[m
[32m+[m[32m                .filter(a -> a.getEspecie().equalsIgnoreCase(especie))[m
[32m+[m[32m                .collect(Collectors.toList());[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public void disponibilizarParaAdocao(String id) {[m
[32m+[m[32m        Animal animal = buscarPorId(id);[m
[32m+[m[41m        [m
[32m+[m[32m        if ("CRITICO".equalsIgnoreCase(animal.getEstadoSaude())) {[m
[32m+[m[32m            throw new AnimalIndisponivelException("Animais em tratamento crítico não podem ser disponibilizados para adoção.");[m
[32m+[m[32m        }[m
[32m+[m[41m        [m
[32m+[m[32m        animal.setStatusAdocao("DISPONIVEL");[m
[32m+[m[32m    }[m
[32m+[m[32m}[m
\ No newline at end of file[m
[1mdiff --git a/backend/service/AtividadeService.java b/backend/service/AtividadeService.java[m
[1mindex e69de29..5aaba58 100644[m
[1m--- a/backend/service/AtividadeService.java[m
[1m+++ b/backend/service/AtividadeService.java[m
[36m@@ -0,0 +1,30 @@[m
[32m+[m[32mpackage backend.service;[m
[32m+[m
[32m+[m[32mimport backend.model.Atividade;[m
[32m+[m[32mimport backend.model.Voluntario;[m
[32m+[m[32mimport java.util.ArrayList;[m
[32m+[m[32mimport java.util.List;[m
[32m+[m
[32m+[m[32mpublic class AtividadeService {[m
[32m+[m[32m    private List<Atividade> atividades = new ArrayList<>();[m
[32m+[m
[32m+[m[32m    public void cadastrarAtividade(Atividade atividade) {[m
[32m+[m[32m        atividades.add(atividade);[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public void inscreverVoluntario(String atividadeId, Voluntario voluntario) {[m
[32m+[m[32m        Atividade atividade = atividades.stream()[m
[32m+[m[32m                .filter(a -> a.getId().equals(atividadeId))[m
[32m+[m[32m                .findFirst()[m
[32m+[m[32m                .orElseThrow(() -> new RuntimeException("Atividade não encontrada."));[m
[32m+[m
[32m+[m[32m        if (!atividade.getVoluntariosInscritosId().contains(voluntario.getId())) {[m
[32m+[m[32m            atividade.getVoluntariosInscritosId().add(voluntario.getId());[m
[32m+[m[32m            voluntario.getAtividadesInscritas().add(atividade.getTitulo());[m
[32m+[m[32m        }[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public List<Atividade> listarAtividades() {[m
[32m+[m[32m        return atividades;[m
[32m+[m[32m    }[m
[32m+[m[32m}[m
\ No newline at end of file[m
[1mdiff --git a/backend/service/SaudeService.java b/backend/service/SaudeService.java[m
[1mindex e69de29..407d2ba 100644[m
[1m--- a/backend/service/SaudeService.java[m
[1m+++ b/backend/service/SaudeService.java[m
[36m@@ -0,0 +1,36 @@[m
[32m+[m[32mpackage backend.service;[m
[32m+[m
[32m+[m[32mimport backend.exception.VacinaDuplicadaException;[m
[32m+[m[32mimport backend.model.RegistroMedico;[m
[32m+[m[32mimport java.time.LocalDate;[m
[32m+[m[32mimport java.util.ArrayList;[m
[32m+[m[32mimport java.util.List;[m
[32m+[m[32mimport java.util.stream.Collectors;[m
[32m+[m
[32m+[m[32mpublic class SaudeService {[m
[32m+[m[32m    private List<RegistroMedico> registrosMedicos = new ArrayList<>();[m
[32m+[m
[32m+[m[32m    public void registrarAtendimento(String id, String animalId, String veterinarioCrmv, String descricao, String tipo, LocalDate data) {[m
[32m+[m[41m        [m
[32m+[m[32m        if ("VACINA".equalsIgnoreCase(tipo)) {[m
[32m+[m[32m            boolean vacinaDuplicada = registrosMedicos.stream()[m
[32m+[m[32m                    .anyMatch(r -> r.getAnimalId().equals(animalId)[m
[32m+[m[32m                            && "VACINA".equalsIgnoreCase(r.getTipo())[m
[32m+[m[32m                            && r.getDescricao().equalsIgnoreCase(descricao)[m
[32m+[m[32m                            && r.getData().equals(data));[m
[32m+[m
[32m+[m[32m            if (vacinaDuplicada) {[m
[32m+[m[32m                throw new VacinaDuplicadaException("Já existe um registro desta mesma vacina para este animal na data de " + data);[m
[32m+[m[32m            }[m
[32m+[m[32m        }[m
[32m+[m
[32m+[m[32m        RegistroMedico registro = new RegistroMedico(id, animalId, veterinarioCrmv, descricao, tipo, data);[m
[32m+[m[32m        registrosMedicos.add(registro);[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    public List<RegistroMedico> consultarHistoricoAnimal(String animalId) {[m
[32m+[m[32m        return registrosMedicos.stream()[m
[32m+[m[32m                .filter(r -> r.getAnimalId().equals(animalId))[m
[32m+[m[32m                .collect(Collectors.toList());[m
[32m+[m[32m    }[m
[32m+[m[32m}[m
