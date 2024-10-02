package com.galileo.cu.operaciones.servicios;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galileo.cu.commons.models.Conexiones;
import com.galileo.cu.operaciones.dto.FtpDTO;
import com.galileo.cu.operaciones.repositorios.ConexionesRepository;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FtpService {
    private final ConexionesRepository conRepo;
    private static final String DEFAULT_DIRECTORY = "/";
    private static final int DEFAULT_FTP_PORT = 21;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public FtpService(ConexionesRepository conRepo) {
        this.conRepo = conRepo;
    }

    private Conexiones getFTPConnection() throws IOException {
        try {
            Conexiones con = conRepo.findFirstByServicioContaining("FTP");
            if (con == null) {
                String err = "Fallo, no existe una conexión FTP.";
                log.error(err);
                throw new IOException(err);
            }
            return con;
        } catch (Exception e) {
            String err = "Fallo al consultar las conexiones FTP en la base de datos.";
            log.error("{} :: {}", err, e.getMessage());
            throw new IOException(err);
        }
    }

    public FtpDTO connectFTP(FTPClient ftp) throws IOException {
        if (ftp == null) {
            ftp = new FTPClient();
        }

        if (ftp == null || !ftp.isConnected()) {
            Conexiones con = getFTPConnection();

            try {
                ftp = makeFTPConnection(con);
            } catch (Exception e) {
                if (e.getMessage().contains("Fallo") || e.getMessage().contains("Falló")) {
                    throw new IOException(e.getMessage());
                }
                String err = "Fallo al conectar con el servidor FTP";
                log.error("{}", err, e);
                throw new IOException(err);
            }
        }

        FtpDTO ftpDto = new FtpDTO(ftp, DEFAULT_DIRECTORY);
        return ftpDto;
    }

    private FTPClient makeFTPConnection(Conexiones con) throws IOException {
        FTPClient ftp = new FTPClient();
        try {
            int puerto = Strings.isNullOrEmpty(con.getPuerto()) ? DEFAULT_FTP_PORT : Integer.parseInt(con.getPuerto());
            ftp.connect(con.getIpServicio(), puerto);
        } catch (Exception e) {
            String err = "Fallo al intentar crear una conexión FTP {}:{}";
            log.error(err, con.getIpServicio(), con.getPuerto(), e);
            throw new IOException(err + con.getIpServicio());
        }

        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            String err = "Fallo intentando conectar con el servidor FTP " + con.getIpServicio();
            log.error(err);
            disconnectFTP(ftp);
            throw new IOException(err);
        }

        authenticateFTP(ftp, con);

        setUpPassiveMode(ftp);

        return ftp;
    }

    private void authenticateFTP(FTPClient ftp, Conexiones con) throws IOException {
        try {
            boolean successLogin = ftp.login(con.getUsuario(), con.getPassword());
            if (!successLogin) {
                String err = "Fallo intentando autenticarse con el servidor FTP";
                log.error("{}", err);
                disconnectFTP(ftp);
                throw new IOException(err);
            }
            log.info("La Autenticación con el servidor FTP, fue exitosa");
        } catch (IOException e) {
            String err = "Fallo intentando autenticarse con el servidor FTP";
            log.error("{}", err, e);
            disconnectFTP(ftp);
            throw new IOException(err, e);
        }
    }

    private void setUpPassiveMode(FTPClient ftp) throws IOException {
        try {
            ftp.enterLocalPassiveMode();
            ftp.setControlKeepAliveTimeout(1000);
        } catch (Exception e) {
            String err = "Fallo al configurar el modo pasivo en la conexión FTP";
            log.error("{}", err, e);
            disconnectFTP(ftp);
            throw new IOException(err, e);
        }
    }

    private String getFTPDirectory(FTPClient ftp) throws IOException {
        try {
            return ftp.printWorkingDirectory();
        } catch (Exception e) {
            String err = "Fallo al obtener el directorio predeterminado del servidor FTP";
            log.error(err, e);
            disconnectFTP(ftp);
            throw new IOException(err, e);
        }
    }

    private void disconnectFTP(FTPClient ftp) throws IOException {
        if (ftp != null && ftp.isConnected()) {
            try {
                ftp.logout();
                ftp.disconnect();
                ftp = null;
            } catch (IOException e) {
                String err = "Fallo al desconectar el servidor FTP";
                log.error("{}", err, e);
                throw new IOException(err);
            }
        }
    }
}
