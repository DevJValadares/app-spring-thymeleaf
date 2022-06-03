package br.com.appspringthymeleaf.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import br.com.appspringthymeleaf.model.Pessoa;
import br.com.appspringthymeleaf.model.Telefone;
import br.com.appspringthymeleaf.repository.PessoaRepository;
import br.com.appspringthymeleaf.repository.ProfissaoRepository;
import br.com.appspringthymeleaf.repository.TelefoneRepository;

@Controller
public class PessoaController {

	@Autowired
	private PessoaRepository pessoaRepository; // DI - Dependecy Injection
	@Autowired
	private TelefoneRepository telefoneRepository;
	@Autowired
	private ReportUtil reportUtil;
	@Autowired
	private ProfissaoRepository profissaoRepository;

	@GetMapping(value = "/home")
	public ModelAndView home() {

		ModelAndView modelAndView = new ModelAndView("listapessoas");
		// modelAndView.addObject("pessoaobj", new Pessoa());
		return pessoas();
	}

	@GetMapping(value = "/cadastropessoa")
	public ModelAndView cadastroPessoa() {
		ModelAndView modelAndView = new ModelAndView("cadastro/cadastropessoa");
		modelAndView.addObject("profissoes", profissaoRepository.findAll());
		modelAndView.addObject("pessoaobj", new Pessoa());
		return modelAndView;
	}

	@PostMapping(value = "/salvarpessoa", consumes = { "multipart/form-data" })
	public ModelAndView salvar(@Valid Pessoa pessoa, BindingResult bindingResult, final MultipartFile file)
			throws IOException {
		pessoa.setTelefones(telefoneRepository.getTelefones(pessoa.getId()));
		ModelAndView mv = new ModelAndView("cadastro/cadastropessoa");
		if (bindingResult.hasErrors()) {
			ModelAndView modelandView = new ModelAndView("cadastro/cadastropessoa");
			// Iterable<Pessoa> pessoasIt = pessoaRepository.findAll();
			// modelandView.addObject("pessoas", pessoasIt);
			modelandView.addObject("pessoaobj", pessoa); // retorna para a tela de cadastro com as as devidas
															// informações de
			// erros, porém mantendo os dados na tela
			List<String> msg = new ArrayList<String>();
			for (ObjectError objectError : bindingResult.getAllErrors()) {
				msg.add(objectError.getDefaultMessage()); // vem das anotações @NotEmpty e outras
			}

			modelandView.addObject("msg", msg);
			modelandView.addObject("profissoes", profissaoRepository.findAll());
			return modelandView;
		}

		if (file.getSize() > 0) { /* Cadastrando um curriculo */
			pessoa.setCurriculo(file.getBytes());
			pessoa.setTipoFileCurriculo(file.getContentType());
			pessoa.setNomeFileCurriculo(file.getOriginalFilename());
		} else {
			if (pessoa.getId() != null && pessoa.getId() > 0) {// editando

				Pessoa pessoalTemp = pessoaRepository.findById(pessoa.getId()).get();

				pessoa.setCurriculo(pessoalTemp.getCurriculo());
				pessoa.setTipoFileCurriculo(pessoalTemp.getTipoFileCurriculo());
				pessoa.setNomeFileCurriculo(pessoalTemp.getNomeFileCurriculo());
			}
		}

		pessoaRepository.save(pessoa);
		mv.addObject("profissoes", profissaoRepository.findAll());
		mv.addObject("pessoaobj", new Pessoa());

		return mv;
	}

	@GetMapping("**/baixarcurriculo/{idpessoa}")
	public void baixarcurriculo(@PathVariable("idpessoa") Long idpessoa, HttpServletResponse response)
			throws IOException {

		/* Consultar o obejto pessoa no banco de dados */
		Pessoa pessoa = pessoaRepository.findById(idpessoa).get();
		if (pessoa.getCurriculo() != null) {

			/* Setar tamanho da resposta */
			response.setContentLength(pessoa.getCurriculo().length);

			/*
			 * Tipo do arquivo para download ou pode ser generica application/octet-stream
			 */
			response.setContentType(pessoa.getTipoFileCurriculo());

			/* Define o cabeçalho da resposta */
			String headerKey = "Content-Disposition";
			String headerValue = String.format("attachment; filename=\"%s\"", pessoa.getNomeFileCurriculo());
			response.setHeader(headerKey, headerValue);

			/* Finaliza a resposta passando o arquivo */
			response.getOutputStream().write(pessoa.getCurriculo());

		}

	}

	@GetMapping(value = "/listapessoas")
	public ModelAndView pessoas() {
		ModelAndView andView = new ModelAndView("listapessoas");
		// Iterable<Pessoa> pessoasIt = pessoaRepository.findAll();
		andView.addObject("pessoas", pessoaRepository.findAll(PageRequest.of(0, 5, Sort.by("nome"))));
		andView.addObject("profissoes", profissaoRepository.findAll());
		andView.addObject("pessoaobj", new Pessoa());
		return andView;
	}

	@GetMapping("/editarpessoa/{idpessoa}")
	public ModelAndView editar(@PathVariable("idpessoa") Long idpessoa) {

		Optional<Pessoa> pessoa = pessoaRepository.findById(idpessoa);

		ModelAndView modelAndView = new ModelAndView("cadastro/cadastropessoa");
		modelAndView.addObject("pessoaobj", pessoa.get());
		modelAndView.addObject("profissoes", profissaoRepository.findAll());
		return modelAndView;

	}

	@GetMapping("/removerpessoa/{idpessoa}")
	public ModelAndView excluir(@PathVariable("idpessoa") Long idpessoa) {

		pessoaRepository.deleteById(idpessoa);

		ModelAndView modelAndView = new ModelAndView("listapessoas");
		modelAndView.addObject("pessoas", pessoaRepository.findAll(PageRequest.of(0, 5, Sort.by("nome"))));
		modelAndView.addObject("pessoaobj", new Pessoa());
		return modelAndView;

	}

	@GetMapping("**/pesquisarpessoa")
	public void imprimePdf(@RequestParam("nomepesquisa") String nomepesquisa,
			@RequestParam("sexopesquisa") String sexopesquisa, HttpServletRequest request, HttpServletResponse response,
			@PageableDefault(size = 5) Pageable pageable) throws Exception {

		Page<Pessoa> pessoas = retornaPesquisaPessoa(nomepesquisa, sexopesquisa, pageable);

		/* Chame o serviço que faz a geração do relatorio */
		byte[] pdf = reportUtil.gerarRelatorio(pessoas.toList(), "pessoa", request.getServletContext());

		/* Tamanho da resposta */
		response.setContentLength(pdf.length);

		/* Definir na resposta o tipo de arquivo */
		response.setContentType("application/octet-stream");

		/* Definir o cabeçalho da resposta */
		String headerKey = "Content-Disposition";
		String headerValue = String.format("attachment; filename=\"%s\"", "relatorio.pdf");
		response.setHeader(headerKey, headerValue);

		/* Finaliza a resposta pro navegador */
		response.getOutputStream().write(pdf);

	}

	public Page<Pessoa> retornaPesquisaPessoa(String nomepesquisa, String sexopesquisa, Pageable pageable) {
		Page<Pessoa> pessoas = null;// new ArrayList<Pessoa>();

		if ((sexopesquisa != null && !sexopesquisa.isEmpty()) && (nomepesquisa != null && !nomepesquisa.isEmpty())) {
			pessoas = pessoaRepository.findPessoaByNameSexoPage(nomepesquisa.trim().toUpperCase(), sexopesquisa,
					pageable);
		} else if (nomepesquisa != null && !nomepesquisa.isEmpty()) {
			pessoas = pessoaRepository.findPessoaByNamePage(nomepesquisa.trim().toUpperCase(), pageable);
		} else if (sexopesquisa != null && !sexopesquisa.isEmpty()) {
			pessoas = pessoaRepository.findPessoaBySexoPage(sexopesquisa, pageable);
		} else {
			pessoas = pessoaRepository.findAll(PageRequest.of(pageable.getPageNumber(), 5, Sort.by("nome")));
		}
		return pessoas;
	}

	@PostMapping("**/pesquisarpessoa")
	public ModelAndView pesquisar(@RequestParam("nomepesquisa") String nomepesquisa,
			@RequestParam("sexopesquisa") String sexopesquisa,
			@PageableDefault(size = 5, sort = { "nome" }) Pageable pageable) {

		Page<Pessoa> pessoas = retornaPesquisaPessoa(nomepesquisa, sexopesquisa, pageable);
		ModelAndView modelAndView = new ModelAndView("listapessoas");
		modelAndView.addObject("pessoas", pessoas);
		modelAndView.addObject("pessoaobj", new Pessoa());
		modelAndView.addObject("nomepesquisa", nomepesquisa);
		modelAndView.addObject("sexopesquisa", sexopesquisa);
		return modelAndView;
	}

	@GetMapping("**/telefones/{idpessoa}")
	public ModelAndView telefones(@PathVariable("idpessoa") Long idpessoa) {

		Optional<Pessoa> pessoa = pessoaRepository.findById(idpessoa);

		ModelAndView modelAndView = new ModelAndView("cadastro/telefones");
		modelAndView.addObject("pessoaobj", pessoa.get());
		modelAndView.addObject("telefones", telefoneRepository.getTelefones(idpessoa));
		return modelAndView;

	}

	@PostMapping("**/addfonePessoa/{pessoaid}")
	public ModelAndView addFonePessoa(Telefone telefone, @PathVariable("pessoaid") Long pessoaid) {

		Pessoa pessoa = pessoaRepository.findById(pessoaid).get();

		if (telefone != null && telefone.getNumero().isEmpty() || telefone.getTipo().isEmpty()) {

			ModelAndView modelAndView = new ModelAndView("cadastro/telefones");
			modelAndView.addObject("pessoaobj", pessoa);
			modelAndView.addObject("telefones", telefoneRepository.getTelefones(pessoaid));

			List<String> msg = new ArrayList<String>();
			if (telefone.getNumero().isEmpty()) {
				msg.add("Numero deve ser informado");
			}

			if (telefone.getTipo().isEmpty()) {
				msg.add("Tipo deve ser informado");
			}
			modelAndView.addObject("msg", msg);

			return modelAndView;

		}

		ModelAndView modelAndView = new ModelAndView("cadastro/telefones");

		telefone.setPessoa(pessoa);

		telefoneRepository.save(telefone);

		modelAndView.addObject("pessoaobj", pessoa);
		modelAndView.addObject("telefones", telefoneRepository.getTelefones(pessoaid));
		return modelAndView;
	}

	@GetMapping("/removertelefone/{idtelefone}")
	public ModelAndView removertelefone(@PathVariable("idtelefone") Long idtelefone) {

		Pessoa pessoa = telefoneRepository.findById(idtelefone).get().getPessoa();

		telefoneRepository.deleteById(idtelefone);

		ModelAndView modelAndView = new ModelAndView("cadastro/telefones");
		modelAndView.addObject("pessoaobj", pessoa);
		modelAndView.addObject("telefones", telefoneRepository.getTelefones(pessoa.getId()));
		return modelAndView;

	}

	@GetMapping("/pessoaspag")
	public ModelAndView carregaPessoaPorPaginacao(@PageableDefault(size = 5) Pageable pageable, ModelAndView model,
			@RequestParam("nomepesquisa") String nomepesquisa, @RequestParam("sexopesquisa") String sexopesquisa) {
		Page<Pessoa> pagePessoa = retornaPesquisaPessoa(nomepesquisa, sexopesquisa, pageable);// pessoaRepository.findPessoaByNamePage(nomepesquisa,
																								// pageable);
		model.addObject("pessoas", pagePessoa);
		model.addObject("pessoaobj", new Pessoa());
		model.addObject("nomepesquisa", nomepesquisa);
		model.addObject("sexopesquisa", sexopesquisa);
		model.setViewName("listapessoas");
		return model;
	}

}
