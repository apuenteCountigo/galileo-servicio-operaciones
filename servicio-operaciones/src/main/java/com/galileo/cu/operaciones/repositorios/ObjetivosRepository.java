package com.galileo.cu.operaciones.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.galileo.cu.commons.models.Objetivos;

public interface ObjetivosRepository extends CrudRepository<Objetivos, Long>{
	@Query("SELECT o FROM Objetivos o WHERE o.operaciones.Id=:idoperacion")
	public List<Objetivos> listaObjetivos(long idoperacion);
}
