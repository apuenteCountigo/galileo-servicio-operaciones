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
        return Optional.ofNullable(conRepo.findFirstByServicioContaining("FTP"))
                .orElseThrow(() -> {
                    String err = "Fallo, no existe una conexión FTP.";
                    log.error(err);
                    return new IOException(err);
                });
    }

    public FtpDTO connectFTP() throws IOException {
        log.info("INICIO connectFTP");
        FTPClient ftp = null;
        Conexiones con = getFTPConnection();

        if (con != null) {
            try {
                ftp = makeFTPConnection(con);
                return new FtpDTO(ftp, DEFAULT_DIRECTORY);
            } catch (Exception e) {
                if (ftp != null) {
                    disconnectFTP(ftp);
                }
                if (e.getMessage().contains("Fallo") || e.getMessage().contains("Falló")) {
                    throw new IOException(e.getMessage());
                }
                String err = "Fallo al conectar con el servidor FTP";
                log.error("{}", err, e);
                throw new IOException(err);
            }
        } else {
            String err = "Fallo, no existe una conexión FTP, entre las conexiones.";
            log.error("{}", err);
            throw new IOException(err);
        }
    }

    private FTPClient makeFTPConnection(Conexiones con) throws IOException {
        FTPClient ftp = new FTPClient();
        try {
            int puerto = Strings.isNullOrEmpty(con.getPuerto()) ? DEFAULT_FTP_PORT : Integer.parseInt(con.getPuerto());
            ftp.connect(con.getIpServicio(), puerto);

            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                String err = "Fallo intentando conectar con el servidor FTP " + con.getIpServicio();
                log.error(err);

                if (ftp != null && ftp.isConnected()) {
                    disconnectFTP(ftp);
                }

                throw new IOException(err);
            }

            authenticateFTP(ftp, con);
            setUpPassiveMode(ftp);
            return ftp;

        } catch (Exception e) {
            String err = String.format("Fallo al intentar crear una conexión FTP %s:%s", con.getIpServicio(),
                    con.getPuerto());
            log.error(err, e);

            if (ftp != null && ftp.isConnected()) {
                disconnectFTP(ftp);
            }

            throw new IOException(err);
        }
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
            log.info("La autenticación con el servidor FTP fue exitosa");
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

    private void disconnectFTP(FTPClient ftp) throws IOException {
        if (ftp != null && ftp.isConnected()) {
            try {
                ftp.logout();
            } catch (IOException e) {
                log.error("Fallo durante el logout del servidor FTP: {}", e.getMessage(), e);
            } finally {
                try {
                    ftp.disconnect();
                } catch (IOException e) {
                    log.error("Fallo durante la desconexión del servidor FTP: {}", e.getMessage(), e);
                }
            }
        }
    }
}