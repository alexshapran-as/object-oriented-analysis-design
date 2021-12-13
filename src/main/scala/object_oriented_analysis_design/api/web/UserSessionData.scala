package object_oriented_analysis_design.api.web

import com.softwaremill.session.{MultiValueSessionSerializer, RefreshTokenData}
import object_oriented_analysis_design.dao.MainDAO
import object_oriented_analysis_design.{MSA, MSS}
import org.slf4j.LoggerFactory

import java.util.Date
import scala.util.Try

case class UserSessionData(username: String, groups: List[String]) {
    def toMSA(data: RefreshTokenData[UserSessionData], iat: Long) = Map(
        "_id" -> data.selector, "username" -> data.forSession.username,
        "groups" -> data.forSession.groups, "tokenHash" -> data.tokenHash,
        "exp" -> new Date(data.expires), "iat" -> new Date(iat)
    )
}

case object UserSessionData {
    protected val logger = LoggerFactory.getLogger(getClass)
    implicit def serializer: MultiValueSessionSerializer[UserSessionData] = {
        new MultiValueSessionSerializer(
            toMap = (userSessionData: UserSessionData) =>
                Map("username" -> userSessionData.username, "groups" -> userSessionData.groups.mkString(";")),
            fromMap = (mss: MSS) => Try { UserSessionData(username = mss("username"), groups = mss("groups").split(";").toList) }
        )
    }
    def fromMSAToRefTokenData(msa: MSA): RefreshTokenData[UserSessionData] = {
        Try {
            RefreshTokenData[UserSessionData](
                forSession = UserSessionData(msa("username").toString, msa("groups").asInstanceOf[List[String]]),
                selector = msa("_id").toString, tokenHash = msa("tokenHash").toString,
                expires = msa("exp").asInstanceOf[Date].getTime
            )
        }.recover {
            case ex: Exception => sys.error(s"Error mapping user session data with selector = ${msa("_id").toString} (${ex.getMessage})")
        }.get
    }
    def fromMSAToUserSessionData(msa: MSA): UserSessionData = {
        Try { UserSessionData(msa("username").toString, msa("groups").asInstanceOf[List[String]]) }
            .recover { case ex: Exception => sys.error(s"Error mapping user session data with selector = ${msa("_id").toString} (${ex.getMessage})") }
            .get
    }
    def save(data: RefreshTokenData[UserSessionData], iat: Long): Unit = {
        MainDAO.saveUserSessionData(
            selector = data.selector, userSessionDataMSA = data.forSession.toMSA(data, iat)
        )
        MainDAO.createIndexesIfNotExists()
    }
    def remove(selector: String): Unit = MainDAO.removeUserSessionData(selector)
    def findBySelector(selector: String): Option[RefreshTokenData[UserSessionData]] = MainDAO.tryToGetRefTokenData(selector)
}
