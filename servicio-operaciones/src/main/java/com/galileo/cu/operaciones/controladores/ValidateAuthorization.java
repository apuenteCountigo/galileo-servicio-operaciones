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
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        if (req == null) {
            log.error("Error Validando Autorización la Petición no debe ser Nula");
            throw new RuntimeException("Error Validando Autorización la Petición no debe ser Nula");
        }
        if (!Strings.isNullOrEmpty(req.getHeader("Authorization"))) {
            String token = req.getHeader("Authorization").replace("Bearer ", "");
            try {
                String[] chunks = token.split("\\.");
                Base64.Decoder decoder = Base64.getUrlDecoder();
                String header = new String(decoder.decode(chunks[0]));
                String payload = new String(decoder.decode(chunks[1]));

                jwtObjectMap = objectMapper.readValue(payload.toString().replace("Perfil", "perfil"),
                        JwtObjectMap.class);

                if (!Strings.isNullOrEmpty(req.getParameter("idAuth"))
                        && jwtObjectMap.getId().equals(req.getParameter("idAuth"))) {
                    return true;
                } else if (Strings.isNullOrEmpty(req.getParameter("idAuth")) && !req.getMethod().equals("GET")) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                log.error("Fallo, Validando Autorización: " + e.getMessage());
                throw new RuntimeException("Fallo, Validando Autorización");
            }
        } else {
            log.error("Fallo, Debe Enviar una Cabecera de Autorización y un Token Válido");
            throw new RuntimeException("Fallo, Debe Enviar una Cabecera de Autorización y un Token Válido");
        }
    }
}
