package backend;

import backend.exception.AnimalIndisponivelException;
import backend.exception.VacinaDuplicadaException;
import backend.model.*;
import backend.service.*;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== SISTEMA DE GERENCIAMENTO DE ABRIGOS E RESGATES ===");

        AnimalService animalService = new AnimalService();
        AdocaoService adocaoService = new AdocaoService();
        SaudeService saudeService = new SaudeService();
        AtividadeService atividadeService = new AtividadeService();

        Animal rex = new Animal("A001", "Rex", "Cachorro", "Labrador", 3, "Grande", "Saudável", LocalDate.now().minusDays(10), "DISPONIVEL");
        animalService.cadastrarAnimal(rex);
        System.out.println("Animal cadastrado: " + rex.getNome() + " | Status: " + rex.getStatusAdocao());

        System.out.println("\n--- Testando Controle de Saúde ---");
        LocalDate dataVacina = LocalDate.now();
        
        try {
            saudeService.registrarAtendimento("R001", "A001", "CRMV1234", "Vacina V10", "VACINA", dataVacina);
            System.out.println("1ª Aplicação da vacina registrada com sucesso.");

            saudeService.registrarAtendimento("R002", "A001", "CRMV1234", "Vacina V10", "VACINA", dataVacina);
        } catch (VacinaDuplicadaException e) {
            System.out.println("[EXCEÇÃO CAPTURADA COM SUCESSO]: " + e.getMessage());
        }

        System.out.println("\n--- Testando Processo de Adoção ---");
        try {
            adocaoService.solicitarAdocao("S001", rex, "Maria Silva", "(11) 98888-7777");
            System.out.println("Solicitação de adoção para " + rex.getNome() + " realizada com sucesso.");

            adocaoService.aprovarAdocao("S001");
            System.out.println("Adoção aprovada pelo Administrador! Novo status do animal: " + rex.getStatusAdocao());

            adocaoService.solicitarAdocao("S002", rex, "João Santos", "(11) 99999-1111");

        } catch (AnimalIndisponivelException e) {
            System.out.println("[EXCEÇÃO CAPTURADA COM SUCESSO]: " + e.getMessage());
        }

        System.out.println("\n--- Testando Inscrição de Voluntários ---");
        Voluntario voluntario = new Voluntario("V001", "Carlos Lima", "carlos@email.com", "(11) 97777-6666");
        Atividade mutirao = new Atividade("AT001", "Mutirão de Limpeza", "Limpeza dos recintos", LocalDate.now().plusDays(5));
        
        atividadeService.cadastrarAtividade(mutirao);
        atividadeService.inscreverVoluntario("AT001", voluntario);
        
        System.out.println("Voluntário " + voluntario.getNome() + " inscrito na atividade: " + voluntario.getAtividadesInscritas().get(0));
        
        System.out.println("\n=== EXECUÇÃO FINALIZADA COM SUCESSO ===");
    }
}