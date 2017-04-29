package jp.dip.oyasirazu.artags;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.xml.sax.SAXException;

import jp.dip.oyasirazu.artags.Artags.Arxml;
import jp.dip.oyasirazu.artags.Artags.Record;

import lombok.Data;

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
        try (BufferedWriter bw = Files.newBufferedWriter(
                    Paths.get(outputFilePathStr),
                    Charset.forName(charset),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
            for (Record record : allRecords) {
                bw.write(record.buildRecordString() + "\n");
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

    @Data
    static class Options {

        @Option(name = "-h", aliases = "--help", usage = "print help.")
        private boolean isHelp;

        @Option(name = "-o", aliases = "--output", usage = "output file path.(default: " + OUTPUT_FILE_PATH_DEFAULT + ")", metaVar = "OUTPUT_FILE")
        private String outputFilePathStr;

        @Option(name = "-c", aliases = "--charset", usage = "output file charset.(default:" + CHARSET_DEFAULT + ")", metaVar = "OUTPUT_FILE_CHARSET")
        private String charset;

        @Option(name = "-e", aliases = "--exclude", usage = "exclude path pattern.", metaVar = "ExCLUDE_PATH_PATTERN")
        private String excludePattern;

        @Argument(required = true)
        private List<String> targetDirectories;
    }
}

