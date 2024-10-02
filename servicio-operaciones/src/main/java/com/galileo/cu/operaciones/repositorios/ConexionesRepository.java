package com.galileo.cu.operaciones.repositorios;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RestResource;

import com.galileo.cu.commons.models.Conexiones;

@RestResource(exported = false)
public interface ConexionesRepository extends CrudRepository<Conexiones, Long> {
    Conexiones findFirstByServicioContaining(String servicio);
}
