package com.galileo.cu.operaciones.interceptores;

import com.galileo.cu.commons.models.Operaciones;
import com.galileo.cu.commons.models.dto.JwtObjectMap;
import com.galileo.cu.commons.models.dto.OriginCascading;
import com.galileo.cu.operaciones.repositorios.OperacionesRepository;

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

@Component
public class OperacionesInterceptor implements HandlerInterceptor {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private OperacionesRepository oper;

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
								System.out.println("EL USUARIO ENVIADO NO COINCIDE CON EL AUTENTICADO");
								response = Msg(response,
										"{\"errorMessage\":\"EL USUARIO ENVIADO NO COINCIDE CON EL AUTENTICADO! Servicio-Operaciones\"}");
								return false;
							}
						}
					} catch (Exception e) {
						System.out.println("NO HAY TOKEN");
						response = Msg(response,
								"{\"errorMessage\":\"ERROR en Interceptor de Seguriad Servicio-Operaciones\",\"errorOficial\":\""
										+ e.getMessage() + "\"}");
						return false;
					}
				} else {
					System.out.println("NO HAY TOKEN");
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
			throw new RuntimeException("ERROR, Construyendo Respuesta Interceptor Operaciones");
		} catch (IOException e) {
			throw new RuntimeException("ERROR, Construyendo Respuesta Interceptor Operaciones");
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
		if (ex != null) {
			System.out.println("**********------OCURRIO UN ERROR");
			// response=Msg(response,"{\"errorMessage\":\"DETECTANDO MIS ERRORES!
			// Servicio-Operaciones\"}");
		} else {
			System.out.println("!!!!!!!------NO OCURRIO UN ERROR");
		}
	}

}
