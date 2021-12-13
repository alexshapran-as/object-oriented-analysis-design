package object_oriented_analysis_design.dao

import com.mongodb
import com.mongodb.casbah
import com.mongodb.casbah.Imports.{DBObject, _}
import com.mongodb.casbah.commons.MongoDBObject
import object_oriented_analysis_design.MSA
import object_oriented_analysis_design.configurations.Conf
import object_oriented_analysis_design.util.Utils
import org.slf4j.{Logger, LoggerFactory}

object MongoUtils {
  protected val logger: Logger = LoggerFactory.getLogger(getClass)

  private var currentConnection: MongoClient = null

  private def getMongoAddresses(nameDaoConfig: String): List[casbah.Imports.ServerAddress] = {
    Conf.getMongoAddressesByName(nameDaoConfig).map { tup =>
      val (host, port) = tup
      new ServerAddress(host, port)
    }
  }

  def getMongoDbConnection(nameDaoConfig: String): MongoDB = {
    currentConnection = (Conf.getMongoCredentialsByName(nameDaoConfig) match {
      case Some((login, pwd)) =>
        val credentials1 = MongoCredential.createScramSha1Credential(
          login, Conf.getMongoStorageName(nameDaoConfig), pwd.toCharArray
        )
        MongoClient(getMongoAddresses(nameDaoConfig), List(credentials1))
      case None =>
        MongoClient(getMongoAddresses(nameDaoConfig))
    })
    currentConnection(Conf.getMongoStorageName(nameDaoConfig))
  }

  def closeMongoConnection() = {
    currentConnection.underlying.close()
  }


  def createIndex(coll: MongoCollection, indexes: MSA): Unit = {
    val indexesDBO = map2dbo(indexes)
    logger.info(s"Create indexes if not exists ${indexesDBO.toString} on ${coll.getName}: started")
    coll.createIndex(indexesDBO, DBObject("background" -> true))
    logger.info(s"Create indexes if not exists ${indexesDBO.toString} on ${coll.getName}: finished")
  }

  def createTTLIndex(coll: MongoCollection, indexes: MSA): Unit = {
    val indexesDBO = map2dbo(indexes)
    logger.info(s"Create indexes if not exists ${indexesDBO.toString} on ${coll.getName}: started")
    coll.createIndex(indexesDBO, DBObject("expireAfterSeconds" -> 0))
    logger.info(s"Create indexes if not exists ${indexesDBO.toString} on ${coll.getName}: finished")
  }

  def dbo2map(obj: DBObject): Map[String, Any] = {
    obj.map { item =>
      item._1 -> (item._2 match {
        case lst: Array[Int] => lst
        case lst: Array[Long] => lst
        case lst: Array[Map[String, Any]] => lst.toList
        case lst: mongodb.BasicDBList =>
          lst.map {
            case x: Int => x
            case x: Long => x
            case x: DBObject => dbo2map(x.asInstanceOf[DBObject])
            case x => x.toString
          }.toList
        case subObj: mongodb.DBObject => dbo2map(subObj)
        case other => other
      })
    }.toMap
  }

  def map2dbo(msa: Map[String, Any]): MongoDBObject = {
    val builder = MongoDBObject.newBuilder
    msa foreach { item =>
      val value = item._2 match {
        case bi: BigInt => bi.toLong
        case bd: BigDecimal => bd.toDouble
        case msa: Map[String, Any] => map2dbo(msa)
        case list: List[Any] => list.map {
          case bi: BigInt => bi.toLong
          case bd: BigDecimal => bd.toDouble
          case msa: Map[String, Any] => map2dbo(msa)
          case any => any
        }
        case other => other
      }
      builder += item._1 -> value
    }
    builder.result()
  }
}
