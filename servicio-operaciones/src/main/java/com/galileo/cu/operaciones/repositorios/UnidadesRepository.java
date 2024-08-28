package com.galileo.cu.operaciones.repositorios;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RestResource;

import com.galileo.cu.commons.models.Unidades;

@RestResource(exported = false)
public interface UnidadesRepository extends CrudRepository<Unidades,Long> {
    
}
