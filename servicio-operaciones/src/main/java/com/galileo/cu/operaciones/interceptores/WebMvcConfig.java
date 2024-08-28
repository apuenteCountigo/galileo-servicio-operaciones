package com.galileo.cu.operaciones.interceptores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.MappedInterceptor;

@Component
public class WebMvcConfig {

	@Autowired
	OperacionesInterceptor operacionesInterceptor;

	@Bean
    public MappedInterceptor operacionesIntercept() {
        return new MappedInterceptor(new String[]{"/**"}, operacionesInterceptor);
    }
}
