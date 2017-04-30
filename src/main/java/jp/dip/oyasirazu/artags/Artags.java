package jp.dip.oyasirazu.artags;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Artags の機能を提供するクラス。
 */
public class Artags {

    private static XPath xpath;

    static {
        XPathFactory xpathfactory = XPathFactory.newInstance();
        xpath = xpathfactory.newXPath();
    }

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

        List<Arxml> arxmls;

        if (excludePattern == null) {
            arxmls = findArxmls(baseDirectories);
        } else {
            arxmls = findArxmlsWithExclude(baseDirectories, excludePattern);
        }

        return arxmls;
    }

    /**
     * 指定したディレクトリ以下の arxml ファイルを検索します。
     *
     * @param baseDirectories arxml ファイルを探すディレクトリのリスト
     *
     * @return 指定したディレクトリ以下の arxml インスタンスのリスト
     */
    public static List<Arxml> findArxmls(List<String> baseDirectories)
            throws IOException {

        // 指定されたディレクトリ以下の arxml ファイルを抽出し、 arxmls に格納
        List<Arxml> arxmls = new ArrayList<>();
        for (String baseDirectory : baseDirectories) {

            // 今回のループで指定されたディレクトリ以下の arxml ファイルを抽出
            Files.walkFileTree(Paths.get(baseDirectory),
                new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attr) throws IOException {

                        // arxml ファイルであれば、リストに追加する
                        if (file.toString().endsWith("arxml")) {
                            try {
                                arxmls.add(new Arxml(file.toString(), createDocument(file)));
                            } catch (SAXException|ParserConfigurationException e) {
                                // TODO: エラー処理をまじめに考える
                                System.err.println(e);
                            }
                        }

                        return FileVisitResult.CONTINUE;
                    }
                });
        }

        return arxmls;
    }

    /**
     * 指定したディレクトリ以下の arxml ファイルを検索します。
     *
     * @param baseDirectories arxml ファイルを探すディレクトリのリスト
     * @param excludePattern 除外するパスの正規表現パターン文字列
     *
     * @return 指定したディレクトリ以下の arxml インスタンスのリスト
     */
    public static List<Arxml> findArxmlsWithExclude(List<String> baseDirectories, String excludePattern)
            throws IOException {

        // 除外ディレクトリ用のパターンをコンパイルする
        Pattern pattern = Pattern.compile(excludePattern);

        // 指定されたディレクトリ以下の arxml ファイルを抽出し、 arxmls に格納
        List<Arxml> arxmls = new ArrayList<>();
        for (String baseDirectory : baseDirectories) {

            // 今回のループで指定されたディレクトリ以下の arxml ファイルを抽出
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
                            Path file,
                            BasicFileAttributes attr) throws IOException {

                        // excludePattern にマッチしたら、このファイルはスキップ
                        Matcher m = pattern.matcher(file.toString());
                        boolean isMatch = m.matches();
                        if (isMatch) {
                            return FileVisitResult.CONTINUE;
                        }

                        // arxml ファイルであれば、リストに追加する
                        if (file.toString().endsWith("arxml")) {
                            try {
                                arxmls.add(new Arxml(file.toString(), createDocument(file)));
                            } catch (SAXException|ParserConfigurationException e) {
                                // TODO: エラー処理をまじめに考える
                                System.err.println(e);
                            }
                        }

                        return FileVisitResult.CONTINUE;
                    }
                });
        }

        return arxmls;
    }


    /**
     * 指定された arxml ファイルから、参照先を設定しているノードを探す。
     *
     * @param arxml 参照先を設定しているノードを探したい arxml ファイルインスタンス
     *
     * @return 参照先を設定しているノードインスタンスのリスト
     */
    private static List<Node> searchRefNodes(Arxml arxml) throws XPathExpressionException {

        // get reference node list
        NodeList refNodeList = (NodeList)xpath.evaluate(
                "//*[@DEST]",
                arxml.getElement(),
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
        HashSet<Record> tags = new HashSet<>();
        for (Node n : refNodes) {
            String arHierarchyPath = n.getTextContent();
            String[] arHierarchyArray = arHierarchyPath.split("/");
            String symbol = arHierarchyArray[arHierarchyArray.length - 1];

            // 実体を探すための XPath 式を組み立てる
            String entitySearchXPath = buildEntitySearchXPath(arHierarchyPath);

            // 全 arxml に対して直前で組み立てた XPath 式を使って実体を取得
            for (Arxml arxml : avarableArxmls) {
                NodeList targetNodeList = (NodeList)xpath.evaluate(
                        entitySearchXPath,
                        arxml.getElement(),
                        XPathConstants.NODESET);

                // 実体が見つかったら返却用 Set に詰め込む
                int targetNodeSize = targetNodeList.getLength();
                for (int i = 0; i < targetNodeSize; i++) {
                    tags.add(new Record(
                            symbol,
                            arxml.getFilePath(),
                            convertDomToString(targetNodeList.item(i), symbol),
                            targetNodeList.item(i).getNodeName(),
                            arHierarchyPath));
                }
            }
        }

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
     * DOM ノードを XML 文字列に変換する。
     *
     * @param node XML 文字列に変換したいノードインスタンス
     * @param symbol 
     *
     * TODO: 引数の symbol を無くす。そもそも名が体を表していない処理になってる。
     *       ここでは XML 文字列に直すだけにしないとダメですね。
     */
    private static String convertDomToString(Node node, String symbol) throws TransformerException {
        StringWriter sw = new StringWriter();
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer transformer = tfactory.newTransformer();

        // XML Header disable.
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        transformer.transform(
                new DOMSource(node), new StreamResult(sw));

        // TODO: この辺ちゃんと整理したいですねー
        String xmlString = sw.toString();
        xmlString = xmlString.substring(0, (xmlString.length() < 256 ? xmlString.length() : 256));
        xmlString = xmlString.replace("<", "\\_s\\{-\\}<");
        xmlString = xmlString.replace(">", ">\\_s\\{-\\}");
        xmlString = xmlString.substring("\\_s\\{-\\}".length(), xmlString.lastIndexOf("\\_s\\{-\\}") - 1);
        xmlString = xmlString.replace("/", "\\/");
        xmlString = xmlString.replace("\t", "");
        xmlString = xmlString.replace("\n", "");
        xmlString = xmlString.replace("\r", "");
        xmlString = xmlString.replace("\\_s\\{-\\}\\_s\\{-\\}", "\\_s\\{-\\}");
        xmlString = xmlString.substring(xmlString.indexOf(symbol), xmlString.length());

        return "/" + xmlString + "/";
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
     * タグファイルの 1 レコードを表すクラス。
     */
    // SYMBO\\t.\\PATH\\TO\\FILE\\tSEARCH_STR/;"\\tTYPE\\tAR_HIERARCHY_PATH\\tfile:
    @AllArgsConstructor
    @Data
    public static class Record {
        private String symbol;
        private String filePath;
        private String searchStr;
        private String type;
        private String arHierarchyPath;

        /**
         * タグファイルの 1 レコードとして出力する文字列を組み立てる。
         *
         * @return タグファイルの 1 レコードとして出力する文字列
         */
        public String buildRecordString() {
            return symbol + "\t" + filePath + "\t" + searchStr + ";\"\t\t" + arHierarchyPath + " (" + type + ")\tfile:";
        }
    }

    /**
     * Artags が扱う arxml ファイルを表すクラス。
     */
    @AllArgsConstructor
    @Data
    public static class Arxml {
        private String filePath;
        private Document element;
    }
}

