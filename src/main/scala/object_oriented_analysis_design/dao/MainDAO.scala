package object_oriented_analysis_design.dao

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.softwaremill.session.RefreshTokenData
import object_oriented_analysis_design.MSA
import object_oriented_analysis_design.api.web.UserSessionData
import object_oriented_analysis_design.authenticator.UserAuthData
import object_oriented_analysis_design.company.{Employee, StructuralUnit}
import object_oriented_analysis_design.dao.MongoUtils._
import org.slf4j.{Logger, LoggerFactory}

object MainDAO {
  protected val logger: Logger = LoggerFactory.getLogger(getClass)

  private[dao] val mainDb = getMongoDbConnection("main")

  protected val sessionDataColl = mainDb("session_data")
  protected val authDataColl = mainDb("auth_data")

  protected val employeesColl = mainDb("employees")
  protected val structuralUnitsColl = mainDb("structural_units")


  /*
   * User session data methods
   */

  def saveUserSessionData(selector: String, userSessionDataMSA: MSA): Unit =
    sessionDataColl.update("_id" $eq selector, map2dbo(userSessionDataMSA), upsert = true)

  def tryToGetUserSessionData(selector: String): Option[UserSessionData] =
    sessionDataColl.findOne("_id" $eq selector).map(x => UserSessionData.fromMSAToUserSessionData(dbo2map(x)))

  def tryToGetRefTokenData(selector: String): Option[RefreshTokenData[UserSessionData]] =
    sessionDataColl.findOne("_id" $eq selector).map(x => UserSessionData.fromMSAToRefTokenData(dbo2map(x)))

  def removeUserSessionData(selector: String): Unit =
    sessionDataColl.remove(MongoDBObject("_id" -> selector))

  /*
   * User auth data methods
   */

  def saveUserAuthData(username: String, userAuthDataMSA: MSA): Unit =
    authDataColl.update("username" $eq username, map2dbo(userAuthDataMSA), upsert = true)

  def tryToGetUserAuthData(username: String): Option[UserAuthData] =
    authDataColl.findOne("username" $eq username).map(x => UserAuthData.fromMSA(dbo2map(x)))

  def tryToGetAllUserAuthData: List[MSA] =
    authDataColl.find().map(x => dbo2map(x)).toList

  def removeUserAuthData(username: String): Unit =
    authDataColl.remove(MongoDBObject("username" -> username))

  /*
   * Employees methods
   */

  def saveEmployee(employee: Employee) =
    employeesColl.update("_id" $eq employee._id, employee.toMSA, upsert = true)

  def removeEmployee(employeeId: String) =
    employeesColl.remove("_id" $eq employeeId)

  def findEmployees(query: MSA): Iterator[Employee] =
    employeesColl.find(query).map(MongoUtils.dbo2map).map(Employee.fromMSA)

  /*
   * Structural Units methods
   */

  def saveStructuralUnit(structuralUnit: StructuralUnit) =
    structuralUnitsColl.update("_id" $eq structuralUnit._id, structuralUnit.toMSA, upsert = true)

  def removeStructuralUnit(structuralUnitId: String) =
    structuralUnitsColl.remove("_id" $eq structuralUnitId)

  def findStructuralUnits(query: MSA): Iterator[StructuralUnit] =
    structuralUnitsColl.find(query).map(MongoUtils.dbo2map).map(StructuralUnit.fromMSA)

  // --------------------------------------------------------------------------------
  // create indexes
  // --------------------------------------------------------------------------------

  def createIndexesIfNotExists(): Unit = {
    createTTLIndex(sessionDataColl, Map("exp" -> 1))
  }

}