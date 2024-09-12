package com.galileo.cu.operaciones.controladores;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

// default package
// Generated 16 may. 2022 00:39:42 by Hibernate Tools 5.6.7.Final

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.StringUtils;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "conexiones")
public class Conexiones implements java.io.Serializable {

    private static final long serialVersionUID = 2075085911818166859L;
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long Id;
    private String servicio;
    private String ipServicio;
    private String usuario;
    private String password;
    private String puerto;
    @Column(insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    private Integer dmaID;
    private Integer viewIDs;

    public String buildHttpUriDataminer() {
        return "http://" + ipServicio;
    }

    public String buildHttpUriTraccar() {
        if (StringUtils.hasText(puerto)) {
            return "http://" + ipServicio + ":" + puerto;
        }
        return "http://" + ipServicio;
    }
}
