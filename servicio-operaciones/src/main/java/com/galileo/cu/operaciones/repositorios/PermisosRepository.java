package com.galileo.cu.operaciones.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.galileo.cu.commons.models.Permisos;

public interface PermisosRepository extends PagingAndSortingRepository<Permisos, Long> {
	@Query("SELECT p FROM Permisos p WHERE p.tipoEntidad.id=6 AND p.idEntidad=:idEntidad")
	List<Permisos> filtar(Long idEntidad);
}
