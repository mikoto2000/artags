package jp.dip.oyasirazu.artags;

import java.nio.file.Path;
import java.nio.file.Paths;

import jp.dip.oyasirazu.artags.Artags.Record;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

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
        assertThat(recordString01, is("symbol01\tpath\\to\\file\\01\\test.arxml\tsearchStr01;\"\t\tarHierarchyPath01 (type01)\tfile:"));
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

        Path outputFilePath = Paths.get("path", "to", "output", "file", "tags");
        String recordString01 = record01.buildRecordString(outputFilePath);

        // 'path/to/output/file' までがディレクトリで、
        // 'tags' が出力ファイルなので、レコードに格納する相対パスとしては
        // 'path/to/output/file/tags' から
        // 'path/to/file/01/test.arxml' までの相対パスになる
        assertThat(recordString01, is("symbol01\t..\\..\\file\\01\\test.arxml\tsearchStr01;\"\t\tarHierarchyPath01 (type01)\tfile:"));
    }
    @Before
    public void setup() {
    }

    @After
    public void tearDown() {
    }
}


