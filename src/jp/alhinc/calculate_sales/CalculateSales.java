package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author trainee1276
 *
 */
public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String BRANCH＿FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String COMMODITY_FILE_NOT_EXIST = "商品定義ファイルが存在しません";
	private static final String BRANCH_FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String COMMODITY_FILE_INVALID_FORMAT = "商品定義ファイルのフォーマットが不正です";
	private static final String SERIAL_NUMBER_ERROR = "売上ファイル名が連番になっていません";
	private static final String FILE_ERROR_FORMAT = "のフォーマットが不正です";
	private static final String BRACH_CODE_ERROR = "の支店コードが不正です";
	private static final String COMMODITY_CODE_ERROR = "の商品コードが不正です";
	private static final String AMOUNT_OVER = "合計金額が10桁を超えました";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();

		Map<String, String> commodityNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		Map<String, Long> commoditySales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales, "^\\d{3}$", BRANCH＿FILE_NOT_EXIST,
				BRANCH_FILE_INVALID_FORMAT)) {
			return;
		}
		if (!readFile(args[0], FILE_NAME_COMMODITY_LST, commodityNames, commoditySales,
				"^[a-zA-Z0-9]{8}$", COMMODITY_FILE_NOT_EXIST, COMMODITY_FILE_INVALID_FORMAT)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		File[] files = new File(args[0]).listFiles();
		List<File> rcdFiles = new ArrayList<>();

		// ファイル配列から条件に該当するrcdファイルをリストへ格納
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile() && files[i].getName().matches("^\\d{8}\\.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}
		Collections.sort(rcdFiles);

		for (int i = 0; i < rcdFiles.size() - 1; i++) {
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));

			if (latter - former != 1) {
				System.out.println(SERIAL_NUMBER_ERROR);
				return;
			}
		}

		// 支店売上・商品売上の集計
		BufferedReader br = null;

		for (int i = 0; i < rcdFiles.size(); i++) {
			try {
				FileReader fr = new FileReader(rcdFiles.get(i));
				br = new BufferedReader(fr);
				List<String> saleData = new ArrayList<>();

				String line;
				while ((line = br.readLine()) != null) {
					saleData.add(line);
				}
				if (saleData.size() != 3) {
					System.out.println(rcdFiles.get(i) + FILE_ERROR_FORMAT);
					return;
				}
				if (!branchNames.containsKey(saleData.get(0))) {
					System.out.println(rcdFiles.get(i) + BRACH_CODE_ERROR);
					return;
				}
				if (!commodityNames.containsKey(saleData.get(1))) {
					System.out.println(rcdFiles.get(i) + COMMODITY_CODE_ERROR);
					return;
				}
				if (!saleData.get(2).matches("^\\d+$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				long fileSale = Long.parseLong(saleData.get(2));
				long branchSalesAmount = branchSales.get(saleData.get(0)) + fileSale;
				long commoditySalesAmount = commoditySales.get(saleData.get(1)) + fileSale;
				long[] salesAmounts = { branchSalesAmount, commoditySalesAmount };

				for (int j = 0; j < salesAmounts.length; j++) {
					if (salesAmounts[j] >= 10000000000L) {
						System.out.println(AMOUNT_OVER);
						return;
					}
				}
				branchSales.replace(saleData.get(0), salesAmounts[0]);
				commoditySales.replace(saleData.get(1), salesAmounts[1]);

			} catch (IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}

		// 支店別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}
		if (!writeFile(args[0], FILE_NAME_COMMODITY_OUT, commodityNames, commoditySales)) {
			return;
		}
	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> names,
			Map<String, Long> sales, String condition, String notExist, String invalidFormat) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			if (!file.exists()) {
				System.out.println(notExist);
				return false;
			}
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while ((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				String[] Items = line.split(",");

				if (Items.length != 2 || !Items[0].matches(condition)) {
					System.out.println(invalidFormat);
					return false;
				}
				names.put(Items[0], Items[1]);
				sales.put(Items[0], 0L);
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> Names,
			Map<String, Long> Sales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			for (String key : Names.keySet()) {
				bw.write(key + "," + Names.get(key) + "," + Sales.get(key));
				bw.newLine();
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

}