package com.corum.backend.migration;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** 구 홈페이지 FTP 서버에서 첨부파일/인라인 이미지 원본을 내려받기 위한 클라이언트. */
@Slf4j
public class LegacyFtpClient implements AutoCloseable {

    private final FTPClient ftp = new FTPClient();

    public LegacyFtpClient(String host, int port, String username, String password) throws IOException {
        // 구 서버가 Windows 계열(IIS/기본 FTP)이라 파일명을 UTF-8이 아닌 한글 코드페이지(MS949)로
        // 주고받는다. 기본 UTF-8 제어 인코딩을 쓰면 한글 파일명이 깨져 "550 문법 오류"로 실패한다.
        ftp.setControlEncoding("MS949");
        ftp.connect(host, port);
        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            throw new IOException("FTP 연결 실패: " + host + ":" + port);
        }
        if (!ftp.login(username, password)) {
            ftp.disconnect();
            throw new IOException("FTP 로그인 실패: " + username);
        }
        ftp.enterLocalPassiveMode();
        ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
    }

    public byte[] download(String remotePath) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean ok = ftp.retrieveFile(remotePath, out);
        if (!ok) {
            throw new IOException("FTP 파일 다운로드 실패(reply=" + ftp.getReplyString().trim() + "): " + remotePath);
        }
        return out.toByteArray();
    }

    @Override
    public void close() {
        try {
            if (ftp.isConnected()) {
                ftp.logout();
                ftp.disconnect();
            }
        } catch (IOException e) {
            log.warn("FTP 연결 종료 중 오류: {}", e.getMessage());
        }
    }
}
