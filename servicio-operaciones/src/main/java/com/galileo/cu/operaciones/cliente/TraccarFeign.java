package com.galileo.cu.operaciones.cliente;

import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.galileo.cu.commons.models.Operaciones;

@FeignClient(name="servicio-apis")
public interface TraccarFeign {

	@PostMapping("/salvarOperacionDataMiner")
	public Operaciones salvar(@RequestBody Operaciones operaciones);

	@PostMapping("/estadoEnvioNombZip")
	public String estadoZip(@RequestParam Integer idDataminer, @RequestParam Integer idElement);
	
	@DeleteMapping("/borrarOperacionDataMiner")
	String borrar(@RequestBody Operaciones operacion);
}
