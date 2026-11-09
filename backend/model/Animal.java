package backend.model;
import java.time.LocalDate;

public class Animal {
    private String id;
    private String nome;
    private String especie;
    private String raca;
    private int idade;
    private String porte;
    private String estadoSaude;
    private LocalDate dataResgate;
    private String statusAdocao;

    public Animal(String id, String nome, String especie, String raca, int idade, String porte, String estadoSaude, LocalDate dataResgate, String statusAdocao) {
        this.id = id;
        this.nome = nome;
        this.especie = especie;
        this.raca = raca;
        setIdade(idade);
        this.porte = porte;
        this.estadoSaude = estadoSaude;
        this.dataResgate = dataResgate;
        this.statusAdocao = statusAdocao;
    }

    public String getId() { 
        return id; 
    }
    public void setId(String id) { 
        this.id = id; 
    }

    public String getNome() { 
        return nome; 
    }
    public void setNome(String nome) { 
        this.nome = nome; 
    }

    public String getEspecie() { 
        return especie; 
    }
    public void setEspecie(String especie) { 
        this.especie = especie; 
    }

    public String getRaca() { 
        return raca; 
    }
    public void setRaca(String raca) { 
        this.raca = raca; 
    }

    public int getIdade() { 
        return idade; 
    }
    public void setIdade(int idade) {
        if (idade < 0) {
            throw new IllegalArgumentException("A idade não pode ser negativa.");
        }
        this.idade = idade;
    }

    public String getPorte() { 
        return porte; 
    }
    public void setPorte(String porte) { 
        this.porte = porte; 
    }

    public String getEstadoSaude() { 
        return estadoSaude; 
    }
    public void setEstadoSaude(String estadoSaude) { 
        this.estadoSaude = estadoSaude; 
    }

    public LocalDate getDataResgate() { 
        return dataResgate; 
    }
    public void setDataResgate(LocalDate dataResgate) { 
        this.dataResgate = dataResgate; 
    }

    public String getStatusAdocao() { 
        return statusAdocao; 
    }
    public void setStatusAdocao(String statusAdocao) { 
        this.statusAdocao = statusAdocao; 
    }

    public boolean estaDisponivelParaAdocao() {
        return "DISPONIVEL".equalsIgnoreCase(statusAdocao)
                && !"CRITICO".equalsIgnoreCase(estadoSaude);
    }
}