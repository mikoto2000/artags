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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
     * 指定された arxml ファイルから、参照先を設定しているノードを探す。
     *
     * @param arxml 参照先を設定しているノードを探したい arxml ファイルインスタンス
     *
     * @return 参照先を設定しているノードインスタンスのリスト
     */
    private static List<Node> searchRefNodes(Arxml arxml) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        // get reference node list
        NodeList refNodeList = (NodeList)xpath.evaluate(
                "//*[@DEST]",
                createDocument(arxml.getFilePath()),
                XPathConstants.NODESET);

        // move NodeList to ArrayList<Node>.
        int refNodeListSize = refNodeList.getLength();
        ArrayList<Node> refNodes = new ArrayList<>(refNodeListSize);
        for (int i = 0; i < refNodeListSize; i++) {
            refNodes.add(refNodeList.item(i));
        }

        return refNodes;
    }

    /**
     * タグファイルのレコードを生成する。
     *
     * @param referredArxml 解析対象の arxml インスタンス。(参照の定義をしているほう)
     * @param avarableArxmls 実体を探す対象の arxml インスタンスのリスト
     *
     * @return 解析結果のタグファイルレコードのセット
     */
    public static Set<Record> createTagsString(
            Arxml referredArxml,
            List<Arxml> avarableArxmls)
            throws SAXException,
                    XPathExpressionException,
                    TransformerException,
                    ParserConfigurationException,
                    IOException {

        // 参照しているノードのリストを取得する
        List<Node> refNodes = searchRefNodes(referredArxml);

        // avarableArxmls から、 refNode の実体を探す
        Set<Record> tags = refNodes.parallelStream()
            .map((n) -> searchNodeElementFromAvarableArxmls(n, avarableArxmls))
            .flatMap(v -> v.stream()).collect(Collectors.toSet());

        return tags;
    }


    /**
     * arHierarchyPath から実体のエレメントを探すための XPath 式を組み立てる。
     *
     * @param arHierarchyPath 参照文字列
     *
     * @return XPath 式文字列
     */
    private static String buildEntitySearchXPath(String arHierarchyPath) {
        List<String> splitedArHierarchyPath = Arrays.asList(arHierarchyPath.split("/"));

        StringBuilder sb = new StringBuilder();
        sb.append("//SHORT-NAME[text()=\"");

        int depth = splitedArHierarchyPath.size();
        for (int i = 1; i < depth - 1; i++) {
            sb.append(splitedArHierarchyPath.get(i));
            sb.append("\"]/..//SHORT-NAME[text()=\"");
        }
        sb.append(splitedArHierarchyPath.get(depth - 1));
        sb.append("\"]/..");

        return sb.toString();
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
        return builder.parse(arxmlFilePath.toString());
    }

    /**
     * 指定された参照元ノードに記載されている参照先ノードを探し、
     * ctags のレコード形式にして記録する。
     *
     * @param n 参照元ノード
     * @param avarableArxmls 参照先を探す対象の ARXML ファイルリスト
     *
     * @return ctags のレコードを表すオブジェクトのセット
     */
    public static Set<Record> searchNodeElementFromAvarableArxmls(Node n,
            List<Arxml> avarableArxmls) {
        String arHierarchyPath = n.getTextContent();
        String[] arHierarchyArray = arHierarchyPath.split("/");
        String symbol = arHierarchyArray[arHierarchyArray.length - 1];

        // 実体を探すための XPath 式を組み立てる
        String entitySearchXPath = buildEntitySearchXPath(arHierarchyPath);

        // 全 arxml に対して直前で組み立てた XPath 式を使って実体を取得
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        Set<Record> t = new HashSet<>();
        for (Arxml arxml : avarableArxmls) {
            try {

                // prolog のテキストが無視されてしまい行数が特定できないので、
                // prolog だけテキストとして読み込んで改行文字を数える。
                long prologLines = getPrologLineNumber(arxml);

                NodeList targetNodeList = (NodeList)xpath.evaluate(
                            entitySearchXPath,
                            createDocument(arxml.getFilePath()),
                            XPathConstants.NODESET);

                // 実体が見つかったら返却用 Set に詰め込む
                int targetNodeSize = targetNodeList.getLength();
                for (int i = 0; i < targetNodeSize; i++) {
                    Node targetNode = targetNodeList.item(i);
                    String lineNumber = String.valueOf(
                            prologLines + getLineNumber(targetNode));
                    t.add(new Record(
                            symbol,
                            arxml.getFilePath(),
                            lineNumber,
                            targetNode.getNodeName(),
                            arHierarchyPath));
                }
            } catch (ParserConfigurationException
                    |SAXException
                    |XPathExpressionException
                    |IOException e) {
                throw new RuntimeException("filePath : " + arxml.getFilePath() + ".", e);
            }
        }
        return t;
    }

    /**
     * ARXML ファイルのプロローグの行数を取得する。
     *
     * @param 対象の ARXML ファイル
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
         * タグファイルに出力するパスを、 basePath からの相対パスにする。
         *
         * @param basePath タグファイルに出力するパスを、この basePath からの相対パスにする
         * @return タグファイルの 1 レコードとして出力する文字列
         */
        public String buildRecordString(Path basePath) {
            return symbol + "\t" + basePath.getParent().relativize(filePath).toString() + "\t" + searchStr + ";\"\t\t" + arHierarchyPath + " (" + type + ")\tfile:";
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

