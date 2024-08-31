package com.galileo.cu.operaciones.servicios;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galileo.cu.commons.models.Operaciones;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OperacionesOrquestador {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OperacionesOrquestador(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void iniciarCreacionOperacion(Operaciones operacion) throws JsonProcessingException {
        log.info("iniciarCreacionOperacion");
        String operacionJson = objectMapper.writeValueAsString(operacion);
        kafkaTemplate.send("crear-operacion-dma", operacionJson);
    }

    @KafkaListener(topics = "dma-operacion-creada")
    public void manejarDMACreado(String mensaje) throws JsonProcessingException {
        // DMAResultadoDTO resultado = objectMapper.readValue(mensaje,
        // DMAResultadoDTO.class);
        // Añadir idDMA a la operación y enviar a Traccar
        kafkaTemplate.send("crear-operacion-traccar", objectMapper.writeValueAsString(resultado));
    }

    @KafkaListener(topics = "traccar-operacion-creada")
    public void manejarTraccarCreado(String mensaje) throws JsonProcessingException {
        // TraccarResultadoDTO resultado = objectMapper.readValue(mensaje,
        // TraccarResultadoDTO.class);
        // Añadir idTraccar a la operación y enviar para almacenamiento
        kafkaTemplate.send("almacenar-operacion", objectMapper.writeValueAsString(resultado));
    }

    @KafkaListener(topics = "operacion-almacenada")
    public void manejarOperacionAlmacenada(String mensaje) {
        // Notificar que la operación se ha completado
        log.info("Operación creada y almacenada exitosamente: " + mensaje);
    }

    // Manejo de errores y compensación
    @KafkaListener(topics = { "dma-error", "traccar-error", "almacenamiento-error" })
    public void manejarError(String mensaje) {
        // Implementar lógica de compensación según el error
        log.info("Error en el proceso: " + mensaje);
        // Iniciar proceso de compensación
    }
}
