package com.galileo.cu.operaciones.dto;

import org.apache.commons.net.ftp.FTPClient;

import lombok.Value;

@Value
public class FtpDTO {
    public FTPClient ftp;
    public String ruta;
}
