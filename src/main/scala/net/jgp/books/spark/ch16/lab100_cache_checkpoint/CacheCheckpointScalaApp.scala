package net.jgp.books.spark.ch16.lab100_cache_checkpoint

import java.util.List
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{Dataset, Row, SparkSession}
import scala.collection.JavaConversions._
import net.jgp.books.spark.ch16.lab100_cache_checkpoint.Mode._
/**
  * Measuring performance without cache, with cache, and with checkpoint.
  *
  * @author rambabu.posa
  */
object CacheCheckpointScalaApp {

  /**
    * main() is your entry point to the application.
    *
    * @param args
    */
  def main(args: Array[String]): Unit = {

    // Creates a session on a local master
    val spark = SparkSession.builder
      .appName("Example of cache and checkpoint")
      .master("local[*]")
      .config("spark.executor.memory", "70g")
      .config("spark.driver.memory", "50g")
      .config("spark.memory.offHeap.enabled", true)
      .config("spark.memory.offHeap.size", "16g")
      .getOrCreate()
    
    spark.sparkContext.setCheckpointDir("/tmp")

    // Specify the number of records to generate
    val recordCount = 500000

    // Create and process the records without cache or checkpoint
    val t0 = processDataframe(recordCount, NoCacheNoCheckPoint, spark)

    // Create and process the records with cache
    val t1 = processDataframe(recordCount, Cache, spark)

    // Create and process the records with a checkpoint
    val t2 = processDataframe(recordCount, CheckPoint, spark)

    val t3 = processDataframe(recordCount, CheckPointNonEager, spark)

    println("\nProcessing times")
    println("Without cache ............... " + t0 + " ms")
    println("With cache .................. " + t1 + " ms")
    println("With checkpoint ............. " + t2 + " ms")
    println("With non-eager checkpoint ... " + t3 + " ms")
  }

  def processDataframe(recordCount: Int, mode: Mode, spark: SparkSession): Long = {
    val df: Dataset[Row] = RecordGeneratorScalaUtils.createDataframe(spark, recordCount)
    val t0: Long = System.currentTimeMillis
    var topDf: Dataset[Row] = df.filter(col("rating").equalTo(5))
    mode match {
      case Cache =>
        topDf = topDf.cache
      case CheckPoint =>
        topDf = topDf.checkpoint
      case CheckPointNonEager =>
        topDf = topDf.checkpoint(false)
      case NoCacheNoCheckPoint =>
        topDf = topDf
    }
    val langDf: List[Row] = topDf.groupBy("lang").count.orderBy("lang").collectAsList
    val yearDf: List[Row] = topDf.groupBy("year").count.orderBy(col("year").desc).collectAsList
    val t1: Long = System.currentTimeMillis
    println("Processing took " + (t1 - t0) + " ms.")
    println("Five-star publications per language")

    for (r <- langDf) {
      println(r.getString(0) + " ... " + r.getLong(1))
    }
    println("\nFive-star publications per year")

    for (r <- yearDf) {
      println(r.getInt(0) + " ... " + r.getLong(1))
    }
    t1 - t0
  }

}
