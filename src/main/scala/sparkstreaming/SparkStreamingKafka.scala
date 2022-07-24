package sparkstreaming

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.hadoop.hbase.client.{ColumnFamilyDescriptorBuilder, Connection, ConnectionFactory, Put, Table, TableDescriptor, TableDescriptorBuilder}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.{Seconds, StreamingContext}

object SparkStreamingKafka {

  // https://stackoverflow.com/questions/25250774/writing-to-hbase-via-spark-task-not-serializable

  def main(args: Array[String]): Unit = {

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "localhost:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "SparkStreamingKafka",
      "auto.offset.reset" -> "latest"
    )

    val topics = Array("java")
    val sparkConf = new SparkConf().setMaster("local[*]").setAppName("SparkStreamingKafkaApp")
    val streamingContext = new StreamingContext(sparkConf, Seconds(1))

    val kafkaStream = KafkaUtils.createDirectStream[String, String](
      streamingContext,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )
    streamingContext.sparkContext.setLogLevel("ERROR")

    // HBase connection
    val conf: Configuration = HBaseConfiguration.create()
    conf.set("hbase.zookeeper.quorum", "localhost")
    val c: Connection = ConnectionFactory.createConnection(conf)

    // HBase create namespace
    //val namespace: NamespaceDescriptor = NamespaceDescriptor.create("ns_test").build()
    //c.getAdmin().createNamespace(namespace)

    // HBase table creation
    val tn: TableName = TableName.valueOf("table_test2")
    val tdb: TableDescriptorBuilder = TableDescriptorBuilder.newBuilder(tn)
    tdb.setColumnFamily(
      ColumnFamilyDescriptorBuilder.newBuilder("user".getBytes).build()
    )
    val td: TableDescriptor = tdb.build()
    c.getAdmin.createTable(td)

    // HBase connection to table
    //val tableName: TableName = TableName.valueOf("table_test2")
    //val table: Table = c.getTable(tableName)

    //table.close()
    c.close()

    val result = kafkaStream.map(record => (record.key(), record.value()))

    result.foreachRDD(x => {
      val tuple = x.map(x => {
        val elements = x._2.split(",")
        val id = x._1
        val name = elements(0)
        val city = elements(1)
        val age = elements(2)

        (id, name, city, age)
      })

      tuple.foreachPartition(x => {
        // HBase connection
        val conf: Configuration = HBaseConfiguration.create()
        conf.set("hbase.zookeeper.quorum", "localhost")
        val c: Connection = ConnectionFactory.createConnection(conf)

        // HBase connection to table
        val tableName: TableName = TableName.valueOf("table_test2")
        val table: Table = c.getTable(tableName)

        x.foreach(x => {
          // x = ["id", "name", "city", "age"]
          val put: Put = new Put(Bytes.toBytes(x._1))

          put.addColumn(Bytes.toBytes("user"), Bytes.toBytes("name"), Bytes.toBytes(x._2))
          put.addColumn(Bytes.toBytes("user"), Bytes.toBytes("city"), Bytes.toBytes(x._3))
          put.addColumn(Bytes.toBytes("user"), Bytes.toBytes("age"), Bytes.toBytes(x._4))

          table.put(put)
          println("user added")
        })

        table.close()
        c.close()
      })
    })

    streamingContext.start()
    streamingContext.awaitTermination()
  }

}
