package object_oriented_analysis_design.api.web

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.softwaremill.session.CsrfDirectives._
import com.softwaremill.session.CsrfOptions._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.softwaremill.session._
import object_oriented_analysis_design.MSA
import object_oriented_analysis_design.authenticator.InMongoDBRefreshTokenStorage
import object_oriented_analysis_design.configurations.Conf
import org.json4s.JsonInput
import org.json4s.native.JsonMethods.parseOpt
import org.json4s.native.Serialization.{read, write}

trait HttpRouteUtils extends Directives {

  implicit val formats = org.json4s.DefaultFormats

  def get[L](pathRoute: PathMatcher[L]): Directive[L] = Directives.get & path(pathRoute)

  def post[L](pathRoute: PathMatcher[L]): Directive[L] = Directives.post & path(pathRoute)

  def respondWithJsonContentType: Directive0 =
    mapResponse(response => response.mapEntity(entity => entity.withContentType(ContentTypes.`application/json`)))

  def readOpt[T: Manifest](json: JsonInput): Option[T] = {
    parseOpt(json).flatMap(_.extractOpt[T])
  }

  def extractPostRequest: Directive[(String, MSA)] =
    entity(as[String]) map { postData =>
      (postData, read[MSA](postData))
    }

  def getOkResponse(): String = {
    write(Map("success" -> true))
  }

  def getOkResponse(data: MSA): String = {
    write(Map("success" -> true, "result" -> data))
  }

  def getOkResponse(data: Iterable[MSA]): String = {
    write(Map("success" -> true, "result" -> data))
  }

  def getOkResponse(data: String): String = {
    write(Map("success" -> true, "result" -> data))
  }

  def getErrorResponse(code: Int, description: String): String = {
    write(Map("success" -> false, "error" -> Map("code" -> code, "description" -> description)))
  }

  // -------------------------------------------------------------------------------------------------------------------
  // akka-http-session route utils
  // -------------------------------------------------------------------------------------------------------------------

  private implicit val system = ActorSystem("akka-http-session-utils")
  private implicit val materializer = ActorMaterializer()

  import system.dispatcher

  private val sessionConfig = SessionConfig.fromConfig(Conf.conf)
  private implicit val sessionManager = new SessionManager[UserSessionData](sessionConfig)
  private implicit val refreshTokenStorage = InMongoDBRefreshTokenStorage

  def setRefreshableSession(userSessionData: UserSessionData) = setSession(refreshable, usingCookies, userSessionData)
  def setOneLogInSession(userSessionData: UserSessionData) = setSession(oneOff, usingCookies, userSessionData)

  val validateRequiredSession = requiredSession(refreshable, usingCookies)
  val invalidateRequiredSession = invalidateSession(refreshable, usingCookies)

  def tokenCsrfProtectionDirective = randomTokenCsrfProtection(checkHeader)
  def setCsrfToken = setNewCsrfToken(checkHeader)

}