package com.galileo.cu.operaciones.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RestResource;

import com.galileo.cu.commons.models.Objetivos;
import com.galileo.cu.commons.models.Operaciones;

@RestResource(exported = false)
public interface ObjetivosRepository extends CrudRepository<Objetivos, Long> {
	@Query("SELECT o FROM Objetivos o WHERE o.operaciones.Id=:idoperacion")
	public List<Objetivos> listaObjetivos(long idoperacion);

	Boolean existsByOperaciones(Operaciones operaciones);
}
