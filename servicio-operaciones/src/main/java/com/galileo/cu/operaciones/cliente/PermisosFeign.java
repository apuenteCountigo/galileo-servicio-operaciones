package com.galileo.cu.operaciones.cliente;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.galileo.cu.commons.models.Operaciones;

@FeignClient(name="servicio-permisos")
public interface PermisosFeign {
	@GetMapping("/asignados_operaciones")
	public Operaciones conpermisos(@RequestParam int idUsuario, @RequestParam int idOperacion);
}
