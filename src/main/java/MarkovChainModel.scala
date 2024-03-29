/**
  * @author linsu
  *         2020/5/4 youth day, sencond algorithm
  */
//import com.typesafe.config.Config
package org.apache.spark.ml.algo
import org.apache.hadoop.fs.Path
import org.apache.spark.ml.algo.cluster.PST
import org.apache.spark.ml.{Model, algo, linalg}
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.util.{DefaultParamsReader, DefaultParamsWriter, MLReadable, MLReader, MLWritable, MLWriter}
import org.apache.spark.sql.{DataFrame, Dataset, Row, SaveMode}
import org.apache.spark.sql.functions.{col, struct, udf}
import org.apache.spark.sql.types.StructType

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class MarkovChainModel(override val uid: String)
  extends Model[MarkovChainModel]  with MLWritable with MarkovChainParam{
  var tree:PST=_
  var treeMap:Map[String,PST]=_
  def this()=this("MarkovChainModel")

  override def write: MLWriter = new MarkovChainModel.ModelWriter(this)

  override def copy(extra: ParamMap): MarkovChainModel = ???
  //  override def setConfig(c: Config): this.type = {
  //    super.setConfig(c)
  //    if(c.hasPath("minCount"))
  //      set(minCount->c.getInt("minCount"))
  //    if(c.hasPath("treeDepth"))
  //      set(treeDepth->c.getInt("treeDepth"))
  //    if(c.hasPath("vocSize"))
  //      set(vocSize->c.getInt("vocSize"))
  //    if(c.hasPath("prioriProbability"))
  //      set(prioriProbability->c.getDoubleList("prioriProbability").asScala.toArray.map(_.toDouble))
  //    if(c.hasPath("isStandard"))
  //      set(isStandard->c.getBoolean("isStandard"))
  //    this
  //  }

  def transform(dataset:Dataset[_]):DataFrame={
    val probs = tree.nextActionsCount
    val calProbability = udf{
      (actionIds:Seq[Int])=>
        var simularity = 1.0
        if(actionIds.size==0) // 为了意外情况
          math.log(PST.epsilon)
        else {
          for (i <- 1.to(actionIds.size)) {
            var tmp = new ListBuffer[Int]()
            val curList = actionIds.toArray.take(i)
            tmp.appendAll(curList.reverse)
            val head = tmp.remove(0)
            tmp = tmp.take(5)
            tmp.append(head)
            val score = tree.findCondiProb(tmp.toArray)
            val preProb = probs(head)
            val normScore = score/preProb
            //            println(s"${score} ${math.log(score)}")
            simularity = simularity*normScore
          }
          var finalScore = 1.0
          //算分这块有问题， 不能把start包括进来， 而且第一个action的条件概率肯定是1； 这些都要考虑
          if(actionIds.length>1)
            finalScore = math.pow(simularity,1.0/(actionIds.length.toDouble-1))
          finalScore
        }
    }
    dataset.withColumn(${outputCols}(0),calProbability(col(${inputCols}(0))))
  }

  override def transformSchema(schema: StructType): StructType = ???
}
object MarkovChainModel extends MLReadable[MarkovChainModel] {
  private[MarkovChainModel] class ModelWriter(instance: MarkovChainModel) extends MLWriter  {
    override protected def saveImpl(path: String): Unit = {
      // Save metadata and Params
      DefaultParamsWriter.saveMetadata(instance, path, sc)
      val dataPath = new Path(path,"stat")
      instance.tree.save(dataPath.toString,sparkSession)

    }
  }

  override def read: MLReader[MarkovChainModel] = new MarkovChainModel.ModelReader

  private class ModelReader extends MLReader[MarkovChainModel] {
    /** Checked against metadata when loading model */
    private val className = classOf[MarkovChainModel].getName
    override def load(path: String): MarkovChainModel = {
      val model = new DefaultParamsReader[MarkovChainModel].load(path)  // model 必须实现 this（uid:String）方法
      val dataPath = new Path(path,"stat").toString
      model.tree = PST.load(dataPath,sparkSession,model.getTreeDepth())
      model
    }
  }
}