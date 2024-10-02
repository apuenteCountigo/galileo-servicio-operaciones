package com.galileo.cu.operaciones.controladores;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.tomcat.jni.Error;
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
import com.galileo.cu.operaciones.servicios.FtpService;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

import com.galileo.cu.operaciones.cliente.TraccarFeign;
import com.galileo.cu.operaciones.dto.FtpDTO;
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

	@Autowired
	FtpService ftpService;

	public OperacionesEventHandler(HttpServletRequest request) {
		this.req = request;
	}

	@HandleBeforeCreate
	public void handleOperacionesCreate(Operaciones operaciones) {
		this.req.setAttribute("operaciones", operaciones);
		this.req.setAttribute("handleBeforeCreate", false);
		this.req.setAttribute("handleBD", false);
		this.req.setAttribute("handleAfterCreate", false);
		this.req.setAttribute("operationPath", "");

		String operationPath = "";

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
			operationPath = CrearDirectorios(operaciones);
		} catch (Exception e) {
			log.info("Fallo Creando Directorios para Evidencias");
			log.info(e.getMessage());
			if (e.getMessage().contains("Fallo")) {
				throw new RuntimeException(e.getMessage());
			} else {
				throw new RuntimeException("Fallo Creando Directorios para Evidencias");
			}
		}

		if (!Strings.isNullOrEmpty(operationPath))
			this.req.setAttribute("operationPath", operationPath);

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
			} else if (e.getMessage().contains("se ha excedido la cantidad de elementos en el DataMiner")) {
				String err = "Fallo, se ha excedido la cantidad de elementos en el servidor DMA.";
				log.error(err, e.getMessage());
				throw new RuntimeException(err);
			} else if (e.getMessage().contains(" es nulo") ||
					e.getMessage().contains("Fallo")) {
				String err = "Fallo, creando operación, en las apis externas. VER LOGS.";
				log.error(err, e.getMessage());
				throw new RuntimeException(err);
			} else {
				String err = "Fallo creando operación en apis externas, VER LOGS.";
				log.error(err, e.getMessage());
				throw new RuntimeException(err);
			}
		}

		this.req.setAttribute("handleBeforeCreate", true);

		if (!Strings.isNullOrEmpty(operaciones.getDescripcion()) && operaciones.getDescripcion().equals("BDFail"))
			operaciones.setDescripcion("OPEVI");
	}

	@HandleAfterCreate
	public void handleOperacionesAfterCreate(Operaciones operaciones) {
		this.req.setAttribute("handleBD", true);
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
		this.req.setAttribute("handleAfterCreate", true);
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

	public String CrearDirectorios(Operaciones op) throws Exception {
		FtpDTO ftpDto = ftpService.connectFTP();
		String operationPath = "";

		if (ftpDto.getFtp() != null && ftpDto.getFtp().isConnected()) {
			String baseDir = "/";
			if (Strings.isNullOrEmpty(ftpDto.getRuta())) {
				baseDir = ftpDto.getRuta();
			}

			Unidades uni = null;
			try {
				uni = uniRep.findById(op.getUnidades().getId()).get();
			} catch (Exception e) {
				log.error("Fallo obteniendo la unidad a la que pertenece la operación");
				log.error(e.getMessage());
				throw new IOException("Fallo obteniendo la unidad a la que pertenece la operación");
			}

			boolean dirExists = ftpDto.getFtp().changeWorkingDirectory(baseDir);
			if (!dirExists) {
				log.error("Fallo, la ruta suministrada en la conexión FTP no es válida");
				baseDir = "/";
				ftpDto.getFtp().changeWorkingDirectory(baseDir);
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
					ftpDto.getFtp().mkd(unidadesDir);
					ftpDto.getFtp().mkd(unidadesDir + carpetaUnidad);
					ftpDto.getFtp().mkd(unidadesDir + carpetaUnidad + "/INFORMES " + carpetaOperacion);

					operationPath = unidadesDir + carpetaUnidad + "/INFORMES " + carpetaOperacion;

					ftpDto.getFtp()
							.mkd(unidadesDir + carpetaUnidad + "/INFORMES " + carpetaOperacion + "/PERSONALIZADOS");
					log.info(unidadesDir + carpetaUnidad + "/INFORMES " + carpetaOperacion + "/PERSONALIZADOS");
				} catch (Exception e) {
					Desconectar(ftpDto.getFtp());
					log.error("Fallo creando estructura de Directorios " + e.getMessage());
					throw new Exception("Fallo Creando Estructura de Directorios para la Operación");
				}

				try {
					ftpDto.getFtp()
							.changeWorkingDirectory(unidadesDir + carpetaUnidad + "/INFORMES " + carpetaOperacion);
					String currentDir = ftpDto.getFtp().printWorkingDirectory();
					log.info("Directorio actual: " + currentDir);
					ftpDto.getFtp().mkd("FIRMADOS");
				} catch (Exception e) {
					Desconectar(ftpDto.getFtp());
					log.info("Fallo creando segunda Carpeta FIRMADOS " + e.getMessage());
					throw new Exception("Fallo Creando Estructura de Directorios para la Operación");
				}

				try {
					ftpDto.getFtp().mkd("ORIGINALES");
				} catch (Exception e) {
					Desconectar(ftpDto.getFtp());
					log.info("Fallo creando Carpeta ORIGINALES " + e.getMessage());
					throw new Exception("Fallo Creando Estructura de Directorios para la Operación");
				}

				try {
					ftpDto.getFtp().mkd("PENDIENTES DE FIRMA");
				} catch (Exception e) {
					Desconectar(ftpDto.getFtp());
					log.info("Fallo creando Carpeta PENDIENTES DE FIRMA " + e.getMessage());
					throw new Exception("Fallo Creando Estructura de Directorios para la Operación");
				}

				Desconectar(ftpDto.getFtp());
				return operationPath;
			} catch (Exception e) {
				log.info("Fallo, Creando Estructura de Directorios para la Operación " + e.getMessage());
				Desconectar(ftpDto.getFtp());
				throw new Exception("Fallo Creando Estructura de Directorios para la Operación");
			}
		} else {
			throw new Exception("No Existe un Servicio entre las Conexiones que Contenga la Palabra FTP");
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
