package com.galileo.cu.operaciones.controladores;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
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

	public OperacionesEventHandler(HttpServletRequest request) {
		this.req = request;
	}

	@HandleBeforeCreate
	public void handleOperacionesCreate(Operaciones operaciones) {
		this.req.setAttribute("operaciones", operaciones);

		/* Validando Autorización */
		ValidateAuthorization val = new ValidateAuthorization();
		try {
			log.info("REQUEST HandleBeforeCreate: " + req.getMethod());
			val.setObjectMapper(objectMapper);
			val.setReq(req);
			if (!val.Validate()) {
				throw new RuntimeException("Fallo el Usuario Enviado no Coincide con el Autenticado ");
			}
		} catch (Exception e) {
			log.info("Fallo Antes de Crear la Operación Validando Autorización: " + e.getMessage());
			throw new RuntimeException("Fallo Antes de Crear la Operación Validando Autorización: ");
		}

		try {
			CrearDirectorios(operaciones);
		} catch (Exception e) {
			log.info("Fallo Creando Directorios para Evidencias");
			log.info(e.getMessage());
			if (e.getMessage().contains("Fallo")) {
				throw new RuntimeException(e.getMessage());
			} else {
				throw new RuntimeException("Fallo Creando Directorios para Evidencias");
			}
		}

		try {
			Operaciones operacionesUpdate = traccar.salvar(operaciones);
			operaciones.setIdGrupo(operacionesUpdate.getIdGrupo());
			operaciones.setIdDataminer(operacionesUpdate.getIdDataminer());
			operaciones.setIdElement(operacionesUpdate.getIdElement());
			log.info("**** servidor=" + operaciones.getServidor().getServicio());
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
			} else if (e.getMessage().contains(" es nulo") ||
					e.getMessage().contains("Fallo")) {
				log.error(e.getMessage());
				throw new RuntimeException(e.getMessage());
			} else {
				String err = "Fallo creando operación en apis externas, VER LOGS.";
				log.error(err, e.getMessage());
				throw new RuntimeException(err);
			}
		}
	}

	@HandleAfterCreate
	public void handleOperacionesAfterCreate(Operaciones operaciones) {
		/* Validando Autorización */
		ValidateAuthorization val = new ValidateAuthorization();
		try {
			log.info("REQUEST HandleAfterCreate: " + req.getMethod());
			val.setObjectMapper(objectMapper);
			val.setReq(req);
			if (!val.Validate()) {
				throw new RuntimeException("Fallo el Usuario Enviado no Coincide con el Autenticado ");
			}
		} catch (Exception e) {
			log.info("Fallo Despues de Crear la Operación Validando Autorización: " + e.getMessage());
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
			log.info("Fallo al Insertar la Creación de la Operación en la Trazabilidad");
			log.info(e.getMessage());
			throw new RuntimeException("Fallo al Insertar la Creación de la Operación en la Trazabilidad");
		}
	}

	@HandleBeforeDelete
	public void handleOperacionesDelete(Operaciones operaciones) {
		try {
			traccar.borrar(operaciones);
		} catch (Exception e) {
			log.info("Fallo Eliminando Grupo en Traccar " + e.getMessage());
			throw new RuntimeException("Fallo Eliminando Grupo en Traccar ");
		}

		long idoperacion = operaciones.getId();
		log.info("id operacion: " + idoperacion);
	}

	@HandleAfterDelete
	public void handleOperacionesAfterDelete(Operaciones operaciones) {
		/* Validando Autorización */
		ValidateAuthorization val = new ValidateAuthorization();
		try {
			log.info("REQUEST HandleAfterDelete: " + req.getMethod());
			val.setObjectMapper(objectMapper);
			val.setReq(req);
			if (!val.Validate()) {
				throw new RuntimeException("Fallo el Usuario Enviado no Coincide con el Autenticado ");
			}
		} catch (Exception e) {
			log.info("Fallo Despues de Eliminar la Operación Validando Autorización: " + e.getMessage());
			throw new RuntimeException(
					"Fallo Despues de Eliminar la Operación Validando Autorización: " + e.getMessage());
		}

		try {
			log.info("Eliminar la Baliza en la Trazabilidad AfterDelete");
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
			log.info("Fallo al Insertar la Eliminación de la Operación en la Trazabilidad");
			log.info(e.getMessage());
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
				log.error(
						"Fallo al intentar conectarse al servidor FTP " + con.getIpServicio() + ":" + con.getPuerto());
				log.error(e.getMessage());
				throw new IOException(
						"Fallo al intentar conectarse al servidor FTP " + con.getIpServicio() + ":" + con.getPuerto());
			}
			int reply = ftp.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				log.error("getReplyCode: Conexión Fallida al servidor FTP " + con.getIpServicio() + ":"
						+ con.getPuerto());
				throw new IOException(
						"Fallo al intentar conectarse al servidor FTP " + con.getIpServicio() + ":" + con.getPuerto());
			}

			log.info("CREDENCIALES: " + con.getUsuario() + " :: " + con.getPassword());
			boolean successLogin = ftp.login(con.getUsuario(), con.getPassword());
			int replyCode = ftp.getReplyCode();
			log.info("replyCode");
			log.info("" + replyCode);
			if (successLogin) {
				log.info("La autenticación fue satizfactoria.");
			} else {
				log.info("Fallo intentando la autenticación con el servidor ftp");
				Desconectar(ftp);
				throw new IOException("Fallo intentando la autenticación con el servidor ftp");
			}

			String baseDir = "/";
			if (con.getRuta() != null && con.getRuta() != "") {
				baseDir = con.getRuta();
			}

			Unidades uni = null;
			try {
				uni = uniRep.findById(op.getUnidades().getId()).get();
			} catch (Exception e) {
				log.error("Fallo obtenidendo la unidad a la que pertenece la operación");
				log.error(e.getMessage());
				throw new IOException("Fallo obtenidendo la unidad a la que pertenece la operación");
			}

			boolean dirExists = ftp.changeWorkingDirectory(baseDir);
			if (!dirExists) {
				log.error("Fallo, la ruta suministrada en la conexión ftp, no es válida");
				baseDir = "/";
				ftp.changeWorkingDirectory(baseDir);
			}

			try {
				String carpetaUnidad = uni.getDenominacion();
				String carpetaOperacion = op.getDescripcion();
				log.info("CREANDO CARPETAS:: " + uni.getDenominacion() + "-" + op.getDescripcion());
				log.info("Carpeta " + carpetaUnidad);
				log.info("Carpeta " + carpetaOperacion);
				String fechaI = new SimpleDateFormat("yyyy-MM-dd").format(op.getFechaInicio());
				String fechaF = new SimpleDateFormat("yyyy-MM-dd").format(op.getFechaFin());

				String unidadesDir = baseDir + "/UNIDADES/";
				unidadesDir = unidadesDir.replace("//", "/");
				try {
					ftp.mkd(unidadesDir);
					ftp.mkd(unidadesDir + carpetaUnidad);
					ftp.mkd(unidadesDir + carpetaUnidad + "/INFORMES "
							+ carpetaOperacion);
					ftp.mkd(unidadesDir + carpetaUnidad + "/INFORMES "
							+ carpetaOperacion + "/PERSONALIZADOS");
					log.info(unidadesDir + carpetaUnidad + "/INFORMES "
							+ carpetaOperacion + "/PERSONALIZADOS");
				} catch (Exception e) {
					Desconectar(ftp);
					log.error("Fallo creando estructura de Directorios " + e.getMessage());
					throw new Exception("Fallo Creando Estructura de Directorios para la Operación");
				}

				try {
					ftp.changeWorkingDirectory(unidadesDir + carpetaUnidad + "/INFORMES " + carpetaOperacion);
					String currentDir = ftp.printWorkingDirectory();
					log.info("Directorio actual: " + currentDir);
					ftp.mkd("FIRMADOS");
				} catch (Exception e) {
					Desconectar(ftp);
					log.info("Fallo creando segunda Carpeta FIRMADOS" + e.getMessage());
					throw new Exception("Fallo Creando Estructura de Directorios para la Operación");
				}
				try {
					ftp.mkd("ORIGINALES");
				} catch (Exception e) {
					Desconectar(ftp);
					log.info("Fallo creando Carpeta ORIGINALES" + e.getMessage());
					throw new Exception("Fallo Creando Estructura de Directorios para la Operación");
				}
				try {
					ftp.mkd("PENDIENTES DE FIRMA");
				} catch (Exception e) {
					Desconectar(ftp);
					log.info("Fallo creando Carpeta PENDIENTES DE FIRMA " + e.getMessage());
					throw new Exception("Fallo Creando Estructura de Directorios para la Operación");
				}

				Desconectar(ftp);
			} catch (Exception e) {
				log.info("Fallo, Creando Estructura de Directorios para la Operación " + e.getMessage());
				Desconectar(ftp);
				throw new Exception("Fallo Creando Estructura de Directorios para la Operación");
			}
		} else {
			throw new Exception("No Existe un Servicio entre las Conexiones, que Contenga la Palabra FTP");
		}
	}

	private void Desconectar(FTPClient ftp) {
		try {
			if (ftp.isConnected()) {
				ftp.logout();
				ftp.disconnect();
			}
		} catch (IOException e) {
			log.info("Fallo Desconectando FTP: " + e.getMessage());
			throw new RuntimeException("Fallo Desconectando FTP");
		}
	}
}
