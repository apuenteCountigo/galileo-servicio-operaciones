package com.galileo.cu.operaciones.controladores;

import java.util.Base64;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galileo.cu.commons.models.dto.JwtObjectMap;
import com.google.common.base.Strings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ValidateAuthorization {
    public ObjectMapper objectMapper;    
    public HttpServletRequest req;
    public JwtObjectMap jwtObjectMap;
    
    public boolean Validate() {
        if (req==null) {
            throw new RuntimeException("Error Validando Autorización la Petición no debe ser Nula ");
        }
        System.out.println("ValidateAuthorization**********************");
		System.out.println(req.getHeader("Authorization"));
        System.out.println("METHOD: "+req.getMethod());

        if (!Strings.isNullOrEmpty(req.getHeader("Authorization"))) {
            String token = req.getHeader("Authorization").replace("Bearer ", "");
            System.out.println(token.toString());
            try {
                String[] chunks = token.split("\\.");
                Base64.Decoder decoder = Base64.getUrlDecoder();
                String header = new String(decoder.decode(chunks[0]));
                String payload = new String(decoder.decode(chunks[1]));

                System.out.println(payload.toString());

                jwtObjectMap = objectMapper.readValue(payload.toString().replace("Perfil", "perfil"),
							JwtObjectMap.class);
                System.out.println(jwtObjectMap.getId());

                System.out.println("Path:" + req.getRequestURI());
                System.out.println("-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");
                System.out.println("id parametro: " + req.getParameter("idAuth"));
                if (!Strings.isNullOrEmpty(req.getParameter("idAuth")) && jwtObjectMap.getId().equals(req.getParameter("idAuth"))) {
                    return true;
                }else if(Strings.isNullOrEmpty(req.getParameter("idAuth")) && !req.getMethod().equals("GET")){
                    return true;
                } else{
                    return false;
                }
            } catch (Exception e) {
                System.out.println("ERROR, Validando Autorización: "+e.getMessage());
                throw new RuntimeException("ERROR, Validando Autorización ");
            }
        } else {
            throw new RuntimeException("Error Debe Enviar una Cabecera de Autorización y un Token Válido ");
        }
    }
}
