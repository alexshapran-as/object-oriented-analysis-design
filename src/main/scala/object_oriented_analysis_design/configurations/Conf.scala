package object_oriented_analysis_design.configurations

import com.typesafe.config.{Config, ConfigFactory}

object Conf {
  val conf: Config = ConfigFactory.load("company_service")
  val confSecretKey: String = conf.getString("conf.companyservice.secretKey")
  val confApiServiceInterface: String = conf.getString("conf.apiservice.interface")
  val confApiServicePort: Int = conf.getInt("conf.apiservice.port")
  // DAO configs by Name - main, ...
  def getMongoStorageName(name: String) = conf.getString("conf.mongo." + name + ".name")
  def getMongoPortByName(name: String) = conf.getInt("conf.mongo." + name + ".port")
  def getMongoAddressesByName(name: String) = conf.getString("conf.mongo." + name + ".addresses").split(";").toList.map(x => (x, getMongoPortByName(name)))
  def getMongoCredentialsByName(name: String) =
    if (conf.getBoolean("conf.mongo." + name + ".auth"))
      Some((conf.getString("conf.mongo." + name + ".login"), conf.getString("conf.mongo." + name + ".pass")))
    else
      None
}
