package br.com.dbc.vemser.trabalhofinal.service;

import br.com.dbc.vemser.trabalhofinal.dto.UsuarioCreateDTO;
import br.com.dbc.vemser.trabalhofinal.dto.UsuarioDTO;
import br.com.dbc.vemser.trabalhofinal.entity.TipoUsuario;
import br.com.dbc.vemser.trabalhofinal.entity.Usuario;
import br.com.dbc.vemser.trabalhofinal.exceptions.BancoDeDadosException;
import br.com.dbc.vemser.trabalhofinal.exceptions.RegraDeNegocioException;
import br.com.dbc.vemser.trabalhofinal.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;


@RequiredArgsConstructor
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    public UsuarioDTO adicionar(UsuarioCreateDTO usuario) throws RegraDeNegocioException {
        try {
            Usuario usuarioCriado = usuarioRepository.adicionar(
                    validarUsuario(
                            objectMapper.convertValue(usuario, Usuario.class)));
            try {
                emailService.sendEmailUsuario(usuarioCriado, TipoEmail.USUARIO_CADASTRO);
            } catch (MessagingException | TemplateException | IOException e) {
                usuarioRepository.remover(usuarioCriado.getIdUsuario());
                throw new RegraDeNegocioException("Erro ao enviar o e-mail!");
            }
            return objectMapper.convertValue(usuarioCriado, UsuarioDTO.class);
        } catch (BancoDeDadosException e) {
            throw new RegraDeNegocioException("Erro no Banco!");
        }
    }

    public void remover(Integer id) throws RegraDeNegocioException {
        try {
            getUsuario(id);
            usuarioRepository.remover(id);
        } catch (BancoDeDadosException e) {
            throw new RegraDeNegocioException("Erro no Banco!");
        }
    }

    public UsuarioDTO editar(Integer id, UsuarioCreateDTO usuario) throws RegraDeNegocioException {
        try {
            Usuario usarioEditar = objectMapper.convertValue(usuario, Usuario.class);
            UsuarioDTO usuarioEditado = objectMapper.convertValue(usuarioRepository.editar(id, validarUsuario(usarioEditar)), UsuarioDTO.class);
            return usuarioEditado;
        } catch (BancoDeDadosException e) {
            throw new RegraDeNegocioException("Erro no Banco!");
        }
    }


    public List<UsuarioDTO> listar() throws RegraDeNegocioException {
        try {
            return usuarioRepository.listar().stream().map(usuario ->
                    objectMapper.convertValue(usuario, UsuarioDTO.class
            )).toList();
        } catch (BancoDeDadosException e) {
            throw new RegraDeNegocioException("Erro no banco!");
        }
    }

    // Pensando se passamos essa validação pro Banco, afim de ganhar mais performance
    private Usuario validarUsuario(Usuario usuario) throws RegraDeNegocioException {
        try {

            // Estou iterando com 'fori comum' pois não consigo levantar exceções dentro do forEach. Apesar de poder tratálos...
            List<Usuario> usuarios = usuarioRepository.listar();
            for (Usuario value : usuarios) {
                //Quando estivermos atualizando, devemos verifiar se email e cpf já existem em outro usuário ALÉM do que está sendo atualizado.
                if (value.getCpf().equals(usuario.getCpf()) &&
                        !Objects.equals(value.getIdUsuario(), usuario.getIdUsuario())) {
                    throw new RegraDeNegocioException("Já existe usuário com esse CPF!");
                }
                if (value.getEmail().equals(usuario.getEmail()) &&
                        !Objects.equals(value.getIdUsuario(), usuario.getIdUsuario())) {
                    throw new RegraDeNegocioException("Já existe usuário com esse e-mail!");
                }
            }

            for (String contato: usuario.getContatos()) {
                if (contato.length() > 15) {
                    throw new RegraDeNegocioException("Tamanho do contato não pode superar 15");
                }
            }

            return usuario;
        } catch (BancoDeDadosException e) {
            throw new RegraDeNegocioException("Erro no Banco!");
        }
    }


    public Usuario getUsuario(Integer id) throws RegraDeNegocioException {
        try {
            Usuario ususarioEncontrado = usuarioRepository.getUsuario(id);
            if (ususarioEncontrado == null) {
                throw new RegraDeNegocioException("Usuário não encontrado!");
            }
            return ususarioEncontrado;
        } catch (BancoDeDadosException e) {
            throw new RegraDeNegocioException("Erro no Banco!");
        }
    }

    public UsuarioDTO getById(Integer id) throws RegraDeNegocioException {
        return objectMapper.convertValue(getUsuario(id), UsuarioDTO.class);
    }

    // Verifica a disponilidade do id_usuario

    public void verificarIdUsuario(Integer id, TipoUsuario tipoUsuario) throws RegraDeNegocioException {
        try {
            if (!usuarioRepository.verificarSeValido(id, tipoUsuario)) {
                throw new RegraDeNegocioException("O id de usuário informado não é adequado para esta operação!");
            }
        } catch (BancoDeDadosException e) {
            throw new RegraDeNegocioException("Erro no banco!");
        }
    }

}
