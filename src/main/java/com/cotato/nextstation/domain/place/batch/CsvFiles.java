package com.cotato.nextstation.domain.place.batch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 배치가 주고받는 CSV를 읽기 위한 공통 유틸리티
 *
 * <p>배치 산출물은 엑셀이나 스크립트로 편집되는 일이 있어 BOM이 붙어 들어온다. BOM을 제거하지 않으면
 * 첫 번째 헤더명에 포함되어 이름 기반 조회가 실패하므로, 이름으로 컬럼을 읽는 곳은 이 리더를 쓴다.
 */
final class CsvFiles {

    private static final int BOM = 0xFEFF;

    private CsvFiles() {
    }

    static Reader bomSafeReader(Path path) throws IOException {
        BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        reader.mark(1);
        if (reader.read() != BOM) {
            reader.reset();
        }
        return reader;
    }
}
