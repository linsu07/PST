/**
  * @author linsu
  *         2020/5/4 youth day, sencond algorithm
  */
package org.apache.spark.ml.algo
import com.typesafe.config.Config
import org.apache.commons.math3.fitting.leastsquares.ParameterValidator
import org.apache.spark.ml.Estimator
import org.apache.spark.ml.algo.cluster.PST
import org.apache.spark.ml.param.{BooleanParam, DoubleArrayParam, IntParam, ParamMap, Params, StringArrayParam}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.{Dataset, Row, SaveMode, SparkSession}
import org.apache.spark.sql.types.StructType
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.ml.param.shared.{HasInputCols, HasOutputCols}

import scala.collection.JavaConverters._
import org.apache.spark.sql.functions._

import scala.collection.mutable.ListBuffer
trait MarkovChainParam extends Params with HasInputCols with HasOutputCols{
  val minCount = new IntParam(this,"minCount","prune the branch if less than this number")
  val vocSize = new IntParam(this,"vocSize",doc = "the elements number of the sequences")

  val treeDepth = new IntParam(this, "treeDepth","the depth of the suffix three")
  val  prioriProbability = new DoubleArrayParam(this,"prioriProbability","prioriProbability")
  setDefault(minCount->20,treeDepth->8,vocSize->10,prioriProbability->Array(0.5,0.5)
    )

  def getTreeDepth()={
    ${treeDepth}
  }
}

class MarkovChain(override val uid:String) extends Estimator[MarkovChainModel]  with MarkovChainParam {
  def this() = this(Identifiable.randomUID("MarkovChain"))
  var voc:Array[String] = null

  def fit(dataset: Dataset[_]): MarkovChainModel = {
    // tree is the suffix tree learned from sequence database
    // inputCols(0) is where the sequence database exists
    // this is a distribute process to build a suffix tree
    val tree = dataset.select(${inputCols}(0)).rdd.aggregate(new PST(${vocSize},depth=${treeDepth}))(
      (tree:PST,row:Row)=>{
        val actions = row.getAs[Seq[Int]](${inputCols}(0))
        val buf = new ListBuffer[Int]()
        buf.appendAll(actions)
        // add sequence statistics into the tree of current segment
        tree.build(buf)
      },
      (tree1:PST,tree2:PST)=>{
        // merge small tree to a bigger one.
        tree1.merge(tree2)
      }
    )
    // make probabilities calculations and prune small branches.
    tree.rebuildAll(${minCount})
    val m = copyValues(new MarkovChainModel(uid))
    m.tree = tree
    set(this.prioriProbability->tree.nextActionsCount)
    println(tree.nextActionsCount.mkString(","))
    m
  }

  override def copy(extra: ParamMap): Estimator[MarkovChainModel] =  {
    defaultCopy(extra)
  }

  override def transformSchema(schema: StructType): StructType = ???
}

object MarkovChain{
  def main(args: Array[String]): Unit = {
    //    val t = new StatTree(3)
    //    print(t)
    //    val actIds = new ListBuffer[Int]()
    //    //1000100110
    //    actIds.appendAll(Array(1,0,0,0,1,0,0,1,1,0))
    //    println(s"sequence : ${actIds.mkString(",")}")
    //    val root = new PST(actIds.size+10)
    //    val root1 = new PST(actIds.size+10)
    //    root.build(actIds)
    //    root1.build(actIds)
    //    root.merge(root1)
    //    root.rebuildAll(2)
    //    root.print()
    //    val spark = SparkSession.builder().appName("myapp").master("local").getOrCreate()
    //    root.save("/tmp/PSTexample/",spark)
    //    val tree = PST.load("/tmp/PSTexample/",spark)
    //    tree.print()
    //    spark.close()

    //    println(math.log(PST.epsilon))
  }
}