package jp.dip.oyasirazu.artags;

import java.io.IOException;
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
        List<Arxml> arxmls = Artags.findArxmls(options.getTargetDirectories());

        // 主処理
        // arxml リストを一つずつ読み込み、タグファイルのレコードを作成する。
        Set<Record> allRecords = new HashSet<Record>();
        for (Arxml arxml : arxmls) {
            Set<Record> tags = Artags.createTagsString(arxml, arxmls);
            allRecords.addAll(tags);
        }

        // タグレコードを出力
        // TODO: ファイル出力する
        for (Record record : allRecords) {
            System.out.println(record.buildRecordString());
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

        @Argument(required = true)
        private List<String> targetDirectories;
    }
}

