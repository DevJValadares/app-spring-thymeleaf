package br.com.appspringthymeleaf.repository;

import java.util.List;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.appspringthymeleaf.model.Pessoa;

@Repository
@Transactional
public interface PessoaRepository extends JpaRepository<Pessoa, Long> {

	@Query("select p from Pessoa p where upper(trim(p.nome)) like %?1%")
	List<Pessoa> findPessoaByName(String nome);

	@Query("select p from Pessoa p where upper(trim(p.nome)) like %?1% and p.sexo = ?2")
	List<Pessoa> findPessoaByNameSexo(String nome, String sexo);

	@Query("select p from Pessoa p where p.sexo = ?1")
	List<Pessoa> findPessoaBySexo(String sexo);

	default Page<Pessoa> findPessoaByNameSexoPage(String nome, String sexo, Pageable pageable) {

		Pessoa pessoa = new Pessoa();
		pessoa.setNome(nome);
		pessoa.setSexo(sexo);

		ExampleMatcher ignoringExampleMatcher = ExampleMatcher.matchingAll()
				.withMatcher("nome", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase()) // USA O LIKE (NOME
																										// LIKE '%ANA%'
				.withMatcher("sexo", ExampleMatcher.GenericPropertyMatchers.exact().ignoreCase()); // USA O =(IGUAL)
																									// SEXO = 'FEMININO'

		Example<Pessoa> example = Example.of(pessoa, ignoringExampleMatcher);

		Page<Pessoa> pessoas = findAll(example, pageable);

		return pessoas;

	}

	default Page<Pessoa> findPessoaByNamePage(String nome, Pageable pageable) {

		Pessoa pessoa = new Pessoa();
		pessoa.setNome(nome);

		ExampleMatcher ignoringExampleMatcher = ExampleMatcher.matchingAny().withMatcher("nome",
				ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase());

		Example<Pessoa> example = Example.of(pessoa, ignoringExampleMatcher);

		Page<Pessoa> pessoas = findAll(example, pageable);

		return pessoas;

	}

	default Page<Pessoa> findPessoaBySexoPage(String sexo, Pageable pageable) {

		Pessoa pessoa = new Pessoa();
		pessoa.setSexo(sexo);

		Example<Pessoa> example = Example.of(pessoa);

		Page<Pessoa> pessoas = findAll(example, pageable);

		return pessoas;

	}

}
