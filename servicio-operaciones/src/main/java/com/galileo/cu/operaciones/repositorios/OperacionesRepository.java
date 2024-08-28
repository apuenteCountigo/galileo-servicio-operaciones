package com.galileo.cu.operaciones.repositorios;

import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.RequestContextHolder;

import com.galileo.cu.commons.models.Objetivos;
import com.galileo.cu.commons.models.Operaciones;
import com.galileo.cu.commons.models.dto.ObjetivoResumenDTO;
import com.google.inject.Exposed;

@CrossOrigin
@RepositoryRestResource(collectionResourceRel = "operaciones", path = "operaciones")
public interface OperacionesRepository extends PagingAndSortingRepository<Operaciones, Long> {

	@Query("SELECT o FROM Operaciones o "
			+ "WHERE (:descripcion='' or o.descripcion like %:descripcion%) "
			+ "AND (:idEstado=0 or o.estados=:idEstado) "
			+ " AND ( "
			+ "	(:idAuth IN (SELECT id FROM Usuarios WHERE perfil.id=1)) "
			+ "	OR ( "
			+ "		(:idAuth IN (SELECT id FROM Usuarios WHERE perfil.id=2)) "
			+ "		AND ( "
			+ "				(o.unidades.id=(SELECT unidad.id FROM UnidadesUsuarios WHERE usuario.id=:idAuth AND estado.id=6)) "
			+ "			OR "
			+ "				(o.id IN (SELECT idEntidad FROM Permisos WHERE tipoEntidad.id=6 AND usuarios.id=:idAuth) ) "
			+ "			OR "
			+ "				(o.id IN (SELECT operaciones.id FROM Objetivos WHERE id IN (SELECT idEntidad FROM Permisos WHERE tipoEntidad.id=8 AND usuarios.id=:idAuth)) ) "
			+ "			)"
			+ "		) "
			+ " OR ("
			+ "		(:idAuth IN (SELECT id FROM Usuarios WHERE perfil.id>2)) "
			+ "		AND ("
			+ "			o.id IN (SELECT idEntidad FROM Permisos WHERE tipoEntidad.id=6 AND usuarios.id=:idAuth) "
			+ "			OR "
			+ "			o.id IN ("
			+ "				SELECT operaciones.id FROM Objetivos WHERE id IN "
			+ "					(SELECT idEntidad FROM Permisos WHERE tipoEntidad.id=8 AND usuarios.id=:idAuth)"
			+ "				) "
			+ "			) "
			+ " 	) "
			+ ") "
			+ "AND ((:fechaFin!=null and :fechaInicio!=null and o.fechaCreacion between :fechaInicio and :fechaFin) "
			+ "OR (:fechaFin=null and :fechaInicio!=null and o.fechaCreacion >=:fechaInicio) "
			+ "OR (:fechaFin=null and :fechaInicio=null))  "
			+ "AND (:idunidad=0 or o.unidades.Id = :idunidad) ")
	Page<Operaciones> filtrar(long idAuth, String descripcion, int idunidad, int idEstado,
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin, Pageable p);

	@Query("SELECT o FROM Operaciones o WHERE "
			+ " o.Id IN( Select idEntidad from Permisos where tipoEntidad=6 and (:idUsuario=0 or usuarios.id=:idUsuario)) "
			+ "and (:idOperacion =0 or o.Id=:idOperacion)")
	Page<Operaciones> filtrarPorUsuario(int idUsuario, int idOperacion, Pageable p);

	/*
	 * @Query("SELECT o FROM Operaciones o WHERE "
	 * +"(:perfil in (3,4,0) "
	 * +"and  o.Id IN( Select p.idEntidad from Permisos p where p.tipoEntidad=6 and (:idUsuario=0 or p.usuarios.id=:idUsuario)) "
	 * +"and :idUsuario IN( Select usu from UnidadesUsuarios uu LEFT JOIN uu.usuario usu WHERE uu.unidad.Id=:unidad) "
	 * + "and o.unidades.Id=:unidad "
	 * + "and (:idOperacion =0 or o.Id=:idOperacion)) "
	 * + "or (:perfil=2 "
	 * +"and :idUsuario IN( Select usu from UnidadesUsuarios uu LEFT JOIN uu.usuario usu WHERE uu.unidad.Id=:unidad) "
	 * + "and o.unidades.Id=:unidad "
	 * + ") or (:perfil=1 "
	 * + " )"
	 * +"and o.unidades.Id=:unidad "
	 * + "and (:descripcion='' or o.descripcion like %:descripcion%) "
	 * +
	 * "and ((:fechaFin!=null and :fechaInicio!=null and o.fechaCreacion between :fechaInicio and :fechaFin) "
	 * +
	 * "or (:fechaFin=null and :fechaInicio!=null and o.fechaCreacion >=:fechaInicio) "
	 * + "or (:fechaFin=null and :fechaInicio=null)) "
	 * )
	 * Page<Operaciones> filtro(int perfil,int idOperacion,long unidad,int
	 * idUsuario, String descripcion,@DateTimeFormat(iso =
	 * DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio, @DateTimeFormat(iso
	 * = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,Pageable p);
	 */

	@Query("SELECT o FROM Operaciones o "
			+ "WHERE (:descripcion='' or o.descripcion like %:descripcion%) "
			+ "AND (:idunidad=0 or o.unidades.Id=:idunidad) "
			+ " AND ( "
			+ "	(:idAuth IN (SELECT id FROM Usuarios WHERE perfil.id=1)) "
			+ "	OR ( "
			+ "		(:idAuth IN (SELECT id FROM Usuarios WHERE perfil.id=2)) "
			+ "		AND ( "
			+ "				(o.unidades.id=(SELECT unidad.id FROM UnidadesUsuarios WHERE usuario.id=:idAuth AND estado.id=6)) "
			+ "			OR "
			+ "				(o.id IN (SELECT idEntidad FROM Permisos WHERE tipoEntidad.id=6 AND usuarios.id=:idAuth) ) "
			+ "			)"
			+ "		) "
			+ " OR ("
			+ "		(:idAuth IN (SELECT id FROM Usuarios WHERE perfil.id>2)) "
			+ "		AND ("
			+ "		o.id IN (SELECT idEntidad FROM Permisos WHERE tipoEntidad.id=6 AND usuarios.id=:idAuth) "
			+ "			) "
			+ " 	) "
			+ ") "
			+ "AND ((:fechaFin!=null and :fechaInicio!=null and o.fechaCreacion between :fechaInicio and :fechaFin) "
			+ "OR (:fechaFin=null and :fechaInicio!=null and o.fechaCreacion >=:fechaInicio) "
			+ "OR (:fechaFin=null and :fechaInicio=null)) ")
	public Page<Operaciones> filtro(long idAuth, String descripcion, int idunidad,
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin, Pageable p);

	@Query("SELECT new com.galileo.cu.commons.models.dto.ObjetivoResumenDTO( COUNT(*)) FROM Objetivos o WHERE o.operaciones.id=:idoperaciones "
			+ " AND ( "
			+ "			(:idAuth IN (SELECT up FROM Usuarios up WHERE up.perfil.id=1)) "
			+ "	OR ( "
			+ "		(:idAuth IN (SELECT up FROM Usuarios up WHERE up.perfil.id=2)) "
			+ "		AND (o.operaciones.unidades.id=(SELECT unidad.id FROM UnidadesUsuarios WHERE usuario.id=:idAuth AND estado.id=6)) "
			+ "		) "
			+ " OR ("
			+ "		(:idAuth IN (SELECT up FROM Usuarios up WHERE up.perfil.id>2)) "
			+ "		AND ("
			+ "			o.id IN (SELECT idEntidad FROM Permisos WHERE tipoEntidad.id=8 AND usuarios.id=:idAuth) "
			+ "			OR "
			+ "			o.operaciones.id IN (SELECT p.idEntidad FROM Permisos p WHERE p.tipoEntidad.id=6 AND p.usuarios.id=:idAuth)"
			+ "			) "
			+ " 	) "
			+ ") ")
	public ObjetivoResumenDTO resumen(long idAuth, long idoperaciones);

	@Query("SELECT o FROM Operaciones o WHERE "
			+ "o.unidades.Id=:unidad")
	public List<Operaciones> listaOperaciones(long unidad);
}
