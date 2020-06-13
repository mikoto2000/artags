package jp.dip.oyasirazu.artags;

import java.nio.file.Path;
import java.nio.file.Paths;

import jp.dip.oyasirazu.artags.Artags.Record;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TestRecord
 */
public class TestRecord {

    @Test
    public void testRecord() {
        Record record01 = new Record(
                "symbol01"
                , Paths.get("path", "to", "file", "01", "test.arxml")
                , "searchStr01"
                , "type01"
                , "arHierarchyPath01"
        );

        String recordString01 = record01.buildRecordString();
        Path filePath = Paths.get("path/to/file/01/test.arxml");
        assertEquals(recordString01, "symbol01\t" + filePath.toString() + "\tsearchStr01;\"\t\tarHierarchyPath01 (type01)\tfile:");
    }

    @Test
    public void testRecord_relativePath() {
        Record record01 = new Record(
                "symbol01"
                , Paths.get("path", "to", "file", "01", "test.arxml")
                , "searchStr01"
                , "type01"
                , "arHierarchyPath01"
        );

        Path outputPath = Paths.get("path", "to", "output", "dir");
        String recordString01 = record01.buildRecordString(outputPath);

        // 'path/to/output/dir' ディレクトリに出力するので、
        // レコードに格納する相対パスとしては
        // 'path/to/output/dir' から
        // 'path/to/file/01/test.arxml' までの相対パスになる
        Path filePath = Paths.get("../../file/01/test.arxml");
        assertEquals(recordString01, "symbol01\t" + filePath + "\tsearchStr01;\"\t\tarHierarchyPath01 (type01)\tfile:");
    }
    @BeforeEach
    public void setup() {
    }

    @AfterEach
    public void tearDown() {
    }
}


