package com.algaworks.algamoney.dois.api.resource;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.algaworks.algamoney.dois.api.dto.Anexo;
import com.algaworks.algamoney.dois.api.dto.LancamentoEstatisticaCategoriaDTO;
import com.algaworks.algamoney.dois.api.dto.LancamentoEstatisticaDiaDTO;
import com.algaworks.algamoney.dois.api.event.RecursoCriadoEvent;
import com.algaworks.algamoney.dois.api.exceptionhandler.Erro;
import com.algaworks.algamoney.dois.api.model.Lancamento;
import com.algaworks.algamoney.dois.api.repository.LancamentoRepository;
import com.algaworks.algamoney.dois.api.repository.filter.LancamentoFilter;
import com.algaworks.algamoney.dois.api.repository.projetion.ResumoLancamento;
import com.algaworks.algamoney.dois.api.service.LancamentoService;
import com.algaworks.algamoney.dois.api.service.exception.PessoaInexistenteOuInativaException;
import com.algaworks.algamoney.dois.api.storage.S3;


@RestController
@RequestMapping("/lancamentos")
public class LancamentoResource {

	@Autowired
	private LancamentoRepository lancamentoRepository;
	
	@Autowired
	private LancamentoService service;
	
	@Autowired
	private ApplicationEventPublisher publisher;
	
	@Autowired
	private MessageSource messageSource;
	
	@Autowired
	private S3 s3;
	
//	@GetMapping
//	@ResponseStatus(HttpStatus.OK)
//	public List<Lancamento> listar() {
//		return lancamentoRepository.findAll();
//	}
	
	@PreAuthorize("hasAuthority('ROLE_PESQUISAR_LANCAMENTO') and #oauth2.hasScope('read')")
	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public Page<Lancamento> pesquisar(LancamentoFilter filter, Pageable pageable) {
		return lancamentoRepository.pesquisar(filter, pageable);
	}
	
	@PreAuthorize("hasAuthority('ROLE_PESQUISAR_LANCAMENTO') and #oauth2.hasScope('read')")
	@GetMapping(params = "resumo")
	@ResponseStatus(HttpStatus.OK)
	public Page<ResumoLancamento> resumir(LancamentoFilter filter, Pageable pageable) {
		return lancamentoRepository.resumir(filter, pageable);
	}
	
	@PreAuthorize("hasAuthority('ROLE_PESQUISAR_LANCAMENTO') and #oauth2.hasScope('read')")
	@GetMapping("/{codigo}")
	public ResponseEntity<Lancamento> buscaPeloCodigo(@PathVariable Long codigo) {
		Lancamento lancamento = lancamentoRepository.findOne(codigo);		 
		 return lancamento != null ? ResponseEntity.ok(lancamento) : ResponseEntity.noContent().build();
	}
	
	@PreAuthorize("hasAuthority('ROLE_CADASTRAR_LANCAMENTO') and #oauth2.hasScope('write')")
	@PostMapping
	public ResponseEntity<Lancamento>  cria(@Valid @RequestBody Lancamento lancamento, HttpServletResponse response) {
		lancamento = service.salvar(lancamento);
		publisher.publishEvent(new RecursoCriadoEvent(this, response, lancamento.getCodigo()));
		return ResponseEntity.status(HttpStatus.CREATED).body(lancamento);
	}
	
	@PreAuthorize("hasAuthority('ROLE_REMOVER_LANCAMENTO') and #oauth2.hasScope('write')")
	@DeleteMapping("/{codigo}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@PathVariable Long codigo) {
		lancamentoRepository.delete(codigo);
	}
	
	@PutMapping("/{codigo}")
	@PreAuthorize("hasAuthority('ROLE_CADASTRAR_LANCAMENTO')")
	public ResponseEntity<Lancamento> atualizar(@PathVariable Long codigo, @Valid @RequestBody Lancamento lancamento) {
		try {
			Lancamento lancamentoSalvo = service.atualizar(codigo, lancamento);
			return ResponseEntity.ok(lancamentoSalvo);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.notFound().build();
		}
	}
	
	@ExceptionHandler({ PessoaInexistenteOuInativaException.class })
	public ResponseEntity<Object> handlePessoaInexistenteOuInativaException(PessoaInexistenteOuInativaException ex) {
		String mensagemUsuario = messageSource.getMessage("pessoa.inexistente-ou-inativa", null, LocaleContextHolder.getLocale());
		String mensagemDesenvolvedor = ex.toString();
		List<Erro> erros = Arrays.asList(new Erro(mensagemUsuario, mensagemDesenvolvedor));
		return ResponseEntity.badRequest().body(erros);
	}
	
//	@PreAuthorize("hasAuthority('ROLE_PESQUISAR_LANCAMENTO') and #oauth2.hasScope('read')")
//	@GetMapping("/estatisticas/por-categoria")
//	@ResponseStatus(HttpStatus.OK)    
//	public List<LancamentoEstatisticaCategoriaDTO> porCategoria(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate mesReferencia) {
//		return this.lancamentoRepository.porCategoria(mesReferencia);
//	}
	
	@PreAuthorize("hasAuthority('ROLE_PESQUISAR_LANCAMENTO') and #oauth2.hasScope('read')")
	@GetMapping("/estatisticas/por-categoria")
	@ResponseStatus(HttpStatus.OK)    
	public List<LancamentoEstatisticaCategoriaDTO> porCategoria() {
		return this.lancamentoRepository.porCategoria(LocalDate.of(2018, 2, 1));
	}
	
//	@PreAuthorize("hasAuthority('ROLE_PESQUISAR_LANCAMENTO') and #oauth2.hasScope('read')")
//	@GetMapping("/estatisticas/por-dia")
//	@ResponseStatus(HttpStatus.OK)    
//	public List<LancamentoEstatisticaDiaDTO> porDia(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate mesReferencia) {
//		return this.lancamentoRepository.porPorDia(mesReferencia);
//	}
	
	@GetMapping("/estatisticas/por-dia")
	@PreAuthorize("hasAuthority('ROLE_PESQUISAR_LANCAMENTO') and #oauth2.hasScope('read')")
	public List<LancamentoEstatisticaDiaDTO> porDia() {
		return this.lancamentoRepository.porDia(LocalDate.of(2018, 2, 1));
	}
	
	@PreAuthorize("hasAuthority('ROLE_PESQUISAR_LANCAMENTO') and #oauth2.hasScope('read')")
	@GetMapping("/relatorios/por-pessoa")
	public ResponseEntity<byte[]> relatorioPorPessoa(
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate inicio,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fim) throws Exception {

		byte[] relatorio = this.service.relatorioPorPessoa(inicio, fim);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
				.body(relatorio);
	}
	
	@PreAuthorize("hasAuthority('ROLE_CADASTRAR_LANCAMENTO')")
	@PostMapping("/anexo")
	public Anexo uploadAnexo(@RequestParam MultipartFile anexo) throws IOException {
//		OutputStream out = new FileOutputStream("/home/wellington/Documentos/anexo--" + anexo.getOriginalFilename());
//		out.write(anexo.getBytes());
//		out.close();
		
		String nome = s3.salvarTemporariamente(anexo);
		return new Anexo(nome, s3.configurarUrl(nome));
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
