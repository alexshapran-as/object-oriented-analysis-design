package object_oriented_analysis_design.authenticator

import object_oriented_analysis_design.MSA
import object_oriented_analysis_design.dao.MainDAO

object Roles extends Enumeration {
  val ADMIN = Value
}

case class UserAuthData(username: String, passwordHash: Array[Byte], salt: Array[Byte], roles: List[Roles.Value]) {
  def toMSA: MSA = Map(
    "username" -> username,
    "passwordHash" -> passwordHash,
    "salt" -> salt,
    "roles" -> roles.map(_.toString)
  )

  def save: Boolean = MainDAO.tryToGetUserAuthData(username)
      .map(_ => false)
      .getOrElse {
        MainDAO.saveUserAuthData(username, this.toMSA)
        true
      }
}

case object UserAuthData {

  def fromMSA(msa: MSA): UserAuthData = UserAuthData(
    msa.getOrElse("username", sys.error("Username was not found in db")).toString,
    msa.getOrElse("passwordHash", sys.error("Password was not found in db")).asInstanceOf[Array[Byte]],
    msa.getOrElse("salt", sys.error("Salt was not found in db")).asInstanceOf[Array[Byte]],
    msa.getOrElse("roles", sys.error("Role was not found in db")).asInstanceOf[List[String]].map(Roles.withName)
  )

  def find(username: String): Option[UserAuthData] = MainDAO.tryToGetUserAuthData(username)

  def findAll: List[MSA] = MainDAO.tryToGetAllUserAuthData

  def delete(username: String): Unit = MainDAO.removeUserAuthData(username)

  def create(username: String, password: String, roles: List[Roles.Value]): UserAuthData = {
    val salt = HashApi.generateSalt
    UserAuthData(username, HashApi.generateHash(password.toCharArray, salt), salt, roles)
  }
}
