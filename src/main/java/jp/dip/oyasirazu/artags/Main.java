package jp.dip.oyasirazu.artags;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import jp.dip.oyasirazu.artags.Artags.Arxml;
import jp.dip.oyasirazu.artags.Artags.Record;

import lombok.Data;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.xml.sax.SAXException;

/**
 * Main
 *
 * TODO: コマンドの使い方を説明
 */
public class Main {

    private static final String CHARSET_DEFAULT = "UTF-8";
    private static final String OUTPUT_FILE_PATH_DEFAULT = "./tags";

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

        // ライセンス判定
        if (options.isLicense()) {
            printLicense();
            System.exit(0);
        }

        // Charset 設定
        String charset = options.getCharset();
        if (charset == null) {
            charset = CHARSET_DEFAULT;
        }

        // Exclude 設定
        String excludePattern = options.getExcludePattern();

        // 出力先設定
        String outputFilePathStr = options.getOutputFilePathStr();
        if (outputFilePathStr == null) {
            outputFilePathStr = OUTPUT_FILE_PATH_DEFAULT;
        }

        // 出力先ファイルの妥当性確認
        Path outputFilePath = Paths.get(outputFilePathStr);
        Path outputDirPath = outputFilePath.toAbsolutePath().getParent();

        if (outputDirPath == null) {
            throw new IllegalArgumentException("OUTPUT_FILE is invalid : " + outputFilePathStr);
        }

        // アペンドモードを判定してオプション配列を生成
        StandardOpenOption[] openOptions;
        if (options.isAppend()) {
            openOptions = new StandardOpenOption[]{
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.WRITE};
        } else {
            openOptions = new StandardOpenOption[]{
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE};
        }

        // ファイルを開いて主処理を開始する
        // (主処理に時間がかかるので、先にファイルオープンを試行して
        //  だめならすぐエラーが返るように配慮)
        try (BufferedWriter bw = Files.newBufferedWriter(
                    Paths.get(outputFilePathStr),
                    Charset.forName(charset),
                    openOptions)) {

            // 指定されたディレクトリ以下の arxml ファイル一覧を取得する
            List<Arxml> arxmls = Artags.findArxmls(options.getTargetDirectories(), excludePattern);

            // 主処理
            // arxml リストを一つずつ読み込み、タグファイルのレコードを作成する。
            Set<Record> allRecords = new HashSet<Record>();
            for (Arxml arxml : arxmls) {
                Set<Record> tags = Artags.createTagsString(arxml, arxmls);
                allRecords.addAll(tags);
            }

            // タグレコードを出力
            for (Record record : allRecords) {
                bw.write(record.buildRecordString(outputDirPath) + "\n");
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

    private static void printLicense() throws IOException {
        // ライセンスを表示
        try (
            BufferedReader br = new BufferedReader(
                new InputStreamReader(
                    Main.class.getResourceAsStream("/LICENSE"), "UTF-8"));
        ) {
            String line = br.readLine();
            while (line != null) {
                System.out.println(line);
                line = br.readLine();
            }
        }
    }

    @Data
    static class Options {

        @Option(name = "-h", aliases = "--help", usage = "print help.")
        private boolean isHelp;

        @Option(name = "-a", aliases = "--append", usage = "append mode.")
        private boolean isAppend;

        @Option(name = "-o", aliases = "--output", usage = "output file path.(default: " + OUTPUT_FILE_PATH_DEFAULT + ")", metaVar = "OUTPUT_FILE")
        private String outputFilePathStr;

        @Option(name = "-c", aliases = "--charset", usage = "output file charset.(default:" + CHARSET_DEFAULT + ")", metaVar = "OUTPUT_FILE_CHARSET")
        private String charset;

        @Option(name = "-e", aliases = "--exclude", usage = "exclude path pattern.", metaVar = "ExCLUDE_PATH_PATTERN")
        private String excludePattern;

        @Option(name = "--license", usage = "print license.")
        private boolean license;

        @Argument
        private List<String> targetDirectories;
    }
}

