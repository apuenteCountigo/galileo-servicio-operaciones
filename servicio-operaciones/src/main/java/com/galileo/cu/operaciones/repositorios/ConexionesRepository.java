package com.galileo.cu.operaciones.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RestResource;

import com.galileo.cu.commons.models.Conexiones;

@RestResource(exported = false)
public interface ConexionesRepository extends CrudRepository<Conexiones,Long> {
    @Query("SELECT c FROM Conexiones c WHERE c.servicio=:servicio")
    public List<Conexiones> buscarFtp(String servicio);
}
