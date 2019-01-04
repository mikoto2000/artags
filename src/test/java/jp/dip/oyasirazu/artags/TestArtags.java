package jp.dip.oyasirazu.artags;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import jp.dip.oyasirazu.artags.Artags.Arxml;
import jp.dip.oyasirazu.artags.Artags.Record;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * TestArtags
 * TODO: テストバリエーションをまじめに考える
 */
public class TestArtags {

    @Test
    public void testFindArxmls() {
        // TODO: Path の比較ってどうするのがスマートなんだろうか？？？
        try {
            List<String> oneFiles = createStringList("./src/test/resources/one_file/");
            List<Arxml> arxmls1 = Artags.findArxmls(oneFiles, null);
            assertEquals(arxmls1.size(), 1);
            assertEquals(arxmls1.get(0).getFilePath().toString(),
                    newPath("./src/test/resources/one_file/test.arxml").toString());


            List<String> nestedDirectory = createStringList("./src/test/resources/nested_directory/");
            List<Arxml> arxmls2 = Artags.findArxmls(nestedDirectory, null);
            assertEquals(arxmls2.size(), 1);
            assertEquals(arxmls2.get(0).getFilePath().toString(),
                    newPath("./src/test/resources/nested_directory/nest_directory/test.arxml").toString());


            List<String> nestedDirectoryFiles = createStringList("./src/test/resources/nested_directory_files/");
            List<Arxml> arxmls3 = Artags.findArxmls(nestedDirectoryFiles, null);
            assertEquals(arxmls3.size(), 2);
            // TODO: 順番に依存しない書き方にしないと...
            assertEquals(arxmls3.get(0).getFilePath().toString(),
                    newPath("./src/test/resources/nested_directory_files/common/common.arxml").toString());
            assertEquals(arxmls3.get(1).getFilePath().toString(),
                    newPath("./src/test/resources/nested_directory_files/system/system.arxml").toString());


            List<String> multiDir = createStringList(
                        "./src/test/resources/one_file/",
                        "./src/test/resources/nested_directory/",
                        "./src/test/resources/nested_directory_files/");
            List<Arxml> arxmls4 = Artags.findArxmls(multiDir, null);
            assertEquals(arxmls4.size(), 4);
            // TODO: 順番に依存しない書き方にしないと...
            assertEquals(arxmls4.get(0).getFilePath().toString(),
                    newPath("./src/test/resources/one_file/test.arxml").toString());
            assertEquals(arxmls4.get(1).getFilePath().toString(),
                    newPath("./src/test/resources/nested_directory/nest_directory/test.arxml").toString());
            assertEquals(arxmls4.get(2).getFilePath().toString(),
                    newPath("./src/test/resources/nested_directory_files/common/common.arxml").toString());
            assertEquals(arxmls4.get(3).getFilePath().toString(),
                    newPath("./src/test/resources/nested_directory_files/system/system.arxml").toString());


            List<String> exclude = createStringList(
                        "./src/test/resources/one_file/",
                        "./src/test/resources/nested_directory/",
                        "./src/test/resources/nested_directory_files/");
            List<Arxml> arxmls5 = Artags.findArxmls(exclude, ".*system.*");
            assertEquals(arxmls5.size(), 3);
            // TODO: 順番に依存しない書き方にしないと...
            assertEquals(arxmls5.get(0).getFilePath().toString(),
                    newPath("./src/test/resources/one_file/test.arxml").toString());
            assertEquals(arxmls5.get(1).getFilePath().toString(),
                    newPath("./src/test/resources/nested_directory/nest_directory/test.arxml").toString());
            assertEquals(arxmls5.get(2).getFilePath().toString(),
                    newPath("./src/test/resources/nested_directory_files/common/common.arxml").toString());
        } catch (IOException e) {
            fail("例外が出ちゃいましたねー : " + e.getMessage());
        }
    }

    @Test
    public void testCreateTagsString() {
        try {
            Arxml arxml1 = new Arxml(Paths.get("./src/test/resources/one_file/test.arxml"));
            List<Arxml> avarableArxmls1 = new ArrayList<>();
            avarableArxmls1.add(arxml1);

            Set<Record> records1 = Artags.createTagsString(arxml1, avarableArxmls1);
            assertEquals(records1.size(), 6);

            for (Record record : records1) {
                if (record.getSymbol().equals("sint8")) {
                    assertEquals(record.getSearchStr(), "7");
                } else if (record.getSymbol().equals("ImplDataType")) {
                    assertEquals(record.getSearchStr(), "20");
                } else if (record.getSymbol().equals("Interface")) {
                    assertEquals(record.getSearchStr(), "36");
                } else if (record.getSymbol().equals("Runnable")) {
                    assertEquals(record.getSearchStr(), "72");
                } else if (record.getSymbol().equals("Port")) {
                    assertEquals(record.getSearchStr(), "55");
                } else if (record.getSymbol().equals("Operation")) {
                    assertEquals(record.getSearchStr(), "40");
                }
            }

        } catch (XPathExpressionException
                | SAXException
                | ParserConfigurationException
                | TransformerException
                | IOException e) {
            fail("例外が出ちゃいましたねー : " + e.getMessage());
        }
    }

    /**
     * 非 ascii 文字を含むパスが問題なく読み込めることを確認。
     */
    @Test
    public void testCreateTagsString_FromNonAsciiPath() {
        try {
            Arxml arxml1 = new Arxml(Paths.get("./src/test/resources/非asciiパス/test.arxml"));
            List<Arxml> avarableArxmls1 = new ArrayList<>();
            avarableArxmls1.add(arxml1);

            Set<Record> records1 = Artags.createTagsString(arxml1, avarableArxmls1);
            assertEquals(records1.size(), 6);

            for (Record record : records1) {
                if (record.getSymbol().equals("sint8")) {
                    assertEquals(record.getSearchStr(), "7");
                } else if (record.getSymbol().equals("ImplDataType")) {
                    assertEquals(record.getSearchStr(), "20");
                } else if (record.getSymbol().equals("Interface")) {
                    assertEquals(record.getSearchStr(), "36");
                } else if (record.getSymbol().equals("Runnable")) {
                    assertEquals(record.getSearchStr(), "72");
                } else if (record.getSymbol().equals("Port")) {
                    assertEquals(record.getSearchStr(), "55");
                } else if (record.getSymbol().equals("Operation")) {
                    assertEquals(record.getSearchStr(), "40");
                }
            }

        } catch (XPathExpressionException
                | SAXException
                | ParserConfigurationException
                | TransformerException
                | IOException e) {
            fail("例外が出ちゃいましたねー : " + e.getMessage());
        }
    }

    /**
     * ヘッダ直後にコメントがある場合にも問題なく行数が取得できるかを確認。
     */
    @Test
    public void testCreateTagsString_CommentImmediatelyAfterHeader() {
        try {
            Arxml arxml1 = new Arxml(Paths.get("./src/test/resources/contents_variation/01_comments.arxml"));
            List<Arxml> avarableArxmls1 = new ArrayList<>();
            avarableArxmls1.add(arxml1);

            Set<Record> records1 = Artags.createTagsString(arxml1, avarableArxmls1);
            assertEquals(records1.size(), 6);

            for (Record record : records1) {
                if (record.getSymbol().equals("sint8")) {
                    assertEquals(record.getSearchStr(), "15");
                } else if (record.getSymbol().equals("ImplDataType")) {
                    assertEquals(record.getSearchStr(), "28");
                } else if (record.getSymbol().equals("Interface")) {
                    assertEquals(record.getSearchStr(), "44");
                } else if (record.getSymbol().equals("Runnable")) {
                    assertEquals(record.getSearchStr(), "80");
                } else if (record.getSymbol().equals("Port")) {
                    assertEquals(record.getSearchStr(), "63");
                } else if (record.getSymbol().equals("Operation")) {
                    assertEquals(record.getSearchStr(), "48");
                }
            }

        } catch (XPathExpressionException
                | SAXException
                | ParserConfigurationException
                | TransformerException
                | IOException e) {
            fail("例外が出ちゃいましたねー : " + e.getMessage());
        }
    }

    @Test
    public void testCreateTagsString_SameShortName() {
        try {
            Arxml arxml1 = new Arxml(Paths.get("./src/test/resources/same_short_name/test.arxml"));
            List<Arxml> avarableArxmls1 = new ArrayList<>();
            avarableArxmls1.add(arxml1);

            Set<Record> records1 = Artags.createTagsString(arxml1, avarableArxmls1);
            assertEquals(records1.size(), 2);

            for (Record record : records1) {
                if (record.getArHierarchyPath().equals("/Parent/sint8")) {
                    assertEquals(record.getSearchStr(), "7");
                } else if (record.getArHierarchyPath().equals("/Parent/Child/sint8")) {
                    assertEquals(record.getSearchStr(), "19");
                }
            }

        } catch (XPathExpressionException
                | SAXException
                | ParserConfigurationException
                | TransformerException
                | IOException e) {
            fail("例外が出ちゃいましたねー : " + e.getMessage());
        }
    }

    private List<String> createStringList(String... strs) {
        List<String> strList = Arrays.asList(strs);
        return strList;
    }

    /**
     * スラッシュ区切りの文字列から Path を生成する。
     *
     * 制限: スラッシュで分割した配列が、最低でも長さ 2 になるような文字列でないと例外出ます。
     */
    private Path newPath(String pathStr) {
        String[] pathArray = pathStr.split("/");
        String first = pathArray[0];
        String[] more = Arrays.copyOfRange(pathArray, 1, pathArray.length);
        return Paths.get(first, more);
    }

    @BeforeEach
    public void setup() {
    }

    @AfterEach
    public void tearDown() {
    }
}

