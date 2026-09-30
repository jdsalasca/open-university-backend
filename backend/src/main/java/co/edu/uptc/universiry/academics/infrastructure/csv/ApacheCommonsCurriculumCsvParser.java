package co.edu.uptc.universiry.academics.infrastructure.csv;

import co.edu.uptc.universiry.academics.application.CurriculumCsvException;
import co.edu.uptc.universiry.academics.application.CurriculumCsvIssue;
import co.edu.uptc.universiry.academics.application.CurriculumCsvParser;
import co.edu.uptc.universiry.academics.application.CurriculumCsvSchema;
import co.edu.uptc.universiry.academics.application.CurriculumImportLimits;
import co.edu.uptc.universiry.academics.application.ParsedCurriculum;
import co.edu.uptc.universiry.academics.application.ParsedCurriculumRow;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ApacheCommonsCurriculumCsvParser implements CurriculumCsvParser {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final CSVFormat FORMAT = CSVFormat.RFC4180.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setAllowMissingColumnNames(true)
            .setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL)
            .setIgnoreEmptyLines(false)
            .get();

    private final CurriculumImportLimits limits;

    public ApacheCommonsCurriculumCsvParser(CurriculumImportLimits limits) {
        this.limits = limits;
    }

    @Override
    public ParsedCurriculum parse(InputStream input) {
        byte[] sourceBytes = readBounded(input);
        if (sourceBytes.length == 0 || isOnlyBom(sourceBytes)) {
            throw failure(CurriculumCsvException.Code.EMPTY_FILE);
        }

        String sourceSha256 = sha256(sourceBytes);
        String csvText = decodeUtf8(sourceBytes);
        if (csvText.isBlank()) {
            throw failure(CurriculumCsvException.Code.EMPTY_FILE);
        }

        try (CSVParser csvParser = CSVParser.parse(csvText, FORMAT)) {
            List<String> headers = csvParser.getHeaderNames();
            validateHeaders(headers);

            List<ParsedCurriculumRow> rows = new ArrayList<>();
            for (CSVRecord record : csvParser) {
                long recordNumber = record.getRecordNumber();
                if (recordNumber > limits.maxRows()) {
                    throw failure(CurriculumCsvException.Code.TOO_MANY_ROWS);
                }
                int sourceRowNumber = Math.toIntExact(recordNumber + 1);
                if (record.size() != headers.size()) {
                    throw new CurriculumCsvException(
                            CurriculumCsvException.Code.INVALID_DATA,
                            List.of(new CurriculumCsvIssue(
                                    sourceRowNumber, null, CurriculumCsvIssue.Code.INVALID_ROW_WIDTH)));
                }

                Map<String, String> values = new LinkedHashMap<>();
                for (int column = 0; column < headers.size(); column++) {
                    values.put(headers.get(column), record.get(column));
                }
                rows.add(new ParsedCurriculumRow(sourceRowNumber, values));
            }

            if (rows.isEmpty()) {
                throw failure(CurriculumCsvException.Code.INVALID_DATA);
            }
            return new ParsedCurriculum(rows, sourceSha256);
        } catch (CurriculumCsvException error) {
            throw error;
        } catch (IOException | UncheckedIOException | IllegalArgumentException error) {
            throw failure(CurriculumCsvException.Code.MALFORMED_CSV);
        }
    }

    private byte[] readBounded(InputStream input) {
        if (input == null) {
            throw failure(CurriculumCsvException.Code.INVALID_DATA);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(limits.maxFileBytes(), 8192));
        byte[] buffer = new byte[8192];
        try {
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (count == 0) {
                    int nextByte = input.read();
                    if (nextByte == -1) {
                        break;
                    }
                    if (output.size() == limits.maxFileBytes()) {
                        throw failure(CurriculumCsvException.Code.FILE_TOO_LARGE);
                    }
                    output.write(nextByte);
                    continue;
                }
                if (count > limits.maxFileBytes() - output.size()) {
                    throw failure(CurriculumCsvException.Code.FILE_TOO_LARGE);
                }
                output.write(buffer, 0, count);
            }
        } catch (IOException error) {
            throw failure(CurriculumCsvException.Code.INVALID_DATA);
        }
        return output.toByteArray();
    }

    private static String decodeUtf8(byte[] sourceBytes) {
        int offset = startsWithBom(sourceBytes) ? UTF8_BOM.length : 0;
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(sourceBytes, offset, sourceBytes.length - offset))
                    .toString();
        } catch (CharacterCodingException error) {
            throw failure(CurriculumCsvException.Code.INVALID_UTF8);
        }
    }

    private static void validateHeaders(List<String> headers) {
        if (headers == null || headers.size() != CurriculumCsvSchema.HEADERS.size()) {
            throw failure(CurriculumCsvException.Code.INVALID_HEADERS);
        }
        Set<String> seen = new HashSet<>();
        for (String header : headers) {
            if (header == null || header.isBlank()
                    || !CurriculumCsvSchema.HEADER_SET.contains(header)
                    || !seen.add(header)) {
                throw failure(CurriculumCsvException.Code.INVALID_HEADERS);
            }
        }
        if (!seen.equals(CurriculumCsvSchema.HEADER_SET)) {
            throw failure(CurriculumCsvException.Code.INVALID_HEADERS);
        }
    }

    private static boolean isOnlyBom(byte[] sourceBytes) {
        return sourceBytes.length == UTF8_BOM.length && startsWithBom(sourceBytes);
    }

    private static boolean startsWithBom(byte[] sourceBytes) {
        if (sourceBytes.length < UTF8_BOM.length) {
            return false;
        }
        for (int index = 0; index < UTF8_BOM.length; index++) {
            if (sourceBytes[index] != UTF8_BOM[index]) {
                return false;
            }
        }
        return true;
    }

    private static String sha256(byte[] sourceBytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(sourceBytes));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available.");
        }
    }

    private static CurriculumCsvException failure(CurriculumCsvException.Code code) {
        return new CurriculumCsvException(code, List.of());
    }
}
