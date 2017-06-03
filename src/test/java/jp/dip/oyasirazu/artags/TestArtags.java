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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

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
            assertThat(arxmls1.size(), is(1));
            assertThat(arxmls1.get(0).getFilePath().toString(),
                    is(newPath("./src/test/resources/one_file/test.arxml").toString()));


            List<String> nestedDirectory = createStringList("./src/test/resources/nested_directory/");
            List<Arxml> arxmls2 = Artags.findArxmls(nestedDirectory, null);
            assertThat(arxmls2.size(), is(1));
            assertThat(arxmls2.get(0).getFilePath().toString(),
                    is(newPath("./src/test/resources/nested_directory/nest_directory/test.arxml").toString()));


            List<String> nestedDirectoryFiles = createStringList("./src/test/resources/nested_directory_files/");
            List<Arxml> arxmls3 = Artags.findArxmls(nestedDirectoryFiles, null);
            assertThat(arxmls3.size(), is(2));
            // TODO: 順番に依存しない書き方にしないと...
            assertThat(arxmls3.get(0).getFilePath().toString(),
                    is(newPath("./src/test/resources/nested_directory_files/common/common.arxml").toString()));
            assertThat(arxmls3.get(1).getFilePath().toString(),
                    is(newPath("./src/test/resources/nested_directory_files/system/system.arxml").toString()));


            List<String> multiDir = createStringList(
                        "./src/test/resources/one_file/",
                        "./src/test/resources/nested_directory/",
                        "./src/test/resources/nested_directory_files/");
            List<Arxml> arxmls4 = Artags.findArxmls(multiDir, null);
            assertThat(arxmls4.size(), is(4));
            // TODO: 順番に依存しない書き方にしないと...
            assertThat(arxmls4.get(0).getFilePath().toString(),
                    is(newPath("./src/test/resources/one_file/test.arxml").toString()));
            assertThat(arxmls4.get(1).getFilePath().toString(),
                    is(newPath("./src/test/resources/nested_directory/nest_directory/test.arxml").toString()));
            assertThat(arxmls4.get(2).getFilePath().toString(),
                    is(newPath("./src/test/resources/nested_directory_files/common/common.arxml").toString()));
            assertThat(arxmls4.get(3).getFilePath().toString(),
                    is(newPath("./src/test/resources/nested_directory_files/system/system.arxml").toString()));


            List<String> exclude = createStringList(
                        "./src/test/resources/one_file/",
                        "./src/test/resources/nested_directory/",
                        "./src/test/resources/nested_directory_files/");
            List<Arxml> arxmls5 = Artags.findArxmls(exclude, ".*system.*");
            assertThat(arxmls5.size(), is(3));
            // TODO: 順番に依存しない書き方にしないと...
            assertThat(arxmls5.get(0).getFilePath().toString(),
                    is(newPath("./src/test/resources/one_file/test.arxml").toString()));
            assertThat(arxmls5.get(1).getFilePath().toString(),
                    is(newPath("./src/test/resources/nested_directory/nest_directory/test.arxml").toString()));
            assertThat(arxmls5.get(2).getFilePath().toString(),
                    is(newPath("./src/test/resources/nested_directory_files/common/common.arxml").toString()));
        } catch (IOException e) {
            fail("例外が出ちゃいましたねー : " + e.getMessage());
        }
    }

    @Test
    public void testCreateTagsString() {
        try {
            Arxml arxml1 = new Arxml("./src/test/resources/one_file/test.arxml");
            List<Arxml> avarableArxmls1 = new ArrayList<>();
            avarableArxmls1.add(arxml1);

            Set<Record> records1 = Artags.createTagsString(arxml1, avarableArxmls1);
            assertThat(records1.size(), is(6));

            for (Record record : records1) {
                if (record.getSymbol().equals("sint8")) {
                    assertThat(record.getSearchStr(), is("7"));
                } else if (record.getSymbol().equals("ImplDataType")) {
                    assertThat(record.getSearchStr(), is("20"));
                } else if (record.getSymbol().equals("Interface")) {
                    assertThat(record.getSearchStr(), is("36"));
                } else if (record.getSymbol().equals("Runnable")) {
                    assertThat(record.getSearchStr(), is("72"));
                } else if (record.getSymbol().equals("Port")) {
                    assertThat(record.getSearchStr(), is("55"));
                } else if (record.getSymbol().equals("Operation")) {
                    assertThat(record.getSearchStr(), is("40"));
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
    public void testCreateTagsString_ヘッダ直後のコメント() {
        try {
            Arxml arxml1 = new Arxml("./src/test/resources/contents_variation/01_comments.arxml");
            List<Arxml> avarableArxmls1 = new ArrayList<>();
            avarableArxmls1.add(arxml1);

            Set<Record> records1 = Artags.createTagsString(arxml1, avarableArxmls1);
            assertThat(records1.size(), is(6));

            for (Record record : records1) {
                if (record.getSymbol().equals("sint8")) {
                    assertThat(record.getSearchStr(), is("15"));
                } else if (record.getSymbol().equals("ImplDataType")) {
                    assertThat(record.getSearchStr(), is("28"));
                } else if (record.getSymbol().equals("Interface")) {
                    assertThat(record.getSearchStr(), is("44"));
                } else if (record.getSymbol().equals("Runnable")) {
                    assertThat(record.getSearchStr(), is("80"));
                } else if (record.getSymbol().equals("Port")) {
                    assertThat(record.getSearchStr(), is("63"));
                } else if (record.getSymbol().equals("Operation")) {
                    assertThat(record.getSearchStr(), is("48"));
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

    @Before
    public void setup() {
    }

    @After
    public void tearDown() {
    }
}

