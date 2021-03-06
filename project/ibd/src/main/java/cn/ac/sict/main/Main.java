package cn.ac.sict.main;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaPairReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import cn.ac.sict.hbase.spark.dao.HBaseSparkDAO;
import cn.ac.sict.signal.TemperSignal;
import cn.ac.sict.source.KafkaStreamSource;
import cn.ac.sict.store.Store;
import cn.ac.sict.vis.Site;
import cn.ac.sict.vis.VisMap;

public class Main {

	public static void main(String[] args) throws InterruptedException, IOException {
		if (args.length != 3) {
			System.err.println("Usage: Main <topics> <numThreads> <interval>");
			System.exit(1);
		}

		Properties configProps = new Properties();
		try {
			configProps.load(Main.class.getClassLoader().getResourceAsStream("sysConfig.properties"));
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("ERROR 1: no Config file.");
			return;
		}

		// 构建 Streaming Context 对象, 1 second batch size
		SparkConf sparkConf = new SparkConf().setAppName("Spark").set("spark.serializer",
				"org.apache.spark.serializer.KryoSerializer");
		JavaSparkContext jsc = new JavaSparkContext(sparkConf);
		JavaStreamingContext jssc = new JavaStreamingContext(jsc, new Duration(Integer.valueOf(args[2])));

		// 从 kafka 消息源获取数据
		JavaPairReceiverInputDStream<String, String> source = KafkaStreamSource.createStringSource(jssc,
				configProps.getProperty("zkQuorum"), configProps.getProperty("consumeGroup"), args[0],
				Integer.valueOf(args[1]));

		// 运行 wordCount demo
		// cn.ac.sict.example.WordCount.wordCount(source);

		// store
		Store.toHBase(HBaseSparkDAO.getDao(jsc), configProps.getProperty("hbase_tableName"),
				configProps.getProperty("hbase_tableColumnFamily"), source, TemperSignal.class);

		// vis
		Site site = new Site();
		List<Integer> list = site.getList();
		VisMap.getKafkaValue(source, list);

		// 启动 Spark Streaming
		jssc.start();
		jssc.awaitTermination();
	}

}
