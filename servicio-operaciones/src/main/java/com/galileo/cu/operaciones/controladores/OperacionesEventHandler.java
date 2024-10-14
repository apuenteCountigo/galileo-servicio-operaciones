package com.galileo.cu.operaciones.controladores;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galileo.cu.commons.models.AccionEntidad;
import com.galileo.cu.commons.models.Conexiones;
import com.galileo.cu.commons.models.Operaciones;
import com.galileo.cu.commons.models.TipoEntidad;
import com.galileo.cu.commons.models.Trazas;
import com.galileo.cu.commons.models.Unidades;
import com.galileo.cu.commons.models.Usuarios;
import com.galileo.cu.operaciones.repositorios.PermisosRepository;
import com.galileo.cu.operaciones.repositorios.TrazasRepository;
import com.galileo.cu.operaciones.repositorios.UnidadesRepository;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

import com.galileo.cu.operaciones.cliente.TraccarFeign;
import com.galileo.cu.operaciones.repositorios.ConexionesRepository;
import com.galileo.cu.operaciones.repositorios.ObjetivosRepository;
import com.galileo.cu.operaciones.repositorios.OperacionesRepository;

@Slf4j
@Component
@RepositoryEventHandler(Operaciones.class)
public class OperacionesEventHandler {
	@Autowired
	private TraccarFeign traccar;

	@Autowired
	private OperacionesRepository operacionesrepo;

	@Autowired
	private PermisosRepository permisosRepo;

	@Autowired
	private ObjectMapper objectMapper;

	private HttpServletRequest req;

	@Autowired
	TrazasRepository trazasRepo;

	@Autowired
	UnidadesRepository uniRep;

	@Autowired
	ConexionesRepository conRepo;

	@Autowired
	ObjetivosRepository objRepo;

	@Autowired
	EntityManager entMg;

	public OperacionesEventHandler(HttpServletRequest request) {
		this.req = request;
	}

	@HandleBeforeCreate
	public void handleOperacionesCreate(Operaciones operaciones) {
		/* Validando Autorización */
		ValidateAuthorization val = new ValidateAuthorization();
		try {
			val.setObjectMapper(objectMapper);
			val.setReq(req);
			if (!val.Validate()) {
				log.error("Fallo el Usuario Enviado no Coincide con el Autenticado");
				throw new RuntimeException("Fallo el Usuario Enviado no Coincide con el Autenticado");
			}
		} catch (Exception e) {
			log.error("Fallo Antes de Crear la Operación Validando Autorización: " + e.getMessage());
			throw new RuntimeException("Fallo Antes de Crear la Operación Validando Autorización: ");
		}

		try {
			CrearDirectorios(operaciones);
		} catch (Exception e) {
			log.error("Fallo Creando Directorios para Evidencias");
			log.error(e.getMessage());
			throw new RuntimeException("Fallo Creando Directorios para Evidencias");
		}

		try {
			Operaciones operacionesUpdate = traccar.salvar(operaciones);

			if (operacionesUpdate.getIdGrupo() == null)
				throw new RuntimeException("Fallo, idGrupo es nulo");
			if (operacionesUpdate.getIdDataminer() == null)
				throw new RuntimeException("Fallo, idDataminer es nulo");
			if (operacionesUpdate.getIdElement() == null)
				throw new RuntimeException("Fallo, idElement es nulo");

			operaciones.setIdGrupo(operacionesUpdate.getIdGrupo());
			operaciones.setIdDataminer(operacionesUpdate.getIdDataminer());
			operaciones.setIdElement(operacionesUpdate.getIdElement());
		} catch (Exception e) {
			if (e.getMessage().contains("ya existe una operacion")) {
				log.error("CONTIENE, ya existe una operacion ");
				if (e.getMessage().contains("DATAMINER")) {
					log.error("Fallo Insertando Grupo en DATAMINER " + e.getMessage());
					throw new RuntimeException("Fallo, ya existe una operación con este nombre");
				} else if (e.getMessage().contains("Traccar")) {
					log.error("Fallo Insertando Grupo en Traccar " + e.getMessage());
					throw new RuntimeException("Fallo, ya existe una operación con este nombre");
				}
			} else if (e.getMessage().contains(" es nulo")) {
				log.error(e.getMessage());
				throw new RuntimeException(e.getMessage());
			} else {
				log.error("Fallo Insertando Grupo en apis externas " + e.getMessage());
				throw new RuntimeException("Fallo, Insertando Grupo en apis externas, VER LOGS." + e.getMessage());
			}
		}
	}

	@HandleAfterCreate
	public void handleOperacionesAfterCreate(Operaciones operaciones) {
		/* Validando Autorización */
		ValidateAuthorization val = new ValidateAuthorization();
		try {
			val.setObjectMapper(objectMapper);
			val.setReq(req);
			if (!val.Validate()) {
				log.error("Fallo el Usuario Enviado no Coincide con el Autenticado");
				throw new RuntimeException("Fallo el Usuario Enviado no Coincide con el Autenticado");
			}
		} catch (Exception e) {
			log.error("Fallo Despues de Crear la Operación Validando Autorización: " + e.getMessage());
			throw new RuntimeException("Fallo Despues de Crear la Operación Validando Autorización: ");
		}

		try {
			Trazas traza = new Trazas();
			AccionEntidad accion = new AccionEntidad();
			Usuarios usuario = new Usuarios();
			TipoEntidad entidad = new TipoEntidad();

			entidad.setId(6);
			accion.setId(1);
			usuario.setId(Long.parseLong(val.getJwtObjectMap().getId()));

			traza.setAccionEntidad(accion);
			traza.setTipoEntidad(entidad);
			traza.setUsuario(usuario);
			traza.setIdEntidad(operaciones.getId().intValue());
			traza.setDescripcion("Fue Creada la Operación: " + operaciones.getDescripcion());
			trazasRepo.save(traza);

		} catch (Exception e) {
			log.error("Fallo al Insertar la Creación de la Operación en la Trazabilidad");
			log.error(e.getMessage());
			throw new RuntimeException("Fallo al Insertar la Creación de la Operación en la Trazabilidad");
		}
	}

	@HandleBeforeSave
	public void handleOperacionesBeforeUpdate(Operaciones operaciones) {
		/* Validando Autorización */
		ValidateAuthorization val = new ValidateAuthorization();
		try {
			val.setObjectMapper(objectMapper);
			val.setReq(req);
			if (!val.Validate()) {
				log.error("Fallo el Usuario Enviado no Coincide con el Autenticado");
				throw new RuntimeException("Fallo el Usuario Enviado no Coincide con el Autenticado");
			}
		} catch (Exception e) {
			log.error("Fallo antes de editar la operación validando autorización: " + e.getMessage());
			throw new RuntimeException("Fallo antes de editar la operación validando autorización");
		}

		entMg.detach(operaciones);

		Operaciones opTmp = operacionesrepo.findById(operaciones.getId()).get();

		if (operaciones.getIdGrupo() == null && opTmp.getIdGrupo() != null)
			operaciones.setIdGrupo(opTmp.getIdGrupo());
		else
			operaciones.setIdGrupo((long) 0);

		if (operaciones.getIdDataminer() == null && opTmp.getIdDataminer() != null)
			operaciones.setIdDataminer(opTmp.getIdDataminer());
		else
			operaciones.setIdDataminer("0");

		if (operaciones.getIdElement() == null && opTmp.getIdElement() != null)
			operaciones.setIdElement(opTmp.getIdElement());
		else
			operaciones.setIdElement("0");

		if (operaciones.getIdGrupo() == null || operaciones.getIdDataminer() == null
				|| operaciones.getIdElement() == null) {
			String err = "Fallo, los id relacionados con Dataminer y Traccar deben tener valor";
			log.error(err);
			throw new RuntimeException(err);
		}

	}

	@HandleAfterSave
	public void handleOperacionesAfterUpdate(Operaciones operaciones) {
		/* Validando Autorización */
		ValidateAuthorization val = new ValidateAuthorization();
		try {
			val.setObjectMapper(objectMapper);
			val.setReq(req);
			if (!val.Validate()) {
				log.error("Fallo el Usuario Enviado no Coincide con el Autenticado");
				throw new RuntimeException("Fallo el Usuario Enviado no Coincide con el Autenticado");
			}
		} catch (Exception e) {
			log.error("Fallo antes de editar la operación validando autorización: " + e.getMessage());
			throw new RuntimeException("Fallo antes de editar la operación validando autorización");
		}

		try {
			Trazas traza = new Trazas();
			AccionEntidad accion = new AccionEntidad();
			Usuarios usuario = new Usuarios();
			TipoEntidad entidad = new TipoEntidad();

			entidad.setId(6);
			accion.setId(3);
			usuario.setId(Long.parseLong(val.getJwtObjectMap().getId()));

			traza.setAccionEntidad(accion);
			traza.setTipoEntidad(entidad);
			traza.setUsuario(usuario);
			traza.setIdEntidad(operaciones.getId().intValue());
			traza.setDescripcion("Fue Modificada la Operación: " + operaciones.getDescripcion());
			trazasRepo.save(traza);

		} catch (Exception e) {
			String err = "Fallo al Insertar la modificación de la operación en la Trazabilidad";
			log.error(err, e);
			throw new RuntimeException(err);
		}
	}

	@HandleBeforeDelete
	public void handleOperacionesDelete(Operaciones operaciones) {
		try {
			if (objRepo.existsByOperaciones(operaciones)) {
				log.error("Fallo, la operación tiene objetivos relacionados.");
				throw new RuntimeException("Fallo, la operación tiene objetivos relacionados.");
			}
		} catch (Exception e) {
			if (e.getMessage().contains("Fallo")) {
				log.error("Fallo, la operación tiene objetivos relacionados.");
				throw new RuntimeException(e.getMessage());
			}
			log.error("Fallo buscando relaciones con objetivos.");
			throw new RuntimeException("Fallo buscando relaciones con objetivos.");
		}

		entMg.detach(operaciones);
		try {
			Operaciones opTmp = operacionesrepo.findById(operaciones.getId()).get();

			if (operaciones.getIdGrupo() == null && opTmp.getIdGrupo() != null)
				operaciones.setIdGrupo(opTmp.getIdGrupo());
			else
				operaciones.setIdGrupo((long) 0);

			if (operaciones.getIdDataminer() == null && opTmp.getIdDataminer() != null)
				operaciones.setIdDataminer(opTmp.getIdDataminer());
			else
				operaciones.setIdDataminer("0");

			if (operaciones.getIdElement() == null && opTmp.getIdElement() != null)
				operaciones.setIdElement(opTmp.getIdElement());
			else
				operaciones.setIdElement("0");
		} catch (Exception e) {
			String err = "Fallo, obteniendo ids de apis externas";
			log.error("{}: {}", err, e.getMessage());
			throw new RuntimeException(err);
		}

		try {
			traccar.borrar(operaciones);
		} catch (Exception e) {
			log.error("Fallo eliminando grupo en las apis externas " + e.getMessage());
			throw new RuntimeException("Fallo eliminando grupo en las apis externas");
		}
	}

	@HandleAfterDelete
	public void handleOperacionesAfterDelete(Operaciones operaciones) {
		/* Validando Autorización */
		ValidateAuthorization val = new ValidateAuthorization();
		try {
			val.setObjectMapper(objectMapper);
			val.setReq(req);
			if (!val.Validate()) {
				log.error("Fallo el Usuario Enviado no Coincide con el Autenticado ");
				throw new RuntimeException("Fallo el Usuario Enviado no Coincide con el Autenticado ");
			}
		} catch (Exception e) {
			log.error("Fallo Despues de Eliminar la Operación Validando Autorización: " + e.getMessage());
			throw new RuntimeException(
					"Fallo Despues de Eliminar la Operación Validando Autorización: " + e.getMessage());
		}

		try {
			Trazas traza = new Trazas();
			AccionEntidad accion = new AccionEntidad();
			Usuarios usuario = new Usuarios();
			TipoEntidad entidad = new TipoEntidad();

			entidad.setId(6);
			accion.setId(2);
			usuario.setId(Long.parseLong(val.getJwtObjectMap().getId()));

			traza.setAccionEntidad(accion);
			traza.setTipoEntidad(entidad);
			traza.setUsuario(usuario);
			traza.setIdEntidad(operaciones.getId().intValue());
			traza.setDescripcion("Fue Eliminada la Operación: " + operaciones.getDescripcion());
			trazasRepo.save(traza);

		} catch (Exception e) {
			log.error("Fallo al Insertar la Eliminación de la Operación en la Trazabilidad");
			log.error(e.getMessage());
			throw new RuntimeException("Fallo al Insertar la Eliminación de la Operación en la Trazabilidad");
		}
	}

	public void CrearDirectorios(Operaciones op) throws Exception {
		FTPClient ftp = new FTPClient();
		Conexiones con = new Conexiones();

		try {
			con = conRepo.buscarFtp("FTP").get(0);
		} catch (Exception e) {
			log.error("Fallo al buscar la conexión al ftp en la BD");
			log.error(e.getMessage());
			throw new IOException(
					"Fallo al buscar la conexión al ftp en la BD");
		}

		if (con != null) {
			log.info("Con Servicio: " + con.getServicio());
			try {
				ftp.connect(con.getIpServicio(),
						Integer.parseInt(((!Strings.isNullOrEmpty(con.getPuerto())) ? con.getPuerto() : "21")));
				log.info("FTP con: " + con.getServicio() + " :" + con.getPuerto());
			} catch (Exception e) {
				log.error("Conexión Fallida al servidor FTP " + con.getIpServicio() + ":" + con.getPuerto());
				log.error(e.getMessage());
				throw new IOException(
						"Conexión Fallida al servidor FTP " + con.getIpServicio() + ":" + con.getPuerto());
			}
			int reply = ftp.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				log.error("getReplyCode: Conexión Fallida al servidor FTP " + con.getIpServicio() + ":"
						+ con.getPuerto());
				throw new IOException(
						"Conexión Fallida al servidor FTP " + con.getIpServicio() + ":" + con.getPuerto());
			}

			try {
				ftp.login(con.getUsuario(), con.getPassword());
			} catch (Exception e) {
				Desconectar(ftp);
				log.error("Usuario o Contraseña Incorrecto, al Intentar Autenticarse en Servidor FTP ");
				throw new IOException("Usuario o Contraseña Incorrecto, al Intentar Autenticarse en Servidor FTP "
						+ con.getIpServicio() + ":" + con.getPuerto());
			}

			try {
				Unidades uni = uniRep.findById(op.getUnidades().getId()).get();

				// Listar archivos y directorios en el directorio actual
				/*
				 * FTPFile[] files = ftp.listFiles();
				 * for (FTPFile file : files) {
				 * if (file.isDirectory()) {
				 * System.out.println("Directorio: " + file.getName());
				 * }
				 * }
				 */

				String carpetaUnidad = uni.getDenominacion();
				String carpetaOperacion = op.getDescripcion();
				String fechaI = new SimpleDateFormat("yyyy-MM-dd").format(op.getFechaInicio());
				String fechaF = new SimpleDateFormat("yyyy-MM-dd").format(op.getFechaFin());

				String currentDir = ftp.printWorkingDirectory();
				try {
					currentDir = currentDir + "/UNIDADES";
					currentDir = currentDir.replace("//", "/");
					ftp.mkd(currentDir);
					ftp.mkd(currentDir + "/" + carpetaUnidad);
					ftp.mkd(currentDir + "/" + carpetaUnidad + "/INFORMES "
							+ carpetaOperacion);
					ftp.mkd(currentDir + "/" + carpetaUnidad + "/INFORMES "
							+ carpetaOperacion + "/PERSONALIZADOS");
				} catch (Exception e) {
					Desconectar(ftp);
					log.error("Fallo creando Primera Carpeta " + e.getMessage());
					throw new Exception("Fallo Creando Estructura de Directorios para la Operación");
				}
				try {
					ftp.changeWorkingDirectory(
							currentDir + "/" + carpetaUnidad + "/INFORMES " + carpetaOperacion);
					ftp.mkd("FIRMADOS");
				} catch (Exception e) {
					Desconectar(ftp);
					log.error("Fallo creando segunda Carpeta FIRMADOS" + e.getMessage());
					throw new Exception("Fallo Creando Estructura de Directorios para la Operación");
				}
				try {
					ftp.mkd("ORIGINALES");
				} catch (Exception e) {
					Desconectar(ftp);
					log.error("Fallo creando Carpeta ORIGINALES" + e.getMessage());
					throw new Exception("Fallo Creando Estructura de Directorios para la Operación");
				}
				try {
					ftp.mkd("PENDIENTES DE FIRMA");
				} catch (Exception e) {
					Desconectar(ftp);
					log.error("Fallo creando Carpeta PENDIENTES DE FIRMA " + e.getMessage());
					throw new Exception("Fallo Creando Estructura de Directorios para la Operación");
				}

				Desconectar(ftp);
			} catch (Exception e) {
				log.error("Fallo, Creando Estructura de Directorios para la Operación " + e.getMessage());
				Desconectar(ftp);
				throw new Exception("Fallo Creando Estructura de Directorios para la Operación");
			}
		} else {
			throw new Exception("No existe un servicio FTP, entre las conexiones");
		}
	}

	private void Desconectar(FTPClient ftp) {
		try {
			if (ftp.isConnected()) {
				ftp.logout();
				ftp.disconnect();
			}
		} catch (IOException e) {
			log.error("Fallo Desconectando FTP: " + e.getMessage());
			throw new RuntimeException("Fallo Desconectando FTP");
		}
	}
}
