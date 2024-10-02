package com.galileo.cu.operaciones.interceptores;

import com.galileo.cu.commons.models.Operaciones;
import com.galileo.cu.commons.models.dto.JwtObjectMap;
import com.galileo.cu.commons.models.dto.OriginCascading;
import com.galileo.cu.operaciones.cliente.TraccarFeign;
import com.galileo.cu.operaciones.dto.FtpDTO;
import com.galileo.cu.operaciones.repositorios.OperacionesRepository;
import com.galileo.cu.operaciones.servicios.FtpService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Set;

import javax.persistence.Convert;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.PushBuilder;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

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
			throws ServletException, IOException {

		// OriginCascading originCascading= new OriginCascading();
		/*
		 * Enumeration<String> names = request.getHeaderNames(); while
		 * (names.hasMoreElements()) System.out.println(names.nextElement());
		 */
		if (request.getMethod().equals("GET")) {
			if (request.getRequestURI().equals("/operaciones/search/filtro")
					|| request.getRequestURI().equals("/operaciones/search/filtrar")
					|| request.getRequestURI().equals("/operaciones/search/filtrarPorUsuario")) {

				if (!Strings.isNullOrEmpty(request.getHeader("Authorization"))) {
					String token = request.getHeader("Authorization").replace("Bearer ", "");

					try {
						String[] chunks = token.split("\\.");
						Base64.Decoder decoder = Base64.getUrlDecoder();
						String header = new String(decoder.decode(chunks[0]));
						String payload = new String(decoder.decode(chunks[1]));

						JwtObjectMap jwtObjectMap = objectMapper.readValue(
								payload.toString().replace("Perfil", "perfil"),
								JwtObjectMap.class);

						if (jwtObjectMap.getPerfil().getDescripcion().equals("Usuario Final")
								|| jwtObjectMap.getPerfil().getDescripcion().equals("Invitado Externo")) {
							if (jwtObjectMap.getId().equals(request.getParameter("idAuth"))) {
								return true;
							} else {
								log.error("Fallo, el usuario no coincide con el autenticado");
								response = Msg(response,
										"{\"errorMessage\":\"Fallo, el usuario no coincide con el autenticado\"}");
								return false;
							}
						}
					} catch (Exception e) {
						log.error("Fallo en Interceptor de Seguriad Servicio-Operaciones", e.getMessage());
						response = Msg(response,
								"{\"errorMessage\":\"Fallo en Interceptor de Seguriad Servicio-Operaciones\"}");
						return false;
					}
				} else {
					log.error("Fallo, no existe token");
					return false;
				}
			}
		} else if (request.getMethod().equals("DELETE")) {
			/*
			 * System.out.println("INTO DELETE");
			 * String s=IOUtils.toString(request.getReader());
			 * if (s != null && s != "") {
			 * System.out.println("getReader");
			 * System.out.println(s);
			 * originCascading= objectMapper.readValue(s,OriginCascading.class) ;
			 * System.out.println("OriginCascading="+originCascading.origin);
			 * }else {
			 * System.out.println("Body null");
			 * }
			 */
		}
		return true;// HandlerInterceptor.super.preHandle(request, response, handler);
	}

	private HttpServletResponse Msg(HttpServletResponse res, String msg) {

		try {
			res.reset();
			res.resetBuffer();
			res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			res.setHeader("Content-Type", "application/json;charset=UTF-8");
			String s = "{\"errorMessage\":\"" + msg + "\"}";
			res.getOutputStream().write(s.getBytes("UTF-8"));
			res.flushBuffer();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Fallo, Construyendo Respuesta Interceptor Operaciones");
		} catch (IOException e) {
			throw new RuntimeException("Fallo, Construyendo Respuesta Interceptor Operaciones");
		}

		return res;
	}

	/*
	 * @Override public void postHandle(HttpServletRequest request,
	 * HttpServletResponse response, Object handler, ModelAndView modelAndView)
	 * throws Exception { // TODO Auto-generated method stub
	 * HandlerInterceptor.super.postHandle(request, response, handler,
	 * modelAndView); }
	 */

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		// HandlerInterceptor.super.afterCompletion(request, response, handler, ex);

		// Recuperar el objeto Operaciones desde los atributos de la solicitud
		Operaciones operaciones = null;
		operaciones = (Operaciones) request.getAttribute("operaciones");
		boolean handleBeforeCreate = request.getAttribute("handleBeforeCreate") != null
				? (boolean) request.getAttribute("handleBeforeCreate")
				: false;
		boolean handleAfterCreate = request.getAttribute("handleAfterCreate") != null
				? (boolean) request.getAttribute("handleAfterCreate")
				: false;
		boolean handleBD = request.getAttribute("handleBD") != null ? (boolean) request.getAttribute("handleBD")
				: false;
		String operationPath = request.getAttribute("operationPath") != null
				? request.getAttribute("operationPath").toString()
				: "";

		if (handleBeforeCreate)
			log.info("handleBeforeCreate==true");

		if (handleBD)
			log.info("handleBD==true");

		if (handleAfterCreate)
			log.info("handleAfterCreate==true");

		if (handleAfterCreate)
			log.info("operationPath=={}", operationPath);

		if (handleBeforeCreate && !handleBD) {
			try {
				log.info("Ejecutando rollback operaciones, por fallo en BD.");
				apis.borrar(operaciones);
				log.info("Fué ejeuctado rollback operaciones, por fallo en BD.");
			} catch (Exception e) {
				String err = "Fallo intentando eliminar la operación en las apis externas, debido a fallo intentando insertar operación en la BD.";
				log.error(err, e);
			}

			if (!Strings.isNullOrEmpty(operationPath)) {
				try {
					removeDirectoriesStruct(operationPath);
				} catch (Exception e) {
					String err = "Fallo intentando eliminar estructura de directorios, ejecutando rollback por fallo en apis externas.";
					log.error("{} : {}", err, e.getMessage());
				}
			}
		} else if (!handleBeforeCreate) {
			if (!Strings.isNullOrEmpty(operationPath)) {
				try {
					removeDirectoriesStruct(operationPath);
				} catch (Exception e) {
					String err = "Fallo intentando eliminar estructura de directorios, ejecutando rollback por fallo en apis externas.";
					log.error("{} : {}", err, e.getMessage());
				}
			}
		}

		if (ex != null) {
			// log.error("Operación = {}", operaciones.getDescripcion());
			log.error("**********afterCompletion Detectando errores en el servicio", ex.getMessage());
		}
	}

	private void removeDirectoriesStruct(String operationPath) throws IOException {
		FtpDTO ftpDto = ftpService.connectFTP();
		// Eliminación del directorio
		// boolean removed = ftpDto.ftp.removeDirectory(operationPath);
		// if (removed) {
		// log.info("El directorio {}, fue eliminado satisfactoriamente, ejecutando
		// rollback.",
		// operationPath);
		// }
	}
}
