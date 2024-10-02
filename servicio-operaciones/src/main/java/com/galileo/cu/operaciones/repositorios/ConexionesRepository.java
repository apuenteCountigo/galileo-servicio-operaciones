package com.galileo.cu.operaciones.repositorios;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import com.galileo.cu.commons.models.Conexiones;

@RestResource(exported = false)
public interface ConexionesRepository extends CrudRepository<Conexiones, Long> {
    Conexiones findFirstByServicioLike(String servicio);

    @Query("SELECT c FROM Conexiones c WHERE LOWER(c.servicio) LIKE LOWER(CONCAT('%', :servicio, '%'))")
    Conexiones findFirstByServicioContaining(@Param("servicio") String servicio);
}
