package com.galileo.cu.operaciones.interceptores;

import com.galileo.cu.commons.models.Operaciones;
import com.galileo.cu.commons.models.dto.JwtObjectMap;
import com.galileo.cu.operaciones.cliente.TraccarFeign;
import com.galileo.cu.operaciones.dto.FtpDTO;
import com.galileo.cu.operaciones.repositorios.OperacionesRepository;
import com.galileo.cu.operaciones.servicios.FtpService;

import java.io.IOException;
import java.util.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OperacionesInterceptor implements HandlerInterceptor {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private OperacionesRepository oper;

	@Autowired
	private TraccarFeign apis;

	@Autowired
	private FtpService ftpService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws IOException {

		if ("GET".equals(request.getMethod())) {
			if (isFiltroUri(request)) {
				return handleAuthentication(request, response);
			}
		}

		return true;
	}

	private boolean isFiltroUri(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return "/operaciones/search/filtro".equals(uri) ||
				"/operaciones/search/filtrar".equals(uri) ||
				"/operaciones/search/filtrarPorUsuario".equals(uri);
	}

	private boolean handleAuthentication(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String authorizationHeader = request.getHeader("Authorization");
		if (!Strings.isNullOrEmpty(authorizationHeader)) {
			String token = authorizationHeader.replace("Bearer ", "");

			try {
				JwtObjectMap jwtObjectMap = decodeJwt(token);
				if (isAuthorizedUser(jwtObjectMap, request)) {
					return true;
				} else {
					log.error("Fallo, el usuario no coincide con el autenticado");
					response = sendErrorResponse(response, "Fallo, el usuario no coincide con el autenticado");
					return false;
				}
			} catch (Exception e) {
				log.error("Fallo en Interceptor de Seguridad Servicio-Operaciones", e);
				response = sendErrorResponse(response, "Fallo en Interceptor de Seguridad Servicio-Operaciones");
				return false;
			}
		} else {
			log.error("Fallo, no existe token");
			response = sendErrorResponse(response, "Fallo, no existe token");
			return false;
		}
	}

	private JwtObjectMap decodeJwt(String token) throws IOException {
		String[] chunks = token.split("\\.");
		Base64.Decoder decoder = Base64.getUrlDecoder();
		String payload = new String(decoder.decode(chunks[1]));
		return objectMapper.readValue(payload.replace("Perfil", "perfil"), JwtObjectMap.class);
	}

	private boolean isAuthorizedUser(JwtObjectMap jwtObjectMap, HttpServletRequest request) {
		String descripcion = jwtObjectMap.getPerfil().getDescripcion();
		return ("Usuario Final".equals(descripcion) || "Invitado Externo".equals(descripcion)) &&
				jwtObjectMap.getId().equals(request.getParameter("idAuth"));
	}

	private HttpServletResponse sendErrorResponse(HttpServletResponse res, String msg) throws IOException {
		res.reset();
		res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		res.setHeader("Content-Type", "application/json;charset=UTF-8");
		String jsonResponse = "{\"errorMessage\":\"" + msg + "\"}";
		res.getOutputStream().write(jsonResponse.getBytes("UTF-8"));
		res.flushBuffer();
		return res;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		Operaciones operaciones = (Operaciones) request.getAttribute("operaciones");
		boolean handleBeforeCreate = request.getAttribute("handleBeforeCreate") != null
				? (boolean) request.getAttribute("handleBeforeCreate")
				: false;
		boolean handleBD = request.getAttribute("handleBD") != null
				? (boolean) request.getAttribute("handleBD")
				: false;
		String operationPath = request.getAttribute("operationPath") != null
				? request.getAttribute("operationPath").toString()
				: "";

		if (handleBeforeCreate && handleBD) {
			executeRollback(operaciones, operationPath);
		} else if (!handleBD && !Strings.isNullOrEmpty(operationPath)) {
			removeDirectories(operationPath,
					"Fallo intentando eliminar estructura de directorios, ejecutando rollback por fallo en apis externas.");
		}

		if (ex != null) {
			log.error("**********afterCompletion Detectando errores en el servicio: {}", ex.getMessage());
		}
	}

	private void executeRollback(Operaciones operaciones, String operationPath) {
		try {
			log.info("Ejecutando rollback operaciones, por fallo en BD.");
			apis.borrar(operaciones);
			log.info("Rollback operaciones ejecutado correctamente.");
		} catch (Exception e) {
			String err = "Fallo intentando eliminar la operación en las apis externas, debido a fallo en la BD.";
			log.error(err, e);
		}

		if (!Strings.isNullOrEmpty(operationPath)) {
			removeDirectories(operationPath,
					"Fallo intentando eliminar estructura de directorios, ejecutando rollback por fallo en BD.");
		}
	}

	private void removeDirectories(String operationPath, String errorMsg) {
		try {
			FtpDTO ftpDto = ftpService.connectFTP(null);
			// boolean removed = ftpDto.ftp.removeDirectory(operationPath);
			// if (removed) {
			// log.info("El directorio {} fue eliminado satisfactoriamente.",
			// operationPath);
			// }
		} catch (Exception e) {
			log.error(errorMsg, e.getMessage());
		}
	}
}