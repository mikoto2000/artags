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

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Main
 */
public class Main {
    public static void main(String[] args)
            throws SAXException,
                    XPathExpressionException,
                    TransformerException,
                    ParserConfigurationException,
                    IOException {

        // コマンドライン引数パース
        Options options = new Options();
        CmdLineParser optionParser = new CmdLineParser(options);

        try {
            optionParser.parseArgument(args);
        } catch (CmdLineException e) {
            printUsage(optionParser);
            System.exit(1);
        }

        // ヘルプ判定
        if (options.isHelp()) {
            printUsage(optionParser);
            System.exit(0);
        }

        // 指定されたディレクトリ以下の arxml ファイル一覧を取得する
        List<Arxml> arxmls = findArxmls(options.getTargetDirectories());


        // 主処理
        // arxml リストを一つずつ読み込み、タグファイルのレコードを作成する。
        Set<Record> allRecords = new HashSet<Record>();
        for (Arxml arxml : arxmls) {
            Set<Record> tags = createTagsString(arxml, arxmls);
            allRecords.addAll(tags);
        }

        // タグレコードを出力
        // TODO: ファイル出力する
        for (Record record : allRecords) {
            System.out.println(record);
        }
    }

    private static void printUsage(CmdLineParser optionParser) {
        // Useage を表示
        System.out.println("Useage:\n"
                + "  Main [options] [SEARCH_DIRECTORIIES...]\n"
                + "\n"
                + "Options:");
        optionParser.printUsage(System.out);
    }

    public static Set<Record> createTagsString(
            Arxml referredArxml,
            List<Arxml> avarableArxmls)
            throws SAXException,
                    XPathExpressionException,
                    TransformerException,
                    ParserConfigurationException,
                    IOException {

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

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

    private static List<Arxml> findArxmls(List<String> baseDirectories)
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

    private static Document createDocument(Path filePath)
            throws SAXException,
                    ParserConfigurationException,
                    IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(filePath.toString());
    }

    private static List<Node> searchRefNodes(Arxml arxml) throws XPathExpressionException {

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

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

    @AllArgsConstructor
    @Data
    static class Arxml {
        private String filePath;
        private Document element;
    }

    // SYMBO\\t.\\PATH\\TO\\FILE\\tSEARCH_STR/;"\\tTYPE\\tAR_HIERARCHY_PATH\\tfile:
    @AllArgsConstructor
    @Data
    static class Record {
        private String symbol;
        private String filePath;
        private String searchStr;
        private String type;
        private String arHierarchyPath;

        @Override
        public String toString() {
            return symbol + "\t" + filePath + "\t" + searchStr + ";\"\t\t" + arHierarchyPath + " (" + type + ")\tfile:";
        }

    }

    @Data
    static class Options {

        @Option(name = "-h", aliases = "--help", usage = "print help.", metaVar = "PATH_TO_BASE_DIR")
        private boolean isHelp;

        @Argument(required = true)
        private List<String> targetDirectories;
    }
}

