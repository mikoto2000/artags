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

        ArrayList<Path> arxmls = new ArrayList<>();
        Files.walkFileTree(Paths.get(options.getTargetDirectories().get(0)),
            new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(
                        Path file,
                        BasicFileAttributes attr) throws IOException {

                    // arxml ファイルであれば、リストに追加する
                    if (file.toString().endsWith("arxml")) {
                        arxmls.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

        // 主処理
        // arxml リストを一つずつ読み込み、タグファイルを作成する。
        // TODO: xml ファイルをまたいだ参照も存在するのでそこを達成すること。
        for (Path arxml : arxmls) {
            Set<Record> tags = createTagsString(arxml.toString());

            for (Record r : tags) {
                System.out.println(r);
            }
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

    public static Set<Record> createTagsString(String filePath)
            throws SAXException,
                    XPathExpressionException,
                    TransformerException,
                    ParserConfigurationException,
                    IOException {
        Document doc = createDocument(filePath);

        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

        // get reference node list
        NodeList refNodeList = (NodeList)xpath.evaluate(
                "//*[@DEST]",
                doc,
                XPathConstants.NODESET);

        int refNodeListSize = refNodeList.getLength();
        ArrayList<Node> refNodes = new ArrayList<>(refNodeListSize);
        for (int i = 0; i < refNodeListSize; i++) {
            refNodes.add(refNodeList.item(i));
        }

        // get node entity of refNodes.
        HashSet<Record> tags = new HashSet<>();
        for (Node n : refNodes) {
            String arHierarchyPath = n.getTextContent();

            // build xpath.
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

            // search node, use xpath.
            NodeList targetNodeList = (NodeList)xpath.evaluate(
                    sb.toString(),
                    doc,
                    XPathConstants.NODESET);

            int targetNodeSize = targetNodeList.getLength();
            for (int i = 0; i < targetNodeSize; i++) {
                String symbol = splitedArHierarchyPath.get(depth - 1);
                tags.add(new Record(
                        symbol,
                        filePath,
                        convertDomToString(targetNodeList.item(i), symbol),
                        targetNodeList.item(i).getNodeName(),
                        arHierarchyPath));
            }
        }

        return tags;
    }

    private static Document createDocument(String filePath)
            throws SAXException,
                    ParserConfigurationException,
                    IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(filePath);
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

