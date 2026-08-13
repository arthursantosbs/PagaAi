package br.com.pagaai.service;

import br.com.pagaai.domain.Cliente;
import br.com.pagaai.dto.ClienteForm;
import br.com.pagaai.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Cadastro de clientes: listar, buscar, salvar e excluir.
 *
 * <p>O metodo {@code salvar} atende criacao e edicao: o {@code id} nulo no
 * formulario significa "cliente novo". Isso mantem um caminho unico e evita
 * duplicar validacao.
 *
 * <p><b>Se voce mexer aqui:</b> {@code excluir} apaga em cascata as dividas e os
 * pagamentos do cliente (ver {@code Cliente.cobrancas}). E irreversivel e nao ha
 * lixeira. Se um dia o negocio exigir historico, o caminho e marcar
 * {@code ativo = false} em vez de apagar — o campo ja existe.
 */
@Service
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    public List<Cliente> listar(String termo) {
        if (termo == null || termo.isBlank()) {
            return repository.findAllByOrderByNomeAsc();
        }
        return repository.buscar(termo.trim());
    }

    public Cliente buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));
    }

    @Transactional
    public Cliente salvar(ClienteForm form) {
        Cliente cliente = form.getId() == null ? new Cliente() : buscarPorId(form.getId());
        form.aplicarEm(cliente);
        return repository.save(cliente);
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(buscarPorId(id));
    }
}
