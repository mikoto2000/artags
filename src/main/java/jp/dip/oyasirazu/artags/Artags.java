package jp.dip.oyasirazu.artags;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Artags の機能を提供するクラス。
 */
public class Artags {

    /**
     * プライベートコンストラクタ。
     */
    private Artags() { }

    /**
     * 指定したディレクトリ以下の arxml ファイルを検索します。
     *
     * @param baseDirectories arxml ファイルを探すディレクトリのリスト
     * @param excludePattern 除外するパスの正規表現パターン文字列(null の場合は除外を行わない)
     *
     * @return 指定したディレクトリ以下の arxml インスタンスのリスト
     */
    public static List<Arxml> findArxmls(List<String> baseDirectories, String excludePattern)
            throws IOException {

        // TODO: もっといい感じにできない？？？
        List<Arxml> arxmls = new ArrayList<>();
        if (excludePattern == null) {
            for (String baseDirectory : baseDirectories) {
                arxmls.addAll(findArxmls(baseDirectory));
            }
        } else {
            for (String baseDirectory : baseDirectories) {
                arxmls.addAll(findArxmlsWithExclude(baseDirectory, excludePattern));
            }
        }

        return arxmls;
    }

    /**
     * 指定したディレクトリ以下の arxml ファイルを検索します。
     *
     * @param baseDirectory arxml ファイルを探すディレクトリのリスト
     *
     * @return 指定したディレクトリ以下の arxml インスタンスのリスト
     */
    private static List<Arxml> findArxmls(String baseDirectory)
            throws IOException {

        // 指定されたディレクトリ以下の arxml ファイルを抽出し、 arxmls に格納
        List<Arxml> arxmls = new ArrayList<>();

        // 指定されたディレクトリ以下の arxml ファイルを抽出
        Files.walkFileTree(Paths.get(baseDirectory),
            new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(
                        Path filePath,
                        BasicFileAttributes attr) throws IOException {

                    // arxml ファイルであれば、リストに追加する
                    if (filePath.toString().endsWith("arxml")) {
                        arxmls.add(new Arxml(filePath));
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

        return arxmls;
    }

    /**
     * 指定したディレクトリ以下の arxml ファイルを検索します。
     *
     * @param baseDirectory arxml ファイルを探すディレクトリ
     * @param excludePattern 除外するパスの正規表現パターン文字列
     *
     * @return 指定したディレクトリ以下の arxml インスタンスのリスト
     */
    private static List<Arxml> findArxmlsWithExclude(String baseDirectory, String excludePattern)
            throws IOException {

        // 除外ディレクトリ用のパターンをコンパイルする
        Pattern pattern = Pattern.compile(excludePattern);

        // 指定されたディレクトリ以下の arxml ファイルを抽出し、 arxmls に格納
        List<Arxml> arxmls = new ArrayList<>();

        // 指定されたディレクトリ以下の arxml ファイルを抽出
        Files.walkFileTree(Paths.get(baseDirectory),
            new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(
                        Path dir,
                        BasicFileAttributes attrs) throws IOException {

                    // excludePattern にマッチしたら、サブツリーをスキップ
                    Matcher m = pattern.matcher(dir.toString());
                    boolean isMatch = m.matches();
                    if (isMatch) {
                        return FileVisitResult.SKIP_SUBTREE;
                    } else {
                        return FileVisitResult.CONTINUE;
                    }

                }

                @Override
                public FileVisitResult visitFile(
                        Path filePath,
                        BasicFileAttributes attr) throws IOException {

                    // excludePattern にマッチしたら、このファイルはスキップ
                    Matcher m = pattern.matcher(filePath.toString());
                    boolean isMatch = m.matches();
                    if (isMatch) {
                        return FileVisitResult.CONTINUE;
                    }

                    // arxml ファイルであれば、リストに追加する
                    if (filePath.toString().endsWith("arxml")) {
                        arxmls.add(new Arxml(filePath));
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

        return arxmls;
    }


    /**
     * 指定された arxml ファイルから、SHORT-NAME が設定された要素を探す。
     *
     * @param arxml SHORT-NAME が設定された要素を探したい arxml ファイルインスタンス
     *
     * @return SHORT-NAME が設定された要素のインスタンスのリスト
     */
    private static List<Node> searchShortNameContainers(Arxml arxml) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        // get reference node list
        NodeList targetNodeList = (NodeList)xpath.evaluate(
                "//SHORT-NAME/..",
                createDocument(arxml.getFilePath()),
                XPathConstants.NODESET);

        // move NodeList to ArrayList<Node>.
        int targetNodeListSize = targetNodeList.getLength();
        ArrayList<Node> targets = new ArrayList<>(targetNodeListSize);
        for (int i = 0; i < targetNodeListSize; i++) {
            targets.add(targetNodeList.item(i));
        }

        return targets;
    }

    /**
     * タグファイルのレコードを生成する。
     *
     * @param arxml 解析対象の arxml インスタンス。
     *
     * @return 解析結果のタグファイルレコードのセット
     */
    public static Set<Record> createTagsString(
            Arxml arxml)
            throws SAXException,
                    XPathExpressionException,
                    TransformerException,
                    ParserConfigurationException,
                    IOException {

        // 参照しているノードのリストを取得する
        List<Node> targets = searchShortNameContainers(arxml);

        // avarableArxmls から、 refNode の実体を探す
        Set<Record> tags = targets.stream()
            .map((n) -> nodeToRecord(arxml, n))
            .collect(Collectors.toSet());

        return tags;
    }

    /**
     * 指定されたノードからタグレコード情報を生成します。
     *
     * @param arxml ノードが定義されている arxml ファイル
     * @param targetNode タグレコードにするノード
     *
     * @return targetNode から生成されたタグレコード
     */
    private static Record nodeToRecord(Arxml arxml, Node targetNode) {
        try {
            String symbol = getShortName(targetNode);
            Path filePath = arxml.getFilePath();
            String searchStr = String.valueOf(getLineNumber(targetNode) + getPrologLineNumber(arxml));
            String type = targetNode.getNodeName();
            String arHierarchyPath = getArHierarcyPath(targetNode);

            Record record = new Record(
                    symbol,
                    filePath,
                    searchStr,
                    type,
                    arHierarchyPath);

            return record;
        } catch (XPathExpressionException|SAXException|ParserConfigurationException|IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 対象ノードの行番号を取得する。
     *
     * この行番号は、ルートノードのある行を 1 行目とした場合の行数である。
     * Artags#getPrologLineNumber の結果と足し合わせてファイルの行番号を取得する。
     *
     * @see Artags#getPrologLineNumber
     */
    private static long getLineNumber(Node node) {
        String beforeLineTextContents = buildBeforeLineTextContents(node);

        // 組み立てたテキスト内の改行コードを数える
        // 行番号は 1 オリジンなので +1.
        long count = 1;
        for (int i = 0; i < beforeLineTextContents.length(); i++) {
            if (beforeLineTextContents.charAt(i) == '\n') {
                count++;
            }
        }

        return count;
    }

    /**
     * 行番号を取得するため、自分より上(前)の行のテキストをすべて取得する。
     */
    private static String buildBeforeLineTextContents(Node node) {
        StringBuilder sb = new StringBuilder();

        Node parent = node.getParentNode();
        if (parent.getNodeType() == Node.DOCUMENT_NODE) {
            return sb.toString();
        }

        sb.append(buildBeforeLineTextContents(parent));

        Node loopTarget = node.getPreviousSibling();
        while (loopTarget != null) {
            sb.append(loopTarget.getTextContent());
            loopTarget = loopTarget.getPreviousSibling();
        }

        return sb.toString();
    }

    /**
     * 指定された arxml ファイルパスをパースし、 Document にして返却する。
     *
     * @param arxmlFilePath arxml ファイルパス
     *
     * @return 引数で指定された arxml ファイルのパース結果である Document インスタンス
     */
    private static Document createDocument(Path arxmlFilePath)
            throws SAXException,
                    ParserConfigurationException,
                    IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(arxmlFilePath.toFile());
    }

    /**
     * ARXML ファイルのプロローグの行数を取得する。
     *
     * @param arxml 対象の ARXML ファイル
     *
     * @return ARXML ファイルのプロローグの行数
     */
    private static long getPrologLineNumber(Arxml arxml)
            throws SAXException, ParserConfigurationException, IOException {

        Path arxmlFilePath = arxml.getFilePath();
        Document document = createDocument(arxmlFilePath);

        String docEncoding = document.getXmlEncoding();
        Charset charset;
        if (docEncoding != null) {
            charset = Charset.forName(docEncoding);
        } else {
            charset = Charset.forName("UTF-8");
        }

        // ファイルの先頭から "<AUTOSAR " が出現する行までの行数を数える
        Iterator<String> lines = Files.lines(arxmlFilePath, charset).iterator();
        long prologLines = 0;
        while (lines.hasNext()) {
            String line = lines.next();
            if (line.contains("<AUTOSAR ")) {
                break;
            }
            prologLines++;
        }

        return prologLines;
    }

    private static String getArHierarcyPath(Node node) throws XPathExpressionException {
        String shortName = getShortName(node);

        Node parentNode = node.getParentNode();
        if (parentNode != null) {
            if (shortName == "") {
                return getArHierarcyPath(parentNode);
            } else {
                return getArHierarcyPath(parentNode) + "/" + shortName;
            }
        } else {
            if (shortName == "") {
                return "";
            } else {
                return "/" + shortName;
            }
        }
    }

    private static String getShortName(Node node) throws XPathExpressionException {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        String shortName = xpath.evaluate(
                    "./SHORT-NAME/text()",
                    node);

        return shortName;
    }

    /**
     * タグファイルの 1 レコードを表すクラス。
     */
    // SYMBO\\t.\\PATH\\TO\\FILE\\tSEARCH_STR/;"\\tTYPE\\tAR_HIERARCHY_PATH\\tfile:
    @AllArgsConstructor
    @Data
    public static class Record {
        private String symbol;
        private Path filePath;
        private String searchStr;
        private String type;
        private String arHierarchyPath;

        /**
         * タグファイルの 1 レコードとして出力する文字列を組み立てる。
         *
         * @return タグファイルの 1 レコードとして出力する文字列
         */
        public String buildRecordString() {
            return symbol + "\t" + filePath.toString() + "\t" + searchStr + ";\"\t\t" + arHierarchyPath + " (" + type + ")\tfile:";
        }

        /**
         * タグファイルの 1 レコードとして出力する文字列を組み立てる。
         *
         * タグファイルに出力するパスを、 baseDirPath からの相対パスにする。
         *
         * @param baseDirPath タグレコードとして出力するパスを、この baseDirPath からの相対パスにする
         * @return タグファイルの 1 レコードとして出力する文字列
         */
        public String buildRecordString(Path baseDirPath) {
            Path relativePath = baseDirPath.toAbsolutePath().relativize(filePath.toAbsolutePath());
            return symbol + "\t" + relativePath.toString() + "\t" + searchStr + ";\"\t\t" + arHierarchyPath + " (" + type + ")\tfile:";
        }
    }

    /**
     * Artags が扱う arxml ファイルを表すクラス。
     */
    @AllArgsConstructor
    @Data
    public static class Arxml {
        private Path filePath;
    }
}

