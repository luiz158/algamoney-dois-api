package com.algaworks.algamoney.dois.api.resource;

import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.algaworks.algamoney.dois.api.event.RecursoCriadoEvent;
import com.algaworks.algamoney.dois.api.model.Pessoa;
import com.algaworks.algamoney.dois.api.repository.PessoaRepository;



@RestController
@RequestMapping("/pessoas")
public class PessoaResource {

	@Autowired
	private PessoaRepository pessoaRepository;
	
	@Autowired
	private ApplicationEventPublisher publisher;
	
	@PostMapping
	public ResponseEntity<Pessoa> criar(@Valid @RequestBody Pessoa pessoa, HttpServletResponse response) {
		Pessoa pessoaSalva = pessoaRepository.save(pessoa);
			
			publisher.publishEvent(new RecursoCriadoEvent(this, response, pessoaSalva.getCodigo()));			
			return  ResponseEntity.status(HttpStatus.CREATED).body(pessoaSalva);
	}
	
	@GetMapping("/{codigo}")
	public ResponseEntity<Pessoa> buscarPeloCodigo(@PathVariable Long codigo) {
		Pessoa pessoa = pessoaRepository.findOne(codigo);
		 return pessoa != null ? ResponseEntity.ok(pessoa) : ResponseEntity.notFound().build();
	}
	
	@DeleteMapping("/{codigo}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remover(@PathVariable Long codigo) {
		pessoaRepository.delete(codigo);
	}

	@GetMapping
	public List<Pessoa> listar() {
		return pessoaRepository.findAll();
	}
}